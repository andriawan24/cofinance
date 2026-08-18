import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.cmp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidxRoom)
    alias(libs.plugins.ksp)
    id("com.codingfeline.buildkonfig")
}

kotlin {
    android {
        namespace = "id.andriawan.cofinance.shared"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
        // ML Kit text recognition loads native libraries through the Android runtime,
        // so the OCR engine cannot be exercised on the JVM host test target.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources.enable = true
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui.tooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            // androidMain build.gradle
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            // CameraK
            implementation(libs.camerak)

            // On-device OCR
            implementation(libs.mlkit.text.recognition)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            // CameraK
            implementation(libs.camerak)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.navigation)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            // Firebase
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.storage)

            // KTOR
            implementation(libs.ktor.client.core)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            // Logging
            implementation(libs.logging)

            // Coil Image
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            // Durable local storage
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonTest.dependencies {
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

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val localPropertiesProvider = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { content ->
        Properties().apply {
            content.reader().use(::load)
        }
    }

fun requiredBuildConfig(localKey: String, environmentKey: String): String {
    val value = providers.environmentVariable(environmentKey)
        .orElse(localPropertiesProvider.map { properties ->
            properties.getProperty(localKey).orEmpty()
        })
        .orNull

    require(!value.isNullOrBlank()) {
        "Register $localKey in local.properties or provide $environmentKey"
    }

    return value
}

buildkonfig {
    packageName = "com.andriawan.cofinance"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "GOOGLE_AUTH_API_KEY",
            requiredBuildConfig("google_auth_client_id", "GOOGLE_AUTH_CLIENT_ID")
        )
    }
}
