package net.imknown.android.forefrontinfo.android.app

import net.imknown.android.forefrontinfo.android.configureCompose
import net.imknown.android.forefrontinfo.ext.findPluginId
import net.imknown.android.forefrontinfo.ext.libsKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libsKotlin.findPluginId("compose"))

            configureCompose()
        }
    }
}