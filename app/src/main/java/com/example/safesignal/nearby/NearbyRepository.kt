package com.example.safesignal.nearby

import com.example.safesignal.model.Device
import com.example.safesignal.model.Message
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyRepository(private val nearbyManager: NearbyManager) {

    val discoveredDevices: StateFlow<List<Device>> = nearbyManager.discoveredDevices
    val connectedDevices: StateFlow<List<Device>> = nearbyManager.connectedDevices
    val meshPeers: StateFlow<List<Device>> = nearbyManager.meshPeers
    val messages: SharedFlow<Message> = nearbyManager.messages
    val isAdvertising: StateFlow<Boolean> = nearbyManager.isAdvertising
    val isDiscovering: StateFlow<Boolean> = nearbyManager.isDiscovering

    fun startAdvertising(userName: String) = nearbyManager.startAdvertising(userName)
    fun stopAdvertising() = nearbyManager.stopAdvertising()

    fun startDiscovery() = nearbyManager.startDiscovery()
    fun stopDiscovery() = nearbyManager.stopDiscovery()

    fun connectToDevice(device: Device, userName: String) = nearbyManager.requestConnection(device, userName)

    fun sendMessage(endpointId: String, content: String) = nearbyManager.sendMessage(endpointId, content)
    fun broadcastMessage(content: String) = nearbyManager.broadcastMessage(content)

    fun disconnectFromDevice(endpointId: String) = nearbyManager.disconnectFromDevice(endpointId)
    
    fun disconnectAll() = nearbyManager.disconnectAll()
}
