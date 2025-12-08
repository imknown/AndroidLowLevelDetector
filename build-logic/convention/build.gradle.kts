plugins {
    `kotlin-dsl`
}

private val packageName = "net.imknown.android.forefrontinfo"

group = "$packageName.buildlogic"

private val buildVersion = libsBuild.versions

kotlin {
    jvmToolchain(buildVersion.javaToolchain.get().toInt())
}

dependencies {
    compileOnly(libsAndroid.android.gradleApiPlugin)
    compileOnly(libsKotlin.kotlin.gradlePlugin)
    compileOnly(libsGoogle.firebase.crashlytics.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        fun android(type: String) = "$packageName.android.$type"
        fun androidApp(pluginName: String) = "${android("app")}.$pluginName"
        fun androidLib(pluginName: String) = "${android("lib")}.$pluginName"

        register("androidApplication") {
            id = libsAndroid.plugins.lowleveldetector.android.application.asProvider().get().pluginId
            implementationClass = androidApp("AndroidApplicationConventionPlugin")
        }
        register("androidFlavors") {
            id = libsAndroid.plugins.lowleveldetector.android.application.flavors.get().pluginId
            implementationClass = androidApp("AndroidApplicationFlavorsConventionPlugin")
        }
        register("androidLibrary") {
            id = libsAndroid.plugins.lowleveldetector.android.library.asProvider().get().pluginId
            implementationClass = androidLib("AndroidLibraryConventionPlugin")
        }
        register("androidApplicationNdkVersion") {
            id = libsAndroid.plugins.lowleveldetector.android.application.ndk.version.get().pluginId
            implementationClass = androidApp("AndroidApplicationNdkVersionConventionPlugin")
        }
        register("androidLibraryNdkVersion") {
            id = libsAndroid.plugins.lowleveldetector.android.library.ndk.version.get().pluginId
            implementationClass = androidLib("AndroidLibraryNdkVersionConventionPlugin")
        }
        register("googleFirebase") {
            id = libsGoogle.plugins.lowleveldetector.google.firebase.get().pluginId
            implementationClass = androidApp("AndroidApplicationFirebaseConventionPlugin")
        }
    }
}