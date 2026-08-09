package com.unidownloader.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.unidownloader.app.data.model.MediaInfo
import com.unidownloader.app.ui.DownloadsScreen
import com.unidownloader.app.ui.HomeScreen
import com.unidownloader.app.ui.MediaScreen
import com.unidownloader.app.ui.SettingsScreen
import com.unidownloader.app.ui.components.BottomNavBar
import com.unidownloader.app.ui.navigation.Screen
import com.unidownloader.app.ui.theme.DarkBackground
import com.unidownloader.app.ui.theme.UniDownloaderTheme

class MainActivity : ComponentActivity() {

    private var sharedUrlState = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Handle shared URL from external apps
        handleIncomingIntent(intent)

        setContent {
            UniDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScaffold(sharedUrl = sharedUrlState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                sharedUrlState.value = sharedText
            }
        }
    }
}

@Composable
fun MainAppScaffold(sharedUrl: String? = null) {
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }
    var currentMediaInfo by remember { mutableStateOf<MediaInfo?>(null) }

    val activeTasks by UniDownloaderApp.downloadManager.activeTasks.collectAsState()
    val activeCount = activeTasks.count { it.isActive }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                activeDownloadsCount = activeCount,
                onNavigate = { route ->
                    currentRoute = route
                }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (currentRoute) {
                Screen.Home.route -> {
                    HomeScreen(
                        onMediaAnalyzed = { mediaInfo ->
                            currentMediaInfo = mediaInfo
                            currentRoute = Screen.MediaInfo.route
                        }
                    )
                }

                Screen.MediaInfo.route -> {
                    currentMediaInfo?.let { mediaInfo ->
                        MediaScreen(
                            mediaInfo = mediaInfo,
                            onNavigateBack = { currentRoute = Screen.Home.route },
                            onDownloadStarted = { currentRoute = Screen.Home.route }
                        )
                    } ?: run {
                        HomeScreen(
                            onMediaAnalyzed = { mediaInfo ->
                                currentMediaInfo = mediaInfo
                                currentRoute = Screen.MediaInfo.route
                            }
                        )
                    }
                }

                Screen.DownloadsHistory.route, Screen.ActiveDownloads.route -> {
                    DownloadsScreen(
                        onNavigateHome = { currentRoute = Screen.Home.route }
                    )
                }

                Screen.Settings.route -> {
                    SettingsScreen()
                }
            }
        }
    }
}
