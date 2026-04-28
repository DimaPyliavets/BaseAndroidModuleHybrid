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
 */
@Composable
fun HybridWebView(
    modifier: Modifier = Modifier,
    bridge: NativeBridge,
    localBundlePath: String? = null
) {
    val context = LocalContext.current

    val webViewConfig = remember { WebViewConfig(context) }
    val assetLoader   = remember { webViewConfig.createAssetLoader() }
    val startUrl      = remember(localBundlePath) {
        webViewConfig.resolveStartUrl(localBundlePath)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewConfig.configureSettings(settings)

                addJavascriptInterface(bridge, AppConfig.JS_BRIDGE_NAME)

                webViewClient = HybridWebViewClient(assetLoader)

                loadUrl(startUrl)
            }
        },
        update = { webView ->
            if (webView.url != startUrl) {
                webView.loadUrl(startUrl)
            }
        }
    )
}

private class HybridWebViewClient(
    private val assetLoader: WebViewAssetLoader
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        // Якщо сталася критична помилка завантаження (наприклад, файл не знайдено),
        // завантажуємо локальну сторінку помилки.
        if (errorCode == ERROR_FILE_NOT_FOUND || errorCode == ERROR_UNKNOWN) {
             view.loadUrl("${WebViewConfig.TRUSTED_ORIGIN}${WebViewConfig.ASSETS_PATH}error.html")
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        return !url.startsWith(WebViewConfig.TRUSTED_ORIGIN)
    }
}
