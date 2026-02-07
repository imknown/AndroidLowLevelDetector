package net.imknown.android.forefrontinfo.android

import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.CommonExtension
import net.imknown.android.forefrontinfo.ext.androidTestImplementation
import net.imknown.android.forefrontinfo.ext.buildVersion
import net.imknown.android.forefrontinfo.ext.libsAndroid
import net.imknown.android.forefrontinfo.ext.testImplementation
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal inline fun <reified T : CommonExtension> Project.configureAndroid() {
    configure<T> {
        configureAndroidSdk(this)
        configureAndroidDesugar(this)
        configureKotlin<KotlinAndroidProjectExtension>()
        configureTest(this)
    }
}

internal fun Project.configureAndroidSdk(commonExtension: CommonExtension) {
    commonExtension.apply {
        val isPreview = buildVersion("isPreview").toBoolean()

        compileSdk {
            version = if (isPreview) {
                val compileSdkPreview = buildVersion("compileSdkPreview")
                preview(compileSdkPreview)
            } else {
                val compileSdk = buildVersion("compileSdk").toInt()
                release(compileSdk) {
                    minorApiLevel = buildVersion("compileSdkMinor").toInt()
                    // sdkExtension = buildVersion("compileSdkExtension").toInt()
                }
            }
        }

        buildToolsVersion = buildVersion(
            if (isPreview) "buildToolsPreview" else "buildTools"
        )

        defaultConfig.apply {
            minSdk = buildVersion("minSdk").toInt()

            if (this is ApplicationBaseFlavor) {
                targetSdk = buildVersion("targetSdk").toInt()
                if (isPreview) {
                    targetSdkPreview = buildVersion("targetSdkPreview")
                }
            }
        }
    }
}

internal inline fun <reified T : CommonExtension> Project.configureAndroidNdk() {
    configure<T> {
        ndkVersion = buildVersion("ndk")
    }
}

internal fun Project.configureTest(commonExtension: CommonExtension) {
    commonExtension.apply {
        defaultConfig.apply {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        dependencies {
            testImplementation(libsAndroid.findBundle("test").get())
            androidTestImplementation(libsAndroid.findBundle("androidTest").get())
        }
    }
}

internal fun Project.configureAndroidDesugar(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileOptions.apply {
            isCoreLibraryDesugaringEnabled = true
        }
    }

    dependencies {
        "coreLibraryDesugaring"(libsAndroid.findLibrary("desugarJdkLibs").get())
    }
}

inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // https://developer.android.com/build/jdks
    // https://kotlinlang.org/docs/gradle-configure-project.html
    // https://docs.gradle.org/current/userguide/toolchains.html
    jvmToolchain(buildVersion("javaToolchain").toInt())

    val compilerOptions = (this as HasConfigurableKotlinCompilerOptions<*>).compilerOptions
    compilerOptions.apply {
        freeCompilerArgs.addAll(
            // region [Experimental in 2.2.0]
            "-Xcontext-parameters",
            "-Xcontext-sensitive-resolution",
            "-Xannotation-target-all",
            "-Xannotation-default-target=param-property",
            "-Xannotations-in-metadata",
            // endregion [Experimental in 2.2.0]

            // region [Experimental in 2.2.20]
            "-Xdata-flow-based-exhaustiveness",
            "-Xallow-reified-type-in-catch",
            "-Xallow-contracts-on-more-functions",
            "-Xallow-condition-implies-returns-contracts",
            "-Xallow-holdsin-contract",
            "-Xwhen-expressions=indy",
            // endregion [Experimental in 2.2.20]

            // region [Experimental in 2.3.0]
            "-Xreturn-value-checker=full",
            "-Xexplicit-backing-fields",
            // endregion [Experimental in 2.3.0]
        )
    }
}