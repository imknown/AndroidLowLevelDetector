plugins {
    alias(libsAndroid.plugins.lowleveldetector.android.library)
}

android {
    namespace = "net.imknown.android.forefrontinfo.base"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}