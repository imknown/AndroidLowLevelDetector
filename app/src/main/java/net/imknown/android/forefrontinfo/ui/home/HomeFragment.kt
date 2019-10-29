package net.imknown.android.forefrontinfo.ui.home

import android.os.Build
import androidx.annotation.ColorInt
import com.topjohnwu.superuser.Shell
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_CRITICAL
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_NO_PROBLEM
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_WARNING
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseListFragment

/**
 * Thanks to:
 *
 * https://github.com/topjohnwu/Magisk/blob/master/scripts/util_functions.sh
 * https://github.com/opengapps/opengapps/blob/master/scripts/inc.installer.sh
 * https://github.com/penn5/TrebleCheck
 *
 */
class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()

        init {
            // Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
            Shell.Config.setFlags(Shell.FLAG_NON_ROOT_SHELL)
            Shell.Config.verboseLogging(BuildConfig.DEBUG)
            // Shell.Config.setTimeout(10)
        }

        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed*/ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        // private const val CMD_ROM_TOTAL_SIZE = "df | grep -v '/apex' | grep -v '/storage' | grep -E 'tmpfs'\\|'/dev'\\|'/data' | awk '{s+=\$2} END {print s/1000000}'"
        private const val CMD_AB_UPDATE = "getprop ro.build.ab_update"
        private const val CMD_SLOT_SUFFIX = "getprop ro.boot.slot_suffix"

        // https://source.android.com/devices/architecture/?hl=en#hidl
        private const val CMD_TREBLE_ENABLED = "getprop ro.treble.enabled"

        // https://source.android.com/devices/architecture/vndk?hl=en
        private const val CMD_VNDK_LITE = "getprop ro.vndk.lite"
        private const val CMD_VNDK_VERSION = "getprop ro.vndk.version"

        // https://source.android.com/devices/bootloader/system-as-root?hl=en
        // https://github.com/topjohnwu/magisk_files/blob/2d7ddefbe4946806de1875a18247b724f5e7d4a0/notes.md
        // https://github.com/topjohnwu/Magisk/blob/master/scripts/util_functions.sh#L193
        // https://github.com/opengapps/opengapps/blob/master/scripts/inc.installer.sh#L710
        private const val CMD_SYSTEM_ROOT_IMAGE = "getprop ro.build.system_root_image"
        private const val CMD_DEV_ROOT = "grep '/dev/root / ' /proc/mounts"
        private const val CMD_SYSTEM =
            "grep ' /system ' /proc/mounts | grep -v 'tmpfs' | grep -v 'none'"

        // https://source.android.com/devices/tech/ota/apex?hl=en
        private const val CMD_APEX_MOUNT = "grep 'tmpfs /apex tmpfs' /proc/mounts"
        private const val CMD_APEX_TZDATA = "grep '/apex/com.android.tzdata' /proc/mounts"
    }

    override fun collectionDataset() {
        // region [Android]
        val androidColor = when {
            isLatestStableAndroid() -> COLOR_STATE_LIST_NO_PROBLEM
            isSupportedByUpstream() -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            MyModel(
                getResultString(
                    R.string.android_info,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT
                ), androidColor
            )
        )

//        if (isAtLeastAndroid6()) {
//            add(
//                MyModel(
//                    getResultString(
//                        R.string.build_preview_sdk_int,
//                        Build.VERSION.PREVIEW_SDK_INT.toString()
//                    )
//                )
//            )
//        }
        // endregion [Android]

        // region [A/B]
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]

        // val romTotalSizeResult = sh(CMD_ROM_TOTAL_SIZE)
        // val romTotalSize = kotlin.math.floor(romTotalSizeResult[0].toFloat()).toString()

        val abUpdateSupportedResult = filterVersion(isAtLeastAndroid7(), CMD_AB_UPDATE)
        val isAbUpdateSupported = isResultTrue(abUpdateSupportedResult)
        val abUpdateSupportedArgs =
            translate(isAbUpdateSupported) /* + getString(R.string.rom_total_size_result, romTotalSize) */

        var abFinalResult =
            getString(R.string.ab_seamless_update_enabled_result, abUpdateSupportedArgs)
        if (isAbUpdateSupported) {
            val slotSuffixResult = sh(CMD_SLOT_SUFFIX)
            val hasVndkVersion = hasResult(slotSuffixResult)
            val slotSuffixUsing = if (hasVndkVersion) {
                slotSuffixResult[0]
            } else {
                getString(android.R.string.unknownName)
            }

            abFinalResult += getString(R.string.current_using_ab_slot_result, slotSuffixUsing)
        }

        add(MyModel(abFinalResult, isAbUpdateSupported))
        // endregion [A/B]

        // region [Treble]
        val trebleEnabledResult = filterVersion(isAtLeastAndroid8(), CMD_TREBLE_ENABLED)
        val isTrebleEnabled = isResultTrue(trebleEnabledResult)

        add(
            MyModel(
                getString(R.string.treble_enabled_result, translate(isTrebleEnabled)),
                isTrebleEnabled
            )
        )
        // endregion [Treble]

        // region [VNDK]
        val hasVndkLiteResult = filterVersion(isAtLeastAndroid8(), CMD_VNDK_LITE)
        val hasVndkLite = isResultTrue(hasVndkLiteResult)

        val vndkVersionResult = filterVersion(isAtLeastAndroid8(), CMD_VNDK_VERSION)
        val hasVndkVersion = hasResult(vndkVersionResult)

        val isVndkBuiltIn = hasVndkLite || hasVndkVersion

        @ColorInt val vndkColor: Int

        var isVndkBuiltInResult = translate(isVndkBuiltIn)
        if (isVndkBuiltIn) {
            val vndkVersion = if (hasVndkVersion) {
                vndkVersionResult[0]
            } else {
                getString(android.R.string.unknownName)
            }

            if (vndkVersion == Build.VERSION_CODES.Q.toString()) {
                vndkColor = COLOR_STATE_LIST_NO_PROBLEM
            } else {
                vndkColor = COLOR_STATE_LIST_WARNING
            }

            isVndkBuiltInResult += getString(R.string.built_in_vndk_version_result, vndkVersion)
        } else {
            vndkColor = COLOR_STATE_LIST_CRITICAL
        }

        add(MyModel(getString(R.string.vndk_built_in_result, isVndkBuiltInResult), vndkColor))
        // endregion [VNDK]

        // region [SAR]
        val systemRootImageResult = filterVersion(isAtLeastAndroid9(), CMD_SYSTEM_ROOT_IMAGE)
        val hasSystemRootImage = isResultTrue(systemRootImageResult)

        val devRootResult = filterVersion(isAtLeastAndroid9(), CMD_DEV_ROOT)
        val hasDevRoot = hasResult(devRootResult)

        val systemResult = filterVersion(isAtLeastAndroid9() && !hasSystemRootImage, CMD_SYSTEM)
        val isSystem = hasResult(systemResult)

        val isSar = isAtLeastAndroid9() && (hasSystemRootImage || hasDevRoot || !isSystem)
        add(MyModel(getString(R.string.sar_enabled_result, translate(isSar)), isSar))
        // endregion [SAR]

        // region [APEX]
        val apexMountedResult = filterVersion(isAtLeastAndroid10(), CMD_APEX_MOUNT)
        val isApexMounted = hasResult(apexMountedResult)

        val apexUsedResult = filterVersion(isAtLeastAndroid10(), CMD_APEX_TZDATA)
        val isApexUsed = hasResult(apexUsedResult)
        val isApex = isApexMounted && isApexUsed
        add(MyModel(getString(R.string.apex_enabled_result, translate(isApex)), isApex))
        // endregion [APEX]
    }

    private fun filterVersion(condition: Boolean, cmd: String) =
        if (condition) {
            sh(cmd)
        } else {
            emptyList()
        }

    private fun sh(cmd: String) = Shell.sh(cmd).exec().out

    private fun hasResult(result: List<String>) = result.isNotEmpty() && result[0].isNotEmpty()

    private fun isResultTrue(result: List<String>) = result.isNotEmpty() && result[0].toBoolean()

    private fun translate(condition: Boolean) = getString(
        if (condition) {
            R.string.result_yes
        } else {
            R.string.result_no
        }
    )
}
