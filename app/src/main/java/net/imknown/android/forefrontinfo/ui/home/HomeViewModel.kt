package net.imknown.android.forefrontinfo.ui.home

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.AttrRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewCompat
import io.github.g00fy2.versioncompare.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.mvvm.Event
import net.imknown.android.forefrontinfo.base.mvvm.booleanEventLiveData
import net.imknown.android.forefrontinfo.ui.base.IAndroidVersion
import net.imknown.android.forefrontinfo.ui.base.JsonIo
import net.imknown.android.forefrontinfo.ui.base.fromJson
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.Subtitle
import java.io.File

class HomeViewModel : BaseListViewModel(), IAndroidVersion {

    companion object : IAndroidVersion {
        // region [Build ID]
        private const val BUILD_ID_SEPARATOR = '.'

        private const val PROP_RO_SYSTEM_BUILD_ID = "ro.system.build.id"
        private const val PROP_RO_VENDOR_BUILD_ID = "ro.vendor.build.id"
        private const val PROP_RO_ODM_BUILD_ID = "ro.odm.build.id"
        // endregion [Build ID]

        // region [Security Patch]
        private const val PROP_SECURITY_PATCH = "ro.build.version.security_patch"

        private const val PROP_VENDOR_SECURITY_PATCH = "ro.vendor.build.security_patch"

        private const val SYSTEM_PROPERTY_LINUX_VERSION = "os.version"
        // endregion [Security Patch]

        // region [A/B]
        // https://source.android.com/devices/tech/ota/ab?hl=en
        // /* root needed */ private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        // private const val PROP_VIRTUAL_AB_ALLOW_NON_AB = "ro.virtual_ab.allow_non_ab"
        private const val PROP_AB_UPDATE = "ro.build.ab_update"
        private const val PROP_VIRTUAL_AB_ENABLED = "ro.virtual_ab.enabled"
        private const val PROP_VIRTUAL_AB_RETROFIT = "ro.virtual_ab.retrofit"
        private const val PROP_SLOT_SUFFIX = "ro.boot.slot_suffix"
        // endregion [A/B]

        // region [Dynamic Partitions]
        // https://source.android.com/devices/tech/ota/dynamic_partitions/ab_legacy?hl=en
        // https://source.android.com/devices/tech/ota/dynamic_partitions/ab_launch?hl=en
        // https://codelabs.developers.google.com/codelabs/using-Android-GSI?hl=en
        private const val PROP_DYNAMIC_PARTITIONS = "ro.boot.dynamic_partitions"
        private const val PROP_DYNAMIC_PARTITIONS_RETROFIT = "ro.boot.dynamic_partitions_retrofit"
        // /* root needed */ private const val CMD_LL_DEV_BLOCK_SUPER = "ls -l /dev/block/by-name/super"
        // endregion [Dynamic Partitions]

        // region [Dynamic System]
        // https://developer.android.com/topic/dsu?hl=en
        private const val PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE =
            "persist.sys.fflag.override.settings_dynamic_system"
        private const val PROP_DYNAMIC_SYSTEM_UPDATE = "sys.fflag.override.settings_dynamic_system"
        // endregion [Dynamic System]

        // region [GSI]
        // https://codelabs.developers.google.com/codelabs/using-Android-GSI?hl=en#2
        // https://developer.android.google.cn/topic/generic-system-image?hl=en
        // https://source.android.com/setup/build/gsi?hl=en
        // https://source.android.com/devices/architecture/vndk/linker-namespace?hl=en
        private const val LD_CONFIG_FILE_ANDROID_11 = "/linkerconfig/ld.config.txt"
        private const val LD_CONFIG_FILE_ANDROID_9 = "/system/etc/ld.config*.txt"
        private const val CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED =
            "cat %s | grep -A 20 '\\[vendor\\]' | grep namespace.default.isolated"
        // endregion [GSI]

        // region [Treble]
        // https://source.android.com/devices/architecture?hl=en#hidl
        // https://source.android.com/devices/architecture/vintf/objects#device-manifest-file
        // https://source.android.com/compatibility/vts/hal-testability
        // https://android.googlesource.com/platform/cts/+/master/hostsidetests/security/src/android/security/cts/SELinuxHostTest.java#268
        // https://android.googlesource.com/platform/system/libvintf/+/master/VintfObject.cpp#286
        // https://android.googlesource.com/platform/system/libvintf/+/master/VintfObject.cpp#313
        private const val PROP_TREBLE_ENABLED = "ro.treble.enabled"
        const val PROP_VENDOR_SKU = "ro.boot.product.vendor.sku"
        private const val PATH_VENDOR_VINTF_SKU = "/vendor/etc/vintf/manifest_%s.xml"
        private const val PATH_VENDOR_VINTF = "/vendor/etc/vintf/manifest.xml"
        private const val PATH_VENDOR_VINTF_FRAGMENTS = "/vendor/etc/vintf/manifest/"
        private const val PATH_VENDOR_LEGACY_NO_FRAGMENTS = "/vendor/manifest.xml"
        // endregion [Treble]

        // region [VNDK]
        // https://source.android.com/devices/architecture/vndk?hl=en
        private const val PROP_VNDK_LITE = "ro.vndk.lite"
        private const val PROP_VNDK_VENDOR_VERSION = "ro.vndk.version"
        // private const val PROP_VNDK_PRODUCT_VERSION = "ro.product.vndk.version"
        // endregion [VNDK]

        // https://source.android.com/devices/bootloader/system-as-root?hl=en
        //
        // https://twitter.com/topjohnwu/status/1174392824625676288
        // https://github.com/topjohnwu/Magisk/blob/master/docs/boot.md
        // https://github.com/topjohnwu/Magisk/blob/master/scripts/util_functions.sh#L239
        // https://github.com/opengapps/opengapps/blob/master/scripts/templates/installer.sh#L1032
        //
        // https://github.com/penn5/TrebleCheck/blob/master/app/src/main/java/tk/hack5/treblecheck/MountDetector.kt
        // https://github.com/kevintresuelo/treble/blob/master/app/src/main/java/com/kevintresuelo/treble/checker/SystemAsRoot.kt
        private const val PROP_SYSTEM_ROOT_IMAGE = "ro.build.system_root_image"

        private const val CMD_MOUNT = "cat /proc/mounts"

        // https://source.android.com/devices/tech/ota/apex?hl=en
        private const val PROP_APEX_UPDATABLE = "ro.apex.updatable"

        private const val SETTINGS_DISABLED = 0
        private const val SETTINGS_ENABLED = 1

        // https://android.googlesource.com/platform/bootable/recovery/+/master/README.md#shows-the-device_but-in-state
        private const val PROP_ADB_SECURE = "ro.adb.secure"

        // https://source.android.com/security/encryption/full-disk
        // https://source.android.com/security/encryption/file-based
        // private const val PROP_CRYPTO_STATE = "ro.crypto.state"

        // region [SELinux]
        // https://source.android.com/security/selinux
        // https://android.googlesource.com/platform/external/selinux/+/master/libsepol/include/sepol/policydb/policydb.h#745
        // https://github.com/torvalds/linux/blob/master/security/selinux/include/security.h#L43
        // private const val SELINUX_MOUNT = "/sys/fs/selinux"
        // private const val CMD_SELINUX_POLICY_VERSION = "cat $SELINUX_MOUNT/policyvers"
        // private const val PROP_BOOT_SELINUX = "ro.boot.selinux"
        private const val CMD_GETENFORCE = "getenforce"
        private const val CMD_ERROR_PERMISSION_DENIED = "Permission denied"
        private const val SELINUX_STATUS_DISABLED = "Disabled"
        private const val SELINUX_STATUS_PERMISSIVE = "Permissive"
        private const val SELINUX_STATUS_ENFORCING = "Enforcing"
        // endregion [SELinux]

        private const val CMD_TOYBOX_VERSION = "toybox --version"

        // region [WebView]
        // Need `android.permission.DUMP`
        // private const val CMD_DUMPSYS_WEBVIEWUPDATE = "dumpsys webviewupdate"
        // private const val CMD_DUMPSYS_WEBVIEWUPDATE = "cmd webviewupdate dump"
        //
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md#webview-provider-options
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/webview-providers.md#package-name
        // https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/aosp-system-integration.md#configuring-the-android-framework
        // endregion [WebView]

        private const val PROP_RO_PRODUCT_FIRST_API_LEVEL = "ro.product.first_api_level"
    }

    private val _subtitle by lazy { MutableLiveData<Subtitle>() }
    val subtitle: LiveData<Subtitle> by lazy { _subtitle }

    private val _outdatedOrderProp by lazy {
        MyApplication.sharedPreferences.booleanEventLiveData(
            viewModelScope,
            MyApplication.getMyString(R.string.function_outdated_target_order_by_package_name_first_key),
            false
        )
    }
    val outdatedOrderProp: LiveData<Event<Boolean>> by lazy { _outdatedOrderProp }

    private val _showOutdatedOrderEvent by lazy { MutableLiveData<Event<Unit>>() }
    val showOutdatedOrderEvent: LiveData<Event<Unit>> by lazy { _showOutdatedOrderEvent }

    override fun collectModels() = viewModelScope.launch(Dispatchers.IO) {
        val allowNetwork = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_allow_network_data_key), false
        )

        if (allowNetwork) {
            try {
                val lldString = GatewayApi.fetchLldJson()

                prepareOnlineLld(lldString)
            } catch (e: Exception) {
                showError(R.string.lld_json_fetch_failed, e)

                prepareOfflineLld()
            }
        } else {
            prepareOfflineLld()
        }
    }

    private suspend fun prepareOnlineLld(lldString: String) {
        try {
            val lld = lldString.fromJson<Lld>()
            val isSuccess = prepareDetect(lld)

            setSubtitle(isSuccess, lld, R.string.lld_json_online)

            try {
                JsonIo.saveLldJsonFile(lldString)
            } catch (e: Exception) {
                showError(R.string.lld_json_save_failed, e)
            }
        } catch (e: Exception) {
            showError(R.string.lld_json_parse_failed, e)

            prepareOfflineLld()
        }
    }

    private suspend fun prepareOfflineLld() {
        val lld = fetchOfflineLld()
        val isSuccess = prepareDetect(lld)

        setSubtitle(isSuccess, lld, R.string.lld_json_offline)
    }

    private fun fetchOfflineLld(): Lld? {
        val lld: Lld?

        try {
            JsonIo.copyJsonIfNeeded()
        } catch (e: Exception) {
            showError(R.string.lld_json_save_failed, e)

            lld = JsonIo.getAssetLld(MyApplication.instance.assets)
            return lld
        }

        lld = try {
            JsonIo.savedLldJsonFile.fromJson()
        } catch (e: Exception) {
            showError(R.string.lld_json_parse_failed, e)

            JsonIo.getAssetLld(MyApplication.instance.assets)
        }

        return lld
    }

    private suspend fun prepareDetect(lld: Lld?): Boolean {
        if (lld == null) {
            return false
        }

        return try {
            detect(lld)

            true
        } catch (e: Exception) {
            showError(R.string.lld_json_detect_failed, e)

            false
        }
    }

    private suspend fun setSubtitle(isSuccess: Boolean, lld: Lld?, @StringRes subtitleResId: Int) {
        val dataVersion = if (isSuccess) {
            lld!!.version.formatToLocalZonedDatetimeString()
        } else {
            MyApplication.getMyString(android.R.string.unknownName)
        }

        withContext(Dispatchers.Main) {
            _subtitle.value = Subtitle(subtitleResId, dataVersion)
        }
    }

    // https://unix.stackexchange.com/questions/91960/can-anyone-explain-the-output-of-mount
    private data class Mount(
        val blockDevice: String,
        val mountPoint: String,
        val type: String,
        val mountOptions: String,
        val dummy0: Int,
        val dummy1: Int
    )

    private fun getMounts(): List<Mount> {
        val mounts = ArrayList<Mount>()

        sh(CMD_MOUNT).output.forEach {
            val columns = it.split(" ")
            if (columns.size == 6) {
                mounts.add(
                    Mount(
                        columns[0],
                        columns[1],
                        columns[2],
                        columns[3],
                        columns[4].toInt(),
                        columns[5].toInt()
                    )
                )
            }
        }

        return mounts
    }

    // region [detect]
    private suspend fun detect(lld: Lld) {
        val tempModels = ArrayList<MyModel>()

        detectAndroid(tempModels, lld)

        detectBuildId(tempModels, lld)

        var securityPatch = if (isAtLeastStableAndroid6()) {
            Build.VERSION.SECURITY_PATCH
        } else {
            getStringProperty(PROP_SECURITY_PATCH)
        }
        detectSecurityPatch(tempModels, lld, securityPatch, R.string.security_patch_level_title)

        securityPatch = getStringProperty(PROP_VENDOR_SECURITY_PATCH, isAtLeastStableAndroid9())
        detectSecurityPatch(
            tempModels,
            lld,
            securityPatch,
            R.string.vendor_security_patch_level_title
        )

        detectPerformanceClass(tempModels)

        detectKernel(tempModels, lld)

        detectAb(tempModels)

        val mounts = getMounts()

        detectSar(tempModels, mounts)

        detectDynamicPartitions(tempModels)

        val isTrebleEnabled = detectTreble(tempModels)

        detectGsiCompatibility(tempModels, isTrebleEnabled)

        detectDsu(tempModels)

        detectMainline(tempModels, lld)

        detectVndk(tempModels, lld)

        detectApex(tempModels, mounts)

        detectDeveloperOptions(tempModels)

        detectAdb(tempModels)

        detectAdbAuthentication(tempModels)

        detectEncryption(tempModels)

        detectSELinux(tempModels)

        detectToybox(tempModels, lld)

        detectWebView(tempModels, lld)

        detectOutdatedTargetSdkVersionApk(tempModels, lld)

        setModels(tempModels)
    }

    private fun detectAndroid(tempModels: ArrayList<MyModel>, lld: Lld) {
        @AttrRes val androidColor = when {
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorNoProblem
            isSupportedByUpstreamAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        val previewVersion = lld.android.preview.version
        val previewApi = lld.android.preview.api
        val previewPhase = lld.android.preview.phase

        var myAndroidVersionName = getAndroidVersionName()
        if (isGoEdition()) {
            myAndroidVersionName += " (Go)"
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
                    myAndroidVersionName,
                    getAndroidApiLevel()
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
                MyApplication.getMyString(
                    R.string.android_info,
                    "$previewVersion $previewPhase",
                    previewApi
                ),
                MyApplication.getMyString(
                    R.string.android_info,
                    lld.android.internal.version,
                    lld.android.internal.api
                )
            ),
            androidColor
        )
    }

    private fun detectBuildId(tempModels: ArrayList<MyModel>, lld: Lld) {
        val buildIdResult = Build.ID
        val systemBuildIdResult =
            getStringProperty(PROP_RO_SYSTEM_BUILD_ID, isAtLeastStableAndroid9())
        val vendorBuildIdResult =
            getStringProperty(PROP_RO_VENDOR_BUILD_ID, isAtLeastStableAndroid9())
        val odmBuildIdResult = getStringProperty(PROP_RO_ODM_BUILD_ID, isAtLeastStableAndroid9())

        fun isBuildIdAndroid8CtsFormat(buildId: String) =
            isAtLeastStableAndroid8() && buildId.split(BUILD_ID_SEPARATOR).size > 2

        val buildIdForCompare = if (
            isBuildIdAndroid8CtsFormat(buildIdResult) || isAtLeastStableAndroid9().not()
        ) {
            buildIdResult
        } else {
            systemBuildIdResult
        }

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

        val firstDetailId = details[0].id
        fun getDate(buildId: String) = buildId.split(BUILD_ID_SEPARATOR)[1]
        fun isUpdateToLatestStable() = buildIdForCompare.first() > firstDetailId.first()
        fun isDateHigherThanConfig() = (
                isLatestStableAndroid(lld)
                        && isBuildIdAndroid8CtsFormat(buildIdForCompare)
                        && getDate(buildIdForCompare) >= getDate(firstDetailId)
                )
                || isUpdateToLatestStable()
                || isLatestPreviewAndroid(lld)

        @AttrRes val buildIdColor = when {
            isDateHigherThanConfig() -> R.attr.colorNoProblem
            isLatestStableAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
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
        @AttrRes val securityPatchColor = when {
            !isPropertyValueNotEmpty(securityPatch) -> R.attr.colorCritical
            securityPatch >= lldSecurityPatch -> R.attr.colorNoProblem
            getSecurityPatchYearMonth(securityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> R.attr.colorWaring
            else -> R.attr.colorCritical
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

    private fun detectPerformanceClass(tempModels: ArrayList<MyModel>) {
        @AttrRes var performanceColorRes = R.attr.colorCritical

        val result = if (isAtLeastStableAndroid12()) {
            val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            if (performanceClass == Build.VERSION.SDK_INT) {
                performanceColorRes = R.attr.colorNoProblem
            } else if (performanceClass == Build.VERSION.SDK_INT - 1) {
                performanceColorRes = R.attr.colorWaring
            }

            if (performanceClass != 0) {
                MyApplication.getMyString(
                    R.string.performance_class_detail_api, performanceClass
                )
            } else {
                MyApplication.getMyString(R.string.result_not_supported)
            }
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.performance_class_title),
            result,
            performanceColorRes
        )
    }

    private fun detectKernel(tempModels: ArrayList<MyModel>, lld: Lld) {
        val linuxVersionString = System.getProperty(SYSTEM_PROPERTY_LINUX_VERSION)
        val linuxVersion = Version(linuxVersionString)

        @AttrRes var linuxColor = R.attr.colorCritical

        val versionsSupported = lld.linux.google.versions
        versionsSupported.forEach {
            if (linuxVersion.major == Version(it).major
                && linuxVersion.minor == Version(it).minor
            ) {
                linuxColor = if (linuxVersion.isAtLeast(it)) {
                    R.attr.colorNoProblem
                } else {
                    R.attr.colorWaring
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
        val isAbUpdateSupported = getBooleanProperty(PROP_AB_UPDATE, isAtLeastStableAndroid7())

        val slotSuffixResult = getStringProperty(PROP_SLOT_SUFFIX, isAtLeastStableAndroid7())

        val isVirtualAb = getBooleanProperty(PROP_VIRTUAL_AB_ENABLED, isAtLeastStableAndroid11())

        val isAbEnable =
            isAbUpdateSupported || isPropertyValueNotEmpty(slotSuffixResult) || isVirtualAb

        var abResult = translate(isAbEnable)

        if (isAbEnable) {
            if (isVirtualAb) {
                val isVirtualAbRetrofit =
                    getBooleanProperty(PROP_VIRTUAL_AB_RETROFIT, isAtLeastStableAndroid11())

                abResult += MyApplication.getMyString(
                    if (isVirtualAbRetrofit) {
                        R.string.virtual_ab_retrofit
                    } else {
                        R.string.virtual_ab
                    }
                )
            }

            abResult += MyApplication.getMyString(
                R.string.current_using_ab_slot_result,
                slotSuffixResult
            )
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.ab_seamless_update_status_title),
            abResult,
            isAbEnable
        )
    }

    private fun detectSar(tempModels: ArrayList<MyModel>, mounts: List<Mount>) {
        var isTheLegacySar = false
        var isThe2siSar = false
        var isRecoverySar = false
        var isSlashSar = false

        val isSar = if (isAtLeastStableAndroid9()) {
            val isLAndroid9TheLegacySar = getBooleanProperty(PROP_SYSTEM_ROOT_IMAGE)

            val isTheLegacySarMount =
                mounts.any { it.blockDevice == "/dev/root" && it.mountPoint == "/" }

            isTheLegacySar = isLAndroid9TheLegacySar && isTheLegacySarMount

            isThe2siSar = isAtLeastStableAndroid10()
                    && mounts.none { it.blockDevice != "none" && it.mountPoint == "/system" && it.type != "tmpfs" }

            isRecoverySar = mounts.any { it.mountPoint == "/system_root" && it.type != "tmpfs" }

            isSlashSar = mounts.any { it.mountPoint == "/" && it.type != "rootfs" }

            isTheLegacySar || isThe2siSar || isRecoverySar || isSlashSar || isAtLeastStableAndroid10()
        } else {
            false
        }

        var result = translate(isSar)

        @AttrRes var color = R.attr.colorCritical
        @StringRes val sarTypeRes: Int

        if (isSar) {
            when {
                isTheLegacySar -> {
                    color = R.attr.colorWaring
                    sarTypeRes = R.string.sar_type_legacy
                }
                isThe2siSar -> {
                    color = R.attr.colorNoProblem
                    sarTypeRes = R.string.sar_type_2si
                }
                isRecoverySar -> {
                    color = R.attr.colorWaring
                    sarTypeRes = R.string.sar_type_recovery
                }
                isSlashSar -> {
                    color = R.attr.colorNoProblem
                    sarTypeRes = R.string.sar_type_slash
                }
                else -> {
                    color = R.attr.colorCritical
                    sarTypeRes = android.R.string.unknownName
                }
            }

            val sarType = MyApplication.getMyString(sarTypeRes)
            result += " ($sarType)"
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.sar_status_title),
            result,
            color
        )
    }

    private fun detectDynamicPartitions(tempModels: ArrayList<MyModel>) {
        val isDynamicPartitions =
            getBooleanProperty(PROP_DYNAMIC_PARTITIONS, isAtLeastStableAndroid10())
        val isDynamicPartitionsRetrofit =
            getBooleanProperty(PROP_DYNAMIC_PARTITIONS_RETROFIT, isAtLeastStableAndroid10())

//        val superPartitionResult = sh(CMD_LL_DEV_BLOCK_SUPER, isAtLeastStableAndroid10())
//        val hasSuperPartition = isShellResultSuccessful(superPartitionResult)

        val isDynamicPartitionsEnabled =
            isDynamicPartitions || isDynamicPartitionsRetrofit // || hasSuperPartition

        var detail = translate(isDynamicPartitionsEnabled)
        if (isDynamicPartitionsRetrofit) {
            detail += MyApplication.getMyString(
                R.string.dynamic_partitions_enabled_retrofitted
            )
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.dynamic_partitions_status_title),
            detail,
            isDynamicPartitionsEnabled
        )
    }

    private fun detectTreble(tempModels: ArrayList<MyModel>): Boolean {
        val isTrebleEnabled = getBooleanProperty(PROP_TREBLE_ENABLED, isAtLeastStableAndroid8())

        var trebleResult = translate(isTrebleEnabled)

        val pathVendorSku = String.format(
            PATH_VENDOR_VINTF_SKU,
            getStringProperty(PROP_VENDOR_SKU, isAtLeastStableAndroid12())
        )

        @AttrRes val trebleColor = if (isTrebleEnabled) {
            when {
                File(pathVendorSku).exists()
                        || File(PATH_VENDOR_VINTF).exists()
                        || File(PATH_VENDOR_VINTF_FRAGMENTS).exists() -> {
                    R.attr.colorNoProblem
                }
                File(PATH_VENDOR_LEGACY_NO_FRAGMENTS).exists() -> {
                    trebleResult += MyApplication.getMyString(R.string.treble_legacy_no_fragments)

                    R.attr.colorWaring
                }
                else -> {
                    trebleResult += MyApplication.getMyString(R.string.treble_other)

                    R.attr.colorWaring
                }
            }
        } else {
            R.attr.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.treble_status_title),
            trebleResult,
            trebleColor
        )

        return trebleColor != R.attr.colorCritical
    }

    private fun detectGsiCompatibility(
        tempModels: ArrayList<MyModel>, isTrebleEnabled: Boolean
    ) {
        val fileLdConfig = when {
            isAtLeastStableAndroid11() -> LD_CONFIG_FILE_ANDROID_11
            isAtLeastStableAndroid9() -> LD_CONFIG_FILE_ANDROID_9
            else -> "NOT_EXIST"
        }
        val cmd = String.format(CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED, fileLdConfig)
        val gsiCompatibilityResult = sh(cmd, isAtLeastStableAndroid9())
        val (@StringRes result, @AttrRes color) = if (gsiCompatibilityResult.isSuccess) {
            val lineResult = gsiCompatibilityResult.output[0].split('=')
            val isCompatible = lineResult.isNotEmpty() && lineResult[1].trim().toBoolean()
            if (isCompatible) {
                Pair(R.string.result_compliant, R.attr.colorNoProblem)
            } else {
                Pair(R.string.result_not_compliant, R.attr.colorWaring)
            }
        } else {
            if (isTrebleEnabled && isAtLeastStableAndroid9()) {
                Pair(R.string.result_unidentified, R.attr.colorWaring)
            } else {
                Pair(R.string.result_not_supported, R.attr.colorCritical)
            }
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.gsi_status_title),
            MyApplication.getMyString(result),
            color
        )
    }

    /** {@link android.util.FeatureFlagUtils} */
    private fun detectDsu(tempModels: ArrayList<MyModel>) {
        val isDsuEnabled =
            getBooleanProperty(PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE, isAtLeastStableAndroid10())
                    || getBooleanProperty(PROP_DYNAMIC_SYSTEM_UPDATE, isAtLeastStableAndroid10())
        add(
            tempModels,
            MyApplication.getMyString(R.string.dsu_status_title),
            translate(isDsuEnabled),
            isDsuEnabled
        )
    }

    /**
     * {@link com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController}
     */
    private fun detectMainline(tempModels: ArrayList<MyModel>, lld: Lld) {
        var versionName = MyApplication.getMyString(R.string.result_not_supported)
        var moduleProvider = MyApplication.getMyString(android.R.string.unknownName)
        val latestGooglePlaySystemUpdates = lld.android.googlePlaySystemUpdates

        fun getResult() = MyApplication.getMyString(
            R.string.mainline_detail,
            versionName, moduleProvider, latestGooglePlaySystemUpdates
        )

        @SuppressLint("DiscouragedApi")
        // com.android.internal.R.string.config_defaultModuleMetadataProvider
        val idConfigDefaultModuleMetadataProvider = Resources.getSystem().getIdentifier(
            "config_defaultModuleMetadataProvider", "string", "android"
        )

        @AttrRes var moduleColor = R.attr.colorCritical

        val result = if (idConfigDefaultModuleMetadataProvider != 0) {
            try {
                // com.android.modulemetadata
                // com.google.android.modulemetadata
                moduleProvider = MyApplication.getMyString(
                    idConfigDefaultModuleMetadataProvider
                )

                val packageManager = MyApplication.instance.packageManager
                val packageInfo = if (isAtLeastStableAndroid13()) {
                    val flags = PackageManager.PackageInfoFlags.of(0)
                    packageManager.getPackageInfo(moduleProvider, flags)
                } else {
                    packageManager.getPackageInfo(moduleProvider, 0)
                }
                versionName = packageInfo?.versionName ?: ""

                if (versionName >= latestGooglePlaySystemUpdates
                    || "$versionName-01" >= latestGooglePlaySystemUpdates
                ) {
                    moduleColor = R.attr.colorNoProblem
                }

                getResult()
            } catch (e: Exception) {
                Log.d(javaClass.simpleName, "Failed to get mainline version.", e)
                getResult()
            }
        } else {
            getResult()
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.mainline_title),
            result,
            moduleColor
        )
    }

    private fun detectVndk(tempModels: ArrayList<MyModel>, lld: Lld) {
        val vndkVersionResult =
            getStringProperty(PROP_VNDK_VENDOR_VERSION, isAtLeastStableAndroid8())
//        val vndkProductVersionResult =
//            getStringProperty(PROP_VNDK_PRODUCT_VERSION, isAtLeastStableAndroid8())
        val hasVndkVersion = isPropertyValueNotEmpty(vndkVersionResult)

        @AttrRes val vndkColor: Int

        var isVndkBuiltInResult = translate(hasVndkVersion)
        if (hasVndkVersion) {
            val hasVndkLite = getBooleanProperty(PROP_VNDK_LITE)

            vndkColor = if (
                (isLatestPreviewAndroid(lld) || vndkVersionResult >= lld.android.stable.api) && !hasVndkLite
            ) {
                R.attr.colorNoProblem
            } else {
                R.attr.colorWaring
            }

            isVndkBuiltInResult += MyApplication.getMyString(
                R.string.built_in_vndk_version_result,
                if (hasVndkLite) "$vndkVersionResult, Lite" else vndkVersionResult
            )
        } else {
            vndkColor = R.attr.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.vndk_built_in_title),
            isVndkBuiltInResult,
            vndkColor
        )
    }

    private fun detectApex(tempModels: ArrayList<MyModel>, mounts: List<Mount>) {
        val apexUpdatable = getBooleanProperty(PROP_APEX_UPDATABLE, isAtLeastStableAndroid10())

        val isFlattenedApexMounted = isAtLeastStableAndroid10() && mounts.any {
            it.mountPoint.startsWith("/apex/") && it.mountPoint.contains("@")
        }

        val isApex = apexUpdatable || isFlattenedApexMounted
        val isLegacyFlattenedApex = !apexUpdatable && isFlattenedApexMounted

        var apexEnabledResult = translate(isApex)
        if (isLegacyFlattenedApex) {
            apexEnabledResult += MyApplication.getMyString(R.string.apex_legacy_flattened)
        }

        @AttrRes val apexColor = when {
            apexUpdatable -> R.attr.colorNoProblem
            isLegacyFlattenedApex -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.apex_status_title),
            apexEnabledResult,
            apexColor
        )
    }

    private fun detectDeveloperOptions(tempModels: ArrayList<MyModel>) {
        val isDeveloperOptionsDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            SETTINGS_DISABLED
        ) == SETTINGS_DISABLED

        add(
            tempModels,
            MyApplication.getMyString(R.string.developer_options_status_title),
            translateDisabled(isDeveloperOptionsDisabled),
            isDeveloperOptionsDisabled
        )
    }

    private fun detectAdb(tempModels: ArrayList<MyModel>) {
        val isAdbDebuggingDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.ADB_ENABLED,
            SETTINGS_DISABLED
        ) == SETTINGS_DISABLED

        add(
            tempModels,
            MyApplication.getMyString(R.string.adb_debugging_status_title),
            translateDisabled(isAdbDebuggingDisabled),
            isAdbDebuggingDisabled
        )
    }

    private fun detectAdbAuthentication(tempModels: ArrayList<MyModel>) {
        val isAdbAuthenticationEnabled =
            getStringProperty(PROP_ADB_SECURE) == SETTINGS_ENABLED.toString()

        add(
            tempModels,
            MyApplication.getMyString(R.string.adb_authentication_status_title),
            translateEnabled(isAdbAuthenticationEnabled),
            isAdbAuthenticationEnabled
        )
    }

    private fun detectEncryption(tempModels: ArrayList<MyModel>) {
        // val cryptoState = getStringProperty(PROP_CRYPTO_STATE)
        val devicePolicyManager = ContextCompat.getSystemService(
            MyApplication.instance, DevicePolicyManager::class.java
        )
        val storageEncryptionStatus = devicePolicyManager?.storageEncryptionStatus
        @StringRes val result: Int
        @AttrRes val color: Int
        when (storageEncryptionStatus) {
            // DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> {
                result = R.string.result_encrypted
                color = R.attr.colorNoProblem
            }
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> {
                result = R.string.result_encrypted_no_key_set
                color = R.attr.colorWaring
            }
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> {
                result = R.string.result_not_encrypted
                color = R.attr.colorCritical
            }
            DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> {
                result = R.string.result_not_supported
                color = R.attr.colorCritical
            }
            else -> {
                result = R.string.result_not_supported
                color = R.attr.colorCritical
            }
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.encryption_status_title),
            MyApplication.getMyString(result),
            color
        )
    }

    private fun detectSELinux(tempModels: ArrayList<MyModel>) {
//        val seLinuxClass = Class.forName("android.os.SELinux")
//        val isSELinuxBooted = seLinuxClass
//            .getDeclaredMethod("isSELinuxEnabled")
//            .invoke(null) as Boolean
//        val isSELinuxEnforceBooted = seLinuxClass
//            .getDeclaredMethod("isSELinuxEnforced")
//            .invoke(null) as Boolean
//
//        val bootSELinuxProp = getStringProperty(PROP_BOOT_SELINUX)

        @StringRes val result: Int
        @AttrRes val color: Int

        val seLinuxStatus = sh(CMD_GETENFORCE)
        val seLinuxStatusResult = seLinuxStatus.output[0]

        if (seLinuxStatus.isSuccess) {
            when (seLinuxStatusResult) {
                SELINUX_STATUS_ENFORCING -> {
                    result = R.string.selinux_status_enforcing_mode
                    color = R.attr.colorNoProblem
                }
                SELINUX_STATUS_PERMISSIVE -> {
                    // val seLinuxPolicyVersion = sh(CMD_SELINUX_POLICY_VERSION, isAtLeastAndroid8())
                    result = R.string.selinux_status_permissive_mode
                    color = R.attr.colorWaring
                }
                SELINUX_STATUS_DISABLED -> {
                    result = R.string.result_disabled
                    color = R.attr.colorCritical
                }
                else -> {
                    result = android.R.string.unknownName
                    color = R.attr.colorCritical
                }
            }
        } else {
            if (seLinuxStatusResult.endsWith(CMD_ERROR_PERMISSION_DENIED)) {
                result = R.string.selinux_status_enforcing_mode
                color = R.attr.colorNoProblem
            } else {
                result = android.R.string.unknownName
                color = R.attr.colorCritical
            }
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.selinux_status),
            MyApplication.getMyString(result),
            color
        )
    }

    private fun detectToybox(tempModels: ArrayList<MyModel>, lld: Lld) {
        val toyboxVersionResult = sh(CMD_TOYBOX_VERSION, isAtLeastStableAndroid6())
        val hasToyboxVersion = toyboxVersionResult.isSuccess

        val toyboxVersion = if (hasToyboxVersion) {
            toyboxVersionResult.output[0]
        } else {
            translate(false)
        }

        @AttrRes val toyboxColor = if (hasToyboxVersion) {
            val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
            val toyboxRealVersion = Version(toyboxRealVersionString)
            when {
                toyboxRealVersion.isAtLeast(lld.toybox.stable.version) -> R.attr.colorNoProblem
                toyboxRealVersion.isAtLeast(lld.toybox.support.version) -> R.attr.colorWaring
                else -> R.attr.colorCritical
            }
        } else {
            R.attr.colorCritical
        }
        add(
            tempModels,
            MyApplication.getMyString(R.string.toybox_built_in_title),
            MyApplication.getMyString(
                R.string.toybox_built_in_detail,
                toyboxVersion,
                lld.toybox.stable.version,
                lld.toybox.support.version,
                lld.toybox.master.version
            ),
            toyboxColor
        )
    }

    private fun getPackageInfo(packageName: String) = try {
        val packageManager = MyApplication.instance.packageManager
        if (isAtLeastStableAndroid13()) {
            val flags = PackageManager.PackageInfoFlags.of(0)
            packageManager.getPackageInfo(packageName, flags)
        } else {
            packageManager.getPackageInfo(packageName, 0)
        }
    } catch (e: Exception) {
        Log.d(javaClass.simpleName, "$packageName not found.")
        null
    }

    private fun detectWebView(tempModels: ArrayList<MyModel>, lld: Lld) {
        val type = if (isAtLeastStableAndroid10()) {
            val standalone = MyApplication.getMyString(R.string.webview_standalone)
            MyApplication.getMyString(R.string.webview_or, "Trichrome", standalone)
        } else if (isAtLeastStableAndroid7()) {
            "Monochrome"
        } else {
            MyApplication.getMyString(R.string.webview_standalone)
        }

        val packageManager = MyApplication.instance.packageManager

        var builtInResult = ""
        var builtInVersionName = ""
        if (isAtLeastStableAndroid7()) {
            val webViewProviderInfoList = getBuildInWebViewProvidersAndroid7()

            val lastIndex = webViewProviderInfoList.size - 1
            webViewProviderInfoList.forEachIndexed { index, webViewProviderInfo ->
                builtInResult += with(webViewProviderInfo) {
                    val packageInfo = getPackageInfo(packageName)
                    val isInstalled = if (packageInfo != null) {
                        packageInfo.versionName?.also {
                            if (Version(it).isHigherThan(builtInVersionName)) {
                                builtInVersionName = it
                            }
                        } ?: MyApplication.getMyString(android.R.string.unknownName)
                    } else {
                        MyApplication.getMyString(R.string.webview_built_in_not_installed)
                    }

                    fun subFormat(text: String) = MyApplication.getMyString(
                        R.string.webview_sub_format, text
                    )

                    fun subFormat(@StringRes stringRes: Int) =
                        subFormat(MyApplication.getMyString(stringRes))

                    var tempResult = packageName +
                            subFormat(description) +
                            subFormat(isInstalled)
                    if (!availableByDefault) {
                        tempResult += subFormat(R.string.webview_must_be_chosen_by_user)
                    }
                    if (isFallback) {
                        tempResult += subFormat(R.string.webview_fallback)
                    }
                    if (signatures.isNotEmpty()) {
                        tempResult += subFormat(R.string.webview_signed)
                    }
                    if (index != lastIndex) {
                        tempResult += MyApplication.getMyString(R.string.webview_ending)
                    }
                    tempResult
                }
            }
        } else {
            val buildInPackageName = getBuildInWebViewProviderAndroid5()
            val buildInPackageInfo = getPackageInfo(buildInPackageName)
            val buildInLabel = buildInPackageInfo?.applicationInfo?.loadLabel(packageManager)
            val buildInVersionName = buildInPackageInfo?.versionName
            builtInResult = """
                |$buildInPackageName
                |  $buildInLabel
                |  $buildInVersionName
                """.trimMargin()
        }

        val implementPackageInfo =
            WebViewCompat.getCurrentWebViewPackage(MyApplication.instance)
        val implementPackageName = implementPackageInfo?.packageName
        val implementLabel = implementPackageInfo?.applicationInfo?.loadLabel(packageManager)
        val implementVersionName = implementPackageInfo?.versionName
        val implementResult = """
                |$implementPackageName
                |  $implementLabel
                |  $implementVersionName
                """.trimMargin()

        val latestResult = MyApplication.getMyString(
            R.string.webview_detail, lld.webView.stable.version, lld.webView.beta.version
        )

        val lldWebViewStable = lld.webView.stable.version
        @AttrRes val webViewColor = when {
            Version(builtInVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorNoProblem
            Version(implementVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        add(
            tempModels,
            MyApplication.getMyString(R.string.webview_title),
            """
            |${MyApplication.getMyString(R.string.webview_built_in_version)}: $type
            |
            |$builtInResult
            |
            |${MyApplication.getMyString(R.string.webview_implement_version)}:
            |$implementResult
            |
            |$latestResult
            """.trimMargin(),
            webViewColor
        )
    }

    private fun getBuildInWebViewProviderAndroid5(): String {
        @SuppressLint("DiscouragedApi")
        // frameworks/base/core/res/res/values/config.xml
        //     com.android.internal.R.string.config_webViewPackageName
        val idConfigWebViewPackageName = Resources.getSystem().getIdentifier(
            "config_webViewPackageName", "string", "android"
        )

        return MyApplication.getMyString(idConfigWebViewPackageName)
    }

    private class WebViewProviderInfo(
        val packageName: String,
        val description: String,
        val availableByDefault: Boolean,
        val isFallback: Boolean,
        val signatures: Array<*>
    )

    @SuppressLint("PrivateApi")
    private fun getBuildInWebViewProvidersAndroid7(): List<WebViewProviderInfo> {
        // frameworks/base/core/res/res/xml/config_webview_packages.xml
        val webViewUpdateServiceClass = Class.forName("android.webkit.WebViewUpdateService")
        val allWebViewPackages =
            webViewUpdateServiceClass.getDeclaredMethod("getAllWebViewPackages")
                .invoke(null) as Array<*>

        fun Any?.getWebViewProviderInfoMember(name: String) =
            Class.forName("android.webkit.WebViewProviderInfo").getDeclaredField(name).get(this)

        return allWebViewPackages.map {
            with(it) {
                val packageName = getWebViewProviderInfoMember("packageName") as String
                val description = getWebViewProviderInfoMember("description") as String
                val availableByDefault =
                    getWebViewProviderInfoMember("availableByDefault") as Boolean
                val isFallback = getWebViewProviderInfoMember("isFallback") as Boolean
                val signatures = getWebViewProviderInfoMember("signatures") as Array<*>
                WebViewProviderInfo(
                    packageName, description, availableByDefault, isFallback, signatures
                )
            }
        }
    }

    private fun getOutdatedTargetSdkVersionApkModel(lld: Lld): MyModel {
        val packageManager = MyApplication.instance.packageManager
        val installedApplications = if (isAtLeastStableAndroid13()) {
            val flags = PackageManager.ApplicationInfoFlags.of(0)
            packageManager.getInstalledApplications(flags)
        } else {
            packageManager.getInstalledApplications(0)
        }
        var systemApkList = installedApplications.filter {
            (it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0)
                    && (it.targetSdkVersion < Build.VERSION.SDK_INT
                    /* */ || (isPreviewAndroid() && it.targetSdkVersion == Build.VERSION.SDK_INT)
                    )
        }

        var result = MyApplication.getMyString(
            R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
            getStringProperty(PROP_RO_PRODUCT_FIRST_API_LEVEL)
        )

        @AttrRes val targetSdkVersionColor = if (systemApkList.isEmpty()) {
            result += MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_result_none)

            if (isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld)) {
                R.attr.colorNoProblem
            } else {
                R.attr.colorWaring
            }
        } else {
            val shouldOrderByPackageNameFirst = MyApplication.sharedPreferences.getBoolean(
                MyApplication.getMyString(R.string.function_outdated_target_order_by_package_name_first_key),
                false
            )

            @StringRes val format = if (shouldOrderByPackageNameFirst) {
                systemApkList = systemApkList.sortedBy(ApplicationInfo::packageName)

                R.string.outdated_target_version_sdk_version_apk_result_format_package_first
            } else {
                systemApkList = systemApkList.sortedWith(
                    compareBy(ApplicationInfo::targetSdkVersion, ApplicationInfo::packageName)
                )

                R.string.outdated_target_version_sdk_version_apk_result_format_target_sdk_version_first
            }

            systemApkList.forEachIndexed { index, applicationInfo ->
                result += "- " + MyApplication.getMyString(
                    format, applicationInfo.packageName, applicationInfo.targetSdkVersion
                )

                if (index != systemApkList.size - 1) {
                    result += "\n"
                }
            }

            R.attr.colorCritical
        }

        return MyModel(
            MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_title),
            result,
            targetSdkVersionColor
        )
    }

    private fun detectOutdatedTargetSdkVersionApk(tempModels: ArrayList<MyModel>, lld: Lld) {
        val myModel = getOutdatedTargetSdkVersionApkModel(lld)

        add(tempModels, myModel.title, myModel.detail, myModel.color)
    }

    fun payloadOutdatedTargetSdkVersionApk(myModels: ArrayList<MyModel>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (myModels.isEmpty()) {
                return@launch
            }

            val lld = fetchOfflineLld()
                ?: return@launch
            myModels.last().detail = getOutdatedTargetSdkVersionApkModel(lld).detail

            withContext(Dispatchers.Main) {
                _showOutdatedOrderEvent.value = Event(Unit)
            }
        }
    // endregion [detect]

    private fun isPropertyValueNotEmpty(result: String) =
        result.isNotEmpty()
                && result != MyApplication.getMyString(R.string.result_not_supported)
                && result != MyApplication.getMyString(R.string.build_not_filled)

    private fun translate(condition: Boolean) = MyApplication.getMyString(
        if (condition) {
            R.string.result_supported
        } else {
            R.string.result_not_supported
        }
    )

    private fun translateEnabled(condition: Boolean) = translateDisabled(!condition)

    private fun translateDisabled(condition: Boolean) = MyApplication.getMyString(
        if (condition) {
            R.string.result_disabled
        } else {
            R.string.result_enabled
        }
    )

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
                R.attr.colorNoProblem
            } else {
                R.attr.colorCritical
            }
        )
    )

    private fun add(
        tempModels: ArrayList<MyModel>,
        title: String,
        detail: String?,
        @AttrRes color: Int
    ) = tempModels.add(MyModel(title, detail.toString(), color))
}