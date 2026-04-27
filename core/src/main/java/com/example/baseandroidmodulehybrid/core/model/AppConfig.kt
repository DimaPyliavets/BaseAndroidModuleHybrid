package com.example.baseandroidmodulehybrid.core.model

/**
 * AppConfig — централізовані константи конфігурації.
 *
 * ⚠️ ЗМІНИТИ:
 *  - GITHUB_VERSION_URL: URL до твого version.json на GitHub
 *  - GITHUB_BUNDLE_BASE_URL: базовий URL до директорії з бандлами
 *  - BUNDLE_DIR_NAME: назва папки у filesDir де зберігаються бандли
 *  - WEB_ENTRY_POINT: ім'я головного HTML файлу у бандлі
 */
object AppConfig {

    // ─── GitHub URLs ───────────────────────────────────────────────────
    // ⚠️ ЗМІНИТИ: твій GitHub raw URL до version.json
    // Приклад: https://raw.githubusercontent.com/USER/REPO/main/dist/version.json
    const val GITHUB_VERSION_URL =
        "https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/dist/version.json"

    // ⚠️ ЗМІНИТИ: базовий URL де лежать zip-бандли
    // Приклад: https://github.com/USER/REPO/releases/download/
    const val GITHUB_BUNDLE_BASE_URL =
        "https://github.com/YOUR_USER/YOUR_REPO/releases/download/"

    // ─── Локальне зберігання ──────────────────────────────────────────
    // ⚠️ ЗМІНИТИ: назву папки для веб-бандлів (Context.filesDir / BUNDLE_DIR_NAME)
    const val BUNDLE_DIR_NAME    = "web_bundle"

    // ⚠️ ЗМІНИТИ: ім'я точки входу у твоєму бандлі
    const val WEB_ENTRY_POINT    = "index.html"

    // ─── Версіонування ────────────────────────────────────────────────
    const val PREFS_NAME         = "hybrid_prefs"
    const val PREFS_KEY_VERSION  = "current_bundle_version"

    // ─── Bridge ───────────────────────────────────────────────────────
    // ⚠️ ЗМІНИТИ: ім'я JS об'єкта (window.Android.method())
    const val JS_BRIDGE_NAME     = "Android"

    // ─── Обмеження безпеки ────────────────────────────────────────────
    const val MAX_JSON_PAYLOAD_BYTES = 512_000 // 512 KB — максимальний розмір JSON від JS
    const val MAX_NOTIFICATION_TITLE_LEN = 100
    const val MAX_NOTIFICATION_BODY_LEN  = 500
}

/**
 * VersionInfo — модель відповіді від version.json на GitHub.
 *
 * ⚠️ ЗМІНИТИ: додай поля якщо розшириш схему version.json
 */
data class VersionInfo(
    val version: String,          // Наприклад: "1.2.3"
    val bundleFileName: String,   // Наприклад: "bundle-1.2.3.zip"
    val sha256: String,           // SHA-256 хеш zip-файлу (hex string)
    val releaseNotes: String = "" // ⚠️ ОПЦІОНАЛЬНО: нотатки релізу
)

/**
 * BundleState — стан локального бандлу.
 */
sealed class BundleState {
    object NotInstalled : BundleState()
    data class Installed(val version: String, val entryPath: String) : BundleState()
    object Corrupted : BundleState()  // SHA-256 не збігся
}
