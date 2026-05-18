package net.imknown.android.forefrontinfo.ext

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import kotlin.reflect.KProperty

fun VersionCatalog.findVersionString(alias: String) = findVersion(alias).get().toString()

fun Project.buildVersion(alias: String) = libsBuild.findVersionString(alias)

fun VersionCatalog.findPluginId(alias: String) = findPlugin(alias).get().get().pluginId
fun Project.androidPlugin(alias: String) = libsAndroid.findPluginId(alias)
fun Project.googlePlugin(alias: String) = libsGoogle.findPluginId(alias)

object ProjectLibProvider {
    operator fun getValue(thisRef: Project, property: KProperty<*>): VersionCatalog =
        thisRef.extensions.getByType<VersionCatalogsExtension>().named(property.name)
}
val Project.libsBuild: VersionCatalog by ProjectLibProvider
val Project.libsAndroid: VersionCatalog by ProjectLibProvider
// val Project.libsKotlin: VersionCatalog by ProjectLibProvider
val Project.libsGoogle: VersionCatalog by ProjectLibProvider
// val Project.libsThirdParty: VersionCatalog by ProjectLibProvider

object NameProvider {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = property.name
}
val implementation by NameProvider
val testImplementation by NameProvider
val androidTestImplementation by NameProvider