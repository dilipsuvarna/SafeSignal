package com.example.safesignal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesignal.model.Device
import com.example.safesignal.model.Message
import com.example.safesignal.nearby.NearbyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(private val repository: NearbyRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedRecipientId = MutableStateFlow<String?>(null) // null means "All"
    val selectedRecipientId: StateFlow<String?> = _selectedRecipientId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.messages.collect { message ->
                _messages.update { it + message }
            }
        }
    }

    fun syncRecipient(meshPeers: List<Device>) {
        val currentId = _selectedRecipientId.value
        if (currentId != null && meshPeers.none { it.id == currentId }) {
            _selectedRecipientId.value = null
        }
    }

    fun selectRecipient(recipientId: String?) {
        _selectedRecipientId.value = recipientId
    }

    fun sendMessage(content: String, currentUserName: String) {
        val myId = "ME"
        val recipientId = _selectedRecipientId.value
        
        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = myId,
            senderName = currentUserName,
            destinationId = recipientId,
            message = content,
            timestamp = System.currentTimeMillis(),
            isBroadcast = recipientId == null,
            isFromMe = true,
            receiverId = recipientId
        )
        
        _messages.update { it + message }
        
        if (recipientId == null) {
            repository.broadcastMessage(content)
        } else {
            repository.sendMessage(recipientId, content)
        }
    }
}
