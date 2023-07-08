// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version Versions.AndroidBuild.androidGradlePlugin apply false
    id("com.android.library") version Versions.AndroidBuild.androidGradlePlugin apply false
    kotlin("android") version Versions.Kotlin.kotlin apply false
    kotlin("plugin.serialization") version Versions.Kotlin.kotlin apply false
    id("com.google.gms.google-services") version Versions.Firebase.googleServices apply false
    id("com.google.firebase.crashlytics") version Versions.Firebase.firebaseCrashlyticsGradlePlugin apply false
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
