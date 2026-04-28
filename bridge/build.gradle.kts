// ============================================================
// bridge/build.gradle.kts
// Модуль JavascriptInterface — міст між JS та Kotlin.
// ⚠️ Залежить від notifications для показу сповіщень через Bridge.
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.example.baseandroidmodulehybrid.bridge"
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
    implementation(project(":notifications"))
    implementation(project(":widget"))

    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
