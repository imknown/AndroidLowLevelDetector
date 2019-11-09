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
            if (GatewayApi.savedFile.exists()) {
                val savedLldVersion = GatewayApi.savedFile
                    .getLld()?.version ?: ""
                val assetLldVersion =
                    assets.open(GatewayApi.LLD_JSON_NAME)
                        .bufferedReader()
                        .use(BufferedReader::readText)
                        .getLld()?.version ?: ""

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
    }
}

internal fun File.getLld(): Lld? = Gson().fromJson(readText(), Lld::class.java)
internal fun String.getLld(): Lld? = Gson().fromJson(this, Lld::class.java)