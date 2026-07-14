import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.cmp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("com.codingfeline.buildkonfig")
}

val generateComposeStorybook = tasks.register<ComposeStorybookGeneratorTask>("generateComposeStorybook") {
    description = "Generate storybook from every components"
    componentsDir.set(
        layout.projectDirectory.dir("src/commonMain/kotlin/id/andriawan/cofinance/components")
    )
    configFile.set(layout.projectDirectory.file("storybook/storybook.properties"))
    outputDir.set(layout.buildDirectory.dir("generated/storybook/webMain/kotlin"))
}

kotlin {
    android {
        namespace = "id.andriawan.cofinance.shared"
        compileSdk = 36
        minSdk = 24
        withHostTest {}
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        val desktopMain = getByName("desktopMain")

        // Intermediate source set for platforms that support PowerSync (Android, iOS, Desktop)
        val nonWebMain = create("nonWebMain") {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(nonWebMain)
        iosMain.get().dependsOn(nonWebMain)
        desktopMain.dependsOn(nonWebMain)

        // webMain is provided and wired to JS/WasmJS by the default hierarchy template.
        getByName("webMain").apply {
            kotlin.srcDir(generateComposeStorybook.map { it.outputDir })
        }

        nonWebMain.dependencies {
            implementation(libs.powersync.core)
            implementation(libs.powersync.compose)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            // androidMain build.gradle
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)

            // CameraK
            implementation(libs.camerak)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            // CameraK
            implementation(libs.camerak)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)

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

            // Supabase
            implementation(project.dependencies.platform(libs.supabasekt.bom))
            implementation(libs.supabasekt.postgrest)
            implementation(libs.supabasekt.auth)
            implementation(libs.supabasekt.storage)

            // KTOR
            implementation(libs.ktor.client.core)

            // Gen AI
            implementation(libs.generativeai.google)

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
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.configureEach {
    if (name.contains("compile", ignoreCase = true) && name.contains("Kotlin", ignoreCase = true)) {
        dependsOn(generateComposeStorybook)
    }
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
        .orElse(localPropertiesProvider.map { properties -> properties.getProperty(localKey).orEmpty() })
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
            "SUPABASE_URL",
            requiredBuildConfig("supabase.project_url", "SUPABASE_PROJECT_URL")
        )

        buildConfigField(
            FieldSpec.Type.STRING,
            "SUPABASE_API_KEY",
            requiredBuildConfig("supabase.public_api_key", "SUPABASE_PUBLIC_API_KEY")
        )

        buildConfigField(
            FieldSpec.Type.STRING,
            "GEMINI_API_KEY",
            requiredBuildConfig("gemini.api_key", "GEMINI_API_KEY")
        )

        buildConfigField(
            FieldSpec.Type.STRING,
            "GOOGLE_AUTH_API_KEY",
            requiredBuildConfig("google_auth_client_id", "GOOGLE_AUTH_CLIENT_ID")
        )

        buildConfigField(
            FieldSpec.Type.STRING,
            "POWERSYNC_URL",
            requiredBuildConfig("powersync.url", "POWERSYNC_URL")
        )
    }
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
