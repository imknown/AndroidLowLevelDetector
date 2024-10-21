import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libsAndroid.plugins.android.application)

    alias(libsKotlin.plugins.kotlin.android)
    alias(libsKotlin.plugins.kotlinx.serialization)

    alias(libsGoogle.plugins.googleServices)
    alias(libsGoogle.plugins.firebase.crashlytics)
}

android {
    namespace = "net.imknown.android.forefrontinfo"

    compileSdk = libsBuild.versions.compileSdk.get().toInt()
    // compileSdkExtension = libsBuild.versions.compileSdkExtension.get().toInt()
    // compileSdkPreview = libsBuild.versions.compileSdkPreview.get()
    buildToolsVersion = libsBuild.versions.buildTools.get()

    defaultConfig {
        versionCode = libsBuild.versions.versionCode.get().toInt()
        versionName = libsBuild.versions.versionName.get()

        val currentDatetime = getCurrentDatetime()
        val currentGitBranchName = providers.execute("git", "rev-parse", "--abbrev-ref", "HEAD")
        base.archivesName.set("lld-$versionName-$versionCode-$currentDatetime-$currentGitBranchName")

        minSdk = libsBuild.versions.minSdk.get().toInt()
        targetSdk = libsBuild.versions.targetSdk.get().toInt()
        // targetSdkPreview = libsBuild.versions.targetSdkPreview.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.addAll(listOf("zh-rCN", "zh-rTW", "fr-rFR"))

        buildConfigField("String", "GIT_BRANCH", "\"$currentGitBranchName\"")

        ndkVersion = libsBuild.versions.ndk.get()

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")

                cFlags += listOf("-D__STDC_FORMAT_MACROS")

                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
            }
        }
    }

    signingConfigs {
        register("release") {
            val keystorePropertiesFile = file("$rootDir/local.properties")
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
        register(IssueTracker.Foss.name)
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

//    java {
//        toolchain {
//            languageVersion = JavaLanguageVersion.of(17)
//        }
//    }

    kotlin {
        jvmToolchain(17)
    }

    externalNativeBuild {
        cmake {
            path("CMakeLists.txt")
            version = libsBuild.versions.cmake.get()
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    lint {
        checkDependencies = true
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}

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

    implementation(project(":base"))

    coreLibraryDesugaring(libsAndroid.desugarJdkLibs)

    // region [AndroidX]
    implementation(libsAndroid.cardView)

    implementation(libsAndroid.constraintLayout)

    implementation(libsAndroid.coordinatorLayout)

    implementation(libsAndroid.recyclerView)

    implementation(libsAndroid.swipeRefreshLayout)
    implementation(libsAndroid.webkit)
    // endregion [AndroidX]

    // region [Test]
    testImplementation(libsAndroid.junit)
    androidTestImplementation(libsAndroid.test.core)
    androidTestImplementation(libsAndroid.test.espresso.core)
    androidTestImplementation(libsAndroid.test.ext.junit)
    // endregion [Test]

    // region [Kotlin]
    implementation(libsKotlin.kotlinx.coroutines.android)
    implementation(libsKotlin.kotlinx.serialization.json)

    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation("io.ktor:ktor-client-logging:3.0.0")
    implementation("org.slf4j:slf4j-android:2.0.7")
    // endregion [Kotlin]

    // region [3rd Parties]
    debugImplementation(libsThirdParty.leakCanary.android)
    // implementation(libsThirdParty.leakCanary.plumber.android)

    implementation(libsThirdParty.versionCompare)
    // endregion [3rd Parties]

    val firebaseImplementation = IssueTracker.Firebase.name + "Implementation"
    firebaseImplementation(platform(libsGoogle.firebase.bom))
    firebaseImplementation(libsGoogle.firebase.analytics)
    firebaseImplementation(libsGoogle.firebase.crashlytics.ndk)
}