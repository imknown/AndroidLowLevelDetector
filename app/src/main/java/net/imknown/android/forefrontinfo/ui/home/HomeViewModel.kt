package net.imknown.android.forefrontinfo.ui.home

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewCompat
import com.g00fy2.versioncompare.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.base.BaseListViewModel
import net.imknown.android.forefrontinfo.base.MyModel
import net.imknown.android.forefrontinfo.base.SingleEvent
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.Subtitle

class HomeViewModel : BaseListViewModel() {

    companion object {
        private val BUILD_VERSION_RELEASE = Build.VERSION.RELEASE
        private val BUILD_VERSION_SDK_INT = Build.VERSION.SDK_INT

        private val BUILD_RO_BUILD_ID = Build.ID
        private const val PROP_RO_SYSTEM_BUILD_ID = "ro.system.build.id"
        private const val PROP_RO_VENDOR_BUILD_ID = "ro.vendor.build.id"
        private const val PROP_RO_ODM_BUILD_ID = "ro.odm.build.id"

        private val BUILD_VERSION_SECURITY_PATCH by lazy {
            // Suppress because of lazy already
            @Suppress Build.VERSION.SECURITY_PATCH
        }
        private const val PROP_SECURITY_PATCH = "ro.build.version.security_patch"

        private const val PROP_VENDOR_SECURITY_PATCH = "ro.vendor.build.security_patch"

        private val SYSTEM_PROPERTY_LINUX_VERSION = System.getProperty("os.version")

        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed */ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        private const val PROP_AB_UPDATE = "ro.build.ab_update"
        private const val PROP_SLOT_SUFFIX = "ro.boot.slot_suffix"

        // https://source.android.com/devices/architecture?hl=en#hidl
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

        private const val CMD_TOYBOX_VERSION = "toybox --version"

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

    val subtitle by lazy { MutableLiveData<Subtitle>() }

    val error by lazy { MutableLiveData<SingleEvent<Exception>>() }

    private fun copyJsonIfNeeded() {
        if (JsonIo.whetherNeedCopyAssets(MyApplication.instance.assets)) {
            JsonIo.copyJsonFromAssetsToContextFilesDir(
                MyApplication.instance.assets,
                GatewayApi.savedLldJsonFile,
                GatewayApi.LLD_JSON_NAME
            )
        }
    }

    override fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
        val allowNetwork = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_allow_network_data_key), false
        )

        if (allowNetwork) {
            GatewayApi.downloadLldJsonFile({
                launch(Dispatchers.IO) {
                    prepareResult(true)
                }
            }, {
                launch(Dispatchers.IO) {
                    onError(
                        Exception(
                            MyApplication.getMyString(
                                R.string.lld_json_download_failed,
                                it.message
                            )
                        )
                    )

                    prepareResult(false)
                }
            })
        } else {
            prepareResult(false)
        }
    }

    private suspend fun onError(exception: Exception) = withContext(Dispatchers.Main) {
        error.value = SingleEvent(exception)
    }

    private suspend fun prepareResult(isOnline: Boolean) {
        if (!isOnline) {
            try {
                copyJsonIfNeeded()
            } catch (e: Exception) {
                onError(e)
            }
        }

        prepareResultWhenOk(isOnline)
    }

    private suspend fun prepareResultWhenOk(isOnline: Boolean) {
        @StringRes var lldDataModeResId: Int
        var dataVersion: String

        try {
            val lld = GatewayApi.savedLldJsonFile.fromJson<Lld>()

            detect(lld)

            lldDataModeResId = if (isOnline) {
                R.string.lld_json_online
            } else {
                R.string.lld_json_offline
            }
            dataVersion = lld.version
        } catch (e: Exception) {
            onError(Exception(MyApplication.getMyString(R.string.lld_json_parse_failed, e.message)))

            lldDataModeResId = R.string.lld_json_offline

            dataVersion = MyApplication.getMyString(android.R.string.unknownName)
        }

        withContext(Dispatchers.Main) {
            subtitle.value = Subtitle(lldDataModeResId, dataVersion)
        }
    }

    // region [detect]
    private suspend fun detect(lld: Lld) {
        val tempModels = ArrayList<MyModel>()

        detectAndroid(tempModels, lld)

        detectBuildId(tempModels, lld)

        var securityPatch = if (isAtLeastAndroid6()) {
            BUILD_VERSION_SECURITY_PATCH
        } else {
            getStringProperty(PROP_SECURITY_PATCH)
        }
        detectSecurityPatch(tempModels, lld, securityPatch, R.string.security_patch_level_title)

        securityPatch = getStringProperty(PROP_VENDOR_SECURITY_PATCH, isAtLeastAndroid9())
        detectSecurityPatch(
            tempModels,
            lld,
            securityPatch,
            R.string.vendor_security_patch_level_title
        )

        detectKernel(tempModels, lld)

        detectAb(tempModels)

        detectTreble(tempModels)

        detectVndk(tempModels, lld)

        detectSar(tempModels)

        detectApex(tempModels)

        detectToybox(tempModels, lld)

        detectWebView(tempModels, lld)

        detectOutdatedTargetSdkVersionApk(tempModels)

        withContext(Dispatchers.Main) {
            models.value = tempModels
        }
    }

    private fun detectAndroid(tempModels: ArrayList<MyModel>, lld: Lld) {
        @ColorRes val androidColor = when {
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.color.colorNoProblem
            isSupportedByUpstreamAndroid(lld) -> R.color.colorWaring
            else -> R.color.colorCritical
        }

        val previewVersion: String
        val previewApi: String
        val previewType: String
        if (lld.android.beta.api.isNotEmpty()) {
            previewVersion = lld.android.beta.version
            previewApi = lld.android.beta.api
            previewType = "Beta"
        } else {
            previewVersion = lld.android.alpha.version
            previewApi = lld.android.alpha.api
            previewType = "Alpha"
        }

        add(
            tempModels,
            MyApplication.getMyString(
                R.string.android_info_title
            ),
            MyApplication.getMyString(
                R.string.android_info_detail,
                MyApplication.getMyString(
                    R.string.android_info,
                    BUILD_VERSION_RELEASE,
                    BUILD_VERSION_SDK_INT
                ),
                MyApplication.getMyString(
                    R.string.android_info,
                    lld.android.stable.version,
                    lld.android.stable.api
                ),
                MyApplication.getMyString(
                    R.string.android_info,
                    lld.android.support.version,
                    lld.android.support.api
                ),
                MyApplication.getMyString(R.string.android_info_preview),
                MyApplication.getMyString(
                    R.string.android_info,
                    "$previewVersion $previewType",
                    previewApi
                )
            ),
            androidColor
        )
    }

    private fun detectBuildId(tempModels: ArrayList<MyModel>, lld: Lld) {
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

        fun getDate(buildId: String) = buildId.split('.')[1]
        fun isDateHigherThanConfig() =
            (buildIdResult.split('.').size == 3 && getDate(buildIdResult) >= getDate(details[0].id))
                    || isLatestPreviewAndroid(lld)
        @ColorRes val buildIdColor = when {
            isDateHigherThanConfig() -> R.color.colorNoProblem
            isLatestStableAndroid(lld) -> R.color.colorWaring
            else -> R.color.colorCritical
        }

        add(
            tempModels,
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

    private fun detectSecurityPatch(
        tempModels: ArrayList<MyModel>,
        lld: Lld,
        securityPatch: String, @StringRes titleId: Int
    ) {
        val lldSecurityPatch = lld.android.securityPatchLevel
        @ColorRes val securityPatchColor = when {
            !hasResult(securityPatch) -> R.color.colorCritical
            securityPatch >= lldSecurityPatch -> R.color.colorNoProblem
            getSecurityPatchYearMonth(securityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> R.color.colorWaring
            else -> R.color.colorCritical
        }

        add(
            tempModels,
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

    private fun detectKernel(tempModels: ArrayList<MyModel>, lld: Lld) {
        val linuxVersionString = SYSTEM_PROPERTY_LINUX_VERSION
        val linuxVersion = Version(linuxVersionString)

        @ColorRes var linuxColor = R.color.colorCritical

        val versionsSupported = lld.linux.google.versions
        versionsSupported.forEach {
            if (linuxVersion.major == Version(it).major
                && linuxVersion.minor == Version(it).minor
            ) {
                linuxColor = if (linuxVersion.isAtLeast(it)) {
                    R.color.colorNoProblem
                } else {
                    R.color.colorWaring
                }

                return@forEach
            }
        }

        add(
            tempModels,
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

    private fun detectAb(tempModels: ArrayList<MyModel>) {
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]

        val isAbUpdateSupported = getStringProperty(PROP_AB_UPDATE, isAtLeastAndroid7()).toBoolean()
        var abUpdateSupportedArgs = translate(isAbUpdateSupported)

        val abFinalResult =
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

            abUpdateSupportedArgs += MyApplication.getMyString(
                R.string.current_using_ab_slot_result,
                slotSuffixUsing
            )
        }

        add(tempModels, abFinalResult, abUpdateSupportedArgs, isAbUpdateSupported)
    }

    private fun detectTreble(tempModels: ArrayList<MyModel>) {
        val isTrebleEnabled =
            getStringProperty(PROP_TREBLE_ENABLED, isAtLeastAndroid8()).toBoolean()

        add(
            tempModels,
            MyApplication.getMyString(R.string.treble_enabled_title),
            translate(isTrebleEnabled),
            isTrebleEnabled
        )
    }

    private fun detectVndk(tempModels: ArrayList<MyModel>, lld: Lld) {
        val vndkVersionResult = getStringProperty(PROP_VNDK_VERSION, isAtLeastAndroid8())
        val hasVndkVersion = hasResult(vndkVersionResult)

        @ColorRes val vndkColor: Int

        var isVndkBuiltInResult = translate(hasVndkVersion)
        if (hasVndkVersion) {
            val vndkVersion = if (hasVndkVersion) {
                vndkVersionResult
            } else {
                MyApplication.getMyString(android.R.string.unknownName)
            }

            val hasVndkLite = getStringProperty(PROP_VNDK_LITE).toBoolean()

            vndkColor = if (vndkVersion == lld.android.stable.api && !hasVndkLite) {
                R.color.colorNoProblem
            } else {
                R.color.colorWaring
            }

            isVndkBuiltInResult += MyApplication.getMyString(
                R.string.built_in_vndk_version_result,
                if (hasVndkLite) "$vndkVersion, Lite" else vndkVersion
            )
        } else {
            vndkColor = R.color.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.vndk_built_in_title),
            isVndkBuiltInResult,
            vndkColor
        )
    }

    private suspend fun detectSar(tempModels: ArrayList<MyModel>) {
        val hasSystemRootImage =
            getStringProperty(PROP_SYSTEM_ROOT_IMAGE, isAtLeastAndroid9()).toBoolean()

        val mountDevRootResult = shAsync(CMD_MOUNT_DEV_ROOT, isAtLeastAndroid9()).await()
        val hasMountDevRoot = hasResult(mountDevRootResult)

        val mountSystemResult =
            shAsync(CMD_MOUNT_SYSTEM, isAtLeastAndroid9() && !hasSystemRootImage).await()
        val hasMountSystem = hasResult(mountSystemResult)

        val isSar =
            isAtLeastAndroid9() && (hasSystemRootImage || hasMountDevRoot || !hasMountSystem)
        add(
            tempModels,
            MyApplication.getMyString(R.string.sar_enabled_title),
            translate(isSar),
            isSar
        )
    }

    private suspend fun detectApex(tempModels: ArrayList<MyModel>) {
        val apexUpdatable = getStringProperty(PROP_APEX_UPDATABLE, isAtLeastAndroid10()).toBoolean()

        val flattenedApexMountedResult =
            shAsync(CMD_FLATTENED_APEX_MOUNT, isAtLeastAndroid10()).await()
        val isFlattenedApexMounted = hasResult(flattenedApexMountedResult)

        val isApex = apexUpdatable || isFlattenedApexMounted
        val isLegacyFlattenedApex = !apexUpdatable && isFlattenedApexMounted

        var apexEnabledResult = translate(isApex)
        if (isLegacyFlattenedApex) {
            apexEnabledResult += MyApplication.getMyString(R.string.apex_legacy_flattened)
        }

        val apexColor = when {
            apexUpdatable -> R.color.colorNoProblem
            isLegacyFlattenedApex -> R.color.colorWaring
            else -> R.color.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.apex_enabled_title),
            apexEnabledResult,
            apexColor
        )
    }

    private suspend fun detectToybox(tempModels: ArrayList<MyModel>, lld: Lld) {
        val toyboxVersionResult = shAsync(CMD_TOYBOX_VERSION, isAtLeastAndroid6()).await()
        val hasToyboxVersion = hasResult(toyboxVersionResult)

        val toyboxVersion = if (hasToyboxVersion) {
            toyboxVersionResult[0]
        } else {
            translate(false)
        }

        @ColorRes val toyboxColor = if (hasToyboxVersion) {
            val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
            val toyboxRealVersion = Version(toyboxRealVersionString)
            when {
                toyboxRealVersion.isAtLeast(lld.toybox.stable.version) -> R.color.colorNoProblem
                toyboxRealVersion.isAtLeast(lld.toybox.support.version) -> R.color.colorWaring
                else -> R.color.colorCritical
            }
        } else {
            R.color.colorCritical
        }
        add(
            tempModels,
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
            MyApplication.instance.packageManager.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "$packageName not found.")
            null
        }

    private fun detectWebView(tempModels: ArrayList<MyModel>, lld: Lld) {
        val builtInWebViewPackageInfo =
            getPackageInfo(WEB_VIEW_BUILT_IN_PACKAGE_NAME)
                ?: getPackageInfo(WEB_VIEW_STABLE_PACKAGE_NAME)
        val builtInWebViewVersion = builtInWebViewPackageInfo?.versionName ?: ""

        val implementWebViewPackageInfo =
            WebViewCompat.getCurrentWebViewPackage(MyApplication.instance)
        val implementWebViewVersion = implementWebViewPackageInfo?.versionName ?: ""

        val lldWebViewStable = lld.webView.stable.version
        @ColorRes val webViewColor = when {
            Version(builtInWebViewVersion).isAtLeast(lldWebViewStable) -> R.color.colorNoProblem
            Version(implementWebViewVersion).isAtLeast(lldWebViewStable) -> R.color.colorWaring
            else -> R.color.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.webview_title),
            """
            |${collectWebViewInfo(builtInWebViewPackageInfo, R.string.webview_built_in_version)}
            |
            |${collectWebViewInfo(implementWebViewPackageInfo, R.string.webview_implement_version)}
            |
            |${MyApplication.getMyString(
                R.string.webview_detail,
                lld.webView.stable.version,
                lld.webView.beta.version
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

    private fun detectOutdatedTargetSdkVersionApk(tempModels: ArrayList<MyModel>) {
        val systemApkList =
            MyApplication.instance.packageManager.getInstalledApplications(0).filter {
                it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0
            }.sortedWith(compareBy(ApplicationInfo::targetSdkVersion, ApplicationInfo::packageName))

        val firstApiLevelProp = getStringProperty(PROP_RO_PRODUCT_FIRST_API_LEVEL)

        val firstApiLevelLine = MyApplication.getMyString(
            R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
            firstApiLevelProp
        )
        var result = firstApiLevelLine

        val outdatedSystemApkList = systemApkList.filter {
            it.targetSdkVersion < BUILD_VERSION_SDK_INT
        }

        outdatedSystemApkList.forEachIndexed { index, applicationInfo ->
            result += "(${applicationInfo.targetSdkVersion}) ${applicationInfo.packageName}"

            if (index != outdatedSystemApkList.size - 1) {
                result += "\n"
            }
        }

        val noOutdatedTotally = (result == firstApiLevelLine)

        @ColorRes val targetSdkVersionColor = if (noOutdatedTotally) {
            result += MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_result_none)

            R.color.colorNoProblem
        } else {
            val outdatedFirstApiLevelSystemApkList =
                if (hasResult(firstApiLevelProp) && firstApiLevelProp != "0") {
                    systemApkList.filter {
                        it.targetSdkVersion < firstApiLevelProp.toInt()
                    }
                } else {
                    outdatedSystemApkList
                }

            if (outdatedFirstApiLevelSystemApkList.isNotEmpty()) {
                R.color.colorCritical
            } else {
                R.color.colorWaring
            }
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_title),
            result,
            targetSdkVersionColor
        )
    }
    // endregion [detect]

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

    @SuppressLint("PrivateApi")
    private fun getStringProperty(key: String, condition: Boolean = true): String {
        return if (condition) {
            Class.forName("android.os.SystemProperties").getDeclaredMethod(
                "get",
                String::class.java,
                String::class.java
            ).invoke(null, key, MyApplication.getMyString(R.string.build_not_filled)) as String
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }
    }

//    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
//    private fun setStringProperty(key: String, value: String) {
//        Class.forName("android.os.SystemProperties").getDeclaredMethod(
//            "set",
//            String::class.java,
//            String::class.java
//        ).invoke(null, key, value)
//    }

    private fun add(
        tempModels: ArrayList<MyModel>,
        title: String,
        detail: String?,
        condition: Boolean
    ) = tempModels.add(
        MyModel(
            title,
            detail.toString(),
            if (condition) {
                R.color.colorNoProblem
            } else {
                R.color.colorCritical
            }
        )
    )

    private fun add(
        tempModels: ArrayList<MyModel>,
        title: String,
        detail: String?,
        @ColorRes color: Int
    ) =
        tempModels.add(MyModel(title, detail.toString(), color))
}