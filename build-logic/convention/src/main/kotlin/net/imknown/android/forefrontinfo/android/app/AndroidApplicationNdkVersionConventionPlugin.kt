package net.imknown.android.forefrontinfo.android.app

import com.android.build.api.dsl.ApplicationExtension
import net.imknown.android.forefrontinfo.android.configureAndroidNdk
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidApplicationNdkVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            configureAndroidNdk<ApplicationExtension>()
        }
    }
}