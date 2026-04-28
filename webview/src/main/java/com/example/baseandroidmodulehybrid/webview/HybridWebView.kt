package com.example.baseandroidmodulehybrid.webview

import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
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

    Log.d("HybridWebView", "Loading URL: $startUrl")

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewConfig.configureSettings(settings)
                addJavascriptInterface(bridge, AppConfig.JS_BRIDGE_NAME)
                
                // Дозволяємо доступ до заліза (камера, мікрофон)
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }

                webViewClient = HybridWebViewClient(assetLoader)
                
                if (localBundlePath != null) {
                    clearCache(true)
                }
                
                loadUrl(startUrl)
            }
        },
        update = { webView ->
            if (webView.url != startUrl) {
                webView.clearCache(true)
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

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        return !url.startsWith(WebViewConfig.TRUSTED_ORIGIN)
    }
}
