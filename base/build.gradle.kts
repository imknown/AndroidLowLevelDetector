plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.android)
}

private val buildVersion = libsBuild.versions

android {
    namespace = "net.imknown.android.forefrontinfo.base"

    val isPreview = buildVersion.isPreview.get().toBoolean()
    compileSdk {
        version = if (isPreview) {
            preview(buildVersion.compileSdkPreview.get())
        } else {
            release(buildVersion.compileSdk.get().toInt()) {
                minorApiLevel = buildVersion.compileSdkMinor.get().toInt()
                // sdkExtension = buildVersion.compileSdkExtension.get().toInt()
            }
        }
    }
    buildToolsVersion = (if (isPreview) buildVersion.buildToolsPreview else buildVersion.buildTools).get()

    defaultConfig {
        minSdk = buildVersion.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

// region [Toolchain]
// https://developer.android.com/build/jdks
// https://kotlinlang.org/docs/gradle-configure-project.html
// https://docs.gradle.org/current/userguide/toolchains.html
kotlin {
    jvmToolchain(buildVersion.javaToolchain.get().toInt())
}
// endregion [Toolchain]

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring(libsAndroid.desugarJdkLibs)

    // region [AndroidX]
    testImplementation(libsAndroid.bundles.test)
    androidTestImplementation(libsAndroid.bundles.androidTest)
    // endregion [AndroidX]
}