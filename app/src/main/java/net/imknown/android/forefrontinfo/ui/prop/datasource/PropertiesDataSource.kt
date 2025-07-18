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
        val defaultsProperties = Properties::class.java
            .getDeclaredField("defaults")
            .also { it.isAccessible = true }
            .get(systemProperties) as Properties

        return (defaultsProperties + systemProperties)
            .toList()
            .sortedBy { it.first.toString() }
    }

    fun getBuildProp() = getShellResult(CMD_GETPROP)
}