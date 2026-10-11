package com.example.safesignal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safesignal.ui.screens.ChatScreen
import com.example.safesignal.ui.screens.ConnectedDevicesScreen
import com.example.safesignal.ui.screens.DiscoveryScreen
import com.example.safesignal.ui.screens.HomeScreen
import com.example.safesignal.ui.viewmodel.ChatViewModel
import com.example.safesignal.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Discovery : Screen("discovery")
    object ConnectedDevices : Screen("connected_devices")
    object Chat : Screen("chat")
}

@Composable
fun SafeSignalNavHost(
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel
) {
    val navController = rememberNavController()
    val uiState by mainViewModel.uiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = uiState,
                onNavigateToDiscovery = { navController.navigate(Screen.Discovery.route) },
                onNavigateToConnected = { navController.navigate(Screen.ConnectedDevices.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onToggleAdvertising = mainViewModel::toggleAdvertising,
                onUpdateName = mainViewModel::setMyName
            )
        }
        composable(Screen.Discovery.route) {
            DiscoveryScreen(
                uiState = uiState,
                onConnect = mainViewModel::connectToDevice,
                onToggleDiscovery = mainViewModel::toggleDiscovery,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ConnectedDevices.route) {
            ConnectedDevicesScreen(
                uiState = uiState,
                onDisconnect = mainViewModel::disconnectFromDevice,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Chat.route) {
            val selectedRecipientId by chatViewModel.selectedRecipientId.collectAsState()
            
            // Sync selected recipient with reachable mesh peers
            LaunchedEffect(uiState.meshPeers) {
                chatViewModel.syncRecipient(uiState.meshPeers)
            }

            ChatScreen(
                messages = messages,
                userName = uiState.myName,
                uiState = uiState,
                selectedRecipientId = selectedRecipientId,
                onSelectRecipient = chatViewModel::selectRecipient,
                onSendMessage = { messageText -> chatViewModel.sendMessage(messageText, uiState.myName) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
