import com.android.tools.r8.internal.he
import java.awt.Desktop
import kotlin.collections.plusAssign

plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.multiplatform)
}

android {
    namespace = "net.imknown.android.forefrontinfo.binderDetector"

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

//        externalNativeBuild {
//            cmake {
//                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")
//
//                cFlags += listOf("-D__STDC_FORMAT_MACROS")
//
//                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
//            }
//        }
    }

    ndkVersion = libsBuild.versions.ndk.get()

//    externalNativeBuild {
//        cmake {
//            path("src/main/cpp/CMakeLists.txt")
//            version = libsBuild.versions.cmake.get()
//        }
//    }
}

// region [Toolchain]
// https://developer.android.com/build/jdks
// https://kotlinlang.org/docs/gradle-configure-project.html
// https://docs.gradle.org/current/userguide/toolchains.html
private val javaToolchain = libsBuild.versions.javaToolchain.get().toInt()
kotlin {
    jvmToolchain(javaToolchain)

    androidTarget()

    val androidTargets = listOf(
        androidNativeArm64(),
        androidNativeArm32(),
        androidNativeX64(),
        androidNativeX86()
    )
    androidTargets.forEach {
        it.binaries {
            executable()
//            sharedLib("BinderDetector")
        }

        with(it) {
            compilations.getByName("main") {
                val binderDetector by cinterops.creating {
                }
            }
        }
    }
}
// endregion [Toolchain]