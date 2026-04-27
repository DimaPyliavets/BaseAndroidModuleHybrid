// ============================================================
// build.gradle.kts (root) — кореневий білд-скрипт
// Тут оголошуються версії плагінів для ВСЬОГО проекту.
// ⚠️ Змінюй версії тут, а не в кожному модулі окремо.
// ============================================================

plugins {
    // Версія AGP — перевіряй актуальну на https://developer.android.com/build/releases/gradle-plugin
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library")     version "8.3.2" apply false

    // Kotlin — має збігатися з версією у libs.versions.toml
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false

    // KSP (для Room / Hilt annotation processing)
    id("com.google.devtools.ksp")  version "1.9.23-1.0.19" apply false

    // Hilt (Dependency Injection)
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
