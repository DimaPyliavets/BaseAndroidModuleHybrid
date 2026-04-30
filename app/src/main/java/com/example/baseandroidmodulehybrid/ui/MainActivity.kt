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
import androidx.compose.material.icons.filled.MoreVert
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
        
        // Робимо системні панелі прозорими
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

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
            var showMenu by remember { mutableStateOf(false) }

            // Повідомлення для Snackbar
            val successMsg = stringResource(R.string.snack_update_success)
            val upToDateMsg = stringResource(R.string.snack_up_to_date)

            LaunchedEffect(updateState) {
                when (updateState) {
                    is UpdateState.Error -> {
                        snackbarHostState.showSnackbar((updateState as UpdateState.Error).message)
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

            // Діалог підтвердження
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) 
                            },
                            actions = {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_update)) },
                                        onClick = {
                                            showMenu = false
                                            updaterViewModel.checkAndUpdate(force = true)
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLandscape) PaddingValues(0.dp) else paddingValues)
                ) {
                    key(installedBundlePath, currentVersion) {
                        HybridWebView(
                            modifier = Modifier.fillMaxSize(),
                            bridge = nativeBridge,
                            localBundlePath = installedBundlePath
                        )
                    }

                    // Плаваюча кнопка для ландшафтного режиму
                    if (isLandscape) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            SmallFloatingActionButton(
                                onClick = { updaterViewModel.checkAndUpdate(force = true) },
                                modifier = Modifier.align(Alignment.TopEnd).size(40.dp),
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
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
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
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

                            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (progressValue != null) {
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier.width(160.dp).height(4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "${(progressValue * 100).toInt()}%", fontSize = 12.sp)
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
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
            Manifest.permission.VIBRATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
