import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")

    id("kotlin-android")

    kotlin("plugin.serialization")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = Versions.AndroidBuild.compileSdk
    // compileSdkPreview = Versions.AndroidBuild.compileSdkPreview
    buildToolsVersion = Versions.AndroidBuild.buildTools

    defaultConfig {
        namespace = "net.imknown.android.forefrontinfo"

        versionCode = Versions.AndroidBuild.versionCode
        versionName = Versions.AndroidBuild.versionName

        val currentDatetime = getCurrentDatetime()
        val currentGitBranchName = "git rev-parse --abbrev-ref HEAD".execute()
        base.archivesName.set("lld-$versionName-$versionCode-$currentDatetime-$currentGitBranchName")

        minSdk = Versions.AndroidBuild.minSdk
        targetSdk = Versions.AndroidBuild.targetSdk
        // targetSdkPreview = Versions.AndroidBuild.targetSdkPreview

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations.addAll(listOf("zh-rCN", "fr-rFR"))

        buildConfigField("String", "GIT_BRANCH", "\"$currentGitBranchName\"")

        ndkVersion = Versions.AndroidBuild.ndk

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")

                cFlags += listOf("-D__STDC_FORMAT_MACROS")

                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
            }
        }
    }

    signingConfigs {
        create("config") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }

        create("release") {
            val keystorePropertiesFile = file("$rootDir/local.properties")
            val keystoreProperties = Properties().apply {
                load(FileInputStream(keystorePropertiesFile))
            }

            storeFile = file(keystoreProperties["storeFile"].toString())
            storePassword = keystoreProperties["storePassword"].toString()
            keyAlias = keystoreProperties["keyAlias"].toString()
            keyPassword = keystoreProperties["keyPassword"].toString()
        }

        getByName("debug") {
            storeFile = file("$rootDir/keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-firebase-rules.pro",
                "proguard-rules-kotlinx-serialization-json.pro"
            )

            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }

        debug {
            isDebuggable = true
            isJniDebuggable = true

            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".$name"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    externalNativeBuild {
        cmake {
            path("CMakeLists.txt")
            version = Versions.AndroidBuild.cmake
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        checkDependencies = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":base"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${Versions.desugarJdkLibs}")

    // region [AndroidX]
    implementation("com.google.android.material:material:${Versions.material}")

    implementation("androidx.cardview:cardview:${Versions.AndroidX.cardView}")

    implementation("androidx.coordinatorlayout:coordinatorlayout:${Versions.AndroidX.coordinatorLayout}")

    implementation("androidx.constraintlayout:constraintlayout:${Versions.AndroidX.constraintLayout}")

    implementation("androidx.recyclerview:recyclerview:${Versions.AndroidX.recyclerView}")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:${Versions.AndroidX.swipeRefreshLayout}")
    implementation("androidx.webkit:webkit:${Versions.AndroidX.webkit}")
    // endregion [AndroidX]

    // region [Test]
    testImplementation("junit:junit:${Versions.AndroidX.Test.junit}")
    androidTestImplementation("androidx.test.ext:junit:${Versions.AndroidX.Test.extJunit}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.AndroidX.Test.espressoCore}")
    // endregion [Test]

    // region [3rd Parties]
    debugImplementation("com.squareup.leakcanary:leakcanary-android:${Versions.ThirdParties.leakCanary}")
    // implementation ("com.squareup.leakcanary:plumber-android:${Versions.ThirdParties.leakCanary}")
    // endregion [3rd Parties]
}