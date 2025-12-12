import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
    id("com.codingfeline.buildkonfig")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
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
        }
    }

    sourceSets {
        commonMain.dependencies {
            // SUPABASE
            implementation(project.dependencies.platform(libs.supabasekt.bom))
            implementation(libs.supabasekt.postgrest)
            implementation(libs.supabasekt.auth)
            implementation(libs.supabasekt.realtime)

            // KTOR
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            // Logging
            implementation(libs.napier)

            // Koin
            api(libs.koin.core)

            // Google gemini
            implementation(libs.generativeai.google)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


buildkonfig {
    packageName = "com.andreasgift.kmpweatherapp"

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

skie {
    features {
        enableFlowCombineConvertorPreview = true
    }
}

android {
    namespace = "id.andriawan24.cofinance"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
