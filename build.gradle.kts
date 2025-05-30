// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libsAndroid.plugins.android.application) apply false
    alias(libsAndroid.plugins.android.library) apply false

    alias(libsKotlin.plugins.compose.compiler) apply false
    alias(libsKotlin.plugins.kotlinx.serialization) apply false

    alias(libsGoogle.plugins.googleServices) apply false
    alias(libsGoogle.plugins.firebase.crashlytics) apply false
}

// https://developer.android.com/build/releases/agp-9-0-0-release-notes#runtime-dependency-on-kotlin-gradle-plugin-upgrade
buildscript {
    dependencies {
        classpath(libsKotlin.kotlin.gradlePlugin)
    }
}