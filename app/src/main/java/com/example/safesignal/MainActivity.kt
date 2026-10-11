package com.example.safesignal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safesignal.nearby.NearbyManager
import com.example.safesignal.nearby.NearbyRepository
import com.example.safesignal.ui.navigation.SafeSignalNavHost
import com.example.safesignal.ui.theme.SafeSignalTheme
import com.example.safesignal.ui.viewmodel.ChatViewModel
import com.example.safesignal.ui.viewmodel.MainViewModel
import com.example.safesignal.util.PermissionUtils

class MainActivity : ComponentActivity() {

    private lateinit var nearbyManager: NearbyManager
    private lateinit var repository: NearbyRepository
    private lateinit var mainViewModel: MainViewModel
    private lateinit var chatViewModel: ChatViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nearbyManager = NearbyManager.getInstance(applicationContext)
        repository = NearbyRepository(nearbyManager)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(repository, applicationContext) as T
                    modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            SafeSignalTheme {
                SafeSignalNavHost(
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel
                )
            }
        }

        if (!PermissionUtils.hasPermissions(this)) {
            permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyManager.disconnectAll()
    }
}
