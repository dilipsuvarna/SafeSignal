package com.example.safesignal.nearby

import android.content.Context
import android.util.Log
import android.provider.Settings
import com.example.safesignal.model.*
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

class NearbyManager private constructor(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.example.safesignal.SERVICE_ID"
    private val strategy = Strategy.P2P_CLUSTER
    private val gson = Gson()
    
    private val myId: String
    private var myName = "User"
    
    private lateinit var meshRoutingManager: MeshRoutingManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var routingJob: Job? = null

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<Device>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering = _isDiscovering.asStateFlow()

    private val _meshPeers = MutableStateFlow<List<Device>>(emptyList())
    val meshPeers = _meshPeers.asStateFlow()

    init {
        val prefs = context.getSharedPreferences("SafeSignalPrefs", Context.MODE_PRIVATE)
        val storedId = prefs.getString("deviceId", null)
        myId = storedId ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
        if (storedId == null) prefs.edit().putString("deviceId", myId).apply()
        Log.d("NearbyManager", "Initialized with ID: $myId")

        meshRoutingManager = MeshRoutingManager(myId, myName)
        scope.launch {
            meshRoutingManager.routingTable.collect { _ ->
                _meshPeers.value = meshRoutingManager.getReachablePeers()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val json = String(bytes)
                handleIncomingPayload(endpointId, json)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleIncomingPayload(senderEndpointId: String, json: String) {
        try {
            Log.d("NearbyManager", "Incoming payload from $senderEndpointId")
            val meshPayload = gson.fromJson(json, MeshPayload::class.java)
            
            when (meshPayload.type) {
                PayloadType.ROUTING_UPDATE -> {
                    Log.d("NearbyManager", "Received routing update from $senderEndpointId")
                    val neighbor = _connectedDevices.value.find { it.id == senderEndpointId }
                    meshRoutingManager.updateRoutingFromNeighbor(
                        neighborId = senderEndpointId,
                        neighborName = neighbor?.name ?: "Unknown",
                        remoteTableJson = meshPayload.data
                    )
                }
                PayloadType.CHAT_MESSAGE -> {
                    val message = gson.fromJson(meshPayload.data, Message::class.java)
                    Log.d("NearbyManager", "Received chat message: ${message.messageId} target: ${message.destinationId}")
                    
                    if (!meshRoutingManager.isMessageNew(message.messageId)) {
                        Log.d("NearbyManager", "Message already processed: ${message.messageId}")
                        return
                    }

                    val isForMe = message.destinationId == null || message.destinationId == myId
                    
                    if (isForMe) {
                        Log.d("NearbyManager", "Processing message for ME: ${message.message}")
                        val uiMessage = message.copy(
                            isFromMe = false,
                            receiverId = if (message.destinationId == null) null else "ME"
                        )
                        _messages.tryEmit(uiMessage)
                    }

                    // Forward if TTL allows
                    if (message.ttl > 1) {
                        val isBroadcast = message.destinationId == null
                        val isForSomeoneElse = message.destinationId != null && message.destinationId != myId
                        
                        if (isBroadcast || isForSomeoneElse) {
                            Log.d("NearbyManager", "Relaying message: ${message.messageId} TTL left: ${message.ttl - 1}")
                            val forwardedMessage = message.copy(
                                ttl = message.ttl - 1,
                                hopCount = message.hopCount + 1
                            )
                            relayMessage(forwardedMessage, senderEndpointId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NearbyManager", "Failed to handle payload", e)
        }
    }

    private fun relayMessage(message: Message, incomingEndpointId: String) {
        val payload = MeshPayload(
            type = PayloadType.CHAT_MESSAGE,
            data = gson.toJson(message),
            senderId = myId
        )
        val json = gson.toJson(payload)
        
        if (message.destinationId == null) {
            // Broadcast: send to all neighbors except where it came from
            val targets = _connectedDevices.value.map { it.id }.filter { it != incomingEndpointId }
            if (targets.isNotEmpty()) {
                connectionsClient.sendPayload(targets, Payload.fromBytes(json.toByteArray()))
            }
        } else {
            // Private: send to next hop
            meshRoutingManager.getNextHop(message.destinationId)?.let { nextHop ->
                if (nextHop != incomingEndpointId) {
                    connectionsClient.sendPayload(nextHop, Payload.fromBytes(json.toByteArray()))
                }
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("NearbyManager", "Connection initiated: $endpointId (Name: ${info.endpointName})")
            
            // For the advertiser, we might not have discovered this device, so add it now
            _discoveredDevices.update { current ->
                if (current.none { it.id == endpointId }) {
                    current + Device(endpointId, info.endpointName, ConnectionStatus.CONNECTING)
                } else current
            }
            
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d("NearbyManager", "Connected successfully to $endpointId")
                    val device = _discoveredDevices.value.find { it.id == endpointId }
                        ?: Device(endpointId, "Remote Device")
                    
                    _connectedDevices.update { it + device.copy(status = ConnectionStatus.CONNECTED) }
                    _discoveredDevices.update { it.filter { d -> d.id != endpointId } }
                    
                    // Trigger immediate routing update
                    scope.launch { broadcastRoutingTable() }
                }
                else -> {
                    Log.e("NearbyManager", "Connection failed to $endpointId: ${result.status.statusMessage}")
                    _discoveredDevices.update { it.filter { d -> d.id != endpointId } }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("NearbyManager", "Disconnected: $endpointId")
            _connectedDevices.update { it.filter { d -> d.id != endpointId } }
            meshRoutingManager.removeNeighbor(endpointId)
            checkBackgroundService()
        }
    }

    fun startAdvertising(userName: String) {
        this.myName = userName
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(userName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener { 
                _isAdvertising.value = true
                Log.d("NearbyManager", "Advertising started")
                NearbyService.startService(context)
                startRoutingUpdates()
            }
            .addOnFailureListener { e -> Log.e("NearbyManager", "Advertising failed", e) }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
        stopRoutingUpdates()
        checkBackgroundService()
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("NearbyManager", "Endpoint found: $endpointId (${info.endpointName})")
            _discoveredDevices.update { current ->
                if (current.none { d -> d.id == endpointId }) {
                    current + Device(endpointId, info.endpointName, ConnectionStatus.DISCONNECTED)
                } else current
            }
            
            // AUTOMATIC MESHING: Automatically request connection to build the 8-device grid
            connectionsClient.requestConnection(myName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e -> Log.e("NearbyManager", "Auto-connect failed to $endpointId", e) }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("NearbyManager", "Endpoint lost: $endpointId")
            _discoveredDevices.update { it.filter { d -> d.id != endpointId } }
        }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { 
                _isDiscovering.value = true
                Log.d("NearbyManager", "Discovery started successfully")
                NearbyService.startService(context)
                startRoutingUpdates()
            }
            .addOnFailureListener { e -> 
                Log.e("NearbyManager", "Discovery failed to start", e)
                _isDiscovering.value = false
            }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        _discoveredDevices.value = emptyList()
        stopRoutingUpdates()
        checkBackgroundService()
    }

    private fun startRoutingUpdates() {
        routingJob?.cancel()
        routingJob = scope.launch {
            while (isActive) {
                broadcastRoutingTable()
                delay(5000)
            }
        }
    }

    private fun stopRoutingUpdates() {
        routingJob?.cancel()
        routingJob = null
    }

    private fun broadcastRoutingTable() {
        val tableJson = meshRoutingManager.getLocalRoutingTableJson()
        val payload = MeshPayload(
            type = PayloadType.ROUTING_UPDATE,
            data = tableJson,
            senderId = myId
        )
        val json = gson.toJson(payload)
        val targets = _connectedDevices.value.map { it.id }
        if (targets.isNotEmpty()) {
            connectionsClient.sendPayload(targets, Payload.fromBytes(json.toByteArray()))
        }
    }

    private fun checkBackgroundService() {
        if (!_isAdvertising.value && !_isDiscovering.value && _connectedDevices.value.isEmpty()) {
            NearbyService.stopService(context)
        }
    }

    fun requestConnection(device: Device, userName: String) {
        connectionsClient.requestConnection(userName, device.id, connectionLifecycleCallback)
            .addOnSuccessListener { Log.d("NearbyManager", "Connection requested to ${device.id}") }
            .addOnFailureListener { e -> Log.e("NearbyManager", "Connection request failed", e) }
    }

    fun sendMessage(endpointId: String, content: String) {
        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = myId,
            senderName = myName,
            destinationId = endpointId,
            message = content,
            timestamp = System.currentTimeMillis(),
            isBroadcast = false,
            isFromMe = true,
            receiverId = endpointId,
            ttl = 10,
            hopCount = 0
        )
        meshRoutingManager.isMessageNew(message.messageId)
        
        val meshPayload = MeshPayload(
            type = PayloadType.CHAT_MESSAGE,
            data = gson.toJson(message),
            senderId = myId
        )
        val json = gson.toJson(meshPayload)
        
        meshRoutingManager.getNextHop(endpointId)?.let { nextHop ->
            connectionsClient.sendPayload(nextHop, Payload.fromBytes(json.toByteArray()))
        }
    }

    fun broadcastMessage(content: String) {
        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = myId,
            senderName = myName,
            destinationId = null,
            message = content,
            timestamp = System.currentTimeMillis(),
            isBroadcast = true,
            isFromMe = true,
            receiverId = null,
            ttl = 10,
            hopCount = 0
        )
        meshRoutingManager.isMessageNew(message.messageId)
        
        val meshPayload = MeshPayload(
            type = PayloadType.CHAT_MESSAGE,
            data = gson.toJson(message),
            senderId = myId
        )
        val json = gson.toJson(meshPayload)
        val targets = _connectedDevices.value.map { it.id }
        if (targets.isNotEmpty()) {
            connectionsClient.sendPayload(targets, Payload.fromBytes(json.toByteArray()))
        }
    }

    fun disconnectFromDevice(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        _connectedDevices.update { it.filter { d -> d.id != endpointId } }
        Log.d("NearbyManager", "Manually disconnected from $endpointId")
    }
    
    fun disconnectAll() {
        connectionsClient.stopAllEndpoints()
        _connectedDevices.value = emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: NearbyManager? = null

        fun getInstance(context: Context): NearbyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NearbyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
