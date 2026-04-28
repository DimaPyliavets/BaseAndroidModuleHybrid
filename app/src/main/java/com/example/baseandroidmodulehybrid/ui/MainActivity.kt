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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            updaterViewModel.checkAndUpdate()
        }

        setContent {
            val updateState by updaterViewModel.updateState.collectAsState()
            val installedBundlePath by updaterViewModel.installedBundlePath.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(updateState) {
                if (updateState is UpdateState.Error) {
                    snackbarHostState.showSnackbar((updateState as UpdateState.Error).message)
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
                    HybridWebView(
                        modifier = Modifier.fillMaxSize(),
                        bridge = nativeBridge,
                        localBundlePath = installedBundlePath
                    )

                    if (updateState is UpdateState.Downloading) {
                        LinearProgressIndicator(
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
