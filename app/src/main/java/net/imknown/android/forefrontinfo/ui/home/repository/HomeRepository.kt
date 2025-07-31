package net.imknown.android.forefrontinfo.ui.home.repository

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
import androidx.webkit.WebViewCompat
import io.github.g00fy2.versioncompare.Version
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.toColoredMyModel
import net.imknown.android.forefrontinfo.ui.common.getAndroidApiLevel
import net.imknown.android.forefrontinfo.ui.common.getAndroidVersionName
import net.imknown.android.forefrontinfo.ui.common.getBooleanProperty
import net.imknown.android.forefrontinfo.ui.common.getShellResult
import net.imknown.android.forefrontinfo.ui.common.getStringProperty
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid10
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid11
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid12
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid13
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid6
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid7
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid8
import net.imknown.android.forefrontinfo.ui.common.isAtLeastStableAndroid9
import net.imknown.android.forefrontinfo.ui.common.isGoEdition
import net.imknown.android.forefrontinfo.ui.common.isLatestPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isLatestStableAndroid
import net.imknown.android.forefrontinfo.ui.common.isPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isSupportedByUpstreamAndroid
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import java.io.File

class HomeRepository(
    private val lldDataSource: LldDataSource,
    private val mountDataSource: MountDataSource,
    private val appInfoDataSource: AppInfoDataSource
) {
    fun fetchOfflineLldFileOrThrow() = lldDataSource.fetchOfflineLldFileOrThrow()
    suspend fun fetchOnlineLldJsonStringOrThrow() = lldDataSource.fetchOnlineLldJsonStringOrThrow()

    fun detectMode(lld: Lld?, modeResId: Int): MyModel {
        @AttrRes val color: Int
        val datetimeFormatted: String
        if (lld != null) {
            datetimeFormatted = lld.version.formatToLocalZonedDatetimeString()
            color = if (modeResId == R.string.lld_json_online) {
                R.attr.colorNoProblem
            } else {
                R.attr.colorCritical
            }
        } else {
            datetimeFormatted = MyApplication.getMyString(android.R.string.unknownName)
            color = R.attr.colorCritical
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.lld_json_mode_title),
            MyApplication.getMyString(modeResId, datetimeFormatted),
            color
        )
    }

    fun detectAndroid(lld: Lld): MyModel {
        // region [Mine]
        var myAndroidVersionName = getAndroidVersionName()
        if (MyApplication.instance.isGoEdition()) {
            myAndroidVersionName += " (Go)"
        }
        val mine = MyApplication.getMyString(R.string.android_info, myAndroidVersionName, getAndroidApiLevel())
        // endregion [Mine]

        // region [LatestStable]
        val stable = lld.android.stable
        val latestStable = MyApplication.getMyString(R.string.android_info, stable.version, stable.api)
        // endregion [LatestStable]

        // region [LowestSupport]
        val support = lld.android.support
        val lowestSupport = MyApplication.getMyString(R.string.android_info, support.version, support.api)
        // endregion [LowestSupport]

        // region [Beta]
        val lldStablePreview = lld.android.stablePreview
        val stablePreviewVersion = lldStablePreview.version
        val stablePreviewApi = lldStablePreview.api
        val stablePreview = MyApplication.getMyString(R.string.android_info, stablePreviewVersion, stablePreviewApi)
        // endregion [Beta]

        // region [Canary]
        val lldPreview = lld.android.preview
        val previewVersion = lldPreview.version
        val previewApi = lldPreview.api
        val latestPreview = MyApplication.getMyString(R.string.android_info, previewVersion, previewApi)
        // endregion [Canary]

        // region [LatestInternal]
        val internal = lld.android.internal
        val latestInternal = MyApplication.getMyString(R.string.android_info, internal.version, internal.api)
        // endregion [LatestInternal]

        @AttrRes val color = when {
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorNoProblem
            isSupportedByUpstreamAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        val infoDetailArgs = arrayOf(mine, latestStable, lowestSupport, stablePreview, latestPreview, latestInternal)
        return toColoredMyModel(
            MyApplication.getMyString(R.string.android_info_title),
            MyApplication.getMyString(R.string.android_info_detail, *infoDetailArgs),
            color
        )
    }

    fun detectBuildId(lld: Lld): MyModel {
        val buildIdResult = Build.ID
        val systemBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_SYSTEM_BUILD_ID, isAtLeastStableAndroid9())
        val vendorBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_VENDOR_BUILD_ID, isAtLeastStableAndroid9())
        val odmBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_ODM_BUILD_ID, isAtLeastStableAndroid9())

        fun isBuildIdAndroid8CtsFormat(buildId: String) =
            isAtLeastStableAndroid8() && buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR).size >= 3

        val myBuildIdForCompare = if (
            isBuildIdAndroid8CtsFormat(buildIdResult) || isAtLeastStableAndroid9().not()
        ) {
            buildIdResult
        } else {
            systemBuildIdResult
        }

        var builds = ""
        val build = lld.android.build
        val lldDetails = build.details
        lldDetails.forEachIndexed { index, detail ->
            builds += MyApplication.getMyString(R.string.android_build_id, detail.id, detail.revision)

            if (index != lldDetails.size - 1) {
                builds += "\n"
            }
        }

        // region [Color]
        val lldFirstBuildId = lldDetails[0].id
        fun getDate(buildId: String) = buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR)[1] // "250705"
        fun isDateHigherThanConfig(): Boolean {
            if (!isBuildIdAndroid8CtsFormat(myBuildIdForCompare)) {
                return false
            }

            val myBuildIdDate = getDate(myBuildIdForCompare)
            val myBuildIdDateIntOrNull = myBuildIdDate.toIntOrNull()
            val lldFirstBuildIdDate = getDate(lldFirstBuildId)
            val lldFirstBuildIdDateIntOrNull = lldFirstBuildIdDate.toIntOrNull()
            if (myBuildIdDateIntOrNull == null || lldFirstBuildIdDateIntOrNull == null) {
                return false
            }

            val offset = if (isLatestStableAndroid(lld)) {
               0
            } else {
                /** [isLatestPreviewAndroid] */
                250205 - 250101
            }

            return (myBuildIdDateIntOrNull + offset) >= lldFirstBuildIdDateIntOrNull
        }

        @AttrRes val buildIdColor = when {
            isDateHigherThanConfig() -> R.attr.colorNoProblem
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }
        // endregion [Color]

        return toColoredMyModel(
            MyApplication.getMyString(R.string.android_build_id_title),
            MyApplication.getMyString(R.string.android_build_id_detail, buildIdResult, systemBuildIdResult, vendorBuildIdResult, odmBuildIdResult, build.version, builds),
            buildIdColor
        )
    }

    // region [SecurityPatch]
    fun detectSecurityPatches(lld: Lld): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        val mySecurityPatch = if (isAtLeastStableAndroid6()) {
            Build.VERSION.SECURITY_PATCH
        } else {
            getStringProperty(AndroidDataSource.PROP_SECURITY_PATCH)
        }
        tempModels += detectSecurityPatch(lld, mySecurityPatch, R.string.security_patch_level_title)

        val mySecurityPatchVendor = getStringProperty(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, isAtLeastStableAndroid9())
        tempModels += detectSecurityPatch(lld, mySecurityPatchVendor, R.string.vendor_security_patch_level_title)

        return tempModels
    }

    fun detectSecurityPatch(lld: Lld, mySecurityPatch: String, @StringRes titleId: Int): MyModel {
        val lldSecurityPatch = lld.android.securityPatchLevel
        @AttrRes val securityPatchColor = when {
            !isPropertyValueNotEmpty(mySecurityPatch) -> R.attr.colorCritical
            mySecurityPatch >= lldSecurityPatch -> R.attr.colorNoProblem
            getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        return toColoredMyModel(
            MyApplication.getMyString(titleId),
            MyApplication.getMyString(R.string.security_patch_level_detail, mySecurityPatch, lldSecurityPatch),
            securityPatchColor
        )
    }

    private fun getSecurityPatchYearMonth(securityPatch: String) =
        securityPatch.substringBeforeLast('-')
    // endregion [SecurityPatch]

    fun detectPerformanceClass(): MyModel {
        @AttrRes var performanceColorRes = R.attr.colorCritical

        val result = if (isAtLeastStableAndroid12()) {
            val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            if (performanceClass == Build.VERSION.SDK_INT) {
                performanceColorRes = R.attr.colorNoProblem
            } else if (performanceClass == Build.VERSION.SDK_INT - 1) {
                performanceColorRes = R.attr.colorWaring
            }

            if (performanceClass != 0) {
                MyApplication.getMyString(R.string.performance_class_detail_api, performanceClass)
            } else {
                MyApplication.getMyString(R.string.result_not_supported)
            }
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.performance_class_title),
            result,
            performanceColorRes
        )
    }

    fun detectKernel(lld: Lld): MyModel {
        val linuxVersionString = System.getProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION)
        val linuxVersion = Version(linuxVersionString)

        @AttrRes var linuxColor = R.attr.colorCritical

        val linux = lld.linux
        val versionsSupported = linux.google.versions
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

        return toColoredMyModel(
            MyApplication.getMyString(R.string.linux_title),
            MyApplication.getMyString(R.string.linux_version_detail, linuxVersionString, versionsSupported.joinToString("｜"), linux.mainline.version),
            linuxColor
        )
    }

    fun detectAb(): MyModel {
        val isAbUpdateSupported = getBooleanProperty(AndroidDataSource.PROP_AB_UPDATE, isAtLeastStableAndroid7())
        val slotSuffixResult = getStringProperty(AndroidDataSource.PROP_SLOT_SUFFIX, isAtLeastStableAndroid7())
        val isVirtualAb = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_ENABLED, isAtLeastStableAndroid11())
        val isAbEnable = isAbUpdateSupported || isPropertyValueNotEmpty(slotSuffixResult) || isVirtualAb

        var abResult = toSupportOrNotString(isAbEnable)

        if (isAbEnable) {
            if (isVirtualAb) {
                val isVirtualAbRetrofit = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_RETROFIT, isAtLeastStableAndroid11())
                // val isVirtualAbCompressionXorEnabled = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_COMPRESSION_XOR_ENABLED, isAtLeastStableAndroid13())
                // val isVirtualAbUserspaceSnapshotsEnabled = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_USERSPACE_SNAPSHOTS_ENABLED, isAtLeastStableAndroid13())
                // val isAllowNonAb = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_ALLOW_NON_AB, isAtLeastStableAndroid13())
                // val isCompressionEnabled = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_COMPRESSION_ENABLED, isAtLeastStableAndroid13())
                // val isIoUringEnabled = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_IO_URING_ENABLED, isAtLeastStableAndroid13())

                abResult += MyApplication.getMyString(
                    if (isVirtualAbRetrofit) {
                        R.string.virtual_ab_retrofit
                    } else {
                        R.string.virtual_ab
                    }
                )
            }

            abResult += MyApplication.getMyString(R.string.current_using_ab_slot_result, slotSuffixResult)
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.ab_seamless_update_status_title),
            abResult,
            isAbEnable
        )
    }

    private val mounts by lazy { mountDataSource.getMounts() }

    fun detectSar(): MyModel {
        var isTheLegacySar = false
        var isThe2siSar = false
        var isRecoverySar = false
        var isSlashSar = false

        val isSar = if (isAtLeastStableAndroid9()) {
            val isLAndroid9TheLegacySar = getBooleanProperty(AndroidDataSource.PROP_SYSTEM_ROOT_IMAGE)

            val isTheLegacySarMount = mounts.any {
                it.blockDevice == "/dev/root" && it.mountPoint == "/"
            }

            isTheLegacySar = isLAndroid9TheLegacySar && isTheLegacySarMount

            isThe2siSar = isAtLeastStableAndroid10()
                    && mounts.none {
                        it.blockDevice != "none" && it.mountPoint == "/system" && it.type != "tmpfs"
                    }

            isRecoverySar = mounts.any {
                it.mountPoint == "/system_root" && it.type != "tmpfs"
            }

            isSlashSar = mounts.any {
                it.mountPoint == "/" && it.type != "rootfs"
            }

            isTheLegacySar || isThe2siSar || isRecoverySar || isSlashSar || isAtLeastStableAndroid10()
        } else {
            false
        }

        var result = toSupportOrNotString(isSar)

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

        return toColoredMyModel(
            MyApplication.getMyString(R.string.sar_status_title),
            result,
            color
        )
    }

    fun detectDynamicPartitions(): MyModel {
        val isDynamicPartitions =
            getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_PARTITIONS, isAtLeastStableAndroid10())
        val isDynamicPartitionsRetrofit =
            getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_PARTITIONS_RETROFIT, isAtLeastStableAndroid10())

//        val superPartitionResult = sh(CMD_LL_DEV_BLOCK_SUPER, isAtLeastStableAndroid10())
//        val hasSuperPartition =  superPartitionResult.isSuccess

        val isDynamicPartitionsEnabled = isDynamicPartitions || isDynamicPartitionsRetrofit // || hasSuperPartition

        var detail = toSupportOrNotString(isDynamicPartitionsEnabled)
        if (isDynamicPartitionsRetrofit) {
            detail += MyApplication.getMyString(
                R.string.dynamic_partitions_enabled_retrofitted
            )
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.dynamic_partitions_status_title),
            detail,
            isDynamicPartitionsEnabled
        )
    }

    // region [Treble & GSI]
    private fun detectTreble(): Pair<MyModel, Boolean> {
        val isTrebleEnabled = getBooleanProperty(AndroidDataSource.PROP_TREBLE_ENABLED, isAtLeastStableAndroid8())

        var trebleResult = toSupportOrNotString(isTrebleEnabled)

        val pathVendorSku = String.format(
            AndroidDataSource.PATH_VENDOR_VINTF_SKU,
            getStringProperty(AndroidDataSource.PROP_VENDOR_SKU, isAtLeastStableAndroid12())
        )

        @AttrRes val trebleColor = if (isTrebleEnabled) {
            when {
                File(pathVendorSku).exists()
                        || File(AndroidDataSource.PATH_VENDOR_VINTF).exists()
                        || File(AndroidDataSource.PATH_VENDOR_VINTF_FRAGMENTS).exists() -> {
                    R.attr.colorNoProblem
                }
                File(AndroidDataSource.PATH_VENDOR_LEGACY_NO_FRAGMENTS).exists() -> {
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

        val myModel = toColoredMyModel(
            MyApplication.getMyString(R.string.treble_status_title),
            trebleResult,
            trebleColor
        )

        val isTrebleSupported = trebleColor != R.attr.colorCritical

        return myModel to isTrebleSupported
    }

    fun detectTrebleAndGsiCompatibility(): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        val (myModel, isTrebleEnabled) = detectTreble()
        tempModels += myModel

        val fileLdConfig = when {
            isAtLeastStableAndroid11() -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_11
            isAtLeastStableAndroid9() -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_9
            else -> "NOT_EXIST"
        }
        val cmd = String.format(AndroidDataSource.CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED, fileLdConfig)
        val gsiCompatibilityResult = getShellResult(cmd, isAtLeastStableAndroid9())
        val (@StringRes result, @AttrRes color) = if (gsiCompatibilityResult.isSuccess) {
            val firstLine = gsiCompatibilityResult.output.getOrNull(0)
                ?: MyApplication.getMyString(android.R.string.unknownName)
            val lineResult = firstLine.split('=')
            val isCompatible = lineResult.isNotEmpty()
                    && lineResult.getOrNull(1)?.trim().toBoolean()
            if (isCompatible) {
                R.string.result_compliant to R.attr.colorNoProblem
            } else {
                R.string.result_not_compliant to R.attr.colorWaring
            }
        } else {
            if (isTrebleEnabled && isAtLeastStableAndroid9()) {
                R.string.result_unidentified to R.attr.colorWaring
            } else {
                R.string.result_not_supported to R.attr.colorCritical
            }
        }

        tempModels += toColoredMyModel(
            MyApplication.getMyString(R.string.gsi_status_title),
            MyApplication.getMyString(result),
            color
        )

        return tempModels
    }
    // endregion [Treble & GSI]

    /** {@link android.util.FeatureFlagUtils} */
    fun detectDsu(): MyModel {
        val isDsuEnabled = getBooleanProperty(AndroidDataSource.PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE, isAtLeastStableAndroid10())
                || getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_SYSTEM_UPDATE, isAtLeastStableAndroid10())
        return toColoredMyModel(
            MyApplication.getMyString(R.string.dsu_status_title),
            toSupportOrNotString(isDsuEnabled),
            isDsuEnabled
        )
    }

    /** {@link com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController} */
    fun detectMainline(lld: Lld): MyModel {
        var versionName = MyApplication.getMyString(R.string.result_not_supported)
        var moduleProvider = MyApplication.getMyString(android.R.string.unknownName)
        val latestGooglePlaySystemUpdates = lld.android.googlePlaySystemUpdates

        fun getResult() = MyApplication.getMyString(
            R.string.mainline_detail, versionName, moduleProvider, latestGooglePlaySystemUpdates
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
                moduleProvider = MyApplication.getMyString(idConfigDefaultModuleMetadataProvider)
                val packageInfo = getPackageInfoOrNull(moduleProvider)
                versionName = packageInfo?.versionName ?: ""

                if (versionName >= latestGooglePlaySystemUpdates
                    || "$versionName-01" >= latestGooglePlaySystemUpdates
                ) {
                    moduleColor = R.attr.colorNoProblem
                }

                getResult()
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "Failed to get mainline version. ${e.fullMessage}")
                getResult()
            }
        } else {
            getResult()
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.mainline_title),
            result,
            moduleColor
        )
    }

    fun detectVndk(lld: Lld): MyModel {
        val vndkVersionResult = getStringProperty(AndroidDataSource.PROP_VNDK_VERSION, isAtLeastStableAndroid8())
        // val vendorVndkVersionResult = getStringProperty(AndroidDataSource.PROP_VENDOR_VNDK_VERSION, isAtLeastStableAndroid8())
        // val productVndkVersionResult = getStringProperty(AndroidDataSource.PROP_PRODUCT_VNDK_VERSION, isAtLeastStableAndroid8())

        val hasVndkVersion = isPropertyValueNotEmpty(vndkVersionResult)

        @AttrRes val vndkColor: Int

        var isVndkBuiltInResult = toSupportOrNotString(hasVndkVersion)
        if (hasVndkVersion) {
            val hasVndkLite = getBooleanProperty(AndroidDataSource.PROP_VNDK_LITE)

            vndkColor = if (
                (isLatestPreviewAndroid(lld) || vndkVersionResult >= lld.android.stable.api)
                    && !hasVndkLite
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

        return toColoredMyModel(
            MyApplication.getMyString(R.string.vndk_built_in_title),
            isVndkBuiltInResult,
            vndkColor
        )
    }

    fun detectApex(): MyModel {
        val apexUpdatable = getBooleanProperty(AndroidDataSource.PROP_APEX_UPDATABLE, isAtLeastStableAndroid10())

        val isFlattenedApexMounted = isAtLeastStableAndroid10() && mounts.any {
            it.mountPoint.startsWith("/apex/") && it.mountPoint.contains("@")
        }

        val isApex = apexUpdatable || isFlattenedApexMounted
        val isLegacyFlattenedApex = !apexUpdatable && isFlattenedApexMounted

        var apexEnabledResult = toSupportOrNotString(isApex)
        if (isLegacyFlattenedApex) {
            apexEnabledResult += MyApplication.getMyString(R.string.apex_legacy_flattened)
        }

        @AttrRes val apexColor = when {
            apexUpdatable -> R.attr.colorNoProblem
            isLegacyFlattenedApex -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.apex_status_title),
            apexEnabledResult,
            apexColor
        )
    }

    fun detectDeveloperOptions(): MyModel {
        val isDeveloperOptionsDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            AndroidDataSource.SETTINGS_DISABLED
        ) == AndroidDataSource.SETTINGS_DISABLED

        return toColoredMyModel(
            MyApplication.getMyString(R.string.developer_options_status_title),
            translateDisabled(isDeveloperOptionsDisabled),
            isDeveloperOptionsDisabled
        )
    }

    fun detectAdb(): MyModel {
        val isAdbDebuggingDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.ADB_ENABLED,
            AndroidDataSource.SETTINGS_DISABLED
        ) == AndroidDataSource.SETTINGS_DISABLED

        return toColoredMyModel(
            MyApplication.getMyString(R.string.adb_debugging_status_title),
            translateDisabled(isAdbDebuggingDisabled),
            isAdbDebuggingDisabled
        )
    }

    fun detectAdbAuthentication(): MyModel {
        val isAdbAuthenticationEnabled =
            getStringProperty(AndroidDataSource.PROP_ADB_SECURE) == AndroidDataSource.SETTINGS_ENABLED.toString()

        return toColoredMyModel(
            MyApplication.getMyString(R.string.adb_authentication_status_title),
            translateEnabled(isAdbAuthenticationEnabled),
            isAdbAuthenticationEnabled
        )
    }

    fun detectEncryption(): MyModel {
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

        return toColoredMyModel(
            MyApplication.getMyString(R.string.encryption_status_title),
            MyApplication.getMyString(result),
            color
        )
    }

    fun detectSELinux(): MyModel {
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

        val seLinuxStatus = getShellResult(AndroidDataSource.CMD_GETENFORCE)
        val seLinuxStatusResult = seLinuxStatus.output.getOrNull(0)

        if (seLinuxStatus.isSuccess) {
            when (seLinuxStatusResult) {
                AndroidDataSource.SELINUX_STATUS_ENFORCING -> {
                    result = R.string.selinux_status_enforcing_mode
                    color = R.attr.colorNoProblem
                }
                AndroidDataSource.SELINUX_STATUS_PERMISSIVE -> {
                    // val seLinuxPolicyVersion = sh(CMD_SELINUX_POLICY_VERSION, isAtLeastAndroid8())
                    result = R.string.selinux_status_permissive_mode
                    color = R.attr.colorWaring
                }
                AndroidDataSource.SELINUX_STATUS_DISABLED -> {
                    result = R.string.result_disabled
                    color = R.attr.colorCritical
                }
                else -> {
                    result = android.R.string.unknownName
                    color = R.attr.colorCritical
                }
            }
        } else {
            if (seLinuxStatusResult?.endsWith(AndroidDataSource.CMD_ERROR_PERMISSION_DENIED) == true) {
                result = R.string.selinux_status_enforcing_mode
                color = R.attr.colorNoProblem
            } else {
                result = android.R.string.unknownName
                color = R.attr.colorCritical
            }
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.selinux_status),
            MyApplication.getMyString(result),
            color
        )
    }

    fun detectToybox(lld: Lld): MyModel {
        val toyboxVersionResult = getShellResult(AndroidDataSource.CMD_TOYBOX_VERSION, isAtLeastStableAndroid6())
        val hasToyboxVersion = toyboxVersionResult.isSuccess

        val toyboxVersion = if (hasToyboxVersion) {
            toyboxVersionResult.output.getOrNull(0)
                ?: MyApplication.getMyString(android.R.string.unknownName)
        } else {
            toSupportOrNotString(false)
        }

        val toybox = lld.toybox
        val stable = toybox.stable
        val support = toybox.support
        val master = toybox.master

        @AttrRes val toyboxColor = if (hasToyboxVersion) {
            val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
            val toyboxRealVersion = Version(toyboxRealVersionString)
            when {
                toyboxRealVersion.isAtLeast(stable.version) -> R.attr.colorNoProblem
                toyboxRealVersion.isAtLeast(support.version) -> R.attr.colorWaring
                else -> R.attr.colorCritical
            }
        } else {
            R.attr.colorCritical
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.toybox_built_in_title),
            MyApplication.getMyString(R.string.toybox_built_in_detail, toyboxVersion, stable.version, support.version, master.version),
            toyboxColor
        )
    }

    // region [WebView]
    fun detectWebView(lld: Lld): MyModel {
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
                    val packageInfo = getPackageInfoOrNull(packageName)
                    val isInstalled = if (packageInfo != null) {
                        packageInfo.versionName?.also {
                            if (Version(it).isHigherThan(builtInVersionName)) {
                                builtInVersionName = it
                            }
                        } ?: MyApplication.getMyString(android.R.string.unknownName)
                    } else {
                        MyApplication.getMyString(R.string.webview_built_in_not_installed)
                    }

                    fun subFormat(text: String) =
                        MyApplication.getMyString(R.string.webview_sub_format, text)

                    fun subFormat(@StringRes stringRes: Int) =
                        subFormat(MyApplication.getMyString(stringRes))

                    var tempResult = packageName + subFormat(description) + subFormat(isInstalled)
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
            val buildInPackageInfo = getPackageInfoOrNull(buildInPackageName)
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

        val webView = lld.webView
        val lldWebViewStable = webView.stable.version
        val latestResult = MyApplication.getMyString(
            R.string.webview_detail, lldWebViewStable, webView.beta.version
        )

        @AttrRes val webViewColor = when {
            Version(builtInVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorNoProblem
            Version(implementVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        return toColoredMyModel(
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
    // endregion [WebView]

    fun getOutdatedTargetSdkVersionApkModel(lld: Lld): MyModel {
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
            getStringProperty(AndroidDataSource.PROP_RO_PRODUCT_FIRST_API_LEVEL)
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

    // region [Common]
    private fun isPropertyValueNotEmpty(result: String) =
        result.isNotEmpty()
            && result != MyApplication.getMyString(R.string.result_not_supported)
            && result != MyApplication.getMyString(R.string.build_not_filled)

    private fun toSupportOrNotString(condition: Boolean) = MyApplication.getMyString(
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

    private fun getPackageInfoOrNull(packageName: String) = try {
        appInfoDataSource.getPackageInfoOrThrow(MyApplication.instance.packageManager, packageName)
    } catch (e: Exception) {
        Log.w(javaClass.simpleName, "$packageName not found. ${e.fullMessage}")
        null
    }
    // endregion [Common]
}