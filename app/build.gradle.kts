plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nhbox.htmltovideo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nhbox.htmltovideo"
        minSdk = 29
        targetSdk = 34
        versionCode = 17
        versionName = "4.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    // Koi external dependency nahi. Sirf Android framework use hota hai.
}
