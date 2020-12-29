package net.imknown.android.forefrontinfo.base

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter

object JsonIo {
    const val LLD_JSON_NAME = "lld.json"

    val savedLldJsonFile by lazy {
        File(MyApplication.getDownloadDir(), LLD_JSON_NAME)
    }

    private fun deleteDirtyDirectory() {
        if (!savedLldJsonFile.deleteRecursively()) {
            Log.e(javaClass.simpleName, "Delete dirty directory failed.")
        } else {
            Log.i(javaClass.simpleName, "Dirty directory deleted.")
        }
    }

    fun copyJsonIfNeeded() {
        var shouldCopy = false

        if (savedLldJsonFile.exists()) {
            if (savedLldJsonFile.isDirectory) {
                deleteDirtyDirectory()
                shouldCopy = true
            } else {
                val savedLldVersion = try {
                    savedLldJsonFile.fromJson<Lld>().version
                } catch (e: Exception) {
                    val message =
                        MyApplication.getMyString(R.string.lld_json_parse_failed, e.message)
                    Log.e(javaClass.simpleName, message, e)
                    null
                }

                if (savedLldVersion == null) {
                    shouldCopy = true
                } else {
                    val assetLldVersion = getAssetLldVersion(MyApplication.instance.assets)
                    if (assetLldVersion != null && savedLldVersion < assetLldVersion) {
                        shouldCopy = true
                    }
                }
            }
        } else {
            shouldCopy = true
        }

        if (!shouldCopy) {
            return
        }

        copyAssetsFileToContextFilesDir(
            MyApplication.instance.assets,
            savedLldJsonFile,
            LLD_JSON_NAME
        )
    }

    /**
     * https://discuss.kotlinlang.org/t/copy-file-from-res/7068/10
     */
    private fun copyAssetsFileToContextFilesDir(
        assets: AssetManager,
        savedFile: File,
        assetName: String
    ) {
        assets.open(assetName).use { inStream ->
            savedFile.parentFile?.mkdirs()

            savedFile.outputStream().use { outStream ->
                outStream.let {
                    inStream.copyTo(it)
                }
            }
        }
    }

    fun saveLldJsonFile(lldString: String) {
        if (savedLldJsonFile.exists() && savedLldJsonFile.isDirectory) {
            deleteDirtyDirectory()
        }

        FileWriter(savedLldJsonFile).use {
            it.write(lldString)
        }
    }

    fun getAssetLld(assets: AssetManager): Lld? =
        try {
            assets.open(LLD_JSON_NAME)
                .bufferedReader()
                .use(BufferedReader::readText)
                .fromJson()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun getAssetLldVersion(assets: AssetManager): String? = getAssetLld(assets)?.version
}

@Throws
inline fun <reified T : Any> File.fromJson(): T =
    readText().fromJson()

@Throws
inline fun <reified T : Any> String.fromJson(): T =
    Json { ignoreUnknownKeys = true }.decodeFromString(this)