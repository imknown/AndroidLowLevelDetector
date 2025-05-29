// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libsAndroid.plugins.android.application) apply false
    alias(libsAndroid.plugins.android.library) apply false

    alias(libsKotlin.plugins.kotlin.android) apply false
    alias(libsKotlin.plugins.kotlinx.serialization) apply false

    alias(libsGoogle.plugins.googleServices) apply false
    alias(libsGoogle.plugins.firebase.crashlytics) apply false
}