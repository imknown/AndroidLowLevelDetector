package net.imknown.android.forefrontinfo.ui.home.detect

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.detect.detectAll
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModelType
import net.imknown.android.forefrontinfo.ui.home.datasource.LldLoader
import net.imknown.android.forefrontinfo.ui.home.mapper.HomeModelMapper
import net.imknown.android.forefrontinfo.ui.home.model.HomeEntryId
import net.imknown.android.forefrontinfo.ui.home.model.OutdatedTargetSdkVersionApkState
import net.imknown.android.forefrontinfo.ui.home.model.detector
import net.imknown.android.forefrontinfo.ui.home.model.rawDetector
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsStore

/**
 * One home detection run's inputs. Only the network switch travels as a value, because only the
 * load path reads it, at the moment the run starts.
 */
data class HomeDetectionRequest(
    val allowNetwork: Boolean
)

/**
 * One run's rows plus what a later re-render needs: the value the order switch had when this run
 * rendered its row (so a switch that moved mid-run can be noticed), and the domain object behind
 * that row. A failed entry still produces a [DetectResult.Failed] here (the repository guards
 * itself), and its failure row carries no [MyModelType.OutdatedTargetSdkApk], so re-rendering
 * simply finds no row to replace; the null case only covers an entry that never ran.
 */
data class HomeDetectionResult(
    val rows: List<MyModel>,
    val orderByPackageNameFirst: Boolean,
    val outdatedTargetSdk: DetectResult<OutdatedTargetSdkVersionApkState>?
)

/**
 * The home page's detection orchestration, taken out of the ViewModel (issues-cn #11's second
 * half): which entries exist and in what order, the shared inputs they run on, and the lld
 * fallback chain that feeds them. The ViewModel derives state from this result and keeps nothing
 * of the sequence itself.
 */
@Inject
class HomeDetectorCatalog(
    private val repository: HomeRepository,
    private val mapper: HomeModelMapper,
    private val lldLoader: LldLoader,
    private val settingsStore: SettingsStore
) {

    suspend fun detect(request: HomeDetectionRequest): HomeDetectionResult = withContext(Dispatchers.IO) {
        // Stage one: the shared inputs, fetched once per run. The lld comes from the loader's
        // fallback chain (online, saved copy, bundled asset) as one payload plus its failures.
        val (lld, source, failures) = lldLoader.load(request.allowNetwork)
        val mountQuery = repository.getMounts()

        // The order switch only decides how the outdated-target row reads, so it is read here to
        // render that row once, and no further: a switch that moves while this run is going is
        // caught by the caller, which re-renders that single row against this result.
        val orderByPackageNameFirst = settingsStore.outdatedOrderFirst.value
        var outdatedTargetSdk: DetectResult<OutdatedTargetSdkVersionApkState>? = null

        // Stage two: the entry list is data (ids are the enum's semantic ids, list order is
        // display order), and the engine runs it serially with the per-entry guard.
        val detectors = listOf(
            detector(
                HomeEntryId.HOME_DETECT_MODE,
                { repository.detectMode(lld, failures, source) }
            ) {
                listOf(mapper.toModeModel(it))
            },
            detector(HomeEntryId.HOME_ANDROID_VERSION, { repository.detectAndroid(lld) }) {
                listOf(mapper.toAndroidModel(it))
            },
            detector(HomeEntryId.HOME_SDK_EXTENSION_VERSION, { repository.detectSdkExtension(lld) }) {
                listOf(mapper.toSdkExtensionModel(it))
            },
            detector(HomeEntryId.HOME_BUILD_ID, { repository.detectBuildId(lld) }) {
                listOf(mapper.toBuildIdModel(it))
            },
            rawDetector(
                HomeEntryId.HOME_SECURITY_PATCH,
                run = { mapper.toSecurityPatchModels(repository.detectSecurityPatches(lld)) },
                failureRows = mapper::toSecurityPatchFailureRows
            ),
            detector(HomeEntryId.HOME_PERFORMANCE_CLASS, { repository.detectPerformanceClass() }) {
                listOf(mapper.toPerformanceClassModel(it))
            },
            detector(HomeEntryId.HOME_KERNEL, { repository.detectKernel(lld) }) {
                listOf(mapper.toKernelModel(it))
            },
            detector(HomeEntryId.HOME_AB_UPDATE, { repository.detectAb() }) {
                listOf(mapper.toAbModel(it))
            },
            detector(HomeEntryId.HOME_SAR, { repository.detectSar(mountQuery) }) {
                listOf(mapper.toSarModel(it))
            },
            detector(HomeEntryId.HOME_DYNAMIC_PARTITIONS, { repository.detectDynamicPartitions() }) {
                listOf(mapper.toDynamicPartitionsModel(it))
            },
            rawDetector(
                HomeEntryId.HOME_TREBLE_GSI,
                run = { mapper.toTrebleAndGsiModels(repository.detectTrebleAndGsiCompatibility()) },
                failureRows = mapper::toTrebleAndGsiFailureRows
            ),
            detector(HomeEntryId.HOME_DSU, { repository.detectDsu() }) {
                listOf(mapper.toDsuModel(it))
            },
            detector(HomeEntryId.HOME_MAINLINE, { repository.detectMainline(lld) }) {
                listOf(mapper.toMainlineModel(it))
            },
            detector(HomeEntryId.HOME_VNDK, { repository.detectVndk(lld) }) {
                listOf(mapper.toVndkModel(it))
            },
            detector(HomeEntryId.HOME_APEX, { repository.detectApex(mountQuery) }) {
                listOf(mapper.toApexModel(it))
            },
            detector(HomeEntryId.HOME_DEVELOPER_OPTIONS, { repository.detectDeveloperOptions() }) {
                listOf(mapper.toDeveloperOptionsModel(it))
            },
            detector(HomeEntryId.HOME_ADB, { repository.detectAdb() }) {
                listOf(mapper.toAdbModel(it))
            },
            detector(HomeEntryId.HOME_ADB_AUTHENTICATION, { repository.detectAdbAuthentication() }) {
                listOf(mapper.toAdbAuthenticationModel(it))
            },
            detector(HomeEntryId.HOME_ENCRYPTION, { repository.detectEncryption() }) {
                listOf(mapper.toEncryptionModel(it))
            },
            detector(HomeEntryId.HOME_SELINUX, { repository.detectSELinux() }) {
                listOf(mapper.toSELinuxModel(it))
            },
            detector(HomeEntryId.HOME_TOYBOX, { repository.detectToybox(lld) }) {
                listOf(mapper.toToyboxModel(it))
            },
            detector(HomeEntryId.HOME_WEBVIEW, { repository.detectWebView(lld) }) {
                listOf(mapper.toWebViewModel(it))
            },
            detector(
                HomeEntryId.HOME_OUTDATED_TARGET_SDK_APK,
                // The one entry whose result is kept: the order switch re-renders this row from
                // it, so the run's list and the re-render share one detection.
                { repository.getOutdatedTargetSdkVersionApkState(lld).also { outdatedTargetSdk = it } }
            ) {
                listOf(mapper.toOutdatedTargetSdkVersionApkModel(it, orderByPackageNameFirst))
            }
        )

        HomeDetectionResult(
            rows = detectors.detectAll(),
            orderByPackageNameFirst = orderByPackageNameFirst,
            outdatedTargetSdk = outdatedTargetSdk
        )
    }

    /**
     * The detail the outdated-target row renders with this order. Null only when the entry never
     * ran; a failed entry yields its failure-row text, which the caller cannot apply because that
     * row carries no OutdatedTargetSdkApk type.
     */
    fun outdatedRowDetail(result: HomeDetectionResult, orderByPackageNameFirst: Boolean): String? =
        result.outdatedTargetSdk
            ?.let { mapper.toOutdatedTargetSdkVersionApkModel(it, orderByPackageNameFirst).detail }

    /** The same rows with the outdated-target row re-rendered; every other row is untouched. */
    fun rowsWithOutdatedRow(
        result: HomeDetectionResult,
        orderByPackageNameFirst: Boolean
    ): List<MyModel> {
        val state = result.outdatedTargetSdk ?: return result.rows
        val row = mapper.toOutdatedTargetSdkVersionApkModel(state, orderByPackageNameFirst)
        return result.rows.map { if (it.type == MyModelType.OutdatedTargetSdkApk) row else it }
    }
}
