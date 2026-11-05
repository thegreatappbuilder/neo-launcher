plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Valorile vin din GitHub Actions (vezi .github/workflows/build.yml)
val ksPath: String? = System.getenv("KEYSTORE_FILE")
val dbUrl: String = (System.getenv("FIREBASE_DB_URL") ?: "").trim()
    .ifEmpty { "https://neo-launcher-e5657-default-rtdb.europe-west1.firebasedatabase.app" }

android {
    namespace = "com.neo.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neo.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "FIREBASE_DB_URL", "\"$dbUrl\"")
    }

    // Două versiuni din același cod:
    //  github = APK distribuit direct (cu popup-uri și auto-actualizare)
    //  play   = pentru Google Play (doar banner, fără popup și fără auto-actualizare)
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            applicationId = "com.neo.launcher"
            buildConfigField("boolean", "IS_PLAY", "false")
        }
        create("play") {
            dimension = "store"
            applicationId = "com.thegreatappbuilder.neolauncher"
            buildConfigField("boolean", "IS_PLAY", "true")
        }
    }

    signingConfigs {
        if (ksPath != null && file(ksPath).exists()) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
