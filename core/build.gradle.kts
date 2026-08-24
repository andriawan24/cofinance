import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "id.andriawan.cofinance.core"
        compileSdk = 37
        minSdk = 24

        withHostTest {
            /* no-op */
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { _ -> /* framework packaging stays with composeApp */ }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Client-side encryption
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }

        androidMain.dependencies {
            // On-device OCR
            implementation(libs.mlkit.text.recognition)

            // App lock: the biometric prompt and its enrollment-invalidating Keystore key
            implementation(libs.androidx.biometric)
        }

        commonTest.dependencies {
            implementation(projects.coreTesting)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}
