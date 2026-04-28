package com.example.baseandroidmodulehybrid.core.model

/**
 * AppConfig — централізовані константи конфігурації.
 */
object AppConfig {

    // ─── GitHub URLs ───────────────────────────────────────────────────
    // ⚠️ ПЕРЕВІРТЕ: чи репозиторій публічний і чи гілка називається 'main'
    // Якщо файл не знайдено (404), спробуйте відкрити це посилання в браузері.
    const val GITHUB_VERSION_URL =
        "https://raw.githubusercontent.com/DimaPyliavets/BaseAndroidModuleHybrid/refs/heads/main/webview/src/main/assets/version.json?token=GHSAT0AAAAAAD2EVIUDANDUTNGHPB4FS22A2PQORRQ"

    // Базовий URL для завантаження бандлів (релізів)
    const val GITHUB_BUNDLE_BASE_URL =
        "https://github.com/DimaPyliavets/BaseAndroidModuleHybrid/releases/download/"

    // ─── Локальне зберігання ──────────────────────────────────────────
    const val BUNDLE_DIR_NAME    = "web_bundle"
    const val WEB_ENTRY_POINT    = "index.html"

    // ─── Версіонування ────────────────────────────────────────────────
    const val PREFS_NAME         = "hybrid_prefs"
    const val PREFS_KEY_VERSION  = "current_bundle_version"

    // ─── Bridge ───────────────────────────────────────────────────────
    const val JS_BRIDGE_NAME     = "Android"

    // ─── Обмеження безпеки ────────────────────────────────────────────
    const val MAX_JSON_PAYLOAD_BYTES = 512_000
    const val MAX_NOTIFICATION_TITLE_LEN = 100
    const val MAX_NOTIFICATION_BODY_LEN  = 500
}

/**
 * VersionInfo — модель відповіді від version.json на GitHub.
 */
data class VersionInfo(
    val version: String,
    val bundleFileName: String,
    val sha256: String,
    val releaseNotes: String = ""
)

/**
 * BundleState — стан локального бандлу.
 */
sealed class BundleState {
    object NotInstalled : BundleState()
    data class Installed(val version: String, val entryPath: String) : BundleState()
    object Corrupted : BundleState()
}
