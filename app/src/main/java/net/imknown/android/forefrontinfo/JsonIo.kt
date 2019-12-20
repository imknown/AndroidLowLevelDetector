package net.imknown.android.forefrontinfo

import android.content.res.AssetManager
import com.google.gson.Gson
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import java.io.BufferedReader
import java.io.File

class JsonIo {
    companion object {
        /**
         * https://www.programiz.com/kotlin-programming/examples/string-date
         */
        fun whetherNeedCopyAssets(assets: AssetManager): Boolean {
            if (GatewayApi.savedLldJsonFile.exists()) {
                val savedLldVersion = GatewayApi.savedLldJsonFile.fromJson<Lld>().version
                val assetLldVersion = getAssetLldVersion(assets)

                if (savedLldVersion >= assetLldVersion) {
                    return false
                }
            }

            return true
        }

        /**
         * https://discuss.kotlinlang.org/t/copy-file-from-res/7068/10
         */
        fun copyJsonFromAssetsToContextFilesDir(
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

        fun getAssetLldVersion(assets: AssetManager) =
            assets.open(GatewayApi.LLD_JSON_NAME)
                .bufferedReader()
                .use(BufferedReader::readText)
                .fromJson<Lld>().version
    }
}

inline fun <reified T : Any> File.fromJson(): T =
    Gson().fromJson(readText(), T::class.java)

inline fun <reified T : Any> String.fromJson(): T =
    Gson().fromJson(this, T::class.java)