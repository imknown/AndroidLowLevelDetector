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
import androidx.core.backported.fixes.BackportedFixManager
import androidx.core.backported.fixes.KnownIssue
import androidx.core.backported.fixes.KnownIssues
import androidx.core.backported.fixes.Status
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewCompat
import io.github.g00fy2.versioncompare.Version
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.formatToLocalZonedDatetimeString
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.toColoredMyModel
import net.imknown.android.forefrontinfo.ui.common.CODENAME_CANARY
import net.imknown.android.forefrontinfo.ui.common.getBooleanProperty
import net.imknown.android.forefrontinfo.ui.common.getSdkExtension
import net.imknown.android.forefrontinfo.ui.common.getShellResult
import net.imknown.android.forefrontinfo.ui.common.getStringProperty
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid10
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid11
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid12
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid13
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid6
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid7
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid8
import net.imknown.android.forefrontinfo.ui.common.isAtLeastAndroid9
import net.imknown.android.forefrontinfo.ui.common.isGoEdition
import net.imknown.android.forefrontinfo.ui.common.isLatestPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isLatestStableAndroid
import net.imknown.android.forefrontinfo.ui.common.isPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isSupportedByUpstreamAndroid
import net.imknown.android.forefrontinfo.ui.common.myAndroid
import net.imknown.android.forefrontinfo.ui.common.toPascalCase
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import java.io.File
import android.R as androidR

class HomeRepository(
    private val lldDataSource: LldDataSource,
    private val mountDataSource: MountDataSource,
    private val appInfoDataSource: AppInfoDataSource
) {
    fun fetchOfflineLldFileOrThrow() = lldDataSource.fetchOfflineLldFileOrThrow()
    suspend fun fetchOnlineLldJsonStringOrThrow() = lldDataSource.fetchOnlineLldJsonStringOrThrow()

    fun detectMode(lld: Lld?, errors: List<String?>, modeResId: Int): MyModel {
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
            datetimeFormatted = MyApplication.getMyString(androidR.string.unknownName)
            color = R.attr.colorCritical
        }

        val result = errors.filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n\n", prefix = "\n\n")
            .orEmpty()

        return toColoredMyModel(
            MyApplication.getMyString(R.string.lld_json_mode_title),
            MyApplication.getMyString(modeResId, datetimeFormatted) + result,
            color
        )
    }

    fun detectAndroid(lld: Lld?): MyModel {
        val lldAndroid = lld?.android

        // region [Mine]
        val android = lldAndroid?.known?.find {
            it.apiFull == myAndroid.apiFull
        }

        fun String.toStableOrPreview(): String {
            var versionTemp = this
            if (isPreviewAndroid()) {
                val preview = if (Build.VERSION.CODENAME == CODENAME_CANARY) {
                    CODENAME_CANARY.toPascalCase()
                } else {
                    MyApplication.getMyString(R.string.android_info_preview)
                }
                versionTemp += " $preview"
            }
            return versionTemp
        }

        if (android != null) {
            with(android) {
                myAndroid.api = api.toInt()
                myAndroid.apiFull = apiFull
                myAndroid.version = version
                myAndroid.dessert = name
            }
        }

        val version = myAndroid.version.toStableOrPreview()
        val dessert = myAndroid.dessert
            ?: MyApplication.getMyString(androidR.string.unknownName)
        var myVersionAndDessert = "$version, $dessert"
        if (MyApplication.instance.isGoEdition()) {
            myVersionAndDessert += " (Go)"
        }

        val mine = MyApplication.getMyString(R.string.android_info, myVersionAndDessert, myAndroid.apiFull)
        // endregion [Mine]

        fun oneLine(android: Lld.Androids.Android?): String {
            val version = android?.version
                ?: MyApplication.getMyString(androidR.string.unknownName)
            val apiFull = android?.apiFull
                ?: MyApplication.getMyString(androidR.string.unknownName)
            return MyApplication.getMyString(R.string.android_info, version, apiFull)
        }

        val latestStable = oneLine(lldAndroid?.stable)
        val lowestSupport = oneLine(lldAndroid?.support)
        val stablePreview = oneLine(lldAndroid?.stablePreview) // Beta
        val latestPreview = oneLine(lldAndroid?.preview) // Canary
        val latestInternal = oneLine(lldAndroid?.internal)

        val infoDetailArgs = arrayOf(mine, latestStable, lowestSupport, stablePreview, latestPreview, latestInternal)

        @AttrRes val color = when {
            lld == null -> R.attr.colorCritical
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorNoProblem
            isSupportedByUpstreamAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.android_info_title),
            MyApplication.getMyString(R.string.android_info_detail, *infoDetailArgs),
            color
        )
    }

    fun detectBackportedFix(): MyModel {
        val manager = BackportedFixManager()
        fun createKi(
            id: Long, alias: Int?, precondition: () -> Boolean = { true }
        ) = KnownIssue::class.java.getDeclaredConstructor(
            Long::class.javaPrimitiveType,
            Int::class.javaObjectType,
            Function0::class.java
        ).apply {
            isAccessible = true
        }.newInstance(id, alias, precondition)

        // https://cs.android.com/android/platform/superproject/+/android-latest-release:cts/backported_fixes/approved/
        listOf(
            KnownIssues.KI_350037023, // Alias 1
            KnownIssues.KI_372917199, // Alias 2
            KnownIssues.KI_350037348, // Alias 3
            createKi(385124056L, 4), // Alias 4
            KnownIssues.KI_398591036, // Alias 5
            KnownIssues.KI_452390376, // Alias 6
        ).forEach {
            val status = manager.getStatus(it)
            when (status) {
                Status.Unknown -> "Unknown"
                Status.Fixed -> "Fixed"
                Status.NotApplicable -> "NotApplicable"
                Status.NotFixed -> "NotFixed"
            }
        }

        return MyModel("TODO", "TODO")
    }

    fun detectSdkExtension(lld: Lld?): MyModel {
        val lldStableExtension = lld?.android?.stable?.extension

        val (myExtension, color) = if (isAtLeastAndroid11()) {
            val myExtension = getSdkExtension(Build.VERSION.SDK_INT)
            val color = lldStableExtension != null && myExtension >= lldStableExtension
            myExtension to color
        } else {
            MyApplication.getMyString(R.string.result_not_supported) to false
        }
        val lldStableExtensionString = lldStableExtension?.toString()
            ?: MyApplication.getMyString(androidR.string.unknownName)
        return toColoredMyModel(
            MyApplication.getMyString(R.string.android_sdk_extension_title),
            MyApplication.getMyString(R.string.android_sdk_extension_detail, myExtension, lldStableExtensionString),
            color
        )
    }

    fun detectBuildId(lld: Lld?): MyModel {
        val buildIdResult = Build.ID
        val systemBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_SYSTEM_BUILD_ID, isAtLeastAndroid9())
        val vendorBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_VENDOR_BUILD_ID, isAtLeastAndroid9())
        val odmBuildIdResult = getStringProperty(AndroidDataSource.PROP_RO_ODM_BUILD_ID, isAtLeastAndroid9())

        val build = lld?.android?.build

        val version = build?.version
            ?: MyApplication.getMyString(androidR.string.unknownName)

        val lldDetails = build?.details
        val builds = lldDetails?.joinToString("\n") { detail ->
            MyApplication.getMyString(R.string.android_build_id, detail.id, detail.revision)
        } ?: MyApplication.getMyString(androidR.string.unknownName)

        // region [Color]
        fun isDateHigherThanConfig(): Boolean {
            if (lld == null) {
                return false
            }

            fun isBuildIdAndroid8CtsFormat(buildId: String) =
                isAtLeastAndroid8() && buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR).size >= 3

            val myBuildIdForCompare = if (
                isBuildIdAndroid8CtsFormat(buildIdResult) || isAtLeastAndroid9().not()
            ) {
                buildIdResult
            } else {
                systemBuildIdResult
            }

            if (!isBuildIdAndroid8CtsFormat(myBuildIdForCompare)) {
                return false
            }

            fun getDate(buildId: String) = buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR)[1] // "250705"

            val myBuildIdDate = getDate(myBuildIdForCompare)
            val myBuildIdDateIntOrNull = myBuildIdDate.toIntOrNull()
                ?: return false

            val lldFirstDetail = lldDetails?.getOrNull(0)
                ?: return false
            val lldFirstBuildId = lldFirstDetail.id
            val lldFirstBuildIdDate = getDate(lldFirstBuildId)
            val lldFirstBuildIdDateIntOrNull = lldFirstBuildIdDate.toIntOrNull()
                ?: return false

            val offset = if (isLatestStableAndroid(lld)) {
                0
            } else {
                /** [isLatestPreviewAndroid] */
                250205 - 250101
            }

            return (myBuildIdDateIntOrNull + offset) >= lldFirstBuildIdDateIntOrNull
        }

        @AttrRes val buildIdColor = when {
            lld == null -> R.attr.colorCritical
            isDateHigherThanConfig() -> R.attr.colorNoProblem
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }
        // endregion [Color]

        val infoDetailArgs = arrayOf(buildIdResult, systemBuildIdResult, vendorBuildIdResult, odmBuildIdResult, version, builds)

        return toColoredMyModel(
            MyApplication.getMyString(R.string.android_build_id_title),
            MyApplication.getMyString(R.string.android_build_id_detail).format(*infoDetailArgs), // Fix weird NPE on Android 6
            buildIdColor
        )
    }

    // region [SecurityPatch]
    fun detectSecurityPatches(lld: Lld?): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        val mySecurityPatch = if (isAtLeastAndroid6()) {
            Build.VERSION.SECURITY_PATCH
        } else {
            getStringProperty(AndroidDataSource.PROP_SECURITY_PATCH)
        }
        tempModels += detectSecurityPatch(lld, mySecurityPatch, R.string.security_patch_level_title)

        val mySecurityPatchVendor = getStringProperty(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, isAtLeastAndroid9())
        tempModels += detectSecurityPatch(lld, mySecurityPatchVendor, R.string.vendor_security_patch_level_title)

        return tempModels
    }

    private fun detectSecurityPatch(lld: Lld?, mySecurityPatch: String, @StringRes titleId: Int): MyModel {
        val lldSecurityPatch = lld?.android?.securityPatchLevel
        @AttrRes val securityPatchColor = when {
            lldSecurityPatch == null -> R.attr.colorCritical
            !isPropertyValueNotEmpty(mySecurityPatch) -> R.attr.colorCritical
            mySecurityPatch >= lldSecurityPatch -> R.attr.colorNoProblem
            getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> R.attr.colorWaring
            else -> R.attr.colorCritical
        }

        val infoDetailArgs = arrayOf(
            mySecurityPatch,
            lldSecurityPatch
                ?: MyApplication.getMyString(androidR.string.unknownName)
        )

        return toColoredMyModel(
            MyApplication.getMyString(titleId),
            MyApplication.getMyString(R.string.security_patch_level_detail, *infoDetailArgs),
            securityPatchColor
        )
    }

    private fun getSecurityPatchYearMonth(securityPatch: String) =
        securityPatch.substringBeforeLast('-')
    // endregion [SecurityPatch]

    fun detectPerformanceClass(): MyModel {
        @AttrRes var performanceColorRes = R.attr.colorCritical

        val result = if (isAtLeastAndroid12()) {
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

    fun detectKernel(lld: Lld?): MyModel {
        val linuxVersionString = System.getProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION)
        val linuxVersion = Version(linuxVersionString)

        @AttrRes var linuxColor = R.attr.colorCritical

        val linux = lld?.linux
        val versionsSupported = linux?.google?.versions
        versionsSupported?.forEach {
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

        val support = versionsSupported?.joinToString("｜")
            ?: MyApplication.getMyString(androidR.string.unknownName)

        val mainline = linux?.mainline?.version
            ?: MyApplication.getMyString(androidR.string.unknownName)

        return toColoredMyModel(
            MyApplication.getMyString(R.string.linux_title),
            MyApplication.getMyString(R.string.linux_version_detail, linuxVersionString, support, mainline),
            linuxColor
        )
    }

    fun detectAb(): MyModel {
        val isAbUpdateSupported = getBooleanProperty(AndroidDataSource.PROP_AB_UPDATE, isAtLeastAndroid7())
        val slotSuffixResult = getStringProperty(AndroidDataSource.PROP_SLOT_SUFFIX, isAtLeastAndroid7())
        val isVirtualAb = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_ENABLED, isAtLeastAndroid11())
        val isAbEnable = isAbUpdateSupported || isPropertyValueNotEmpty(slotSuffixResult) || isVirtualAb

        var abResult = toSupportOrNotString(isAbEnable)

        if (isAbEnable) {
            if (isVirtualAb) {
                val isVirtualAbRetrofit = getBooleanProperty(AndroidDataSource.PROP_VIRTUAL_AB_RETROFIT, isAtLeastAndroid11())
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

        val isSar = if (isAtLeastAndroid9()) {
            val isLAndroid9TheLegacySar = getBooleanProperty(AndroidDataSource.PROP_SYSTEM_ROOT_IMAGE)

            val isTheLegacySarMount = mounts.any {
                it.blockDevice == "/dev/root" && it.mountPoint == "/"
            }

            isTheLegacySar = isLAndroid9TheLegacySar && isTheLegacySarMount

            isThe2siSar = isAtLeastAndroid10()
                    && mounts.none {
                        it.blockDevice != "none" && it.mountPoint == "/system" && it.type != "tmpfs"
                    }

            isRecoverySar = mounts.any {
                it.mountPoint == "/system_root" && it.type != "tmpfs"
            }

            isSlashSar = mounts.any {
                it.mountPoint == "/" && it.type != "rootfs"
            }

            isTheLegacySar || isThe2siSar || isRecoverySar || isSlashSar || isAtLeastAndroid10()
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
                    sarTypeRes = androidR.string.unknownName
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
            getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_PARTITIONS, isAtLeastAndroid10())
        val isDynamicPartitionsRetrofit =
            getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_PARTITIONS_RETROFIT, isAtLeastAndroid10())

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
        val isTrebleEnabled = getBooleanProperty(AndroidDataSource.PROP_TREBLE_ENABLED, isAtLeastAndroid8())

        var trebleResult = toSupportOrNotString(isTrebleEnabled)

        val pathVendorSku = AndroidDataSource.PATH_VENDOR_VINTF_SKU.format(
            getStringProperty(AndroidDataSource.PROP_VENDOR_SKU, isAtLeastAndroid12())
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
            isAtLeastAndroid11() -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_11
            isAtLeastAndroid9() -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_9
            else -> "NOT_EXIST"
        }
        val cmd = AndroidDataSource.CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED.format(fileLdConfig)
        val gsiCompatibilityResult = getShellResult(cmd, isAtLeastAndroid9())
        val (@StringRes result, @AttrRes color) = if (gsiCompatibilityResult.isSuccess) {
            val firstLine = gsiCompatibilityResult.output.getOrNull(0)
                ?: MyApplication.getMyString(androidR.string.unknownName)
            val lineResult = firstLine.split('=')
            val isCompatible = lineResult.isNotEmpty()
                    && lineResult.getOrNull(1)?.trim().toBoolean()
            if (isCompatible) {
                R.string.result_compliant to R.attr.colorNoProblem
            } else {
                R.string.result_not_compliant to R.attr.colorWaring
            }
        } else {
            if (isTrebleEnabled && isAtLeastAndroid9()) {
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
        val isDsuEnabled = getBooleanProperty(AndroidDataSource.PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE, isAtLeastAndroid10())
                || getBooleanProperty(AndroidDataSource.PROP_DYNAMIC_SYSTEM_UPDATE, isAtLeastAndroid10())
        val result = MyApplication.getMyString(
            if (isDsuEnabled) {
                R.string.result_supported
            } else {
                androidR.string.unknownName
            }
        )
        return toColoredMyModel(
            MyApplication.getMyString(R.string.dsu_status_title),
            result,
            isDsuEnabled
        )
    }

    /** {@link com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController} */
    fun detectMainline(lld: Lld?): MyModel {
        var versionName = MyApplication.getMyString(R.string.result_not_supported)
        var moduleProvider = MyApplication.getMyString(androidR.string.unknownName)
        val latestGooglePlaySystemUpdates = lld?.android?.googlePlaySystemUpdates

        fun getResult() = MyApplication.getMyString(
            R.string.mainline_detail,
            versionName,
            moduleProvider,
            latestGooglePlaySystemUpdates
                ?: MyApplication.getMyString(androidR.string.unknownName)
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

                if (latestGooglePlaySystemUpdates != null) {
                    if (versionName >= latestGooglePlaySystemUpdates
                        || "$versionName-01" >= latestGooglePlaySystemUpdates
                    ) {
                        moduleColor = R.attr.colorNoProblem
                    }
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

    fun detectVndk(lld: Lld?): MyModel {
        val vndkVersionResult = getStringProperty(AndroidDataSource.PROP_VNDK_VERSION, isAtLeastAndroid8())
        // val vendorVndkVersionResult = getStringProperty(AndroidDataSource.PROP_VENDOR_VNDK_VERSION, isAtLeastStableAndroid8())
        // val productVndkVersionResult = getStringProperty(AndroidDataSource.PROP_PRODUCT_VNDK_VERSION, isAtLeastStableAndroid8())

        val hasVndkVersion = isPropertyValueNotEmpty(vndkVersionResult)

        @AttrRes val vndkColor: Int

        var isVndkBuiltInResult = toSupportOrNotString(hasVndkVersion)
        if (hasVndkVersion) {
            val hasVndkLite = getBooleanProperty(AndroidDataSource.PROP_VNDK_LITE)

            vndkColor = if (lld != null) {
                if (
                    (isLatestPreviewAndroid(lld) || vndkVersionResult >= lld.android.stable.api)
                        && !hasVndkLite
                ) {
                    R.attr.colorNoProblem
                } else {
                    R.attr.colorWaring
                }
            } else {
                R.attr.colorCritical
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
        val apexUpdatable = getBooleanProperty(AndroidDataSource.PROP_APEX_UPDATABLE, isAtLeastAndroid10())

        val isFlattenedApexMounted = isAtLeastAndroid10() && mounts.any {
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
                    result = androidR.string.unknownName
                    color = R.attr.colorCritical
                }
            }
        } else {
            if (seLinuxStatusResult?.endsWith(AndroidDataSource.CMD_ERROR_PERMISSION_DENIED) == true) {
                result = R.string.selinux_status_enforcing_mode
                color = R.attr.colorNoProblem
            } else {
                result = androidR.string.unknownName
                color = R.attr.colorCritical
            }
        }

        return toColoredMyModel(
            MyApplication.getMyString(R.string.selinux_status),
            MyApplication.getMyString(result),
            color
        )
    }

    fun detectToybox(lld: Lld?): MyModel {
        val toyboxVersionResult = getShellResult(AndroidDataSource.CMD_TOYBOX_VERSION, isAtLeastAndroid6())
        val hasToyboxVersion = toyboxVersionResult.isSuccess

        val toyboxVersion = if (hasToyboxVersion) {
            toyboxVersionResult.output.getOrNull(0)
                ?: MyApplication.getMyString(androidR.string.unknownName)
        } else {
            toSupportOrNotString(false)
        }

        val toybox = lld?.toybox
        val stableVersion = toybox?.stable?.version
        val supportVersion = toybox?.support?.version
        val masterVersion = toybox?.master?.version

        @AttrRes val toyboxColor = if (hasToyboxVersion) {
            val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
            val toyboxRealVersion = Version(toyboxRealVersionString)
            when {
                stableVersion != null && toyboxRealVersion.isAtLeast(stableVersion) -> R.attr.colorNoProblem
                supportVersion != null && toyboxRealVersion.isAtLeast(supportVersion) -> R.attr.colorWaring
                else -> R.attr.colorCritical
            }
        } else {
            R.attr.colorCritical
        }

        val infoDetailArgs = arrayOf(
            toyboxVersion,
            stableVersion
                ?: MyApplication.getMyString(androidR.string.unknownName),
            supportVersion
                ?: MyApplication.getMyString(androidR.string.unknownName),
            masterVersion
                ?: MyApplication.getMyString(androidR.string.unknownName)
        )

        return toColoredMyModel(
            MyApplication.getMyString(R.string.toybox_built_in_title),
            MyApplication.getMyString(R.string.toybox_built_in_detail, *infoDetailArgs),
            toyboxColor
        )
    }

    // region [WebView]
    fun detectWebView(lld: Lld?): MyModel {
        val type = if (isAtLeastAndroid10()) {
            val standalone = MyApplication.getMyString(R.string.webview_standalone)
            MyApplication.getMyString(R.string.webview_or, "Trichrome", standalone)
        } else if (isAtLeastAndroid7()) {
            "Monochrome"
        } else {
            MyApplication.getMyString(R.string.webview_standalone)
        }

        val packageManager = MyApplication.instance.packageManager

        var builtInResult = ""
        var builtInVersionName = ""
        if (isAtLeastAndroid7()) {
            val webViewProviderInfoList = getBuildInWebViewProvidersAndroid7()

            if (webViewProviderInfoList.isEmpty()) {
                builtInResult = MyApplication.getMyString(R.string.build_not_filled)
            } else {
                val lastIndex = webViewProviderInfoList.size - 1
                webViewProviderInfoList.forEachIndexed { index, webViewProviderInfo ->
                    builtInResult += with(webViewProviderInfo) {
                        val packageInfo = getPackageInfoOrNull(packageName)
                        val isInstalled = if (packageInfo != null) {
                            packageInfo.versionName?.also {
                                if (Version(it).isHigherThan(builtInVersionName)) {
                                    builtInVersionName = it
                                }
                            } ?: MyApplication.getMyString(androidR.string.unknownName)
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

        val webView = lld?.webView
        val lldWebViewStable = webView?.stable?.version
        val lldWebViewBeta = webView?.beta?.version
        val latestResult = MyApplication.getMyString(
            R.string.webview_detail,
            lldWebViewStable
                ?: MyApplication.getMyString(androidR.string.unknownName),
            lldWebViewBeta
                ?: MyApplication.getMyString(androidR.string.unknownName)
        )

        @AttrRes val webViewColor = when {
            lldWebViewStable != null && Version(builtInVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorNoProblem
            lldWebViewStable != null && Version(implementVersionName).isAtLeast(lldWebViewStable) -> R.attr.colorWaring
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
        val allWebViewPackages = try {
            val webViewUpdateServiceClass = Class.forName("android.webkit.WebViewUpdateService")
            webViewUpdateServiceClass.getDeclaredMethod("getAllWebViewPackages")
                .invoke(null) as Array<*>
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "getAllWebViewPackages: ${e.fullMessage}")
            emptyArray<Any>()
        }

        fun Any?.getWebViewProviderInfoMemberOrThrow(name: String) =
            Class.forName("android.webkit.WebViewProviderInfo").getDeclaredField(name).get(this)

        fun String.log(e: Exception) {
            Log.w(javaClass.simpleName, "WebViewProvider $this: ${e.fullMessage}")
        }

        return allWebViewPackages.toList().map {
            with(it) {
                val packageNameField = "packageName"
                val packageName = try {
                    getWebViewProviderInfoMemberOrThrow(packageNameField) as String
                } catch (e: Exception) {
                    packageNameField.log(e)
                    MyApplication.getMyString(R.string.build_not_filled)
                }

                val descriptionField = "description"
                val description = try {
                    getWebViewProviderInfoMemberOrThrow(descriptionField) as String
                } catch (e: Exception) {
                    descriptionField.log(e)
                    MyApplication.getMyString(R.string.build_not_filled)
                }

                val availableByDefaultField = "availableByDefault"
                val availableByDefault = try {
                    getWebViewProviderInfoMemberOrThrow(availableByDefaultField) as Boolean
                } catch (e: Exception) {
                    availableByDefaultField.log(e)
                    false
                }

                val isFallbackField = "isFallback"
                val isFallback = try {
                    getWebViewProviderInfoMemberOrThrow(isFallbackField) as Boolean
                } catch (e: Exception) {
                    isFallbackField.log(e)
                    false
                }

                val signaturesField = "signatures"
                val signatures = try {
                    getWebViewProviderInfoMemberOrThrow(signaturesField) as Array<*>
                } catch (e: Exception) {
                    signaturesField.log(e)
                    emptyArray<Any>()
                }

                WebViewProviderInfo(
                    packageName, description, availableByDefault, isFallback, signatures
                )
            }
        }
    }
    // endregion [WebView]

    fun getOutdatedTargetSdkVersionApkModel(lld: Lld?): MyModel {
        val packageManager = MyApplication.instance.packageManager
        val installedApplications = if (isAtLeastAndroid13()) {
            val flags = PackageManager.ApplicationInfoFlags.of(0)
            packageManager.getInstalledApplications(flags)
        } else {
            packageManager.getInstalledApplications(0)
        }
        var systemApkList = installedApplications.filter {
            val systemFlags = it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM)
            (systemFlags > 0) && (it.targetSdkVersion < myAndroid.api)
        }

        var result = MyApplication.getMyString(
            R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
            getStringProperty(AndroidDataSource.PROP_RO_PRODUCT_FIRST_API_LEVEL)
        )

        @AttrRes val targetSdkVersionColor = if (systemApkList.isEmpty()) {
            result += MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_result_none)

            if (lld != null) {
                if (isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld)) {
                    R.attr.colorNoProblem
                } else {
                    R.attr.colorWaring
                }
            } else {
                R.attr.colorCritical
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

            result += systemApkList.joinToString("\n") { applicationInfo ->
                "- " + MyApplication.getMyString(
                    format, applicationInfo.packageName, applicationInfo.targetSdkVersion
                )
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