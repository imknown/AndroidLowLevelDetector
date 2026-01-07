package net.imknown.android.forefrontinfo.android.app

import com.android.build.api.dsl.CommonExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import net.imknown.android.forefrontinfo.android.Flavor
import net.imknown.android.forefrontinfo.ext.implementation
import net.imknown.android.forefrontinfo.ext.libsGoogle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

class AndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.gms.google-services")
            apply(plugin = "com.google.firebase.crashlytics")

            dependencies {
                val firebaseImplementation = Flavor.Firebase.name + implementation.uppercaseFirstChar()

                val bom = libsGoogle.findLibrary("firebase.bom").get()
                firebaseImplementation(platform(bom))
                val analytics = libsGoogle.findLibrary("firebase.analytics").get()
                firebaseImplementation(analytics)
                val crashlytics = libsGoogle.findLibrary("firebase.crashlytics.ndk").get()
                firebaseImplementation(crashlytics)
            }

            configure<CommonExtension> {
                buildTypes.configureEach {
                    configure<CrashlyticsExtension> {
                        nativeSymbolUploadEnabled = true
                    }

                    if (isMinifyEnabled) {
                        proguardFiles += file("proguard-firebase-rules.pro")
                    }
                }
            }

            // region [Disable for Foss]
            fun Task.disable() {
                println("Task $name disabled.")
                enabled = false
            }

            tasks.configureEach {
                val task = this

                val flavorFoss = Flavor.Foss.name

                val isGoogleServices = task.name.startsWith("process$flavorFoss")
                        && task.name.endsWith("GoogleServices")
                if (isGoogleServices) {
                    task.disable()
                    return@configureEach
                }

                val isFirebaseCrashlytics = task.name.contains("Crashlytics")
                        && task.name.contains(flavorFoss)
                if (isFirebaseCrashlytics) {
                    task.disable()
                    return@configureEach
                }
            }
            // endregion [Disable for Foss]
        }
    }
}