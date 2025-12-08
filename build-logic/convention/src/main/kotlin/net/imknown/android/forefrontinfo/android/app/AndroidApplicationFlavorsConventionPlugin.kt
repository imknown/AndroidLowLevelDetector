package net.imknown.android.forefrontinfo.android.app

import com.android.build.api.dsl.ApplicationExtension
import net.imknown.android.forefrontinfo.android.configureFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configure<ApplicationExtension> {
                configureFlavors(this)
            }
        }
    }
}