package net.imknown.android.forefrontinfo.ui.home

import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.webkit.WebViewCompat
import com.g00fy2.versioncompare.Version
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_CRITICAL
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_NO_PROBLEM
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_WARNING
import net.imknown.android.forefrontinfo.base.BaseListFragment
import net.imknown.android.forefrontinfo.ui.home.model.Lld

class HomeFragment : BaseListFragment() {

    companion object {
        fun newInstance() = HomeFragment()

        init {
            // Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
            Shell.Config.setFlags(Shell.FLAG_NON_ROOT_SHELL)
            Shell.Config.verboseLogging(BuildConfig.DEBUG)
            // Shell.Config.setTimeout(10)
        }

        // https://source.android.com/setup/start/build-numbers
        // https://source.android.com/security/enhancements/enhancements9
        // https://source.android.com/setup/start/p-release-notes
        // https://developer.android.com/about/versions/10
        //
        // https://www.android.com
        // https://source.android.com/security/bulletin
        // https://ci.android.com
        // https://developer.android.com/preview/overview
        //
        // https://en.wikipedia.org/wiki/Android_version_history
        // https://developer.android.com/about/dashboards/?hl=en
        // https://www.bidouille.org/misc/androidcharts
        //
        // https://mta.qq.com/mta/data/device/os
        // https://compass.umeng.com/#hardwareList
        // https://tongji.baidu.com/research/app
        private val BUILD_VERSION_RELEASE = Build.VERSION.RELEASE
        private val BUILD_VERSION_SDK_INT = Build.VERSION.SDK_INT

        // https://source.android.com/security/bulletin
        private val BUILD_VERSION_SECURITY_PATCH by lazy {
            // Suppress because of lazy already
            @Suppress Build.VERSION.SECURITY_PATCH
        }
        private const val PROP_SECURITY_PATCH = "ro.build.version.security_patch"

        // https://android.googlesource.com/kernel/common/+refs
        // https://source.android.com/setup/build/building-kernels#downloading
        private val SYSTEM_PROPERTY_LINUX_VERSION = System.getProperty("os.version")

        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed */ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        // private const val CMD_ROM_TOTAL_SIZE = "df | grep -v '/apex' | grep -v '/storage' | grep -E 'tmpfs'\\|'/dev'\\|'/data' | awk '{s+=\$2} END {print s/1000000}'"
        private const val PROP_AB_UPDATE = "ro.build.ab_update"
        private const val PROP_SLOT_SUFFIX = "ro.boot.slot_suffix"

        // https://source.android.com/devices/architecture/?hl=en#hidl
        private const val PROP_TREBLE_ENABLED = "ro.treble.enabled"

        // https://source.android.com/devices/architecture/vndk?hl=en
        private const val PROP_VNDK_LITE = "ro.vndk.lite"
        private const val PROP_VNDK_VERSION = "ro.vndk.version"

        // https://source.android.com/devices/bootloader/system-as-root?hl=en
        // https://github.com/topjohnwu/Magisk/blob/master/scripts/util_functions.sh#L193
        // https://github.com/opengapps/opengapps/blob/master/scripts/inc.installer.sh#L710
        // https://github.com/penn5/TrebleCheck/blob/master/app/src/main/java/tk/hack5/treblecheck/MountDetector.kt
        private const val PROP_SYSTEM_ROOT_IMAGE = "ro.build.system_root_image"
        private const val CMD_MOUNT_DEV_ROOT = "grep '/dev/root / ' /proc/mounts"
        private const val CMD_MOUNT_SYSTEM =
            "grep ' /system ' /proc/mounts | grep -v 'tmpfs' | grep -v 'none'"

        // https://source.android.com/devices/tech/ota/apex?hl=en
        private const val PROP_APEX_UPDATABLE = "ro.apex.updatable"
        private const val CMD_FLATTENED_APEX_MOUNT = "grep 'tmpfs /apex tmpfs' /proc/mounts"

        // https://github.com/landley/toybox
        // https://android.googlesource.com/platform/external/toybox/+refs
        // https://chromium.googlesource.com/aosp/platform/system/core/+/upstream/shell_and_utilities/
        private const val CMD_TOYBOX_VERSION = "toybox --version"

        // https://www.chromium.org/developers/calendar
        //
        // https://en.wikipedia.org/wiki/Google_Chrome_version_history
        // https://en.wikipedia.org/wiki/WebKit
        // https://en.wikipedia.org/wiki/Chromium_(web_browser)
        private const val WEB_VIEW_BUILT_IN_PACKAGE_NAME = "com.android.webview"
        private const val WEB_VIEW_STABLE_PACKAGE_NAME = "com.google.android.webview"
        private const val WEB_VIEW_BETA_PACKAGE_NAME = "com.google.android.webview.beta"
        private const val WEB_VIEW_DEV_IN_PACKAGE_NAME = "com.google.android.webview.dev"
        private const val WEB_VIEW_CANARY_PACKAGE_NAME = "com.google.android.webview.canary"
        private const val CHROME_STABLE_PACKAGE_NAME = "com.android.chrome"
        private const val CHROME_BETA_PACKAGE_NAME = "com.chrome.beta"
        private const val CHROME_DEV_IN_PACKAGE_NAME = "com.chrome.dev"
        private const val CHROME_CANARY_PACKAGE_NAME = "com.chrome.canary"
    }

    private fun copyJsonIfNeeded() {
        if (JsonIo.whetherNeedCopyAssets(MyApplication.instance.assets)) {
            JsonIo.copyJsonFromAssetsToContextFilesDir(
                MyApplication.instance.assets,
                GatewayApi.savedLldJsonFile,
                GatewayApi.LLD_JSON_NAME
            )
        }
    }

    override suspend fun collectionDataset() {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(MyApplication.instance)
        val allowNetwork = sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.network_allow_network_data_key), false
        )

        if (allowNetwork) {
            GatewayApi.downloadLldJsonFile({
                launch(Dispatchers.IO) {
                    prepareResult(true)
                }
            }, {
                launch(Dispatchers.IO) {
                    showError(it)

                    toast(R.string.lld_json_download_failed)

                    prepareResult(false)
                }
            })
        } else {
            prepareResult(false)
        }
    }

    private suspend fun prepareResult(isOnline: Boolean) {
        if (!isActivityAndFragmentOk(this)) {
            return
        }

        if (!isOnline) {
            try {
                copyJsonIfNeeded()
            } catch (e: Exception) {
                showError(e)
            }
        }

        if (isActivityAndFragmentOk(this)) {
            initSubtitle(isOnline)
        }
    }

    private suspend fun initSubtitle(isOnline: Boolean) {
        @StringRes var lldDataModeResId: Int
        var dataVersion: String

        try {
            val lld = GatewayApi.savedLldJsonFile.fromJson<Lld>()
            dataVersion = lld.version
            fillDataset(lld)

            if (isActivityAndFragmentOk(this)) {
                showResult()
            }

            lldDataModeResId = if (isOnline) {
                R.string.lld_json_online
            } else {
                R.string.lld_json_offline
            }
        } catch (e: Exception) {
            showError(e)

            lldDataModeResId = R.string.lld_json_offline

            dataVersion = MyApplication.getMyString(android.R.string.unknownName)
        }

        withContext(Dispatchers.Main) {
            if (isActivityAndFragmentOk(this@HomeFragment)) {
                val actionBar = (activity as AppCompatActivity).supportActionBar
                actionBar?.subtitle = MyApplication.getMyString(lldDataModeResId, dataVersion)
            }
        }
    }

    private fun getSecurityPatchYearMonth(securityPatch: String) =
        securityPatch.substringBeforeLast('-')

    private fun getPackageInfo(packageName: String) =
        try {
            MyApplication.instance.packageManager?.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "$packageName not found.")
            null
        }

    private fun detectWebView(lld: Lld) {
        val builtInWebViewPackageInfo =
            getPackageInfo(WEB_VIEW_BUILT_IN_PACKAGE_NAME)
                ?: getPackageInfo(WEB_VIEW_STABLE_PACKAGE_NAME)
        val builtInWebViewVersion = builtInWebViewPackageInfo?.versionName ?: ""

        val implementWebViewPackageInfo =
            WebViewCompat.getCurrentWebViewPackage(MyApplication.instance)
        val implementWebViewVersion = implementWebViewPackageInfo?.versionName ?: ""

        val lldWebViewStable = lld.webView.stable.version
        @ColorInt val webViewColor = when {
            Version(builtInWebViewVersion).isAtLeast(lldWebViewStable) -> COLOR_STATE_LIST_NO_PROBLEM
            Version(implementWebViewVersion).isAtLeast(lldWebViewStable) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            """
            |${collectWebViewInfo(builtInWebViewPackageInfo, R.string.webview_built_in_version)}
            |
            |${collectWebViewInfo(implementWebViewPackageInfo, R.string.webview_implement_version)}
            """.trimMargin(),
            webViewColor
        )
    }

    private fun getWebViewAppName(packageName: String?) = MyApplication.getMyString(
        when (packageName) {
            WEB_VIEW_BUILT_IN_PACKAGE_NAME -> R.string.webview_built_in

            WEB_VIEW_STABLE_PACKAGE_NAME -> R.string.webview_stable
            WEB_VIEW_BETA_PACKAGE_NAME -> R.string.webview_beta
            WEB_VIEW_DEV_IN_PACKAGE_NAME -> R.string.webview_developer
            WEB_VIEW_CANARY_PACKAGE_NAME -> R.string.webview_canary

            CHROME_STABLE_PACKAGE_NAME -> R.string.chrome_stable
            CHROME_BETA_PACKAGE_NAME -> R.string.chrome_beta
            CHROME_DEV_IN_PACKAGE_NAME -> R.string.chrome_developer
            CHROME_CANARY_PACKAGE_NAME -> R.string.chrome_canary

            else -> android.R.string.unknownName
        }
    )

    private fun collectWebViewInfo(packageInfo: PackageInfo?, @StringRes descId: Int): String {
        val desc = MyApplication.getMyString(descId)
        val appName = getWebViewAppName(packageInfo?.packageName)
        val versionName =
            packageInfo?.versionName ?: MyApplication.getMyString(android.R.string.unknownName)

        return "$desc\n$appName\n$versionName"
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
            MyApplication.getMyString(
                R.string.android_info,
                BUILD_VERSION_RELEASE,
                BUILD_VERSION_SDK_INT
            ),
            androidColor
        )

//        if (isAtLeastAndroid6()) {
//            add(
//                MyApplication.getMyString(R.string.build_preview_sdk_int, Build.VERSION.PREVIEW_SDK_INT.toString())
//            )
//        }
        // endregion [Android]

        // region [Security patch]
        val securityPatch = if (isAtLeastAndroid6()) {
            BUILD_VERSION_SECURITY_PATCH
        } else {
            getStringProperty(PROP_SECURITY_PATCH)
        }

        val lldSecurityPatch = lld.android.securityPatchLevel
        @ColorInt val securityPatchColor = when {
            securityPatch >= lldSecurityPatch -> COLOR_STATE_LIST_NO_PROBLEM
            getSecurityPatchYearMonth(securityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            MyApplication.getMyString(R.string.build_security_patch, securityPatch),
            securityPatchColor
        )
        // endregion [Security patch]

        // region [Kernel]
        val linuxVersion = SYSTEM_PROPERTY_LINUX_VERSION
        val isAtLeast = Version(linuxVersion).isAtLeast(lld.linux.stable.version)
        val isSupported = Version(linuxVersion).isAtLeast(lld.linux.support.version)
        @ColorInt val linuxColor = when {
            isAtLeast -> COLOR_STATE_LIST_NO_PROBLEM
            isSupported -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }
        add(MyApplication.getMyString(R.string.linux_version, linuxVersion), linuxColor)
        // endregion [Kernel]

        // region [A/B]
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]

        // val romTotalSizeResult = sh(CMD_ROM_TOTAL_SIZE)
        // val romTotalSize = kotlin.math.floor(romTotalSizeResult[0].toFloat()).toString()

        val isAbUpdateSupported = getStringProperty(PROP_AB_UPDATE, isAtLeastAndroid7()).toBoolean()
        val abUpdateSupportedArgs =
            translate(isAbUpdateSupported) /* + MyApplication.getMyString(R.string.rom_total_size_result, romTotalSize) */

        var abFinalResult =
            MyApplication.getMyString(
                R.string.ab_seamless_update_enabled_result,
                abUpdateSupportedArgs
            )
        if (isAbUpdateSupported) {
            val slotSuffixResult = getStringProperty(PROP_SLOT_SUFFIX)
            val hasVndkVersion = slotSuffixResult.isNotEmpty()
            val slotSuffixUsing = if (hasVndkVersion) {
                slotSuffixResult
            } else {
                MyApplication.getMyString(android.R.string.unknownName)
            }

            abFinalResult += MyApplication.getMyString(
                R.string.current_using_ab_slot_result,
                slotSuffixUsing
            )
        }

        add(abFinalResult, isAbUpdateSupported)
        // endregion [A/B]

        // region [Treble]
        val isTrebleEnabled =
            getStringProperty(PROP_TREBLE_ENABLED, isAtLeastAndroid8()).toBoolean()

        add(
            MyApplication.getMyString(R.string.treble_enabled_result, translate(isTrebleEnabled)),
            isTrebleEnabled
        )
        // endregion [Treble]

        // region [VNDK]
        val vndkVersionResult = getStringProperty(PROP_VNDK_VERSION, isAtLeastAndroid8())
        val hasVndkVersion = hasResult(vndkVersionResult)

        @ColorInt val vndkColor: Int

        var isVndkBuiltInResult = translate(hasVndkVersion)
        if (hasVndkVersion) {
            val vndkVersion = if (hasVndkVersion) {
                vndkVersionResult
            } else {
                MyApplication.getMyString(android.R.string.unknownName)
            }

            val hasVndkLite = getStringProperty(PROP_VNDK_LITE).toBoolean()

            vndkColor = if (vndkVersion == lld.android.stable.api && !hasVndkLite) {
                COLOR_STATE_LIST_NO_PROBLEM
            } else {
                COLOR_STATE_LIST_WARNING
            }

            isVndkBuiltInResult += MyApplication.getMyString(
                R.string.built_in_vndk_version_result,
                if (hasVndkLite) "$vndkVersion, Lite" else vndkVersion
            )
        } else {
            vndkColor = COLOR_STATE_LIST_CRITICAL
        }

        add(
            MyApplication.getMyString(R.string.vndk_built_in_result, isVndkBuiltInResult),
            vndkColor
        )
        // endregion [VNDK]

        // region [SAR]
        val hasSystemRootImage =
            getStringProperty(PROP_SYSTEM_ROOT_IMAGE, isAtLeastAndroid9()).toBoolean()

        val mountDevRootResult = sh(CMD_MOUNT_DEV_ROOT, isAtLeastAndroid9())
        val hasMountDevRoot = hasResult(mountDevRootResult)

        val mountSystemResult = sh(CMD_MOUNT_SYSTEM, isAtLeastAndroid9() && !hasSystemRootImage)
        val hasMountSystem = hasResult(mountSystemResult)

        val isSar =
            isAtLeastAndroid9() && (hasSystemRootImage || hasMountDevRoot || !hasMountSystem)
        add(MyApplication.getMyString(R.string.sar_enabled_result, translate(isSar)), isSar)
        // endregion [SAR]

        // region [APEX]
        val apexUpdatable = getStringProperty(PROP_APEX_UPDATABLE, isAtLeastAndroid10()).toBoolean()

        val flattenedApexMountedResult = sh(CMD_FLATTENED_APEX_MOUNT, isAtLeastAndroid10())
        val isFlattenedApexMounted = hasResult(flattenedApexMountedResult)

        val isApex = apexUpdatable || isFlattenedApexMounted
        val isLegacyFlattenedApex = !apexUpdatable && isFlattenedApexMounted

        var apexEnabledResult = translate(isApex)
        if (isLegacyFlattenedApex) {
            apexEnabledResult += MyApplication.getMyString(R.string.apex_legacy_flattened)
        }

        val apexColor = when {
            apexUpdatable -> COLOR_STATE_LIST_NO_PROBLEM
            isLegacyFlattenedApex -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(MyApplication.getMyString(R.string.apex_enabled_result, apexEnabledResult), apexColor)
        // endregion [APEX]

        // region [ToyBox]
        val toyboxVersionResult = sh(CMD_TOYBOX_VERSION, isAtLeastAndroid6())
        val hasToyboxVersion = hasResult(toyboxVersionResult)
        var hasToyboxResult = translate(hasToyboxVersion)

        @ColorInt val toyboxColor = if (hasToyboxVersion) {
            val toyboxVersion = toyboxVersionResult[0]
            hasToyboxResult += MyApplication.getMyString(
                R.string.built_in_toybox_version_result,
                toyboxVersion
            )

            val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
            val toyboxRealVersion = Version(toyboxRealVersionString)
            when {
                toyboxRealVersion.isAtLeast(lld.toybox.stable.version) -> COLOR_STATE_LIST_NO_PROBLEM
                toyboxRealVersion.isAtLeast(lld.toybox.support.version) -> COLOR_STATE_LIST_WARNING
                else -> COLOR_STATE_LIST_CRITICAL
            }
        } else {
            COLOR_STATE_LIST_CRITICAL
        }
        add(
            MyApplication.getMyString(R.string.toybox_built_in_result, hasToyboxResult),
            toyboxColor
        )
        // endregion [ToyBox]

        detectWebView(lld)
    }

    private fun hasResult(result: String) =
        result != MyApplication.getMyString(R.string.build_not_filled)

    private fun hasResult(result: List<String>) = result.isNotEmpty() && result[0].isNotEmpty()

    private fun translate(condition: Boolean) = MyApplication.getMyString(
        if (condition) {
            R.string.result_yes
        } else {
            R.string.result_no
        }
    )
}
