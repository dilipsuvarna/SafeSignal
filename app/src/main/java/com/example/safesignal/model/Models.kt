package com.example.safesignal.model

data class Device(
    val id: String,
    val name: String,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val hopDistance: Int = 0,
    val nextHopId: String? = null
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class Message(
    val messageId: String,
    val senderId: String,
    val senderName: String,
    val destinationId: String?, // null for broadcast
    val message: String,
    val timestamp: Long,
    val isBroadcast: Boolean,
    val isFromMe: Boolean,
    val receiverId: String? = null, // UI helper, matches destinationId or "Everyone"
    val ttl: Int = 10,
    val hopCount: Int = 0
)

data class MeshPayload(
    val type: PayloadType,
    val data: String, // JSON serialized object based on type
    val senderId: String
)

enum class PayloadType {
    CHAT_MESSAGE,
    ROUTING_UPDATE
}

data class RoutingTable(
    val entries: List<RoutingEntry>
)

data class RoutingEntry(
    val deviceId: String,
    val deviceName: String,
    val nextHopId: String,
    val hopDistance: Int,
    val lastSeen: Long
)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val connectedDevices: List<Device> = emptyList(), // Direct neighbors
    val meshPeers: List<Device> = emptyList(), // All reachable devices
    val discoveredDevices: List<Device> = emptyList(),
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val myName: String = "User"
)
