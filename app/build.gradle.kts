// ============================================================
// app/build.gradle.kts — білд головного модуля
// ⚠️ Обов'язково зміни:
//   - applicationId: твій реальний package name
//   - versionCode / versionName: при кожному релізі
//   - minSdk / targetSdk: перевір актуальні значення
// ============================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    // ⚠️ ЗМІНИТИ: унікальний ідентифікатор твого додатка в Play Store
    namespace         = "com.example.baseandroidmodulehybrid"
    compileSdk        = 34

    defaultConfig {
        // ⚠️ ЗМІНИТИ: applicationId має бути твоїм реальним package name
        applicationId = "com.example.baseandroidmodulehybrid"
        minSdk        = 26   // Notification Channels вимагають API 26+
        targetSdk     = 34
        versionCode   = 1    // ⚠️ ЗМІНИТИ: інкрементуй при кожному релізі
        versionName   = "1.0.0" // ⚠️ ЗМІНИТИ: семантична версія

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            // ⚠️ Для відладки WebView: увімкни WebView DevTools у MainActivity
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // ⚠️ Має збігатися з версією Kotlin у libs.versions.toml
        kotlinCompilerExtensionVersion = "1.5.13"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // --- Підключення внутрішніх модулів ---
    implementation(project(":core"))
    implementation(project(":updater"))
    implementation(project(":webview"))
    implementation(project(":bridge"))
    implementation(project(":notifications"))
    implementation(project(":widget"))

    // --- Core ---
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.lifecycle.vm.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- Compose ---
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling)
    implementation(libs.activity.compose)

    // --- Hilt (DI) ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}
