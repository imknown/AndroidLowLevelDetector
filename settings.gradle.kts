import org.gradle.kotlin.dsl.support.uppercaseFirstChar

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }

    versionCatalogs {
        fun String.toToml() {
            val originalName = this
            val libName = "libs${originalName.uppercaseFirstChar()}"
            register(libName) {
                from(files("gradle/toml/$originalName.toml"))
            }
        }

        "build".toToml()
        "android".toToml()
        "kotlin".toToml()
        "google".toToml()
        "thirdParty".toToml()
    }
}

rootProject.name = "AndroidLowLevelDetector"

include(":binderDetector")
include(":base")
include(":app")

// region [Build Scan]
// https://gradle.com/scans/gradle/
// https://docs.gradle.com/develocity/gradle-plugin/current/
plugins {
    // https://plugins.gradle.org/plugin/com.gradle.develocity
    id("com.gradle.develocity") version "4.3.2"

    // https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:configuring_daemon_jvm
    // https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"

        publishing.onlyIf { false }
    }
}
// endregion [Build Scan]