package com.example.safesignal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesignal.model.ChatState
import com.example.safesignal.model.Device
import com.example.safesignal.nearby.NearbyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val repository: NearbyRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("SafeSignalPrefs", Context.MODE_PRIVATE)
    private val _myName = MutableStateFlow(
        prefs.getString("userName", null) ?: "User_${(1000..9999).random()}"
    )
    
    val uiState: StateFlow<ChatState> = combine(
        repository.discoveredDevices,
        repository.connectedDevices,
        repository.meshPeers,
        repository.isAdvertising,
        repository.isDiscovering,
        _myName
    ) { args: Array<Any> ->
        ChatState(
            discoveredDevices = args[0] as List<Device>,
            connectedDevices = args[1] as List<Device>,
            meshPeers = args[2] as List<Device>,
            isAdvertising = args[3] as Boolean,
            isDiscovering = args[4] as Boolean,
            myName = args[5] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatState())

    fun toggleAdvertising() {
        if (uiState.value.isAdvertising) {
            repository.stopAdvertising()
        } else {
            repository.startAdvertising(uiState.value.myName)
        }
    }

    fun toggleDiscovery() {
        if (uiState.value.isDiscovering) {
            repository.stopDiscovery()
        } else {
            repository.startDiscovery()
        }
    }

    fun connectToDevice(device: Device) {
        repository.connectToDevice(device, uiState.value.myName)
    }

    fun disconnectFromDevice(device: Device) {
        repository.disconnectFromDevice(device.id)
    }

    fun setMyName(name: String) {
        _myName.value = name
        prefs.edit().putString("userName", name).apply()
    }
}
