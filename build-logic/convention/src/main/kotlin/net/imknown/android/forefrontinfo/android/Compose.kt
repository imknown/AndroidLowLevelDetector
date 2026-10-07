package net.imknown.android.forefrontinfo.android

import com.android.build.api.dsl.CommonExtension
import net.imknown.android.forefrontinfo.ext.androidTestImplementation
import net.imknown.android.forefrontinfo.ext.debugImplementation
import net.imknown.android.forefrontinfo.ext.implementation
import net.imknown.android.forefrontinfo.ext.libsAndroid
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal inline fun <reified T : CommonExtension> Project.configureCompose() {
    configure<T> {
        // The receiver's static type is T, and its bound CommonExtension exposes only getBuildFeatures():
        // the buildFeatures {} lambda is declared on ApplicationExtension and LibraryExtension alone,
        // so this can only be a property assignment, never the lambda form.
        buildFeatures.compose = true
    }

    dependencies {
        val bom = libsAndroid.findLibrary("compose-bom").get()
        val platform = platform(bom)
        implementation(platform)
        androidTestImplementation(platform)
        implementation(libsAndroid.findBundle("compose").get())
        debugImplementation(libsAndroid.findLibrary("compose-ui-tooling").get())
    }
}
