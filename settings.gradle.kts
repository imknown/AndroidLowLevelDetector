import org.gradle.kotlin.dsl.support.uppercaseFirstChar

pluginManagement {
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
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    versionCatalogs {
        fun String.toToml() {
            val originalName = this
            val libName = "libs${originalName.uppercaseFirstChar()}"
            create(libName) {
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

include(":base")
include(":app")