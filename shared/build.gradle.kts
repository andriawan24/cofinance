import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("com.codingfeline.buildkonfig")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                    // Performance optimizations
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-Xjsr305=strict"
                    )
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
            // iOS performance optimizations
            export(libs.napier)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // SUPABASE - Use BOM for version alignment
            implementation(project.dependencies.platform(libs.supabasekt.bom))
            implementation(libs.supabasekt.postgrest)
            implementation(libs.supabasekt.auth)
            implementation(libs.supabasekt.realtime)

            // KTOR - Core networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            // Logging
            api(libs.napier) // Use api for iOS framework export

            // Koin - Lightweight DI
            api(libs.koin.core) // Use api for shared DI

            // Google gemini
            implementation(libs.generativeai.google)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        androidMain.dependencies {
            // Android-specific optimizations can be added here
        }
        
        iosMain.dependencies {
            // iOS-specific optimizations can be added here
        }
    }
}

buildkonfig {
    packageName = "com.andreasgift.kmpweatherapp"
    
    // Optimize configuration generation
    objectName = "BuildConfig"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        val localProperties = gradleLocalProperties(rootDir, providers)
        val supabaseUrl = localProperties.getProperty("supabase.project_url")
        val supabaseApiKey = localProperties.getProperty("supabase.public_api_key")
        val geminiApiKey = localProperties.getProperty("gemini.api_key")

        require(supabaseUrl.isNotEmpty()) { "Register supabase url on local.properties" }
        require(supabaseApiKey.isNotEmpty()) { "Register supabase api key on local.properties" }
        require(geminiApiKey.isNotEmpty()) { "Register gemini api key on local.properties" }

        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_API_KEY", supabaseApiKey)
        buildConfigField(FieldSpec.Type.STRING, "GEMINI_API_KEY", geminiApiKey)
    }
}

android {
    namespace = "id.andriawan24.cofinance"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    // Performance optimizations
    buildFeatures {
        buildConfig = false // Disable if not needed to reduce APK size
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        
        // Performance optimizations
        isCoreLibraryDesugaringEnabled = false // Disable if not using newer APIs
    }
    
    // Optimize build performance
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}
