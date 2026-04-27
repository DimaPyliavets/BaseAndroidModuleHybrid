// ============================================================
// core/build.gradle.kts
// Модуль без Android-залежностей (де можливо) — тільки спільні
// моделі, константи, утиліти. Це найстабільніший модуль.
// ⚠️ НЕ додавай сюди залежності від інших модулів проекту!
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.example.baseandroidmodulehybrid.core"
    compileSdk = 34
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)

    // Room — для WidgetDataRepository та інших сховищ
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
