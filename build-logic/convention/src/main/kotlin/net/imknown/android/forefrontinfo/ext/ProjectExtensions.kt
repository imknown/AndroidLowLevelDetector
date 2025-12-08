package net.imknown.android.forefrontinfo.ext

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

fun VersionCatalog.findVersionString(alias: String) = findVersion(alias).get().toString()

fun Project.buildVersion(alias: String) = libsBuild.findVersionString(alias)

fun Project.libs(suffix: String) =
    extensions.getByType<VersionCatalogsExtension>()
        .named("libs${suffix.uppercaseFirstChar()}")
val Project.libsBuild get() = libs("build")
val Project.libsAndroid get() = libs("android")
val Project.libsKotlin get() = libs("kotlin")
val Project.libsGoogle get() = libs("google")
val Project.libsThirdParty get() = libs("thirdParty")

const val implementation = "implementation"
val testImplementation = "test" + implementation.uppercaseFirstChar()
val androidTestImplementation = "android" + testImplementation.uppercaseFirstChar()