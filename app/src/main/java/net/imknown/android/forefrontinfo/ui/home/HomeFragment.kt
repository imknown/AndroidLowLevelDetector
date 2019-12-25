package net.imknown.android.forefrontinfo.ui.home

import android.content.pm.ApplicationInfo
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

        // https://source.android.com/setup/start/build-numbers?hl=en#source-code-tags-and-builds
        // https://android.googlesource.com/platform/frameworks/base/+refs"
        private val BUILD_RO_BUILD_ID = Build.ID
        private const val PROP_RO_SYSTEM_BUILD_ID = "ro.system.build.id"
        private const val PROP_RO_VENDOR_BUILD_ID = "ro.vendor.build.id"
        private const val PROP_RO_ODM_BUILD_ID = "ro.odm.build.id"

        // https://source.android.com/security/bulletin
        private val BUILD_VERSION_SECURITY_PATCH by lazy {
            // Suppress because of lazy already
            @Suppress Build.VERSION.SECURITY_PATCH
        }
        private const val PROP_SECURITY_PATCH = "ro.build.version.security_patch"

        private const val PROP_VENDOR_SECURITY_PATCH = "ro.vendor.build.security_patch"

        // https://android.googlesource.com/kernel/common/+refs
        // https://source.android.com/setup/build/building-kernels#downloading
        private val SYSTEM_PROPERTY_LINUX_VERSION = System.getProperty("os.version")

        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed */ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
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

        private const val PROP_RO_PRODUCT_FIRST_API_LEVEL = "ro.product.first_api_level"
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
                    showError(
                        Exception(
                            MyApplication.getMyString(R.string.lld_json_download_failed, it.message)
                        )
                    )

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
            prepareResultWhenOk(isOnline)
        }
    }

    private suspend fun prepareResultWhenOk(isOnline: Boolean) {
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
            showError(
                Exception(MyApplication.getMyString(R.string.lld_json_parse_failed, e.message))
            )

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

    private fun detectAndroid(lld: Lld) {
        @ColorInt val androidColor = when {
            isLatestStableAndroid(lld) -> COLOR_STATE_LIST_NO_PROBLEM
            isSupportedByUpstream(lld) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        val previewType: String
        val previewVersion: String
        val previewApi: String
        if (lld.android.beta.api.isNotEmpty()) {
            previewType = "Beta"
            previewVersion = lld.android.beta.version
            previewApi = lld.android.beta.api
        } else {
            previewType = "Alpha"
            previewVersion = lld.android.alpha.version
            previewApi = lld.android.alpha.api
        }

        add(
            MyApplication.getMyString(
                R.string.android_info_title
            ),
            MyApplication.getMyString(
                R.string.android_info_detail,
                MyApplication.getMyString(
                    R.string.android_info, BUILD_VERSION_RELEASE, BUILD_VERSION_SDK_INT
                ),
                MyApplication.getMyString(
                    R.string.android_info, lld.android.stable.version, lld.android.stable.api
                ),
                MyApplication.getMyString(
                    R.string.android_info, lld.android.support.version, lld.android.support.api
                ),
                previewType,
                MyApplication.getMyString(
                    R.string.android_info, previewVersion, previewApi
                )
            ),
            androidColor
        )
    }

    private fun detectBuildId(lld: Lld) {
        val buildIdResult = BUILD_RO_BUILD_ID
        val systemBuildIdResult = getStringProperty(PROP_RO_SYSTEM_BUILD_ID, isAtLeastAndroid9())
        val vendorBuildIdResult = getStringProperty(PROP_RO_VENDOR_BUILD_ID, isAtLeastAndroid9())
        val odmBuildIdResult = getStringProperty(PROP_RO_ODM_BUILD_ID, isAtLeastAndroid9())

        var builds = ""
        val details = lld.android.build.details
        details.forEachIndexed { index, detail ->
            builds += MyApplication.getMyString(
                R.string.android_build_id,
                detail.id,
                detail.revision
            )

            if (index != details.size - 1) {
                builds += "\n"
            }
        }

        @ColorInt val buildIdColor = when {
            details.map { it.id }.contains(buildIdResult) -> COLOR_STATE_LIST_NO_PROBLEM
            isLatestStableAndroid(lld) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            MyApplication.getMyString(
                R.string.android_build_id_title
            ),
            MyApplication.getMyString(
                R.string.android_build_id_detail,
                buildIdResult,
                systemBuildIdResult,
                vendorBuildIdResult,
                odmBuildIdResult,
                lld.android.build.version,
                builds
            ),
            buildIdColor
        )
    }

    private fun detectSecurityPatch(lld: Lld, securityPatch: String, @StringRes titleId: Int) {
        val lldSecurityPatch = lld.android.securityPatchLevel
        @ColorInt val securityPatchColor = when {
            !hasResult(securityPatch) -> COLOR_STATE_LIST_CRITICAL
            securityPatch >= lldSecurityPatch -> COLOR_STATE_LIST_NO_PROBLEM
            getSecurityPatchYearMonth(securityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> COLOR_STATE_LIST_WARNING
            else -> COLOR_STATE_LIST_CRITICAL
        }

        add(
            MyApplication.getMyString(titleId),
            MyApplication.getMyString(
                R.string.security_patch_level_detail,
                securityPatch,
                lld.android.securityPatchLevel
            ),
            securityPatchColor
        )
    }

    private fun getSecurityPatchYearMonth(securityPatch: String) =
        securityPatch.substringBeforeLast('-')

    private fun detectKernel(lld: Lld) {
        val linuxVersionString = SYSTEM_PROPERTY_LINUX_VERSION
        val linuxVersion = Version(linuxVersionString)

        @ColorInt var linuxColor = COLOR_STATE_LIST_CRITICAL

        val versionsSupported = lld.linux.google.versions
        versionsSupported.forEach {
            if (linuxVersion.major == Version(it).major
                && linuxVersion.minor == Version(it).minor
            ) {
                linuxColor = if (linuxVersion.isAtLeast(it)) {
                    COLOR_STATE_LIST_NO_PROBLEM
                } else {
                    COLOR_STATE_LIST_WARNING
                }

                return@forEach
            }
        }

        add(
            MyApplication.getMyString(R.string.linux_title),
            MyApplication.getMyString(
                R.string.linux_version_detail,
                linuxVersionString,
                versionsSupported.joinToString("｜"),
                lld.linux.mainline.version
            ),
            linuxColor
        )
    }

    private fun detectAb() {
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]

        val isAbUpdateSupported = getStringProperty(PROP_AB_UPDATE, isAtLeastAndroid7()).toBoolean()
        val abUpdateSupportedArgs = translate(isAbUpdateSupported)

        var abFinalResult =
            MyApplication.getMyString(
                R.string.ab_seamless_update_enabled_title
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

        add(abFinalResult, abUpdateSupportedArgs, isAbUpdateSupported)
    }

    private fun detectTreble() {
        val isTrebleEnabled =
            getStringProperty(PROP_TREBLE_ENABLED, isAtLeastAndroid8()).toBoolean()

        add(
            MyApplication.getMyString(R.string.treble_enabled_title),
            translate(isTrebleEnabled),
            isTrebleEnabled
        )
    }

    private fun detectVndk(lld: Lld) {
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
            MyApplication.getMyString(R.string.vndk_built_in_title),
            isVndkBuiltInResult,
            vndkColor
        )
    }

    private fun detectSar() {
        val hasSystemRootImage =
            getStringProperty(PROP_SYSTEM_ROOT_IMAGE, isAtLeastAndroid9()).toBoolean()

        val mountDevRootResult = sh(CMD_MOUNT_DEV_ROOT, isAtLeastAndroid9())
        val hasMountDevRoot = hasResult(mountDevRootResult)

        val mountSystemResult = sh(CMD_MOUNT_SYSTEM, isAtLeastAndroid9() && !hasSystemRootImage)
        val hasMountSystem = hasResult(mountSystemResult)

        val isSar =
            isAtLeastAndroid9() && (hasSystemRootImage || hasMountDevRoot || !hasMountSystem)
        add(MyApplication.getMyString(R.string.sar_enabled_title), translate(isSar), isSar)
    }

    private fun detectApex() {
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

        add(MyApplication.getMyString(R.string.apex_enabled_title), apexEnabledResult, apexColor)
    }

    private fun detectToybox(lld: Lld) {
        val toyboxVersionResult = sh(CMD_TOYBOX_VERSION, isAtLeastAndroid6())
        val hasToyboxVersion = hasResult(toyboxVersionResult)

        val toyboxVersion = if (hasToyboxVersion) {
            toyboxVersionResult[0]
        } else {
            translate(false)
        }

        @ColorInt val toyboxColor = if (hasToyboxVersion) {
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
            MyApplication.getMyString(R.string.toybox_built_in_title),
            MyApplication.getMyString(
                R.string.toybox_built_in_detail,
                toyboxVersion,
                lld.toybox.stable.version,
                lld.toybox.support.version,
                lld.toybox.mainline.version,
                lld.toybox.master.version
            ),
            toyboxColor
        )
    }

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
            MyApplication.getMyString(R.string.webview_title),
            """
            |${collectWebViewInfo(builtInWebViewPackageInfo, R.string.webview_built_in_version)}
            |
            |${collectWebViewInfo(implementWebViewPackageInfo, R.string.webview_implement_version)}
            |
            |${MyApplication.getMyString(
                R.string.webview_detail,
                lld.webView.stable.version,
                lld.webView.beta.version,
                lld.webView.dev.version,
                lld.webView.canary.version
            )}
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

    private fun detectOutdatedTargetSdkVersionApk() {
        val systemApkList = context?.packageManager?.getInstalledApplications(0)?.filter {
            it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0
        }?.sortedWith(compareBy(ApplicationInfo::targetSdkVersion, ApplicationInfo::packageName))

        val firstApiLevelProp = getStringProperty(PROP_RO_PRODUCT_FIRST_API_LEVEL)

        val firstApiLevelLine = MyApplication.getMyString(
            R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
            firstApiLevelProp
        )
        var result = firstApiLevelLine

        val outdatedSystemApkList = systemApkList?.filter {
            it.targetSdkVersion < BUILD_VERSION_SDK_INT
        }

        outdatedSystemApkList?.forEachIndexed { index, applicationInfo ->
            result += "(${applicationInfo.targetSdkVersion}) ${applicationInfo.packageName}"

            if (index != outdatedSystemApkList.size - 1) {
                result += "\n"
            }
        }

        val noOutdatedTotally = (result == firstApiLevelLine)

        @ColorInt val targetSdkVersionColor = if (noOutdatedTotally) {
            result += MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_result_none)

            COLOR_STATE_LIST_NO_PROBLEM
        } else {
            val outdatedFirstApiLevelSystemApkList =
                if (hasResult(firstApiLevelProp) && firstApiLevelProp != "0") {
                    systemApkList?.filter {
                        it.targetSdkVersion < firstApiLevelProp.toInt()
                    }
                } else {
                    outdatedSystemApkList
                }

            if ((outdatedFirstApiLevelSystemApkList?.size ?: 1) > 0) {
                COLOR_STATE_LIST_CRITICAL
            } else {
                COLOR_STATE_LIST_WARNING
            }
        }

        add(
            MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_title),
            result,
            targetSdkVersionColor
        )
    }

    private fun fillDataset(lld: Lld) {
        createNewTempDataset()

        detectAndroid(lld)

        detectBuildId(lld)

        var securityPatch = if (isAtLeastAndroid6()) {
            BUILD_VERSION_SECURITY_PATCH
        } else {
            getStringProperty(PROP_SECURITY_PATCH)
        }
        detectSecurityPatch(lld, securityPatch, R.string.security_patch_level_title)

        securityPatch = getStringProperty(PROP_VENDOR_SECURITY_PATCH, isAtLeastAndroid9())
        detectSecurityPatch(lld, securityPatch, R.string.vendor_security_patch_level_title)

        detectKernel(lld)

        detectAb()

        detectTreble()

        detectVndk(lld)

        detectSar()

        detectApex()

        detectToybox(lld)

        detectWebView(lld)

        detectOutdatedTargetSdkVersionApk()
    }

    private fun hasResult(result: String) =
        result.isNotEmpty()
                && result != MyApplication.getMyString(R.string.result_not_supported)
                && result != MyApplication.getMyString(R.string.build_not_filled)

    private fun hasResult(result: List<String>) = result.isNotEmpty() && result[0].isNotEmpty()

    private fun translate(condition: Boolean) = MyApplication.getMyString(
        if (condition) {
            R.string.result_supported
        } else {
            R.string.result_not_supported
        }
    )
}
