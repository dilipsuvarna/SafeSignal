package com.example.safesignal.nearby

import android.util.Log
import com.example.safesignal.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class MeshRoutingManager(private val myId: String, private val myName: String) {

    private val gson = Gson()
    
    // Message ID cache to prevent loops (Id -> Timestamp)
    private val processedMessageIds = ConcurrentHashMap<String, Long>()
    
    // Routing Table: DeviceId -> RoutingEntry
    private val _routingTable = MutableStateFlow<Map<String, RoutingEntry>>(emptyMap())
    val routingTable = _routingTable.asStateFlow()

    private val TAG = "MeshRoutingManager"

    fun isMessageNew(messageId: String): Boolean {
        val now = System.currentTimeMillis()
        // Clean up old IDs (older than 10 mins)
        processedMessageIds.entries.removeIf { now - it.value > 600000 }
        
        if (processedMessageIds.containsKey(messageId)) return false
        
        processedMessageIds[messageId] = now
        return true
    }

    fun updateRoutingFromNeighbor(neighborId: String, neighborName: String, remoteTableJson: String) {
        try {
            val remoteTable = gson.fromJson(remoteTableJson, RoutingTable::class.java)
            val now = System.currentTimeMillis()

            _routingTable.update { current ->
                val newTable = current.toMutableMap()
                
                // Add/Update the neighbor itself
                newTable[neighborId] = RoutingEntry(
                    deviceId = neighborId,
                    deviceName = neighborName,
                    nextHopId = neighborId,
                    hopDistance = 1,
                    lastSeen = now
                )

                // Process neighbor's known routes
                remoteTable.entries.forEach { entry ->
                    if (entry.deviceId == myId) return@forEach // Don't route to myself
                    
                    val existing = newTable[entry.deviceId]
                    val newDistance = entry.hopDistance + 1
                    
                    if (newDistance > 10) return@forEach // Respect TTL/Max Hops

                    // Bellman-Ford style update: update if shorter path found or if current path is via this neighbor
                    if (existing == null || newDistance < existing.hopDistance || existing.nextHopId == neighborId) {
                        newTable[entry.deviceId] = RoutingEntry(
                            deviceId = entry.deviceId,
                            deviceName = entry.deviceName,
                            nextHopId = neighborId,
                            hopDistance = newDistance,
                            lastSeen = now
                        )
                    }
                }
                newTable
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse routing update", e)
        }
    }

    fun removeNeighbor(neighborId: String) {
        _routingTable.update { current ->
            current.filter { it.value.nextHopId != neighborId }
        }
    }

    fun getNextHop(destinationId: String): String? {
        return _routingTable.value[destinationId]?.nextHopId
    }

    fun getLocalRoutingTableJson(): String {
        val myEntry = RoutingEntry(
            deviceId = myId,
            deviceName = myName,
            nextHopId = myId,
            hopDistance = 0,
            lastSeen = System.currentTimeMillis()
        )
        val table = RoutingTable(entries = _routingTable.value.values.toList() + myEntry)
        return gson.toJson(table)
    }

    fun getReachablePeers(): List<Device> {
        return _routingTable.value.values.map { 
            Device(
                id = it.deviceId,
                name = it.deviceName,
                status = ConnectionStatus.CONNECTED,
                hopDistance = it.hopDistance,
                nextHopId = it.nextHopId
            )
        }
    }
}
