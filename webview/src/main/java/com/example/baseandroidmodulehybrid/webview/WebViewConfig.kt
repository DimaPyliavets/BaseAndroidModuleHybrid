package com.example.baseandroidmodulehybrid.webview

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebViewConfig — фабрика та конфігуратор WebView.
 */
@Singleton
class WebViewConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val TRUSTED_ORIGIN = "https://appassets.androidplatform.net"
        const val ASSETS_PATH    = "/assets/"   // ресурси з assets/ папки
        const val FILES_PATH     = "/files/"    // файли з filesDir
    }

    /**
     * Створює налаштований WebViewAssetLoader.
     */
     fun createAssetLoader(): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .addPathHandler(
                ASSETS_PATH,
                WebViewAssetLoader.AssetsPathHandler(context)
            )
            .addPathHandler(
                FILES_PATH,
                WebViewAssetLoader.InternalStoragePathHandler(
                    context,
                    context.filesDir
                )
            )
            .build()
    }

    /**
     * Налаштовує WebSettings для безпеки та продуктивності.
     */
    fun configureSettings(settings: WebSettings) {
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
    }

    fun resolveStartUrl(localBundlePath: String?): String {
        return if (localBundlePath != null) {
            try {
                val relativePath = File(localBundlePath)
                    .relativeTo(context.filesDir)
                    .path
                    .replace('\\', '/')
                "$TRUSTED_ORIGIN$FILES_PATH$relativePath"
            } catch (e: Exception) {
                "${TRUSTED_ORIGIN}${ASSETS_PATH}index.html"
            }
        } else {
            // Завантажуємо index.html з assets за замовчуванням
            "${TRUSTED_ORIGIN}${ASSETS_PATH}index.html"
        }
    }
}
