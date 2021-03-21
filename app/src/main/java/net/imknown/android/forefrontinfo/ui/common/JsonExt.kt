package net.imknown.android.forefrontinfo.ui.common

import kotlinx.serialization.json.Json
import java.io.File

val json by lazy { Json { ignoreUnknownKeys = true } }

inline fun <reified T : Any> File.toObjectOrThrow(): T  =
    readText().toObjectOrThrow()

inline fun <reified T : Any> String.toObjectOrThrow(): T =
    json.decodeFromString(this)