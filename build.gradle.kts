buildscript {
    dependencies {
        classpath(libs.buildkonfig.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.googleService).apply(false)
    alias(libs.plugins.skie).apply(false)
    alias(libs.plugins.ksp).apply(false)
}
