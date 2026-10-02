import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libsAndroid.plugins.lowleveldetector.android.application)
    alias(libsAndroid.plugins.lowleveldetector.android.application.flavors)
    alias(libsAndroid.plugins.lowleveldetector.android.application.ndk.version)

    alias(libsAndroid.plugins.lowleveldetector.android.application.compose)
    // alias(libsKotlin.plugins.compose)

    alias(libsKotlin.plugins.kotlinx.serialization)
    // Metro DI (compiler plugin, no KSP): only :app compiles Metro-annotated
    // code, so alias it directly here instead of wrapping it in a convention
    // plugin (same precedent as kotlinx.serialization above).
    alias(libsThirdParty.plugins.metro)

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
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar", "*.aar") })

    implementation(projects.binderDetector)
    implementation(projects.base)

    // region [AndroidX]
    implementation(libsAndroid.activity)
    implementation(libsAndroid.bundles.annotation)
    implementation(libsAndroid.bundles.compose)
    implementation(libsAndroid.core)
    implementation(libsAndroid.bundles.lifecycle)
    implementation(libsAndroid.savedState)
    implementation(libsAndroid.webkit)
    // endregion [AndroidX]

    // region [Kotlin]
    implementation(libsKotlin.kotlinx.collections.immutable)
    implementation(libsKotlin.kotlinx.coroutines.android)
    implementation(libsKotlin.kotlinx.serialization.json)

    implementation(libsKotlin.bundles.ktor.client)
    // endregion [Kotlin]

    // region [3rd Parties]
    debugImplementation(libsThirdParty.bundles.leakCanary)

    implementation(libsThirdParty.versionCompare)

    implementation(libsThirdParty.libsu)

    // MetroX ViewModel artifacts: ViewModelGraph/MetroViewModelFactory plus the
    // Compose side (LocalMetroViewModelFactory, metroViewModel()). The metro
    // runtime itself (annotations) comes in transitively via the Gradle plugin.
    implementation(libsThirdParty.bundles.metrox.viewmodel)
    // endregion [3rd Parties]
}