plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val vaeloKeystore = file(System.getenv("VAELO_KEYSTORE_PATH") ?: "${rootDir}/keystore/vaelo-release.jks")
val vaeloKeystorePassword = System.getenv("VAELO_KEYSTORE_PASSWORD") ?: ""
val vaeloSigned = vaeloKeystore.exists() && vaeloKeystorePassword.isNotEmpty()

android {
    namespace = "com.ghost.browser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ghost.browser"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "6.0"
    }

    signingConfigs {
        create("vaelo") {
            if (vaeloSigned) {
                storeFile = vaeloKeystore
                storePassword = vaeloKeystorePassword
                keyAlias = "vaelo"
                keyPassword = vaeloKeystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = if (vaeloSigned) signingConfigs.getByName("vaelo") else signingConfigs.getByName("debug")
        }
        debug {
            if (vaeloSigned) signingConfig = signingConfigs.getByName("vaelo")
        }
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")
}
