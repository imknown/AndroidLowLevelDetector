plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.android)
}

android {
    namespace = "net.imknown.android.forefrontinfo.binderDetector"

    compileSdk {
        version = release(libsBuild.versions.compileSdk.get().toInt()) {
            minorApiLevel = libsBuild.versions.compileSdkMinor.get().toInt()
            // sdkExtension = libsBuild.versions.compileSdkExtension.get().toInt()
        }
    }
    buildToolsVersion = libsBuild.versions.buildTools.get()
    val isPreview = libsBuild.versions.isPreview.get().toBoolean()
    if (isPreview) {
        compileSdk {
            version = preview(libsBuild.versions.compileSdkPreview.get())
        }
        buildToolsVersion = libsBuild.versions.buildToolsPreview.get()
    }

    defaultConfig {
        minSdk = libsBuild.versions.minSdk.get().toInt()

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")

                cFlags += listOf("-D__STDC_FORMAT_MACROS")

                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
            }
        }
    }

    ndkVersion = libsBuild.versions.ndk.get()

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = libsBuild.versions.cmake.get()
        }
    }
}

// region [Toolchain]
// https://developer.android.com/build/jdks
// https://kotlinlang.org/docs/gradle-configure-project.html
// https://docs.gradle.org/current/userguide/toolchains.html
private val javaToolchain = libsBuild.versions.javaToolchain.get().toInt()
kotlin {
    jvmToolchain(javaToolchain)
}
// endregion [Toolchain]