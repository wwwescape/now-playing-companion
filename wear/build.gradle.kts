import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing reads from keystore.properties (gitignored, local-only). Signing is skipped
// if the file isn't present, so debug builds work without a keystore. The phone and watch apps
// MUST share the same applicationId and signing key or the Wearable Data Layer won't pair them.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.wwwescape.nowplayingcompanion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wwwescape.nowplayingcompanion"
        minSdk = 30
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").get().toInt() * 10 + 1
        versionName = providers.gradleProperty("appVersionName").get()
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // The app is English-only; don't ship dependencies' strings for ~80 other locales.
        localeFilters += "en"
    }
    dependenciesInfo {
        // Play reads this from the bundle; leave it out of sideloadable APKs.
        includeInApk = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // Play Services pulls in an old Fragment; ActivityResult APIs need 1.3+.
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.complications.data.source.ktx)
    implementation(libs.wear.remote.interactions)
    implementation(libs.androidx.concurrent.futures.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
