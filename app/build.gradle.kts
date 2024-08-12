import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libsAndroid.plugins.android.application)

    alias(libsKotlin.plugins.kotlin.android)
    alias(libsKotlin.plugins.kotlinx.serialization)
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
        create("release") {
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

        getByName("debug") {
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
        create(IssueTracker.Foss.name)
        create(IssueTracker.Firebase.name)
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

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

    applicationVariants.forEach { variant ->
        if (variant.flavorName == IssueTracker.Firebase.name) {
            plugins {
                alias(libsGoogle.plugins.googleServices)
                alias(libsGoogle.plugins.firebase.crashlytics)
            }

            if (variant.buildType.name == "release") {
                configure<CrashlyticsExtension> {
                    nativeSymbolUploadEnabled = true
                }
            }
        }
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

    // region [3rd Parties]
    debugImplementation(libsThirdParty.leakCanary.android)
    // implementation(libsThirdParty.leakCanary.plumber.android)
    // endregion [3rd Parties]

    val firebaseImplementation = IssueTracker.Firebase.name + "Implementation"
    firebaseImplementation(platform(libsGoogle.firebase.bom))
    firebaseImplementation(libsGoogle.firebase.analytics)
    firebaseImplementation(libsGoogle.firebase.crashlytics.ndk)
}