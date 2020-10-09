package net.imknown.android.forefrontinfo.base

import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter

object JsonIo {
    const val LLD_JSON_NAME = "lld.json"

    val savedLldJsonFile by lazy {
        File(MyApplication.getDownloadDir(), LLD_JSON_NAME)
    }

    fun copyJsonIfNeeded() {
        if (!savedLldJsonFile.exists()) {
            copyAssetsFileToContextFilesDir(
                MyApplication.instance.assets,
                savedLldJsonFile,
                LLD_JSON_NAME
            )
        }
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
        FileWriter(savedLldJsonFile).use {
            it.write(lldString)
        }
    }

    fun getAssetLld(assets: AssetManager) =
        try {
            assets.open(LLD_JSON_NAME)
                .bufferedReader()
                .use(BufferedReader::readText)
                .fromJson<Lld>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun getAssetLldVersion(assets: AssetManager) =
        getAssetLld(assets)?.version ?: 0L.formatToLocalZonedDatetimeString()
}

inline fun <reified T : Any> File.fromJson(): T? =
    Gson().fromJson(readText(), T::class.java)

inline fun <reified T : Any> String.fromJson(): T? =
    Gson().fromJson(this, T::class.java)