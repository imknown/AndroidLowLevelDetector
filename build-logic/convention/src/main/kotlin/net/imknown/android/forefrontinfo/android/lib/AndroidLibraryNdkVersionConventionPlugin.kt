package net.imknown.android.forefrontinfo.android.lib

import com.android.build.api.dsl.LibraryExtension
import net.imknown.android.forefrontinfo.android.configureAndroidNdk
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidLibraryNdkVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            configureAndroidNdk<LibraryExtension>()
        }
    }
}