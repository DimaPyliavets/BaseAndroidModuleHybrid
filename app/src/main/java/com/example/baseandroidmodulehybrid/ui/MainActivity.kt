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


/**
 * MainActivity — єдина Activity в додатку (Single Activity Architecture).
 *
 * ⚠️ ЗМІНИТИ:
 *  - Тему додатка у res/values/themes.xml
 *  - Логіку onNewIntent якщо обробляєш Deep Links
 *  - Додай навігацію якщо потрібні нативні екрани (NavHost)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updaterViewModel: UpdaterViewModel by viewModels()

    @Inject
    lateinit var nativeBridge: NativeBridge

    // ─── Запит дозволу на сповіщення (Android 13+) ────────────────────
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // ⚠️ ЗМІНИТИ: логіку якщо дозвіл відхилено (показ поояснення)
        if (!granted) {
            // TODO: показати rationale dialog або інформаційний banner
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ─── Увімкнути WebView DevTools тільки в Debug builds ─────────
        // ⚠️ ВАЖЛИВО: НІКОЛИ не залишати true в release!
        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // ─── Запит дозволу на сповіщення ──────────────────────────────
        requestNotificationPermissionIfNeeded()

        // ─── Запуск перевірки оновлень при старті ─────────────────────
        // ⚠️ ЗМІНИТИ: замінити на WorkManager якщо потрібні фонові оновлення
        lifecycleScope.launch {
            updaterViewModel.checkAndUpdate()
        }

        setContent {
            // ⚠️ ЗМІНИТИ: назву теми на свою (res/values/themes.xml)
            // BaseAndroidModuleHybridTheme { ... }

            val updateState by updaterViewModel.updateState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            // ─── Показ повідомлень про стан оновлення ─────────────────
            LaunchedEffect(updateState) {
                when (updateState) {
                    is UpdateState.Error -> {
                        snackbarHostState.showSnackbar(
                            (updateState as UpdateState.Error).message
                        )
                    }
                    is UpdateState.UpToDate -> {
                        // Нічого не показуємо — вже актуальна версія
                    }
                    else -> Unit
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
                    // ─── Головний WebView контейнер ────────────────────
                    HybridWebView(
                        modifier = Modifier.fillMaxSize(),
                        bridge = nativeBridge
                    )

                    // ─── Індикатор завантаження під час оновлення ─────
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

    /**
     * ⚠️ ЗМІНИТИ: якщо додаток отримує Deep Links або Notification intents —
     * обробляй їх тут і передавай у WebView через Bridge.
     */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // TODO: обробка intent extras, наприклад:
        // intent.getStringExtra("notification_payload")?.let { bridge.forwardToWeb(it) }
    }
}
