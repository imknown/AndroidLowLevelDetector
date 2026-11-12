package net.imknown.android.forefrontinfo.android

import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.CommonExtension
import net.imknown.android.forefrontinfo.ext.androidTestImplementation
import net.imknown.android.forefrontinfo.ext.buildVersion
import net.imknown.android.forefrontinfo.ext.libsAndroid
import net.imknown.android.forefrontinfo.ext.libsThirdParty
import net.imknown.android.forefrontinfo.ext.testImplementation
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
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
                targetSdk {
                    version = if (isPreview) {
                        val targetSdkPreview = buildVersion("targetSdkPreview")
                        preview(targetSdkPreview)
                    } else {
                        val targetSdk = buildVersion("targetSdk").toInt()
                        release(targetSdk)
                    }
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

        // Local unit tests run on the JUnit Platform (Jupiter). Instrumented tests stay on JUnit 4:
        // the device-side AndroidJUnitRunner and androidx.test.ext:junit are both JUnit 4 based.
        testOptions.unitTests {
            // Robolectric needs the merged app resources on the JVM to resolve R references.
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                // Robolectric's FileDescriptorInterceptor reaches jdk.internal.access.SharedSecrets and
                // loads its native runtime; newer JDK module encapsulation blocks both without these.
                it.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--enable-native-access=ALL-UNNAMED",
                    // LauncherInterceptor (the bridge's global classloader hook) is gated behind
                    // this platform switch; without it the test classes never route into the sandbox.
                    "-Djunit.platform.launcher.interceptors.enabled=true"
                )
            }
        }

        dependencies {
            testImplementation(libsAndroid.findBundle("test").get())
            androidTestImplementation(libsAndroid.findBundle("androidTest").get())
            // Interaction verification for unit tests. Kept next to the bundle wiring so every
            // module applying this plugin sees the same test stack.
            testImplementation(libsThirdParty.findLibrary("mockk").get())
            // Robolectric fakes the Android framework on the JVM so per-entry detection tests run
            // in CI (the primary means per 2a's adjudication table).
            testImplementation(libsThirdParty.findLibrary("robolectric").get())
            // The Jupiter-platform bridge that loads Robolectric (experimental upstream); it relies
            // on the launcher-interceptor switch among the flags above. RobolectricSpikeTest is its
            // standing smoke test.
            testImplementation(libsThirdParty.findLibrary("junit5-robolectric-extension").get())
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
    jvmToolchain {
        val version = buildVersion("javaToolchain").toInt()
        languageVersion.set(JavaLanguageVersion.of(version))

        vendor.set(JvmVendorSpec.ADOPTIUM)
    }

    val compilerOptions = (this as HasConfigurableKotlinCompilerOptions<*>).compilerOptions
    compilerOptions.apply {
        freeCompilerArgs.addAll(
            // region [Experimental in 2.2.0]
            "-Xcontext-sensitive-resolution",
            // endregion [Experimental in 2.2.0]

            // region [Experimental in 2.2.20]
            "-Xwhen-expressions=indy",
            // endregion [Experimental in 2.2.20]

            // region [Experimental in 2.3.0]
            "-Xreturn-value-checker=full",
            // endregion [Experimental in 2.3.0]

            // region [Experimental in 2.4.0]
            "-Xexplicit-context-arguments",
            "-Xcollection-literals",
            // endregion [Experimental in 2.4.0]

            // region [Experimental in 2.5.0]
            "-Xcompanion-blocks-and-extensions",
            "-Xfull-value-classes",
            "-Xstrict-equality-for-structural-classes",
            // endregion [Experimental in 2.5.0]
        )
    }
}
