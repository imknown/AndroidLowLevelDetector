package net.imknown.android.forefrontinfo.ui.prop.repository

import android.provider.Settings
import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.toPropMyModel
import net.imknown.android.forefrontinfo.ui.base.list.toTranslatedDetailMyModel
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import kotlin.reflect.KClass

// Constructor injection puts the dependency chain on the signature and lets Metro build it
// (issues-cn #02); every param is a concrete @Inject type, so no @Provides is needed.
@Inject
class PropRepository(
    private val propertiesDataSource: PropertiesDataSource,
    private val settingsDataSource: SettingsDataSource,
) {
    // No block-level guard (issues-cn #20): row titles are the prop keys themselves and
    // cannot collide. Nothing in here can throw, so no try/catch that could never fire.
    fun getSystemProp(): List<MyModel> =
        propertiesDataSource.getSystemProp().map { [titleOrNull, detailOrNull] ->
            val title = titleOrNull.toString()
            val detail = if (detailOrNull == System.lineSeparator()) {
                PropertiesDataSource.UNIX_LIKE_NEWLINE_ORIGIN
            } else {
                detailOrNull?.toString()
            }
            toTranslatedDetailMyModel(title, detail)
        }

    fun <T : Settings.NameValueTable> getSettings(
        subSettingsKClass: KClass<T>
    ): List<MyModel> {
        // Both queries are plain blocking calls that never cross a suspension point, so
        // CancellationException cannot arise here -- no guard-style cancellation rethrow.
        val keys = try {
            settingsDataSource.getSettingsOrThrow(subSettingsKClass)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            return listOf(toDetectFailedMyModel("Settings.${subSettingsKClass.simpleName}", e))
        }

        return keys.map { settingKey ->
            val title = "${subSettingsKClass.qualifiedName}.$settingKey"
            try {
                val value = settingsDataSource.getStringOrNullOrThrow(
                    subSettingsKClass, MyApplication.instance.contentResolver, settingKey
                )
                toTranslatedDetailMyModel(title, value)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                toDetectFailedMyModel(title, e)
            }
        }
    }

    fun getBuildProp(): List<MyModel> {
        // issues-cn #63 standalone subset: a failed getprop no longer folds into the success
        // path as an empty table -- one source-level row instead, carrying exitCode so the
        // failure is attributable. Parsing is pure string work: no try/catch that could never fire.
        val result = propertiesDataSource.getBuildProp()
        if (!result.isSuccess) {
            return listOf(
                toDetectFailedMyModel("getprop", RuntimeException("exitCode=${result.exitCode}"))
            )
        }

        // Fold the output into complete entries first (single-line "[k]: [v]" or accumulated
        // multi-line ones), then parse each entry.
        val rawEntries = mutableListOf<String>()
        var accumulated = ""
        result.output.forEach { line ->
            if (line.startsWith("[") && line.endsWith("]")) {
                rawEntries += line
            } else {
                accumulated += "$line\n"

                if (line.endsWith("]")) {
                    rawEntries += accumulated

                    accumulated = ""
                }
            }
        }

        return rawEntries.map { rawEntry ->
            toPropMyModel(rawProp = rawEntry)
        }
    }

    // Failure rows keep the plain no-color row shape: the red/yellow/green dot is a
    // Home-only concept, so this is deliberately not toErrorMyModel.
    private fun toDetectFailedMyModel(title: String, e: Exception): MyModel =
        toTranslatedDetailMyModel(
            title,
            MyApplication.getMyString(R.string.result_detect_failed, e.fullMessage)
        )
}