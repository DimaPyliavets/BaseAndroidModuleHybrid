// ============================================================
// webview/build.gradle.kts
// Модуль WebView контейнера. Відповідає за:
//  - Конфігурацію WebSettings
//  - WebViewAssetLoader (file:// → http://appassets.androidplatform.net/)
//  - WebViewClient / WebChromeClient
//  - Composable обгортку HybridWebView
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.example.baseandroidmodulehybrid.webview"
    compileSdk = 34
    defaultConfig { minSdk = 26 }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.13" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":bridge"))

    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)

    // Compose (для AndroidView wrapper)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    // WebViewAssetLoader — безпечне завантаження локальних файлів
    implementation("androidx.webkit:webkit:1.11.0")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
