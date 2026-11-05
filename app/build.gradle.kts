plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Valorile vin din GitHub Actions (vezi .github/workflows/build.yml)
val ksPath: String? = System.getenv("KEYSTORE_FILE")
val dbUrl: String = (System.getenv("FIREBASE_DB_URL") ?: "").trim()

android {
    namespace = "com.neo.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neo.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        buildConfigField("String", "FIREBASE_DB_URL", "\"$dbUrl\"")
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
