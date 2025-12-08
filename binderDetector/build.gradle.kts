plugins {
    alias(libsAndroid.plugins.lowleveldetector.android.library)
    alias(libsAndroid.plugins.lowleveldetector.android.library.ndk.version)
}

private val buildVersion = libsBuild.versions

android {
    namespace = "net.imknown.android.forefrontinfo.binderDetector"

    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_ARM_NEON=TRUE", "-DANDROID_TOOLCHAIN=clang")

                cFlags += listOf("-D__STDC_FORMAT_MACROS")

                cppFlags += listOf("-fexceptions", "-frtti", "-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = buildVersion.cmake.get()
        }
    }
}