package net.imknown.android.forefrontinfo.ui.others.detect

import android.os.Build
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.detectAll
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.model.OthersEntryId
import net.imknown.android.forefrontinfo.ui.others.model.rawDetector
import net.imknown.android.forefrontinfo.ui.others.repository.OthersRepository

/**
 * The others page's detection list (the others part of issues-cn #03): which entries exist, in
 * what order, and their platform gates. Others has no shared input, every entry fetches its
 * own, so there is no request type (compare HomeDetectorCatalog's HomeDetectionRequest).
 */
@Inject
class OthersDetectorCatalog(
    private val othersRepository: OthersRepository
) {

    suspend fun detect(): List<MyModel> = withContext(Dispatchers.IO) {
        // The entry list is data (ids are the OthersEntryId semantic ids, list order is display
        // order); the SDK-gated entries declare their precondition, and the gated calls repeat the gate where lint can see it.
        val detectors = listOf(
            // region [Basic]
            rawDetector(OthersEntryId.OTH_BRAND) { listOf(othersRepository.getBrand()) },
            rawDetector(OthersEntryId.OTH_MANUFACTURER) { listOf(othersRepository.getManufacturer()) },
            rawDetector(OthersEntryId.OTH_MODEL) { listOf(othersRepository.getModel()) },
            rawDetector(OthersEntryId.OTH_DEVICE) { listOf(othersRepository.getDevice()) },
            rawDetector(OthersEntryId.OTH_PRODUCT) { listOf(othersRepository.getProduct()) },
            rawDetector(OthersEntryId.OTH_HARDWARE) { listOf(othersRepository.getHardware()) },
            rawDetector(OthersEntryId.OTH_BOARD) { listOf(othersRepository.getBoard()) },
            rawDetector(OthersEntryId.OTH_SOC_MODEL, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }) {
                // The same SDK_INT gate as the precondition above, repeated where lint's
                // NewApi check can see it (a precondition is data; lint requires a visible
                // runtime gate at the call site). The else is unreachable: the engine skips
                // the entry when the gate is false.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(othersRepository.getSocModel())
                } else {
                    error("unreachable: the precondition gates this entry")
                }
            },
            rawDetector(OthersEntryId.OTH_SOC_MANUFACTURER, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }) {
                // The same SDK_INT gate as the precondition above, repeated where lint's
                // NewApi check can see it (a precondition is data; lint requires a visible
                // runtime gate at the call site). The else is unreachable: the engine skips
                // the entry when the gate is false.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(othersRepository.getSocManufacturer())
                } else {
                    error("unreachable: the precondition gates this entry")
                }
            },
            rawDetector(OthersEntryId.OTH_SKU, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }) {
                // The same SDK_INT gate as the precondition above, repeated where lint's
                // NewApi check can see it (a precondition is data; lint requires a visible
                // runtime gate at the call site). The else is unreachable: the engine skips
                // the entry when the gate is false.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(othersRepository.getSku())
                } else {
                    error("unreachable: the precondition gates this entry")
                }
            },
            rawDetector(OthersEntryId.OTH_VENDOR_SKU, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }) { listOf(othersRepository.getVendorSku()) },
            rawDetector(OthersEntryId.OTH_ODM_SKU, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            }) {
                // The same SDK_INT gate as the precondition above, repeated where lint's
                // NewApi check can see it (a precondition is data; lint requires a visible
                // runtime gate at the call site). The else is unreachable: the engine skips
                // the entry when the gate is false.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(othersRepository.getOdmSku())
                } else {
                    error("unreachable: the precondition gates this entry")
                }
            },
            // endregion [Basic]

            // region [Arch & ABI]
            // region [Binder]
            rawDetector(OthersEntryId.OTH_BINDER_STATUS) {
                listOf(othersRepository.getBinderStatus(ArchitectureDataSource.DRIVER_BINDER))
            },
            // endregion [Binder]

            // region [Process]
            rawDetector(OthersEntryId.OTH_PROCESS_BIT) { listOf(othersRepository.getProcessBit()) },
            rawDetector(OthersEntryId.OTH_PROCESS_ARCHITECTURE) { listOf(othersRepository.getArchitecture()) },
            // endregion [Process]

            // region [ABI]
            rawDetector(OthersEntryId.OTH_CPU_ABI_BUILD) { listOf(othersRepository.getCpuAbi()) },
            rawDetector(OthersEntryId.OTH_CPU_ABI_PROPERTY) { listOf(othersRepository.getPropertyCpuAbi()) },
            rawDetector(OthersEntryId.OTH_SUPPORTED_32_BIT_ABIS) { listOf(othersRepository.getSupported32BitAbis()) },
            rawDetector(OthersEntryId.OTH_SUPPORTED_64_BIT_ABIS) { listOf(othersRepository.getSupported64BitAbis()) },
            // endregion [ABI]
            // endregion [Arch & ABI]

            // region [ROM]
            rawDetector(OthersEntryId.OTH_BUILD_USER) { listOf(othersRepository.getUser()) },
            rawDetector(OthersEntryId.OTH_BUILD_HOST) { listOf(othersRepository.getHost()) },
            rawDetector(OthersEntryId.OTH_BUILD_TIME) { listOf(othersRepository.getTime()) },
            rawDetector(OthersEntryId.OTH_BASE_OS) { listOf(othersRepository.getBaseOs()) },
            // region [Fingerprints]
            rawDetector(OthersEntryId.OTH_FINGERPRINT) { listOf(othersRepository.getFingerprint()) },
            rawDetector(OthersEntryId.OTH_PREVIEW_SDK_FINGERPRINT, precondition = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }) { listOf(othersRepository.getPreviewSdkFingerprint()) },
            rawDetector(OthersEntryId.OTH_PARTITION_FINGERPRINTS) {
                othersRepository.getPartitionFingerprints()
            },
            // endregion [Fingerprints]
            rawDetector(OthersEntryId.OTH_BUILD_ID) { listOf(othersRepository.getId()) },
            rawDetector(OthersEntryId.OTH_DISPLAY) { listOf(othersRepository.getDisplay()) },
            rawDetector(OthersEntryId.OTH_BUILD_TYPE) { listOf(othersRepository.getType()) },
            rawDetector(OthersEntryId.OTH_BUILD_TAGS) { listOf(othersRepository.getTags()) },
            rawDetector(OthersEntryId.OTH_INCREMENTAL) { listOf(othersRepository.getIncremental()) },
            rawDetector(OthersEntryId.OTH_CODENAME) { listOf(othersRepository.getCodename()) },
            rawDetector(OthersEntryId.OTH_PREVIEW_SDK_INT) { listOf(othersRepository.getPreviewSdkInt()) },
            rawDetector(OthersEntryId.OTH_DEFAULT_USER_AGENT) {
                listOf(othersRepository.getDefaultUserAgent(MyApplication.instance))
            },
            rawDetector(OthersEntryId.OTH_KERNEL_VERSION) { listOf(othersRepository.getKernelVersion()) },
            // endregion [ROM]

            // region [Others]
            rawDetector(OthersEntryId.OTH_BOOTLOADER) { listOf(othersRepository.getBootloader()) },
            rawDetector(OthersEntryId.OTH_RADIO_VERSION) { listOf(othersRepository.getRadioVersionOrNull()) },
            // endregion [Others]
        )

        detectors.detectAll()
    }
}
