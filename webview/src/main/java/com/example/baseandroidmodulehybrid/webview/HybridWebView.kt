package com.example.baseandroidmodulehybrid.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.example.baseandroidmodulehybrid.bridge.NativeBridge
import com.example.baseandroidmodulehybrid.core.model.AppConfig

/**
 * HybridWebView — Composable обгортка над Android WebView.
 *
 * Використовує WebViewAssetLoader для безпечного завантаження локальних файлів.
 *
 * ⚠️ ЗМІНИТИ:
 *  - Додай обробку onPageFinished якщо треба виконати JS після завантаження
 *  - Додай WebChromeClient якщо потрібні alert/confirm/geolocation/file picker
 *  - Передай localBundlePath через ViewModel State, а не хардкодь
 */
@Composable
fun HybridWebView(
    modifier: Modifier = Modifier,
    bridge: NativeBridge,
    // ⚠️ ЗМІНИТИ: передавай шлях з UpdaterViewModel State
    localBundlePath: String? = null
) {
    val context = LocalContext.current

    // ─── Пам'ятаємо конфіг щоб не пересоздавати при рекомпозиції ─────
    val webViewConfig = remember { WebViewConfig(context) }
    val assetLoader   = remember { webViewConfig.createAssetLoader() }
    val startUrl      = remember(localBundlePath) {
        webViewConfig.resolveStartUrl(localBundlePath)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // ─── Налаштування ─────────────────────────────────────
                webViewConfig.configureSettings(settings)

                // ─── Підключення Bridge (JavascriptInterface) ─────────
                addJavascriptInterface(bridge, AppConfig.JS_BRIDGE_NAME)
                // ⚠️ ВАЖЛИВО: JS_BRIDGE_NAME = "Android"
                //   У JS: window.Android.showNotification("title", "msg")

                // ─── WebViewClient з AssetLoader ──────────────────────
                webViewClient = HybridWebViewClient(assetLoader)

                // ─── Завантаження початкового URL ─────────────────────
                loadUrl(startUrl)
            }
        },
        update = { webView ->
            // ⚠️ При зміні startUrl (після оновлення бандлу) — перезавантажити
            if (webView.url != startUrl) {
                webView.loadUrl(startUrl)
            }
        }
    )
}

/**
 * HybridWebViewClient — перехоплює запити та обслуговує їх через AssetLoader.
 *
 * ⚠️ ВАЖЛИВО: shouldInterceptRequest є єдиним місцем де дозволено завантаження.
 * Будь-які зовнішні URL будуть заблоковані (повернено null → помилка завантаження).
 */
private class HybridWebViewClient(
    private val assetLoader: WebViewAssetLoader
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // AssetLoader обробляє лише запити до TRUSTED_ORIGIN
        // Всі інші запити → null (зовнішній доступ заблоковано)
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        // ⚠️ ЗМІНИТИ: додай логування помилок (Timber / Firebase Crashlytics)
        // Наприклад: Timber.e("WebView error $errorCode: $description at $failingUrl")
    }

    /**
     * ⚠️ ВАЖЛИВО: Блокуємо навігацію до зовнішніх URL.
     * Якщо веб-частина намагається перейти на зовнішній сайт — ігноруємо.
     */
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        // Дозволяємо лише наш trusted origin
        return !url.startsWith(WebViewConfig.TRUSTED_ORIGIN)
        // ⚠️ ЗМІНИТИ: якщо треба відкривати зовнішні URL у браузері:
        // return if (url.startsWith("https://allowed-domain.com")) {
        //     startActivity(Intent(Intent.ACTION_VIEW, request.url))
        //     true
        // } else {
        //     !url.startsWith(WebViewConfig.TRUSTED_ORIGIN)
        // }
    }
}
