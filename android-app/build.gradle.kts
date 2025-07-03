import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.compose.compiler)
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
        versionName = "1.0.0"

        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${googleClientId}\"")
        
        // Performance optimizations
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Additional exclusions for smaller APK
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }

    // APK splitting for smaller per-architecture APKs
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true // Also generate universal APK
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Disable minification for faster debug builds
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Enable R8 full mode for better optimization
            optimization {
                keepRuntimeTypeAnnotations = false
            }
            
            // Bundle optimization
            bundle {
                language {
                    enableSplit = true
                }
                density {
                    enableSplit = true
                }
                abi {
                    enableSplit = true
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
        // Performance compiler options
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict"
        )
    }
    
    // Compose compiler options for performance
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.core.ktx)

    // Compose BOM for version alignment
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
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

    // Coil image loader with performance optimizations
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
}