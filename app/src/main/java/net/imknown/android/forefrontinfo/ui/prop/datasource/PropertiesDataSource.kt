package net.imknown.android.forefrontinfo.ui.prop.datasource

import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.base.shell.IShell
import java.util.Properties

@Inject
class PropertiesDataSource(private val shell: IShell) {
    companion object {
        private const val CMD_GETPROP = "getprop"

        const val UNIX_LIKE_NEWLINE_ORIGIN = "\\n"
    }

    fun getSystemProp(): List<Pair<Any?, Any?>> {
        val systemProperties = System.getProperties()
        val defaultsProperties = try {
            Properties::class.java
                .getDeclaredField("defaults")
                .also { it.isAccessible = true }
                .get(systemProperties) as? Properties
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val merged = if (defaultsProperties != null) {
            defaultsProperties + systemProperties
        } else {
            systemProperties
        }

        return merged
            .toList()
            .sortedBy { it.first.toString() }
    }

    fun getBuildProp() = shell.execute(CMD_GETPROP)
}