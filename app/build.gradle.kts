plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.damanhour.nursing.rafeeq"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.damanhour.nursing.rafeeq"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // المفتاح بيتقرأ من متغير بيئة (GitHub Secret) وقت البناء - مش مكتوب في الكود
        val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        buildConfig = true
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
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
