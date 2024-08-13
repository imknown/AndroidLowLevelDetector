plugins {
    alias(libsAndroid.plugins.lowleveldetector.android.library)

    alias(libsKotlin.plugins.kotlinx.parcelize)
}

android {
    namespace = "net.imknown.android.forefrontinfo.base"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(fileTree("libs") { include("*.jar", "*.aar") })
}