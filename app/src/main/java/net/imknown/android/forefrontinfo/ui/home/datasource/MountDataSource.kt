package net.imknown.android.forefrontinfo.ui.home.datasource

import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.base.shell.IShell

@Inject
class MountDataSource(private val shell: IShell) {
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

    // issues-cn #63: a failed mount read no longer folds into the success path as an empty
    // table. The result type makes the failure explicit: the two consuming entries (SAR and
    // APEX) each render their own unknown row, carrying exitCode.
    sealed interface MountQueryResult {
        data class Success(val mounts: List<Mount>) : MountQueryResult
        data class Failed(val exitCode: Int?) : MountQueryResult
    }

    fun getMounts(): MountQueryResult {
        val result = shell.execute(CMD_MOUNT)
        if (!result.isSuccess) {
            return MountQueryResult.Failed(result.exitCode)
        }

        val mounts = mutableListOf<Mount>()

        result.output.forEach {
            val columns = it.split(" ")
            if (columns.size == 6) {
                val mount = Mount(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5])
                mounts.add(mount)
            }
        }

        return MountQueryResult.Success(mounts)
    }
}