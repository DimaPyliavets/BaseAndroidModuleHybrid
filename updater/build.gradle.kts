// ============================================================
// updater/build.gradle.kts
// Модуль відповідає за:
//  1. Запит version.json з GitHub
//  2. Порівняння версій
//  3. Завантаження zip-бандлу
//  4. Перевірку SHA-256
//  5. Розпакування у filesDir
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.example.baseandroidmodulehybrid.updater"
    compileSdk = 34
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.vm.ktx)

    // OkHttp — для HTTP запитів (version.json + bundle download)
    implementation(libs.okhttp)

    // WorkManager — для фонових/відкладених оновлень
    implementation(libs.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // ⚠️ ОПЦІОНАЛЬНО: Gson або Moshi для парсингу JSON
    // implementation("com.google.code.gson:gson:2.10.1")
}
