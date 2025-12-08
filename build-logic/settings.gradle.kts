import org.gradle.kotlin.dsl.support.uppercaseFirstChar

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }

    versionCatalogs {
        fun String.toToml() {
            val originalName = this
            val libName = "libs${originalName.uppercaseFirstChar()}"
            register(libName) {
                from(files("../gradle/toml/$originalName.toml"))
            }
        }

        "build".toToml()
        "android".toToml()
        "kotlin".toToml()
        "google".toToml()
        "thirdParty".toToml()
    }
}

rootProject.name = "build-logic"
include(":convention")