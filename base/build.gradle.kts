plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.android)
}

android {
    namespace = "net.imknown.android.forefrontinfo.base"

    compileSdk = libsBuild.versions.compileSdk.get().toInt()
    // compileSdkExtension = libsBuild.versions.compileSdkExtension.get().toInt()
    // compileSdkPreview = libsBuild.versions.compileSdkPreview.get()
    buildToolsVersion = libsBuild.versions.buildTools.get()
    // buildToolsVersion = libsBuild.versions.buildToolsPreview.get()

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
    // region [Test]
    testImplementation(libsAndroid.junit)
    androidTestImplementation(libsAndroid.test.core)
    androidTestImplementation(libsAndroid.test.espresso.core)
    androidTestImplementation(libsAndroid.test.ext.junit)
    // endregion [Test]
    // endregion [AndroidX]

    // region [3rd Parties]
    api(libsThirdParty.libsu)

//    api(libsThirdParty.shizuku.api)
//    api(libsThirdParty.shizuku.provider)
    // endregion [3rd Parties]
}