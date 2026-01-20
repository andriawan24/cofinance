import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.cmp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    id("com.codingfeline.buildkonfig")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
            implementation(libs.ktor.client.okhttp)

            // androidMain build.gradle
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.navigation)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)

            // Supabase
            implementation(project.dependencies.platform(libs.supabasekt.bom))
            implementation(libs.supabasekt.postgrest)
            implementation(libs.supabasekt.auth)
            implementation(libs.supabasekt.compose.auth)
            implementation(libs.supabasekt.compose.auth.ui)
            implementation(libs.supabasekt.realtime)

            // KTOR
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            // Gen AI
            implementation(libs.generativeai.google)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            // Logging
            api(libs.logging)

            // Coil Image
            implementation(libs.coil.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "id.andriawan.cofinance"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.andriawan.cofinance"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        generateLocaleConfig = true
    }
}

buildkonfig {
    packageName = "com.andriawan.cofinance"

    defaultConfigs {
        val localProperties = gradleLocalProperties(rootDir, providers)
        val supabaseUrl = localProperties.getProperty("supabase.project_url")
        val supabaseApiKey = localProperties.getProperty("supabase.public_api_key")
        val geminiApiKey = localProperties.getProperty("gemini.api_key")
        val googleAuthApiKey = localProperties.getProperty("google_auth_client_id")

        require(supabaseUrl.isNotEmpty()) { "Register supabase url on local.properties" }
        require(supabaseApiKey.isNotEmpty()) { "Register supabase api key on local.properties" }
        require(geminiApiKey.isNotEmpty()) { "Register gemini api key on local.properties" }
        require(googleAuthApiKey.isNotEmpty()) { "Register google auth api key on local.properties" }

        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_API_KEY", supabaseApiKey)
        buildConfigField(FieldSpec.Type.STRING, "GEMINI_API_KEY", geminiApiKey)
        buildConfigField(FieldSpec.Type.STRING, "GOOGLE_AUTH_API_KEY", googleAuthApiKey)
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Cofinance"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "com.andriawan.cofinance"
            }

            windows {
                menuGroup = "Cofinance"
                upgradeUuid = "YOUR-UNIQUE-UUID"
            }

            linux {
                packageName = "cofinance"
            }
        }
    }
}

