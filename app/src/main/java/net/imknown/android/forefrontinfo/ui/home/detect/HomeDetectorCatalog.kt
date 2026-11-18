package net.imknown.android.forefrontinfo.ui.home.detect

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.detect.detectAll
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.home.datasource.LldLoader
import net.imknown.android.forefrontinfo.ui.home.mapper.HomeModelMapper
import net.imknown.android.forefrontinfo.ui.home.model.HomeEntryId
import net.imknown.android.forefrontinfo.ui.home.model.detector
import net.imknown.android.forefrontinfo.ui.home.model.rawDetector
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository

/**
 * One home detection run's inputs: the two settings the rows depend on. They arrive as values
 * rather than being read here, because a settings change is what restarts the run (the ViewModel
 * combines them), so the run has to see the value it was started for.
 */
data class HomeDetectionRequest(
    val allowNetwork: Boolean,
    val orderByPackageNameFirst: Boolean
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
    private val lldLoader: LldLoader
) {

    suspend fun detect(request: HomeDetectionRequest): List<MyModel> = withContext(Dispatchers.IO) {
        // Stage one: the shared inputs, fetched once per run. The lld comes from the loader's
        // fallback chain (online, saved copy, bundled asset) as one payload plus its failures.
        val (lld, source, failures) = lldLoader.load(request.allowNetwork)
        val mountQuery = repository.getMounts()

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
                { repository.getOutdatedTargetSdkVersionApkState(lld, request.orderByPackageNameFirst) }
            ) {
                listOf(mapper.toOutdatedTargetSdkVersionApkModel(it))
            }
        )

        detectors.detectAll()
    }
}
