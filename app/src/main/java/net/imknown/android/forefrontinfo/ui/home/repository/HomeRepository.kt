package net.imknown.android.forefrontinfo.ui.home.repository

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewCompat
import dev.zacsweers.metro.Inject
import io.github.g00fy2.versioncompare.Version
import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.shell.IShell
import net.imknown.android.forefrontinfo.base.shell.ShellResult
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.base.list.MyModelType
import net.imknown.android.forefrontinfo.ui.base.list.guardedMyModel
import net.imknown.android.forefrontinfo.ui.base.list.toColoredMyModel
import net.imknown.android.forefrontinfo.ui.common.CODENAME_CANARY
import net.imknown.android.forefrontinfo.ui.common.PropertyReader
import net.imknown.android.forefrontinfo.ui.common.getSdkExtension
import net.imknown.android.forefrontinfo.ui.common.isGoEdition
import net.imknown.android.forefrontinfo.ui.common.isPreviewAndroid
import net.imknown.android.forefrontinfo.ui.common.isStableAndroid
import net.imknown.android.forefrontinfo.ui.common.toPascalCase
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidVersionDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.LldFileStore
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.home.model.AbState
import net.imknown.android.forefrontinfo.ui.home.model.AndroidInfoState
import net.imknown.android.forefrontinfo.ui.home.model.ApexState
import net.imknown.android.forefrontinfo.ui.home.model.BuildIdState
import net.imknown.android.forefrontinfo.ui.home.model.DynamicPartitionsState
import net.imknown.android.forefrontinfo.ui.home.model.GsiState
import net.imknown.android.forefrontinfo.ui.home.model.KernelState
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import net.imknown.android.forefrontinfo.ui.home.model.LldStatusState
import net.imknown.android.forefrontinfo.ui.home.model.MainlineState
import net.imknown.android.forefrontinfo.ui.home.model.PerformanceClassInfo
import net.imknown.android.forefrontinfo.ui.home.model.SarState
import net.imknown.android.forefrontinfo.ui.home.model.SecurityPatchState
import net.imknown.android.forefrontinfo.ui.home.model.SdkExtensionState
import net.imknown.android.forefrontinfo.ui.home.model.TrebleState
import net.imknown.android.forefrontinfo.ui.home.model.VndkState
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import java.io.File
import android.R as androidR

// Constructor injection puts the dependency chain on the signature and lets Metro build it
// (issues-cn #02); the DataSource params are concrete @Inject types, and IShell is an
// interface bound by ShellLibSu's @ContributesBinding, so none of this chain's own types
// needs a @Provides.
@Inject
class HomeRepository(
    private val shell: IShell,
    private val propertyReader: PropertyReader,
    private val lldDataSource: LldDataSource,
    private val lldFileStore: LldFileStore,
    private val mountDataSource: MountDataSource,
    private val appInfoDataSource: AppInfoDataSource
) {
    fun fetchOfflineLldFileOrThrow() = lldDataSource.fetchOfflineLldFileOrThrow()
    suspend fun fetchOnlineLldJsonStringOrThrow() = lldDataSource.fetchOnlineLldJsonStringOrThrow()

    fun saveLldJsonFileOrThrow(lldString: String) = lldFileStore.saveLldJsonFileOrThrow(lldString)
    fun copyJsonIfNeededOrThrow() = lldFileStore.copyJsonIfNeededOrThrow()
    fun getAssetLld(assets: AssetManager): Lld? = lldFileStore.getAssetLld(assets)

    fun detectMode(
        lld: Lld?, errors: List<String?>, source: LldSource
    ): DetectResult<LldStatusState> = try {
        DetectResult.Found(LldStatusState(lld?.version, source, errors.filterNotNull()))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectAndroid(lld: Lld?): DetectResult<AndroidInfoState> = try {
        val lldAndroid = lld?.android
        val androidInfo = AndroidVersionDataSource.currentAndroidInfo

        // The lld known row corrects what this row displays only (version / dessert);
        // the correction never reaches other detections or any global (AndroidVersionDataSource
        // is the single source).
        val known = lldAndroid?.known?.find {
            it.apiFull == androidInfo.apiFull
        }

        val currency = when {
            lld == null -> AndroidInfoState.AndroidCurrency.Unknown
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> AndroidInfoState.AndroidCurrency.Latest
            isSupportedByUpstreamAndroid(lld) -> AndroidInfoState.AndroidCurrency.Supported
            else -> AndroidInfoState.AndroidCurrency.Outdated
        }

        val referenceRows = listOf(
            AndroidInfoState.AndroidReferenceRow(lldAndroid?.stable?.version, lldAndroid?.stable?.apiFull),
            AndroidInfoState.AndroidReferenceRow(lldAndroid?.support?.version, lldAndroid?.support?.apiFull),
            AndroidInfoState.AndroidReferenceRow(lldAndroid?.stablePreview?.version, lldAndroid?.stablePreview?.apiFull),
            AndroidInfoState.AndroidReferenceRow(lldAndroid?.preview?.version, lldAndroid?.preview?.apiFull),
            AndroidInfoState.AndroidReferenceRow(lldAndroid?.internal?.version, lldAndroid?.internal?.apiFull)
        )

        DetectResult.Found(
            AndroidInfoState(
                version = known?.version ?: androidInfo.version,
                dessert = known?.name ?: androidInfo.dessert,
                isPreview = isPreviewAndroid(),
                canaryLabel = if (Build.VERSION.CODENAME == CODENAME_CANARY) {
                    CODENAME_CANARY.toPascalCase()
                } else {
                    null
                },
                isGoEdition = MyApplication.instance.isGoEdition(),
                apiFull = androidInfo.apiFull,
                referenceRows = referenceRows,
                currency = currency
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectSdkExtension(lld: Lld?): DetectResult<SdkExtensionState> = try {
        val lldStableExtension = lld?.android?.stable?.extension
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val myExtension = getSdkExtension(Build.VERSION.SDK_INT)
            DetectResult.Found(
                SdkExtensionState(
                    currentExtension = myExtension,
                    stableExtension = lldStableExtension,
                    meetsStable = lldStableExtension != null && myExtension >= lldStableExtension
                )
            )
        } else {
            DetectResult.Found(
                SdkExtensionState(currentExtension = null, stableExtension = lldStableExtension, meetsStable = false)
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectBuildId(lld: Lld?): DetectResult<BuildIdState> = try {
        val buildIdResult: String = Build.ID
        val systemBuildIdResult = propertyReader.getString(AndroidDataSource.PROP_RO_SYSTEM_BUILD_ID, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val vendorBuildIdResult = propertyReader.getString(AndroidDataSource.PROP_RO_VENDOR_BUILD_ID, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val odmBuildIdResult = propertyReader.getString(AndroidDataSource.PROP_RO_ODM_BUILD_ID, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

        val lldBuild = lld?.android?.build
        val lldDetails = lldBuild?.details?.map { BuildIdState.BuildDetail(it.id, it.revision) }

        val match = when {
            lld == null -> BuildIdState.Match.Unknown
            isBuildIdDateHigherThanConfig(lld, buildIdResult, systemBuildIdResult, lldDetails) -> BuildIdState.Match.Met
            isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld) -> BuildIdState.Match.Lagging
            else -> BuildIdState.Match.Outdated
        }

        DetectResult.Found(
            BuildIdState(
                buildId = buildIdResult,
                systemBuildId = systemBuildIdResult,
                vendorBuildId = vendorBuildIdResult,
                odmBuildId = odmBuildIdResult,
                lldBuildVersion = lldBuild?.version,
                lldDetails = lldDetails,
                match = match
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    // The device build date against the lld's first detail date: a CTS-style three-segment
    // build id carries the date in its second segment, and a device on the preview channel
    // compares with the preview offset.
    private fun isBuildIdDateHigherThanConfig(
        lld: Lld,
        buildIdResult: String,
        systemBuildIdResult: String,
        lldDetails: List<BuildIdState.BuildDetail>?
    ): Boolean {
        fun isBuildIdAndroid8CtsFormat(buildId: String) =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && buildId.split(AndroidDataSource.BUILD_ID_SEPARATOR).size >= 3

        val myBuildIdForCompare = if (
            isBuildIdAndroid8CtsFormat(buildIdResult) || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
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

    // region [SecurityPatch]
    fun detectSecurityPatches(lld: Lld?): List<DetectResult<SecurityPatchState>> = try {
        listOf(
            detectSecurityPatch(lld, Build.VERSION.SECURITY_PATCH),
            detectSecurityPatch(
                lld,
                propertyReader.getString(AndroidDataSource.PROP_VENDOR_SECURITY_PATCH, Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // A fetch-chain failure fails both rows through their own guards (each keeps its title);
        // the current reads cannot throw, this keeps the "reads convert to Failed" contract whole.
        listOf(DetectResult.Failed(e), DetectResult.Failed(e))
    }

    private fun detectSecurityPatch(lld: Lld?, mySecurityPatch: String): DetectResult<SecurityPatchState> = try {
        val lldSecurityPatch = lld?.android?.securityPatchLevel
        val match = when {
            lldSecurityPatch == null -> SecurityPatchState.Match.Unknown
            !isPropertyValueNotEmpty(mySecurityPatch) -> SecurityPatchState.Match.None
            mySecurityPatch >= lldSecurityPatch -> SecurityPatchState.Match.Met
            getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch) -> SecurityPatchState.Match.Lagging
            else -> SecurityPatchState.Match.None
        }

        DetectResult.Found(
            SecurityPatchState(patch = mySecurityPatch, lldPatchLevel = lldSecurityPatch, match = match)
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    private fun getSecurityPatchYearMonth(securityPatch: String) =
        securityPatch.substringBeforeLast('-')
    // endregion [SecurityPatch]

    fun detectPerformanceClass(): DetectResult<PerformanceClassInfo> = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
            val level = when (performanceClass) {
                Build.VERSION.SDK_INT -> PerformanceClassInfo.Level.Met
                Build.VERSION.SDK_INT - 1 -> PerformanceClassInfo.Level.Lagging
                0 -> PerformanceClassInfo.Level.NotDeclared
                else -> PerformanceClassInfo.Level.Outdated
            }
            DetectResult.Found(PerformanceClassInfo(performanceClass, level))
        } else {
            DetectResult.Found(PerformanceClassInfo(0, PerformanceClassInfo.Level.NotSupported))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectKernel(lld: Lld?): DetectResult<KernelState> = try {
        val linuxVersionString = System.getProperty(AndroidDataSource.SYSTEM_PROPERTY_LINUX_VERSION)
        // An absent os.version fails the detection explicitly. Version(null) would not throw:
        // the version library stores it unparsed and compares as 0.0, and the detail would
        // render a literal "null". That silent path is deliberately not kept (batch 4 exception).
        val linuxVersion = Version(checkNotNull(linuxVersionString))

        val linux = lld?.linux
        val versionsSupported = linux?.google?.versions
        var match = KernelState.KernelMatch.None
        versionsSupported?.forEach {
            if (linuxVersion.major == Version(it).major
                && linuxVersion.minor == Version(it).minor
            ) {
                match = if (linuxVersion.isAtLeast(it)) {
                    KernelState.KernelMatch.Met
                } else {
                    KernelState.KernelMatch.Lagging
                }

                return@forEach
            }
        }

        DetectResult.Found(
            KernelState(
                linuxVersionString = linuxVersionString,
                supportedVersions = versionsSupported,
                mainlineVersion = linux?.mainline?.version,
                match = match
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectAb(): DetectResult<AbState> = try {
        val isAbUpdateSupported = propertyReader.getBoolean(AndroidDataSource.PROP_AB_UPDATE)
        val slotSuffixResult = propertyReader.getStringOrNull(AndroidDataSource.PROP_SLOT_SUFFIX)
        val isVirtualAb = propertyReader.getBoolean(AndroidDataSource.PROP_VIRTUAL_AB_ENABLED, Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val isAbEnable = isAbUpdateSupported || !slotSuffixResult.isNullOrEmpty() || isVirtualAb
        val isVirtualAbRetrofit = if (isVirtualAb) {
            propertyReader.getBoolean(AndroidDataSource.PROP_VIRTUAL_AB_RETROFIT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        } else {
            false
        }
        DetectResult.Found(AbState(isAbEnable, isVirtualAb, isVirtualAbRetrofit, slotSuffixResult))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun getMounts() = mountDataSource.getMounts()

    // issues-cn #63: a failed mount read no longer folds into the success path as an empty
    // table; DetectResult.Failed renders the mapper's failure row, carrying exitCode inline
    // (expanded via fullMessage), character-identical to the old path.
    fun detectSar(mountQuery: MountDataSource.MountQueryResult): DetectResult<SarState> = try {
        val mounts = when (mountQuery) {
            is MountDataSource.MountQueryResult.Success -> mountQuery.mounts
            is MountDataSource.MountQueryResult.Failed -> {
                return DetectResult.Failed(RuntimeException("exitCode=${mountQuery.exitCode}"))
            }
        }

        var isTheLegacySar = false
        var isThe2siSar = false
        var isRecoverySar = false
        var isSlashSar = false

        val isSar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val isLAndroid9TheLegacySar = propertyReader.getBoolean(AndroidDataSource.PROP_SYSTEM_ROOT_IMAGE)

            val isTheLegacySarMount = mounts.any { (blockDevice, mountPoint) ->
                blockDevice == "/dev/root" && mountPoint == "/"
            }

            isTheLegacySar = isLAndroid9TheLegacySar && isTheLegacySarMount

            isThe2siSar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && mounts.none { (blockDevice, mountPoint, type) ->
                        blockDevice != "none" && mountPoint == "/system" && type != "tmpfs"
                    }

            isRecoverySar = mounts.any {
                it.mountPoint == "/system_root" && it.type != "tmpfs"
            }

            isSlashSar = mounts.any {
                it.mountPoint == "/" && it.type != "rootfs"
            }

            isTheLegacySar || isThe2siSar || isRecoverySar || isSlashSar
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        } else {
            false
        }

        // A supported SAR matching none of the four known shapes is the old else branch, kept as is.
        val sarType = if (isSar) {
            when {
                isTheLegacySar -> SarState.SarType.Legacy
                isThe2siSar -> SarState.SarType.TwoSi
                isRecoverySar -> SarState.SarType.Recovery
                isSlashSar -> SarState.SarType.Slash
                else -> SarState.SarType.Unknown
            }
        } else {
            null
        }

        DetectResult.Found(SarState(supported = isSar, type = sarType))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectDynamicPartitions(): DetectResult<DynamicPartitionsState> = try {
        val isDynamicPartitions =
            propertyReader.getBoolean(AndroidDataSource.PROP_DYNAMIC_PARTITIONS, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val isDynamicPartitionsRetrofit =
            propertyReader.getBoolean(AndroidDataSource.PROP_DYNAMIC_PARTITIONS_RETROFIT, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        DetectResult.Found(
            DynamicPartitionsState(
                enabled = isDynamicPartitions || isDynamicPartitionsRetrofit,
                retrofit = isDynamicPartitionsRetrofit
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    // region [Treble & GSI]
    // The vintf manifest probes and the vendor.sku property read both happen only while treble is
    // on; null means disabled. The old path read the property unconditionally (PropertyReader
    // never throws, so the row is identical; treble off just skips one read and one possible
    // Log.w, review v1 noted for the record).
    private fun detectTreble(): DetectResult<TrebleState> = try {
        val isTrebleEnabled = propertyReader.getBoolean(AndroidDataSource.PROP_TREBLE_ENABLED, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        val vintf = if (isTrebleEnabled) {
            val pathVendorSku = AndroidDataSource.PATH_VENDOR_VINTF_SKU.format(
                propertyReader.getString(AndroidDataSource.PROP_VENDOR_SKU, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            )
            when {
                File(pathVendorSku).exists()
                        || File(AndroidDataSource.PATH_VENDOR_VINTF).exists()
                        || File(AndroidDataSource.PATH_VENDOR_VINTF_FRAGMENTS).exists() -> TrebleState.Vintf.Present
                File(AndroidDataSource.PATH_VENDOR_LEGACY_NO_FRAGMENTS).exists() -> TrebleState.Vintf.LegacyNoFragments
                else -> TrebleState.Vintf.Other
            }
        } else {
            null
        }

        DetectResult.Found(TrebleState(enabled = isTrebleEnabled, vintf = vintf))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectTrebleAndGsiCompatibility(): Pair<DetectResult<TrebleState>, DetectResult<GsiState>> {
        val trebleResult = detectTreble()
        // The old bridge derived this flag from the treble row's rendered color (critical means
        // unsupported: both the failure row and the disabled row render critical). The fact-level
        // equivalent is "Found and enabled", the same boundary as the old color comparison.
        val isTrebleSupported = trebleResult is DetectResult.Found && trebleResult.value.enabled

        return trebleResult to detectGsi(isTrebleSupported)
    }

    private fun detectGsi(
        isTrebleSupported: Boolean
    ): DetectResult<GsiState> = try {
        val fileLdConfig = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_11
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> AndroidDataSource.LD_CONFIG_FILE_ANDROID_9
            else -> "NOT_EXIST"
        }
        val cmd = AndroidDataSource.CMD_VENDOR_NAMESPACE_DEFAULT_ISOLATED.format(fileLdConfig)
        val gsiCompatibilityResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) shell.execute(cmd) else ShellResult()
        // The old path substituted the unknown text for a missing first line before parsing; it
        // never changed the boolean (anything but "true" is false). Handled here as no line, so
        // display material stays out of the repository.
        val firstLine = gsiCompatibilityResult.output.getOrNull(0)
        val compatibility = if (gsiCompatibilityResult.isSuccess) {
            // String?.toBoolean() yields false for both absence and any non-"true" value: the same
            // function the old path used, without a constant-true comparison.
            val isCompatible = firstLine != null
                    && firstLine.split('=').getOrNull(1)?.trim().toBoolean()
            if (isCompatible) {
                GsiState.Compatibility.Compatible
            } else {
                GsiState.Compatibility.NotCompatible
            }
        } else {
            if (isTrebleSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                GsiState.Compatibility.Unidentified
            } else {
                GsiState.Compatibility.NotSupported
            }
        }

        DetectResult.Found(GsiState(compatibility))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }
    // endregion [Treble & GSI]

    /** {@link android.util.FeatureFlagUtils} */
    fun detectDsu(): DetectResult<Boolean> = try {
        val isDsuEnabled = propertyReader.getBoolean(AndroidDataSource.PROP_PERSIST_DYNAMIC_SYSTEM_UPDATE, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                || propertyReader.getBoolean(AndroidDataSource.PROP_DYNAMIC_SYSTEM_UPDATE, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        DetectResult.Found(isDsuEnabled)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    /** {@link com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController} */
    fun detectMainline(lld: Lld?): DetectResult<MainlineState> = try {
        val latestGooglePlaySystemUpdates = lld?.android?.googlePlaySystemUpdates

        @SuppressLint("DiscouragedApi")
        // com.android.internal.R.string.config_defaultModuleMetadataProvider
        val idConfigDefaultModuleMetadataProvider = Resources.getSystem().getIdentifier(
            "config_defaultModuleMetadataProvider", "string", "android"
        )

        var moduleProvider: String? = null
        var versionName: String? = null
        if (idConfigDefaultModuleMetadataProvider != 0) {
            try {
                // com.android.modulemetadata
                // com.google.android.modulemetadata
                moduleProvider = MyApplication.getMyString(idConfigDefaultModuleMetadataProvider)
                val packageInfo = getPackageInfoOrNull(moduleProvider)
                versionName = packageInfo?.versionName ?: ""
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "Failed to get mainline version. ${e.fullMessage}")
            }
        }

        val match = when {
            latestGooglePlaySystemUpdates == null -> MainlineState.Match.Unknown
            versionName != null && versionName >= latestGooglePlaySystemUpdates -> MainlineState.Match.Met
            versionName != null && "${versionName}-01" >= latestGooglePlaySystemUpdates -> MainlineState.Match.Met
            else -> MainlineState.Match.None
        }

        DetectResult.Found(
            MainlineState(
                moduleProvider = moduleProvider,
                versionName = versionName,
                latestGooglePlaySystemUpdates = latestGooglePlaySystemUpdates,
                match = match
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectVndk(lld: Lld?): DetectResult<VndkState> = try {
        val vndkVersion = propertyReader.getStringOrNull(AndroidDataSource.PROP_VNDK_VERSION, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        // val vendorVndkVersionResult = propertyReader.getString(AndroidDataSource.PROP_VENDOR_VNDK_VERSION, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        // val productVndkVersionResult = propertyReader.getString(AndroidDataSource.PROP_PRODUCT_VNDK_VERSION, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        val hasVndkLite = if (vndkVersion != null) {
            propertyReader.getBoolean(AndroidDataSource.PROP_VNDK_LITE)
        } else {
            false
        }

        val match = when {
            vndkVersion == null -> VndkState.Match.None
            lld == null -> VndkState.Match.Unknown
            (isLatestPreviewAndroid(lld) || vndkVersion >= lld.android.stable.api) && !hasVndkLite -> VndkState.Match.Met
            else -> VndkState.Match.Lagging
        }

        DetectResult.Found(VndkState(vndkVersion = vndkVersion, hasVndkLite = hasVndkLite, match = match))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    // issues-cn #63: a failed mount read no longer folds into the success path as an empty
    // table; DetectResult.Failed renders the mapper's failure row, carrying exitCode inline
    // (expanded via fullMessage), character-identical to the old path.
    fun detectApex(mountQuery: MountDataSource.MountQueryResult): DetectResult<ApexState> = try {
        val mounts = when (mountQuery) {
            is MountDataSource.MountQueryResult.Success -> mountQuery.mounts
            is MountDataSource.MountQueryResult.Failed -> {
                return DetectResult.Failed(RuntimeException("exitCode=${mountQuery.exitCode}"))
            }
        }

        val apexUpdatable = propertyReader.getBoolean(AndroidDataSource.PROP_APEX_UPDATABLE, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val isFlattenedApexMounted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mounts.any {
            it.mountPoint.startsWith("/apex/") && it.mountPoint.contains("@")
        }

        DetectResult.Found(ApexState(updatable = apexUpdatable, flattenedMounted = isFlattenedApexMounted))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    // 2a: facts only; display text and colors belong to HomeModelMapper. A failed read returns
    // Failed normally instead of throwing (the 05 rule); unexpected exceptions are caught by the
    // mapper's chained thin guard, and the failure DEBUG print lives there too (failureRow).
    fun detectDeveloperOptions(): DetectResult<Boolean> = try {
        val isDeveloperOptionsDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            AndroidDataSource.SETTINGS_DISABLED
        ) == AndroidDataSource.SETTINGS_DISABLED
        DetectResult.Found(isDeveloperOptionsDisabled)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectAdb(): DetectResult<Boolean> = try {
        val isAdbDebuggingDisabled = Settings.Global.getInt(
            MyApplication.instance.contentResolver,
            Settings.Global.ADB_ENABLED,
            AndroidDataSource.SETTINGS_DISABLED
        ) == AndroidDataSource.SETTINGS_DISABLED
        DetectResult.Found(isAdbDebuggingDisabled)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectAdbAuthentication(): DetectResult<Boolean> = try {
        val isAdbAuthenticationEnabled =
            propertyReader.getString(AndroidDataSource.PROP_ADB_SECURE) == AndroidDataSource.SETTINGS_ENABLED.toString()
        DetectResult.Found(isAdbAuthenticationEnabled)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DetectResult.Failed(e)
    }

    fun detectEncryption(): MyModel =
        guardedMyModel(R.string.encryption_status_title) {
            // val cryptoState = propertyReader.getString(PROP_CRYPTO_STATE)
            val devicePolicyManager = ContextCompat.getSystemService(
                MyApplication.instance, DevicePolicyManager::class.java
            )
            val storageEncryptionStatus = devicePolicyManager?.storageEncryptionStatus
            @StringRes val result: Int
            val color: StatusColor
            when (storageEncryptionStatus) {
                // DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING,
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> {
                    result = R.string.result_encrypted
                    color = StatusColor.NO_PROBLEM
                }
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> {
                    result = R.string.result_encrypted_no_key_set
                    color = StatusColor.WARNING
                }
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> {
                    result = R.string.result_not_encrypted
                    color = StatusColor.CRITICAL
                }
                DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> {
                    result = R.string.result_not_supported
                    color = StatusColor.CRITICAL
                }
                else -> {
                    result = R.string.result_not_supported
                    color = StatusColor.CRITICAL
                }
            }

            return toColoredMyModel(
                R.string.encryption_status_title,
                MyApplication.getMyString(result),
                color
            )
        }

    fun detectSELinux(): MyModel =
        guardedMyModel(R.string.selinux_status) {
            // val seLinuxClass = Class.forName("android.os.SELinux")
            // val isSELinuxBooted = seLinuxClass
            //     .getDeclaredMethod("isSELinuxEnabled")
            //     .invoke(null) as Boolean
            // val isSELinuxEnforceBooted = seLinuxClass
            //     .getDeclaredMethod("isSELinuxEnforced")
            //     .invoke(null) as Boolean
            //
            // val bootSELinuxProp = propertyReader.getString(PROP_BOOT_SELINUX)

            @StringRes val result: Int
            val color: StatusColor

            val seLinuxStatus = shell.execute(AndroidDataSource.CMD_GETENFORCE)
            val seLinuxStatusResult = seLinuxStatus.output.getOrNull(0)

            if (seLinuxStatus.isSuccess) {
                when (seLinuxStatusResult) {
                    AndroidDataSource.SELINUX_STATUS_ENFORCING -> {
                        result = R.string.selinux_status_enforcing_mode
                        color = StatusColor.NO_PROBLEM
                    }
                    AndroidDataSource.SELINUX_STATUS_PERMISSIVE -> {
                        result = R.string.selinux_status_permissive_mode
                        color = StatusColor.WARNING
                    }
                    AndroidDataSource.SELINUX_STATUS_DISABLED -> {
                        result = R.string.result_disabled
                        color = StatusColor.CRITICAL
                    }
                    else -> {
                        result = androidR.string.unknownName
                        color = StatusColor.CRITICAL
                    }
                }
            } else {
                if (seLinuxStatusResult?.endsWith(AndroidDataSource.CMD_ERROR_PERMISSION_DENIED) == true) {
                    result = R.string.selinux_status_enforcing_mode
                    color = StatusColor.NO_PROBLEM
                } else {
                    result = androidR.string.unknownName
                    color = StatusColor.CRITICAL
                }
            }

            return toColoredMyModel(
                R.string.selinux_status,
                MyApplication.getMyString(result),
                color
            )
        }

    fun detectToybox(lld: Lld?): MyModel =
        guardedMyModel(R.string.toybox_built_in_title) {
            val toyboxVersionResult = shell.execute(AndroidDataSource.CMD_TOYBOX_VERSION)
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

            val toyboxColor = if (hasToyboxVersion) {
                val toyboxRealVersionString = toyboxVersion.replace("toybox ", "")
                val toyboxRealVersion = Version(toyboxRealVersionString)
                when {
                    stableVersion != null && toyboxRealVersion.isAtLeast(stableVersion) -> StatusColor.NO_PROBLEM
                    supportVersion != null && toyboxRealVersion.isAtLeast(supportVersion) -> StatusColor.WARNING
                    else -> StatusColor.CRITICAL
                }
            } else {
                StatusColor.CRITICAL
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
                R.string.toybox_built_in_title,
                MyApplication.getMyString(R.string.toybox_built_in_detail, *infoDetailArgs),
                toyboxColor
            )
        }

    // region [WebView]
    fun detectWebView(lld: Lld?): MyModel =
        guardedMyModel(R.string.webview_title) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val standalone = MyApplication.getMyString(R.string.webview_standalone)
                MyApplication.getMyString(R.string.webview_or, "Trichrome", standalone)
            } else {
                "Monochrome"
            }

            val packageManager = MyApplication.instance.packageManager

            var builtInResult = ""
            var builtInVersionName = ""
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

            val webViewColor = when {
                lldWebViewStable != null && Version(builtInVersionName).isAtLeast(lldWebViewStable) -> StatusColor.NO_PROBLEM
                lldWebViewStable != null && Version(implementVersionName).isAtLeast(lldWebViewStable) -> StatusColor.WARNING
                else -> StatusColor.CRITICAL
            }

            return toColoredMyModel(
                R.string.webview_title,
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

    // The order switch arrives as a parameter: the decision is made by the ViewModel that
    // observes SettingsStore, the repository degrades to a pure function (UDF; the landing
    // spot of issues-cn #08's fifth read path)
    fun getOutdatedTargetSdkVersionApkModel(
        lld: Lld?, orderByPackageNameFirst: Boolean
    ): MyModel =
        guardedMyModel(R.string.outdated_target_version_sdk_version_apk_title) {
            val packageManager = MyApplication.instance.packageManager
            val installedApplications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.ApplicationInfoFlags.of(0)
                packageManager.getInstalledApplications(flags)
            } else {
                packageManager.getInstalledApplications(0)
            }
            var systemApkList = installedApplications.filter {
                val systemFlags = it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM)
                // On pre-release builds platform apps often (not always) target CUR_DEVELOPMENT (10000);
                // such entries stay above this threshold, lower targets are compared as usual.
                (systemFlags > 0) && (it.targetSdkVersion < AndroidVersionDataSource.currentAndroidInfo.api)
            }

            var result = MyApplication.getMyString(
                R.string.outdated_target_version_sdk_version_apk_my_first_api_level,
                propertyReader.getString(AndroidDataSource.PROP_RO_PRODUCT_FIRST_API_LEVEL)
            )

            val targetSdkVersionColor = if (systemApkList.isEmpty()) {
                result += MyApplication.getMyString(R.string.outdated_target_version_sdk_version_apk_result_none)

                if (lld != null) {
                    if (isLatestStableAndroid(lld) || isLatestPreviewAndroid(lld)) {
                        StatusColor.NO_PROBLEM
                    } else {
                        StatusColor.WARNING
                    }
                } else {
                    StatusColor.CRITICAL
                }
            } else {
                @StringRes val format = if (orderByPackageNameFirst) {
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

                StatusColor.CRITICAL
            }

            return MyModel(
                title = MyModelTitle.Res(R.string.outdated_target_version_sdk_version_apk_title),
                detail = result,
                color = targetSdkVersionColor,
                type = MyModelType.OutdatedTargetSdkApk
            )
        }

    // region [Common]
    private fun isLatestStableAndroid(lld: Lld) =
        isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.stable.api.toInt()

    private fun isLatestPreviewAndroid(lld: Lld) =
        isPreviewAndroid() && AndroidVersionDataSource.currentAndroidInfo.apiFull >= lld.android.preview.apiFull

    private fun isSupportedByUpstreamAndroid(lld: Lld) =
        isStableAndroid() && Build.VERSION.SDK_INT >= lld.android.support.api.toInt()

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

    private fun getPackageInfoOrNull(packageName: String) = try {
        appInfoDataSource.getPackageInfoOrThrow(MyApplication.instance.packageManager, packageName)
    } catch (e: Exception) {
        Log.w(javaClass.simpleName, "$packageName not found. ${e.fullMessage}")
        null
    }
    // endregion [Common]
}