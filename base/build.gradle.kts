plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.android)
}

android {
    namespace = "net.imknown.android.forefrontinfo.base"

    compileSdk = libsBuild.versions.compileSdk.get().toInt()
    // compileSdkExtension = libsBuild.versions.compileSdkExtension.get().toInt()
    buildToolsVersion = libsBuild.versions.buildTools.get()
    val isPreview = libsBuild.versions.isPreview.get().toBoolean()
    if (isPreview) {
        compileSdkPreview = libsBuild.versions.compileSdkPreview.get()
        buildToolsVersion = libsBuild.versions.buildToolsPreview.get()
    }

    defaultConfig {
        minSdk = libsBuild.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

//    java {
//        toolchain {
//            languageVersion = JavaLanguageVersion.of(17)
//        }
//    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring(libsAndroid.desugarJdkLibs)

    // region [AndroidX]
    androidTestImplementation(libsAndroid.bundles.test)
    // endregion [AndroidX]

    // region [3rd Parties]
    api(libsThirdParty.libsu)

//    api(libsThirdParty.bundles.shizuku)
    // endregion [3rd Parties]
}