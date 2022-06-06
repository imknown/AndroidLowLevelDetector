plugins {
    id("com.android.library")

    kotlin("android")
}

android {
    compileSdk = Versions.AndroidBuild.compileSdk
    // compileSdkPreview = Versions.AndroidBuild.compileSdkPreview
    buildToolsVersion = Versions.AndroidBuild.buildTools

    defaultConfig {
        namespace = "net.imknown.android.forefrontinfo.base"

        minSdk = Versions.AndroidBuild.minSdk
        targetSdk = Versions.AndroidBuild.targetSdk
        // targetSdkPreview = Versions.AndroidBuild.targetSdkPreview

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    buildFeatures {
        viewBinding = true
    }

    lint {
        checkDependencies = true
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${Versions.desugarJdkLibs}")

    // region [AndroidX]
    api("androidx.activity:activity-ktx:${Versions.AndroidX.activity}")

    api("androidx.annotation:annotation:${Versions.AndroidX.annotation}")
    api("androidx.annotation:annotation-experimental:${Versions.AndroidX.annotationExperimental}")

    api("androidx.appcompat:appcompat:${Versions.AndroidX.appcompat}")
    api("androidx.appcompat:appcompat-resources:${Versions.AndroidX.appcompat}")

    api("androidx.arch.core:core-common:${Versions.AndroidX.archCore}")
    api("androidx.arch.core:core-runtime:${Versions.AndroidX.archCore}")

    api("androidx.core:core-ktx:${Versions.AndroidX.core}")

    api("androidx.fragment:fragment-ktx:${Versions.AndroidX.fragment}")

    api("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.AndroidX.lifecycle}")
    api("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.AndroidX.lifecycle}")
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.AndroidX.lifecycle}")

    api("androidx.preference:preference:${Versions.AndroidX.preference}")

    api("androidx.savedstate:savedstate:${Versions.AndroidX.savedState}")
    // endregion [AndroidX]

    // region [Test]
    testImplementation("junit:junit:${Versions.AndroidX.Test.junit}")
    androidTestImplementation("androidx.test.ext:junit:${Versions.AndroidX.Test.extJunit}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.AndroidX.Test.espressoCore}")
    // endregion [Test]

    // region [Kotlin]
    api(kotlin("stdlib-jdk8:${Versions.Kotlin.kotlin}"))
    // api(kotlin("reflect:${Versions.Kotlin.kotlin}"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.Kotlin.coroutines}")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.Kotlin.serialization}")
    // endregion [Kotlin]

    // region [3rd Parties]
    api("com.github.topjohnwu.libsu:core:${Versions.ThirdParties.libsu}")

    api("com.github.kittinunf.fuel:fuel:${Versions.ThirdParties.fuel}")
    // api("com.github.kittinunf.fuel:fuel-coroutines:$thirdParties.fuel")

    api("io.github.g00fy2:versioncompare:${Versions.ThirdParties.versionCompare}")
    // endregion [3rd Parties]

    api(platform("com.google.firebase:firebase-bom:${Versions.Firebase.billOfMaterials}"))
    api("com.google.firebase:firebase-analytics-ktx")
    api("com.google.firebase:firebase-crashlytics-ndk")
}