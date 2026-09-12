package net.imknown.android.forefrontinfo.ui.prop.datasource

import net.imknown.android.forefrontinfo.ui.common.getShellResult
import java.util.Properties

class PropertiesDataSource {
    companion object {
        private const val CMD_GETPROP = "getprop"

        const val UNIX_LIKE_NEWLINE_ORIGIN = "\\n"
    }

    fun getSystemPropOrThrow(): List<Pair<Any?, Any?>> {
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

    fun getBuildProp() = getShellResult(CMD_GETPROP)
}