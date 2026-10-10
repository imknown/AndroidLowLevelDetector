package net.imknown.android.forefrontinfo.ui.home.datasource

import android.content.res.AssetManager
import android.util.Log
import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter

// File orchestration for the bundled lld.json (copy / save / asset read):
// replaces the deleted static object singleton as an @Inject graph-constructed
// class. The savedLldJsonFileOrThrow path keeps the original by lazy timing
// (resolved on first use, keeping disk work off the main thread) and reads
// through the Application bound by ST-08's graph factory instead of a static
// slot (the companion action of issues-cn #02). LLD_JSON_NAME moved here from
// LldDataSource, making LldDataSource -> LldFileStore a one-way dependency.
// The reference cycle is gone. Methods are line-identical to the original
// (move only, no semantic change); the internal MyApplication.instance.assets
// / getMyString statics stay as is (deeper orchestration refactoring belongs
// to issues-cn #11, getMyString to #01).
@Inject
class LldFileStore(private val application: MyApplication) {
    companion object {
        const val LLD_JSON_NAME = "lld.json"
    }

    val savedLldJsonFileOrThrow: File by lazy { File(application.getDownloadDir(), LLD_JSON_NAME) }

    private fun deleteDirtyDirectoryOrThrow() {
        if (!savedLldJsonFileOrThrow.deleteRecursively()) {
            Log.e(javaClass.simpleName, "Delete dirty directory failed.")
        } else {
            Log.i(javaClass.simpleName, "Dirty directory deleted.")
        }
    }

    fun copyJsonIfNeededOrThrow() {
        var shouldCopy = false

        if (savedLldJsonFileOrThrow.exists()) {
            if (savedLldJsonFileOrThrow.isDirectory) {
                deleteDirtyDirectoryOrThrow()
                shouldCopy = true
            } else {
                val savedLldVersion = try {
                    savedLldJsonFileOrThrow.toObjectOrThrow<Lld>().version
                } catch (e: Exception) {
                    val message = MyApplication.getMyString(R.string.lld_json_parse_failed, e.fullMessage)
                    Log.e(javaClass.simpleName, message)
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

        copyAssetsFileToContextFilesDirOrThrow(
            MyApplication.instance.assets,
            savedLldJsonFileOrThrow,
            LLD_JSON_NAME
        )
    }

    /**
     * https://discuss.kotlinlang.org/t/copy-file-from-res/7068/10
     */
    private fun copyAssetsFileToContextFilesDirOrThrow(
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

    fun saveLldJsonFileOrThrow(lldString: String) {
        if (savedLldJsonFileOrThrow.exists() && savedLldJsonFileOrThrow.isDirectory) {
            deleteDirtyDirectoryOrThrow()
        }

        FileWriter(savedLldJsonFileOrThrow).use {
            it.write(lldString)
        }
    }

    fun getAssetLld(assets: AssetManager): Lld? =
        try {
            assets.open(LLD_JSON_NAME)
                .bufferedReader()
                .use(BufferedReader::readText)
                .toObjectOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun getAssetLldVersion(assets: AssetManager): String? = getAssetLld(assets)?.version
}
