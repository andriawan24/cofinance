import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import com.codingfeline.buildkonfig.compiler.FieldSpec
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
            jvmTarget.set(JvmTarget.JVM_11)
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
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
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
            implementation(libs.supabasekt.realtime)

            // KTOR
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            // Gen AI
            implementation(libs.generativeai.google)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

        require(supabaseUrl.isNotEmpty()) { "Register supabase url on local.properties" }
        require(supabaseApiKey.isNotEmpty()) { "Register supabase api key on local.properties" }
        require(geminiApiKey.isNotEmpty()) { "Register gemini api key on local.properties" }

        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(FieldSpec.Type.STRING, "SUPABASE_API_KEY", supabaseApiKey)
        buildConfigField(FieldSpec.Type.STRING, "GEMINI_API_KEY", geminiApiKey)
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

