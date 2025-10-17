plugins {
    alias(libsAndroid.plugins.android.library)

    alias(libsKotlin.plugins.kotlin.android)
}

private val buildVersion = libsBuild.versions

android {
    namespace = "net.imknown.android.forefrontinfo.binderDetector"

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
        minSdk = buildVersion.minSdk.get().toInt()

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")

                cFlags += listOf("-D__STDC_FORMAT_MACROS")

                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
            }
        }
    }

    ndkVersion = buildVersion.ndk.get()

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = buildVersion.cmake.get()
        }
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