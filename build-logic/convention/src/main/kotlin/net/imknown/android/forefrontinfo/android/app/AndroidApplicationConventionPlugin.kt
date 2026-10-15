package net.imknown.android.forefrontinfo.android.app

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import net.imknown.android.forefrontinfo.android.configureAndroid
import net.imknown.android.forefrontinfo.ext.androidPlugin
import net.imknown.android.forefrontinfo.ext.buildVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val UNKNOWN = "unknown"

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = androidPlugin("android-application"))

            configureName()
            configureAndroid<CommonExtension>()
        }
    }

    private fun Project.configureName() {
        configure<ApplicationExtension> {
            defaultConfig {
                versionCode = buildVersion("versionCode").toInt()
                versionName = buildVersion("versionName")

                val currentDatetime = getCurrentDatetime()
                val currentGitBranchName = runCatching {
                    providers.executeOrThrow("git", "rev-parse", "--abbrev-ref", "HEAD").ifEmpty { UNKNOWN }
                }.getOrDefault(UNKNOWN)
                this@configureName.configure<BasePluginExtension> {
                    archivesName.set("lld-$versionName-$versionCode-$currentDatetime-$currentGitBranchName")
                }

                buildConfigField("String", "GIT_BRANCH", "\"$currentGitBranchName\"")
            }
        }
    }

    private fun getCurrentDatetime(): String {
        val pattern = "yyyyMMdd-HHmm"
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val instant = Instant.now()
        val datetime = instant.atZone(ZoneId.systemDefault())
        return formatter.format(datetime)
    }

    private fun ProviderFactory.executeOrThrow(vararg args: Any): String =
        exec {
            commandLine(*args)
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()
}