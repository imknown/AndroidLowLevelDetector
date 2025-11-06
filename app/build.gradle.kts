import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.FileInputStream
import java.util.Properties
import kotlin.reflect.KFunction1

plugins {
    alias(libsAndroid.plugins.android.application)

    alias(libsKotlin.plugins.kotlin.android)
    alias(libsKotlin.plugins.kotlinx.serialization)

    alias(libsGoogle.plugins.googleServices)
    alias(libsGoogle.plugins.firebase.crashlytics)
}

private val buildVersion = libsBuild.versions

android {
    namespace = "net.imknown.android.forefrontinfo"

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
        versionCode = buildVersion.versionCode.get().toInt()
        versionName = buildVersion.versionName.get()

        val currentDatetime = getCurrentDatetime()
        val currentGitBranchName = providers.execute("git", "rev-parse", "--abbrev-ref", "HEAD")
        base.archivesName.set("lld-$versionName-$versionCode-$currentDatetime-$currentGitBranchName")

        minSdk = buildVersion.minSdk.get().toInt()
        targetSdk = buildVersion.targetSdk.get().toInt()
        if (isPreview) {
            targetSdkPreview = buildVersion.targetSdkPreview.get()
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GIT_BRANCH", "\"$currentGitBranchName\"")
    }

    ndkVersion = buildVersion.ndk.get()

    sourceSets {
        named("main") {
            val javaPathString = java.directories.toList()[0] // "src/main/java"
            val javaPackageName = namespace?.replace('.', File.separatorChar) // "net/imknown/android/forefrontinfo"
            fun String.toResString() = "$javaPathString/$javaPackageName/$this/res"
            val baseResString = "base".toResString()

            res.srcDirs(
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

    flavorDimensions += IssueTracker::class.simpleName.toString()

    productFlavors {
        register(IssueTracker.Foss.name) {
            isDefault = true
            versionNameSuffix = "-$name"
        }
        register(IssueTracker.Firebase.name)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName(name)

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-firebase-rules.pro"
            )

            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }

        debug {
            isDebuggable = true
            isJniDebuggable = true

            signingConfig = signingConfigs.getByName(name)

            applicationIdSuffix = ".$name"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
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

fun Task.disable() {
    println("Task $name disabled.")
    enabled = false
}

// gradle.taskGraph.whenReady {
//    tasks.forEach { task ->
tasks.configureEach {
    val task = this

    val flavorFoss = IssueTracker.Foss.name

    val isGoogleServices = task.name.startsWith("process$flavorFoss")
            && task.name.endsWith("GoogleServices")
    if (isGoogleServices) {
        task.disable()
        return@configureEach
    }

    val isFirebaseCrashlytics = task.name.contains("Crashlytics")
            && task.name.contains(flavorFoss)
    if (isFirebaseCrashlytics) {
        task.disable()
        return@configureEach
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":binderDetector"))
    implementation(project(":base"))

    coreLibraryDesugaring(libsAndroid.desugarJdkLibs)

    // region [AndroidX]
    implementation(libsAndroid.activity.ktx)
    implementation(libsAndroid.bundles.annotation)
    implementation(libsAndroid.bundles.appcompat)
    implementation(libsAndroid.bundles.arch.core)
    implementation(libsAndroid.cardView)
    implementation(libsAndroid.constraintLayout)
    implementation(libsAndroid.coordinatorLayout)
    implementation(libsAndroid.core.ktx)
    implementation(libsAndroid.fragment.ktx)
    implementation(libsAndroid.bundles.lifecycle)
    implementation(libsAndroid.preference.ktx)
    implementation(libsAndroid.recyclerView)
    implementation(libsAndroid.savedState.ktx)
    implementation(libsAndroid.swipeRefreshLayout)
    implementation(libsAndroid.webkit)

    testImplementation(libsAndroid.bundles.test)
    androidTestImplementation(libsAndroid.bundles.androidTest)
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

    val implementationRef: KFunction1<*, *> = ::implementation
    val firebaseImplementation = IssueTracker.Firebase.name + implementationRef.name.uppercaseFirstChar()
    // val firebaseImplementation by configurations
    firebaseImplementation(platform(libsGoogle.firebase.bom))
    firebaseImplementation(libsGoogle.bundles.firebase)
    // endregion [Google]
}