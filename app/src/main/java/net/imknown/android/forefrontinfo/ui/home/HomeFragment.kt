package net.imknown.android.forefrontinfo.ui.home

import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.g00fy2.versioncompare.Version
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_CRITICAL
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_NO_PROBLEM
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_WARNING
import net.imknown.android.forefrontinfo.base.BaseListFragment
import net.imknown.android.forefrontinfo.ui.home.model.Lld

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

        private const val CMD_TOYBOX_VERSION = "toybox --version"

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
        private const val CMD_MOUNT_DEV_ROOT = "grep '/dev/root / ' /proc/mounts"
        private const val CMD_MOUNT_SYSTEM =
            "grep ' /system ' /proc/mounts | grep -v 'tmpfs' | grep -v 'none'"

        // https://source.android.com/devices/tech/ota/apex?hl=en
        private const val CMD_APEX_UPDATABLE = "getprop ro.apex.updatable"
        private const val CMD_FLATTENED_APEX_MOUNT = "grep 'tmpfs /apex tmpfs' /proc/mounts"
    }

    private fun copyJsonIfNeeded() {
        if (JsonIo.whetherNeedCopyAssets(context?.assets!!)) {
            JsonIo.copyJsonFromAssetsToContextFilesDir(
                context?.assets!!,
                GatewayApi.savedFile,
                GatewayApi.LLD_JSON_NAME
            )
        }
    }

    override suspend fun collectionDataset() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val allowNetwork = sharedPreferences.getBoolean(
            getString(R.string.network_allow_network_data_key), false
        )

        if (allowNetwork) {
            prepareResult(GatewayApi.downloadLldJsonFile())
        } else {
            prepareResult(false)
        }
    }

    private suspend fun prepareResult(isOnline: Boolean) {
        if (!isOnline) {
            copyJsonIfNeeded()
        }

        if (isActivityAndFragmentOk()) {
            initSubtitle(isOnline)
        }
    }

    private suspend fun initSubtitle(isOnline: Boolean) {
        @StringRes var lldDataModeResId: Int
        var dataVersion: String

        try {
            val lld = GatewayApi.savedFile.fromJson<Lld>()
            dataVersion = lld.version
            fillDataset(lld)

            showResult()

            lldDataModeResId = if (isOnline) {
                R.string.lld_json_online
            } else {
                R.string.lld_json_offline
            }
        } catch (e: Exception) {
            e.printStackTrace()

            lldDataModeResId = R.string.lld_json_offline
            dataVersion = getString(android.R.string.unknownName)
        }

        if (isActivityAndFragmentOk()) {
            withContext(Dispatchers.Main) {
                val actionBar = (activity as AppCompatActivity).supportActionBar!!
                actionBar.subtitle = getString(lldDataModeResId, dataVersion)
            }
        }
    }

    private fun fillDataset(lld: Lld) {
        createNewTempDataset()

        // region [Android]
        @ColorInt val androidColor = when {
            isLatestStableAndroid(lld) -> COLOR_STATE_LIST_NO_PROBLEM
            isSupportedByUpstream(lld) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            getString(R.string.android_info, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
            androidColor
        )

//        if (isAtLeastAndroid6()) {
//            add(
//                getString(R.string.build_preview_sdk_int, Build.VERSION.PREVIEW_SDK_INT.toString())
//            )
//        }
        // endregion [Android]

        // region [Kernel]
        val linuxVersion = System.getProperty("os.version")
        val isAtLeast = Version(linuxVersion).isAtLeast(lld.linux.stable.version)
        val isSupported = Version(linuxVersion).isAtLeast(lld.linux.support.version)
        @ColorInt val linuxColor = when {
            isAtLeast -> COLOR_STATE_LIST_NO_PROBLEM
            isSupported -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }
        add(getString(R.string.linux_version, linuxVersion), linuxColor)
        // endregion [Kernel]

        // region [A/B]
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]

        // val romTotalSizeResult = sh(CMD_ROM_TOTAL_SIZE)
        // val romTotalSize = kotlin.math.floor(romTotalSizeResult[0].toFloat()).toString()

        val abUpdateSupportedResult = sh(isAtLeastAndroid7(), CMD_AB_UPDATE)
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

        add(abFinalResult, isAbUpdateSupported)
        // endregion [A/B]

        // region [Treble]
        val trebleEnabledResult = sh(isAtLeastAndroid8(), CMD_TREBLE_ENABLED)
        val isTrebleEnabled = isResultTrue(trebleEnabledResult)

        add(getString(R.string.treble_enabled_result, translate(isTrebleEnabled)), isTrebleEnabled)
        // endregion [Treble]

        // region [VNDK]
        val hasVndkLiteResult = sh(isAtLeastAndroid8(), CMD_VNDK_LITE)
        val hasVndkLite = isResultTrue(hasVndkLiteResult)

        val vndkVersionResult = sh(isAtLeastAndroid8(), CMD_VNDK_VERSION)
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

            vndkColor = if (vndkVersion == lld.android.stable.api) {
                COLOR_STATE_LIST_NO_PROBLEM
            } else {
                COLOR_STATE_LIST_WARNING
            }

            isVndkBuiltInResult += getString(R.string.built_in_vndk_version_result, vndkVersion)
        } else {
            vndkColor = COLOR_STATE_LIST_CRITICAL
        }

        add(getString(R.string.vndk_built_in_result, isVndkBuiltInResult), vndkColor)
        // endregion [VNDK]

        // region [SAR]
        val systemRootImageResult = sh(isAtLeastAndroid9(), CMD_SYSTEM_ROOT_IMAGE)
        val hasSystemRootImage = isResultTrue(systemRootImageResult)

        val mountDevRootResult = sh(isAtLeastAndroid9(), CMD_MOUNT_DEV_ROOT)
        val hasMountDevRoot = hasResult(mountDevRootResult)

        val mountSystemResult = sh(isAtLeastAndroid9() && !hasSystemRootImage, CMD_MOUNT_SYSTEM)
        val hasMountSystem = hasResult(mountSystemResult)

        val isSar =
            isAtLeastAndroid9() && (hasSystemRootImage || hasMountDevRoot || !hasMountSystem)
        add(getString(R.string.sar_enabled_result, translate(isSar)), isSar)
        // endregion [SAR]

        // region [APEX]
        val apexUpdatableResult = sh(isAtLeastAndroid10(), CMD_APEX_UPDATABLE)
        val apexUpdatable = isResultTrue(apexUpdatableResult)

        val flattenedApexMountedResult = sh(isAtLeastAndroid10(), CMD_FLATTENED_APEX_MOUNT)
        val isFlattenedApexMounted = hasResult(flattenedApexMountedResult)

        val isApex = apexUpdatable || isFlattenedApexMounted
        val isLegacyFlattenedApex = !apexUpdatable && isFlattenedApexMounted

        var apexEnabledResult = translate(isApex)
        if (isLegacyFlattenedApex) {
            apexEnabledResult += getString(R.string.apex_legacy_flattened)
        }

        val apexColor = when {
            apexUpdatable -> COLOR_STATE_LIST_NO_PROBLEM
            isLegacyFlattenedApex -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(getString(R.string.apex_enabled_result, apexEnabledResult), apexColor)
        // endregion [APEX]

        // region [ToyBox]
        val toyboxVersionResult = sh(isAtLeastAndroid6(), CMD_TOYBOX_VERSION)
        val hasToyboxVersion = hasResult(toyboxVersionResult)
        var hasToyboxResult = translate(hasToyboxVersion)

        @ColorInt val toyboxColor = if (hasToyboxVersion) {
            val toyboxVersion = toyboxVersionResult[0]
            hasToyboxResult += getString(R.string.built_in_toybox_version_result, toyboxVersion)

            val toyboxRealVersion = toyboxVersion
                .replace("toybox ", "")
                .split("-")
            if (toyboxRealVersion.isNotEmpty()) {
                if (Version(toyboxRealVersion[0]).isAtLeast(lld.toybox.stable.version)) {
                    COLOR_STATE_LIST_NO_PROBLEM
                } else {
                    COLOR_STATE_LIST_WARNING
                }
            } else {
                COLOR_STATE_LIST_CRITICAL
            }
        } else {
            COLOR_STATE_LIST_CRITICAL
        }
        add(getString(R.string.toybox_built_in_result, hasToyboxResult), toyboxColor)
        // endregion [ToyBox]
    }

    private fun sh(condition: Boolean, cmd: String) =
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
