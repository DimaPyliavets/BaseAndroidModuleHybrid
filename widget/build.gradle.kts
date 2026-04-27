// ============================================================
// widget/build.gradle.kts
// Модуль Glance Widget
// ============================================================

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.example.baseandroidmodulehybrid.widget"
    compileSdk = 34
    defaultConfig { minSdk = 26 }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Узгоджено з версією Kotlin 1.9.23 у libs.versions.toml
        kotlinCompilerExtensionVersion = "1.5.13"
    }

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

    // Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Compose - Glance 1.0.0 потребує runtime та ui для роботи з Compose compiler
    implementation(platform(libs.compose.bom))

    implementation(libs.compose.ui)


    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
