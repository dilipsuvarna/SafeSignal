package com.example.safesignal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safesignal.model.Device
import com.example.safesignal.model.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    onDisconnect: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${device.id.take(8)}...", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (device.hopDistance <= 1) "Directly Connected" else "${device.hopDistance} hops away",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            if (onDisconnect != null && device.status.name == "CONNECTED") {
                TextButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val containerColor = if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer 
                         else MaterialTheme.colorScheme.secondaryContainer
    
    val shape = if (message.isFromMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!message.isFromMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        
        Surface(
            color = containerColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isBroadcast) {
                    Text(
                        text = "BROADCAST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(text = message.message)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.hopCount > 0) {
                        Text(
                            text = "Hops: ${message.hopCount} | ",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
