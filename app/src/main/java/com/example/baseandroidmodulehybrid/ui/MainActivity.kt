package com.example.baseandroidmodulehybrid.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.baseandroidmodulehybrid.BuildConfig
import com.example.baseandroidmodulehybrid.R
import com.example.baseandroidmodulehybrid.bridge.NativeBridge
import com.example.baseandroidmodulehybrid.ui.components.*
import com.example.baseandroidmodulehybrid.updater.UpdateState
import com.example.baseandroidmodulehybrid.updater.UpdaterViewModel
import com.example.baseandroidmodulehybrid.util.AppUtils
import com.example.baseandroidmodulehybrid.util.PermissionUtils
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
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        requestPermissionLauncher.launch(PermissionUtils.getRequiredPermissions())

        lifecycleScope.launch {
            updaterViewModel.checkAndUpdate()
        }

        setContent {
            MainContent(updaterViewModel, nativeBridge)
        }
    }

    @Composable
    private fun MainContent(
        updaterViewModel: UpdaterViewModel,
        nativeBridge: NativeBridge
    ) {
        val updateState by updaterViewModel.updateState.collectAsState()
        val installedBundlePath by updaterViewModel.installedBundlePath.collectAsState()
        val webVersion by updaterViewModel.currentVersion.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        var showSettings by remember { mutableStateOf(false) }

        BackHandler(enabled = showSettings) {
            showSettings = false
        }

        LaunchedEffect(updateState) {
            when (updateState) {
                is UpdateState.Error -> {
                    snackbarHostState.showSnackbar(getString((updateState as UpdateState.Error).messageResId))
                }
                is UpdateState.UpToDate -> {
                    if (showSettings) {
                        snackbarHostState.showSnackbar(getString(R.string.snack_up_to_date))
                    }
                }
                else -> {}
            }
        }

        // Restart Dialog
        if (updateState is UpdateState.Success) {
            RestartDialog(
                onConfirm = { AppUtils.restartApp(context) },
                onDismiss = { updaterViewModel.resetState() }
            )
        }

        // Ready to Install Dialog
        if (updateState is UpdateState.ReadyToInstall) {
            val readyState = updateState as UpdateState.ReadyToInstall
            ReadyToInstallDialog(
                state = readyState,
                onConfirm = {
                    updaterViewModel.completeInstallation(readyState.versionInfo, readyState.zipFile)
                },
                onDismiss = { updaterViewModel.resetState() }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                key(installedBundlePath) {
                    HybridWebView(
                        modifier = Modifier.fillMaxSize(),
                        bridge = nativeBridge,
                        localBundlePath = installedBundlePath
                    )
                }

                Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }

                AnimatedVisibility(
                    visible = showSettings,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SettingsScreen(
                        appVersion = BuildConfig.VERSION_NAME,
                        webVersion = webVersion ?: "Internal",
                        onClose = { showSettings = false },
                        onCheckUpdate = { updaterViewModel.checkAndUpdate(force = true) },
                        onRestart = { AppUtils.restartApp(context) },
                        onClearCache = {
                            scope.launch {
                                WebView(context).clearCache(true)
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_cache_cleared))
                            }
                        }
                    )
                }

                UpdateOverlay(updateState)
            }
        }
    }
}
