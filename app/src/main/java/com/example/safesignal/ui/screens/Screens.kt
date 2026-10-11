package com.example.safesignal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safesignal.model.ChatState
import com.example.safesignal.model.Device
import com.example.safesignal.model.Message
import com.example.safesignal.ui.components.DeviceCard
import com.example.safesignal.ui.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: ChatState,
    onNavigateToDiscovery: () -> Unit,
    onNavigateToConnected: () -> Unit,
    onNavigateToChat: () -> Unit,
    onToggleAdvertising: () -> Unit,
    onUpdateName: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SafeSignal") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = uiState.myName,
                onValueChange = onUpdateName,
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onNavigateToDiscovery,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Discover Devices")
            }

            Button(
                onClick = onToggleAdvertising,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isAdvertising) MaterialTheme.colorScheme.error 
                                     else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (uiState.isAdvertising) "Stop Advertising" else "Become Discoverable")
            }

            Button(
                onClick = onNavigateToConnected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mesh Network (${uiState.meshPeers.size})")
            }

            Button(
                onClick = onNavigateToChat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Chat")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    uiState: ChatState,
    onConnect: (Device) -> Unit,
    onToggleDiscovery: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discovering...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(
                onClick = onToggleDiscovery,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isDiscovering) "Stop Discovery" else "Start Discovery")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(uiState.discoveredDevices) { device ->
                    DeviceCard(device = device, onClick = { onConnect(device) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesScreen(
    uiState: ChatState,
    onDisconnect: (Device) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Network") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(uiState.meshPeers) { device ->
                val isDirect = uiState.connectedDevices.any { it.id == device.id }
                DeviceCard(
                    device = device, 
                    onClick = {},
                    onDisconnect = if (isDirect) { { onDisconnect(device) } } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    @Suppress("UNUSED_PARAMETER") userName: String,
    uiState: ChatState,
    selectedRecipientId: String?,
    onSelectRecipient: (String?) -> Unit,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Chat")
                        Text(
                            text = if (selectedRecipientId == null) "To: Everyone" 
                                   else "To: ${uiState.meshPeers.find { it.id == selectedRecipientId }?.name ?: "Selected Device"}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    // Recipient Selector
                    if (uiState.meshPeers.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text("To:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
                            }
                            item {
                                InputChip(
                                    selected = selectedRecipientId == null,
                                    onClick = { onSelectRecipient(null) },
                                    label = { Text("Everyone", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                            items(uiState.meshPeers) { device ->
                                InputChip(
                                    selected = selectedRecipientId == device.id,
                                    onClick = { onSelectRecipient(device.id) },
                                    label = { Text(device.name, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (selectedRecipientId == null) "Broadcast message..." else "Private message...") }
                        )
                        IconButton(onClick = {
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }
    }
}
