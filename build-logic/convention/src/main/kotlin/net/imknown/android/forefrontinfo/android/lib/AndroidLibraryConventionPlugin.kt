package net.imknown.android.forefrontinfo.android.lib

import com.android.build.api.dsl.CommonExtension
import net.imknown.android.forefrontinfo.android.configureAndroid
import net.imknown.android.forefrontinfo.ext.androidPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = androidPlugin("android-library"))

            configureAndroid<CommonExtension>()
        }
    }
}