plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rob.glasses"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rob.glasses"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // A fixed key, so each new build installs over the old one and keeps Rob's lists.
    signingConfigs {
        create("rob") {
            storeFile = file("rob-glasses.keystore")
            storePassword = "android"
            keyAlias = "robkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("rob") }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("rob")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
