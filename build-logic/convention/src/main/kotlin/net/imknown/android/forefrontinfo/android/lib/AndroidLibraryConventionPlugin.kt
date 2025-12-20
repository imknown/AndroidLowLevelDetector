package net.imknown.android.forefrontinfo.android.lib
import com.android.build.api.dsl.LibraryExtension
import net.imknown.android.forefrontinfo.android.configureAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            configureAndroid<LibraryExtension>()
        }
    }
}