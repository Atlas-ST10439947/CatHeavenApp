plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Release signing comes from environment variables, so no key or password is ever
// committed. CI sets them from GitHub secrets (see .github/workflows/android-ci.yml).
// Without them, local release builds fall back to the debug key so they still install.
val releaseKeystorePath: String? = providers.environmentVariable("CATHEAVEN_KEYSTORE_PATH").orNull

android {
    namespace = "com.example.catheavenapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.catheavenapp"
        minSdk = 24
        targetSdk = 37
        // CI passes the run number and the release tag, so every published APK
        // has a higher versionCode and installs over the previous one
        versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("VERSION_NAME").orNull ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null) {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("CATHEAVEN_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("CATHEAVEN_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("CATHEAVEN_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (releaseKeystorePath != null) "release" else "debug")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}