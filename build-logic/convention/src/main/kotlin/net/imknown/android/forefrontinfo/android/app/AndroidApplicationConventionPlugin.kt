package net.imknown.android.forefrontinfo.android.app
import com.android.build.api.dsl.ApplicationExtension
import net.imknown.android.forefrontinfo.android.configureAndroid
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

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.android")

            configureName()
            configureAndroid<ApplicationExtension>()
        }
    }

    private fun Project.configureName() {
        configure<ApplicationExtension> {
            defaultConfig {
                versionCode = buildVersion("versionCode").toInt()
                versionName = buildVersion("versionName")

                val currentDatetime = getCurrentDatetime()
                val currentGitBranchName =
                    providers.execute("git", "rev-parse", "--abbrev-ref", "HEAD")
                configure<BasePluginExtension> {
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

    private fun ProviderFactory.execute(vararg args: Any) =
        exec { commandLine(*args) }.standardOutput.asText.get().trim()
}