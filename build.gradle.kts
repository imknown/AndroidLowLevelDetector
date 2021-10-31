// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version Versions.AndroidBuild.androidGradlePlugin apply false
    id("com.android.library") version Versions.AndroidBuild.androidGradlePlugin apply false
    kotlin("android") version Versions.Kotlin.kotlin apply false
    kotlin("plugin.serialization") version Versions.Kotlin.kotlin apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:${Versions.Firebase.googleServices}")

        classpath("com.google.firebase:firebase-crashlytics-gradle:${Versions.Firebase.firebaseCrashlyticsGradlePlugin}")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
