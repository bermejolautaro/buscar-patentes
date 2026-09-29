plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ar.lauta.buscarpatentes"
    compileSdk = 37

    defaultConfig {
        applicationId = "ar.lauta.buscarpatentes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // El plan fija JDK 17. La máquina tiene 21, que compila hacia 17 sin problema.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

// Lo que queda en la app es el arranque: pantallas, base y mapa están en `:shared` (D1 de la
// 006). Compose llega de ahí, en la versión de Compose Multiplatform, así el APK no carga dos.
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.cmp.runtime)
    implementation(libs.room.runtime)
}
