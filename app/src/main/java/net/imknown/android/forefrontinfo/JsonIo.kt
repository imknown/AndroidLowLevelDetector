package net.imknown.android.forefrontinfo

import android.content.res.AssetManager
import com.google.gson.Gson
import net.imknown.android.forefrontinfo.ui.home.Lld
import java.io.File

class JsonIo {
    companion object {
        /**
         * https://discuss.kotlinlang.org/t/copy-file-from-res/7068/10
         */
        fun copyJsonFromAssetsToContextFilesDir(
            assets: AssetManager,
            savedFile: File,
            assetName: String
        ) {
            assets.open(assetName).use { inStream ->
                savedFile.outputStream().use { outStream ->
                    outStream.let {
                        inStream.copyTo(it)
                    }
                }
            }
        }
    }
}

fun File.getLld(): Lld? = Gson().fromJson(readText(), Lld::class.java)