import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libsAndroid.plugins.lowleveldetector.android.application)
    alias(libsAndroid.plugins.lowleveldetector.android.application.flavors)
    alias(libsAndroid.plugins.lowleveldetector.android.application.ndk.version)

    alias(libsKotlin.plugins.compose.compiler)
    alias(libsKotlin.plugins.kotlinx.serialization)

    alias(libsGoogle.plugins.lowleveldetector.google.firebase)
}

android {
    namespace = "net.imknown.android.forefrontinfo"

    sourceSets {
        named("main") {
            val javaPathString = java.directories.toList()[0] // "src/main/java"
            val javaPackageName = namespace?.replace('.', File.separatorChar) // "net/imknown/android/forefrontinfo"
            fun String.toResString() = "$javaPathString/$javaPackageName/$this/res"
            val baseResString = "base".toResString()

            res.directories += listOf(
                baseResString,
                "${baseResString}Launcher",
                "${baseResString}Backup",
                "${baseResString}Theme",
                "ui".toResString(), // Main
                "ui/base/list".toResString(),
                "ui/home".toResString(),
                "ui/others".toResString(),
                "ui/prop".toResString(),
                "ui/settings".toResString()
            )
        }
    }

    androidResources {
        localeFilters += listOf("zh-rCN", "zh-rTW", "fr-rFR")
        generateLocaleConfig = true
    }

    signingConfigs {
        register("release") {
            val keystorePropertiesFile = file("$rootDir/local.properties")
            if (!keystorePropertiesFile.exists()) {
                return@register
            }

            val keystoreProperties = Properties().apply {
                load(FileInputStream(keystorePropertiesFile))
            }

            storeFile = file(keystoreProperties["storeFile"].toString())
            storePassword = keystoreProperties["storePassword"].toString()
            keyAlias = keystoreProperties["keyAlias"].toString()
            keyPassword = keystoreProperties["keyPassword"].toString()

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

        named("debug") {
            storeFile = file("$rootDir/keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.named(name).get()

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            isDebuggable = true
            isJniDebuggable = true

            signingConfig = signingConfigs.named(name).get()

            applicationIdSuffix = ".$name"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar", "*.aar") })

    implementation(project(":binderDetector"))
    implementation(project(":base"))

    // region [AndroidX]
    implementation(libsAndroid.activity.ktx)

    // region [Compose]
    val composeBom = platform(libsAndroid.compose.bom)
    implementation(composeBom)
    implementation(libsAndroid.activity.compose)
    implementation(libsAndroid.lifecycle.runtime.compose)
    implementation(libsAndroid.lifecycle.viewmodel.compose)
    implementation(libsAndroid.compose.material3)
    implementation(libsAndroid.compose.material.icons.core)
    implementation(libsAndroid.compose.ui)
    implementation(libsAndroid.compose.ui.tooling.preview)

    debugImplementation(libsAndroid.compose.ui.test.manifest)
    debugImplementation(libsAndroid.compose.ui.tooling)
    androidTestImplementation(composeBom)
    androidTestImplementation(libsAndroid.compose.ui.test.junit4)
    // endregion [Compose]

    implementation(libsAndroid.bundles.annotation)
    implementation(libsAndroid.bundles.appcompat)
    implementation(libsAndroid.bundles.arch.core)
    implementation(libsAndroid.cardView)
    implementation(libsAndroid.constraintLayout)
    implementation(libsAndroid.constraintLayout.compose)
    implementation(libsAndroid.coordinatorLayout)
    implementation(libsAndroid.core.ktx)
    implementation(libsAndroid.fragment.ktx)
    implementation(libsAndroid.bundles.lifecycle)
    implementation(libsAndroid.preference.ktx)
    implementation(libsAndroid.recyclerView)
    implementation(libsAndroid.savedState.ktx)
    implementation(libsAndroid.swipeRefreshLayout)
    implementation(libsAndroid.webkit)
    // endregion [AndroidX]

    // region [Kotlin]
    implementation(libsKotlin.kotlinx.coroutines.android)
    implementation(libsKotlin.kotlinx.serialization.json)

    implementation(libsKotlin.bundles.ktor.client)
    // endregion [Kotlin]

    // region [3rd Parties]
    debugImplementation(libsThirdParty.bundles.leakCanary)

    implementation(libsThirdParty.versionCompare)

    implementation(libsThirdParty.libsu)
    // endregion [3rd Parties]

    // region [Google]
    implementation(libsGoogle.material)
    // endregion [Google]
}