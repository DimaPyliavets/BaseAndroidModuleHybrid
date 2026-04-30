package com.example.baseandroidmodulehybrid.ui

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.baseandroidmodulehybrid.R
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
    ) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Transparency for status and navigation bars
        enableEdgeToEdge()
        
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
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // Localization
            val successMsg = stringResource(R.string.snack_update_success)
            val upToDateMsg = stringResource(R.string.snack_up_to_date)
            val errorMsg = stringResource(R.string.error_unknown)

            LaunchedEffect(updateState) {
                when (updateState) {
                    is UpdateState.Error -> {
                        snackbarHostState.showSnackbar(getString((updateState as UpdateState.Error).messageResId))
                    }
                    is UpdateState.Success -> {
                        snackbarHostState.showSnackbar(successMsg)
                    }
                    is UpdateState.UpToDate -> {
                        snackbarHostState.showSnackbar(upToDateMsg)
                    }
                    else -> {}
                }
            }

            if (updateState is UpdateState.ReadyToInstall) {
                val readyState = updateState as UpdateState.ReadyToInstall
                AlertDialog(
                    onDismissRequest = { updaterViewModel.resetState() },
                    title = { Text(stringResource(R.string.update_dialog_title)) },
                    text = { Text(stringResource(R.string.update_dialog_text, readyState.versionInfo.version)) },
                    confirmButton = {
                        Button(onClick = {
                            updaterViewModel.completeInstallation(readyState.versionInfo, readyState.zipFile)
                        }) {
                            Text(stringResource(R.string.update_install))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { updaterViewModel.resetState() }) {
                            Text(stringResource(R.string.update_cancel))
                        }
                    }
                )
            }

            Scaffold(
                topBar = {
                    if (!isLandscape) {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                ) 
                            },
                            actions = {
                                TextButton(onClick = { updaterViewModel.checkAndUpdate(force = true) }) {
                                    Text(stringResource(R.string.menu_update))
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLandscape) PaddingValues(0.dp) else paddingValues)
                ) {
                    key(installedBundlePath) {
                        HybridWebView(
                            modifier = Modifier.fillMaxSize(),
                            bridge = nativeBridge,
                            localBundlePath = installedBundlePath
                        )
                    }

                    if (isLandscape) {
                        // Small floating Update button for landscape
                        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                            FilledTonalButton(
                                onClick = { updaterViewModel.checkAndUpdate(force = true) },
                                modifier = Modifier.align(Alignment.TopEnd).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.menu_update), fontSize = 10.sp)
                            }
                        }
                    }

                    UpdateOverlay(updateState)
                }
            }
        }
    }

    @Composable
    fun UpdateOverlay(state: UpdateState) {
        when (state) {
            is UpdateState.Checking, is UpdateState.Downloading, is UpdateState.Extracting, UpdateState.Applying -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val title = when (state) {
                                is UpdateState.Checking -> stringResource(R.string.overlay_checking)
                                is UpdateState.Downloading -> stringResource(R.string.overlay_downloading)
                                is UpdateState.Extracting -> stringResource(R.string.overlay_installing)
                                else -> stringResource(R.string.overlay_applying)
                            }
                            
                            val progressValue = when (state) {
                                is UpdateState.Downloading -> state.progress / 100f
                                is UpdateState.Extracting -> state.progress / 100f
                                else -> null
                            }

                            Text(text = title, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (progressValue != null) {
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier.width(120.dp).height(2.dp)
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Memory (Storage) permissions for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
