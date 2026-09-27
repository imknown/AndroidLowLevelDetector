package net.imknown.android.forefrontinfo.ui.home.datasource

import net.imknown.android.forefrontinfo.ui.common.getShellResult

class MountDataSource {
    companion object {
        private const val CMD_MOUNT = "cat /proc/mounts"
    }

    // https://unix.stackexchange.com/questions/91960/can-anyone-explain-the-output-of-mount
    data class Mount(
        val blockDevice: String,
        val mountPoint: String,
        val type: String,
        val mountOptions: String,
        // Kept as the raw text from /proc/mounts: nothing consumes them, and parsing them as
        // numbers used to be a throw path on malformed lines.
        val dummy0: String,
        val dummy1: String
    )

    fun getMounts(): List<Mount> {
        val mounts = mutableListOf<Mount>()

        getShellResult(CMD_MOUNT).output.forEach {
            val columns = it.split(" ")
            if (columns.size == 6) {
                val mount = Mount(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5])
                mounts.add(mount)
            }
        }

        return mounts
    }
}