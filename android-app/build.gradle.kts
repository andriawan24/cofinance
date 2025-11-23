import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleService)
}

val localProperties = gradleLocalProperties(rootDir, providers)
val googleClientId: String = localProperties.getProperty("google_auth_client_id")

require(googleClientId.isNotEmpty()) { "Google Client ID is empty" }

android {
    namespace = "id.andriawan24.cofinance.andro"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.andriawan24.cofinance.andro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${googleClientId}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.core.ktx)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Google sign in
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Coil image loader
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Logging
    implementation(libs.napier)

    // Koin dependency injection
    implementation(libs.koin.core)
    implementation(libs.koin.android.compose)

    // Splash Screen
    implementation(libs.splashscreen)

    // Dot indicator
    implementation(libs.dotsindicator)

    // Accompanist permission
    implementation(libs.accompanist.permissions)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.compose)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)

    // JSON Serialization
    implementation(libs.kotlinx.serialization.json)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}