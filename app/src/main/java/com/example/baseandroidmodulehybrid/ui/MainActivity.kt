package com.example.baseandroidmodulehybrid.ui

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.baseandroidmodulehybrid.bridge.NativeBridge
import com.example.baseandroidmodulehybrid.updater.UpdaterViewModel
import com.example.baseandroidmodulehybrid.updater.UpdateState
import com.example.baseandroidmodulehybrid.webview.HybridWebView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updaterViewModel: UpdaterViewModel by viewModels()

    @Inject
    lateinit var nativeBridge: NativeBridge

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestRequiredPermissions()

        lifecycleScope.launch {
            updaterViewModel.checkAndUpdate()
        }

        setContent {
            val updateState by updaterViewModel.updateState.collectAsState()
            val installedBundlePath by updaterViewModel.installedBundlePath.collectAsState()
            val currentVersion by updaterViewModel.currentVersion.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(updateState) {
                if (updateState is UpdateState.Error) {
                    snackbarHostState.showSnackbar((updateState as UpdateState.Error).message)
                }
                if (updateState is UpdateState.Success) {
                    snackbarHostState.showSnackbar("Оновлення встановлено!")
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    key(installedBundlePath, currentVersion) {
                        HybridWebView(
                            modifier = Modifier.fillMaxSize(),
                            bridge = nativeBridge,
                            localBundlePath = installedBundlePath
                        )
                    }

                    UpdateOverlay(updateState)
                }
            }
        }
    }

    @Composable
    fun UpdateOverlay(state: UpdateState) {
        when (state) {
            is UpdateState.Downloading, is UpdateState.Extracting, UpdateState.Applying -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val title = when (state) {
                                is UpdateState.Downloading -> "Завантаження оновлення"
                                is UpdateState.Extracting -> "Встановлення..."
                                else -> "Оновлення системи"
                            }
                            
                            val progressValue = when (state) {
                                is UpdateState.Downloading -> state.progress / 100f
                                is UpdateState.Extracting -> state.progress / 100f
                                else -> null
                            }

                            Text(text = title, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            if (progressValue != null) {
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier.width(200.dp).height(8.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${(progressValue * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
