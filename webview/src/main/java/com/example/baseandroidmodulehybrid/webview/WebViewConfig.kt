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
 *
 * Принципи безпеки:
 *  ✅ Тільки локальні файли (file:// через AssetLoader)
 *  ✅ JavaScript лише для довіреного origin
 *  ✅ DOM Storage для кешування стану
 *  ✅ Без доступу до системних файлів
 *  ✅ Без змішаного контенту (HTTP в HTTPS)
 *
 * ⚠️ ЗМІНИТИ:
 *  - TRUSTED_ORIGIN: домен який використовує AssetLoader
 *  - Якщо потрібні додаткові Web API — явно дозволяй у configureSettings()
 */
@Singleton
class WebViewConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ⚠️ ЗМІНИТИ: якщо хочеш інший псевдо-домен для AssetLoader
    // Всі локальні файли будуть доступні за: https://appassets.androidplatform.net/files/...
    companion object {
        const val TRUSTED_ORIGIN = "https://appassets.androidplatform.net"
        const val ASSETS_PATH    = "/assets/"   // ресурси з assets/ папки
        const val FILES_PATH     = "/files/"    // файли з filesDir
    }

    /**
     * Створює налаштований WebViewAssetLoader.
     * Дозволяє WebView завантажувати локальні файли через https://appassets.*/
     fun createAssetLoader(): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            // ─── assets/ папка (вбудована у APK — наприклад error.html) ──
            .addPathHandler(
                ASSETS_PATH,
                WebViewAssetLoader.AssetsPathHandler(context)
            )
            // ─── filesDir (динамічні бандли завантажені з GitHub) ─────────
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
     *
     * ⚠️ ВАЖЛИВО: не змінюй налаштування безпеки без аналізу ризиків!
     */
    fun configureSettings(settings: WebSettings) {
        settings.apply {
            // ─── JavaScript ──────────────────────────────────────────────
            javaScriptEnabled = true  // Потрібен для SPA/PWA

            // ─── Безпека ─────────────────────────────────────────────────
            allowFileAccess              = false // ❌ Заборонити прямий file:// доступ
            allowContentAccess           = false // ❌ Заборонити content:// URI
            // allowUniversalAccessFromFileURLs = false // Вже false за замовчуванням

            // ─── Мішаний контент ─────────────────────────────────────────
            // ⚠️ Залишай MIXED_CONTENT_NEVER_ALLOW якщо не маєш HTTP ресурсів
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // ─── Кешування та сховище ────────────────────────────────────
            domStorageEnabled    = true  // Для localStorage в JS
            databaseEnabled      = true  // ⚠️ ОПЦІОНАЛЬНО: для IndexedDB
            cacheMode            = WebSettings.LOAD_DEFAULT

            // ─── Відображення ────────────────────────────────────────────
            useWideViewPort      = true
            loadWithOverviewMode = true
            setSupportZoom(false) // ⚠️ ЗМІНИТИ: true якщо потрібен zoom

            // ─── Media ───────────────────────────────────────────────────
            mediaPlaybackRequiresUserGesture = true // ⚠️ ЗМІНИТИ: false для автоплею

            // ─── User-Agent ───────────────────────────────────────────────
            // ⚠️ ЗМІНИТИ: додай ідентифікатор свого додатка до UA рядка
            // userAgentString = "${userAgentString} HybridApp/1.0"
        }
    }

    /**
     * Повертає URL для завантаження:
     *  - Якщо бандл встановлено → URL через AssetLoader
     *  - Якщо ні → fallback на вбудований error.html
     */
    fun resolveStartUrl(localBundlePath: String?): String {
        return if (localBundlePath != null) {
            // Конвертуємо абсолютний шлях файлу у AssetLoader URL
            // filesDir/web_bundle/index.html → https://appassets.../files/web_bundle/index.html
            val relativePath = File(localBundlePath)
                .relativeTo(context.filesDir)
                .path
            "$TRUSTED_ORIGIN$FILES_PATH$relativePath"
        } else {
            // Fallback — вбудована сторінка з помилкою
            "${TRUSTED_ORIGIN}${ASSETS_PATH}error.html"
        }
    }
}
