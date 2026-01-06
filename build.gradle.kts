buildscript {
    dependencies {
        classpath(libs.buildkonfig.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.cmp) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kmp) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinSerialization).apply(false)
}
