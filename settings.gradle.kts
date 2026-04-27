// ============================================================
// settings.gradle.kts — кореневий файл налаштувань проекту
// Тут реєструються всі модулі. Щоб додати новий модуль —
// додай рядок include(":назва_модуля") і створи відповідну папку.
// ============================================================

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BaseAndroidModuleHybrid"

// --- Підключення модулів проекту ---
include(":app")          // Головний модуль (Activity, DI-граф)
include(":core")         // Спільні моделі, утиліти, константи
include(":updater")      // Логіка перевірки та завантаження оновлень
include(":webview")      // Конфігурація WebView контейнера
include(":bridge")       // JavascriptInterface — міст між JS та Kotlin
include(":notifications")// Управління push/local сповіщеннями
include(":widget")       // Android Glance віджет
