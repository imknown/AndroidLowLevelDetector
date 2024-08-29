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

    buildFeatures {
        viewBinding = true
    }

    lint {
        checkDependencies = true
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring(libsAndroid.desugarJdkLibs)

    api(libsGoogle.material)

    // region [AndroidX]
    api(libsAndroid.activity)

    api(libsAndroid.annotation)
    api(libsAndroid.annotation.experimental)

    api(libsAndroid.appcompat)
    api(libsAndroid.appcompat.resources)

    api(libsAndroid.arch.core.common)
    api(libsAndroid.arch.core.runtime)

    api(libsAndroid.core)

    api(libsAndroid.fragment)

    api(libsAndroid.lifecycle.viewmodel)
    api(libsAndroid.lifecycle.viewmodel.savedstate)

    api(libsAndroid.preference)

    api(libsAndroid.savedState)

    // region [Test]
    testImplementation(libsAndroid.junit)
    androidTestImplementation(libsAndroid.test.core)
    androidTestImplementation(libsAndroid.test.espresso.core)
    androidTestImplementation(libsAndroid.test.ext.junit)
    // endregion [Test]
    // endregion [AndroidX]

    // region [Kotlin]
    api(libsKotlin.kotlinx.coroutines.android)
    api(libsKotlin.kotlinx.serialization.json)
    // endregion [Kotlin]

    // region [3rd Parties]
    api(libsThirdParty.libsu)

    api(libsThirdParty.fuel)
    // api(libsThirdParty.fuel.coroutines)

    api(libsThirdParty.versionCompare)

//    api(libsThirdParty.shizuku.api)
//    api(libsThirdParty.shizuku.provider)
    // endregion [3rd Parties]
}