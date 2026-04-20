package net.imknown.android.forefrontinfo.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import net.imknown.android.forefrontinfo.ext.androidTestImplementation
import net.imknown.android.forefrontinfo.ext.debugImplementation
import net.imknown.android.forefrontinfo.ext.implementation
import net.imknown.android.forefrontinfo.ext.libsAndroid
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureCompose() {
// configure<T> {
    when (this) {
        is ApplicationExtension -> {
            buildFeatures {
                compose = true
            }
        }

        is LibraryExtension -> {
            buildFeatures {
                compose = true
            }
        }
    }

    dependencies {
        val bom = libsAndroid.findLibrary("compose-bom").get()
        val platform = platform(bom)
        implementation(platform)
        androidTestImplementation(platform)
        implementation(libsAndroid.findBundle("compose").get())
        debugImplementation(libsAndroid.findLibrary("compose-ui-tooling").get())
    }
// }
}