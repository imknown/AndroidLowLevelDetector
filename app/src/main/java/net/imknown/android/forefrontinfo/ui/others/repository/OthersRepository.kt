package net.imknown.android.forefrontinfo.ui.others.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import net.imknown.android.forefrontinfo.ui.base.IRepository
import net.imknown.android.forefrontinfo.ui.others.datasource.ArchitectureDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.BasicDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.KernelDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.OthersDataSource
import net.imknown.android.forefrontinfo.ui.others.datasource.RomDataSource

class OthersRepository(
    private val basicDataSource: BasicDataSource,
    private val architectureDataSource: ArchitectureDataSource,
    private val romDataSource: RomDataSource,
    private val fingerprintDataSource: FingerprintDataSource,
    private val kernelDataSource: KernelDataSource,
    private val othersDataSource: OthersDataSource
) : IRepository {
    // region [Basic]
    fun getBrand() = basicDataSource.getBrand()
    fun getManufacturer() = basicDataSource.getManufacturer()
    fun getModel() = basicDataSource.getModel()
    fun getDevice() = basicDataSource.getDevice()
    fun getProduct() = basicDataSource.getProduct()
    fun getHardware() = basicDataSource.getHardware()
    fun getBoard() = basicDataSource.getBoard()

    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocModel() = basicDataSource.getSocModel()
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocManufacturer() = basicDataSource.getSocManufacturer()
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSku() = basicDataSource.getSku()
    fun getVendorSku() = basicDataSource.getVendorSku()
    @RequiresApi(Build.VERSION_CODES.S)
    fun getOdmSku() = basicDataSource.getOdmSku()
    // endregion [Basic]

    // region [Binder]
    fun getBinderVersionOrThrow(driver: String) = architectureDataSource.getBinderVersionOrThrow(driver)
    // endregion [Binder]

    // region [Process]
    fun isProcess64BitOrThrow() = architectureDataSource.isProcess64BitOrThrow()
    fun getArchitectureOrNullOrThrow() = architectureDataSource.getArchitectureOrNullOrThrow()
    // endregion [Process]

    // region [ABI]
    fun getCpuAbi() = architectureDataSource.getCpuAbi()
    fun getPropertyCpuAbi() = architectureDataSource.getPropertyCpuAbi()
    fun getSupported32BitAbis() = architectureDataSource.getSupported32BitAbis()
    fun getSupported64BitAbis() = architectureDataSource.getSupported64BitAbis()
    // endregion [ABI]

    // region [ROM]
    fun getUser() = romDataSource.getUser()
    fun getHost() = romDataSource.getHost()
    fun getTime() = romDataSource.getTime()
    @RequiresApi(Build.VERSION_CODES.M)
    fun getBaseOs() = romDataSource.getBaseOs()

    fun getId() = romDataSource.getId()
    fun getDisplay() = romDataSource.getDisplay()
    fun getType() = romDataSource.getType()
    fun getTags() = romDataSource.getTags()
    fun getIncremental() = romDataSource.getIncremental()
    fun getCodename() = romDataSource.getCodename()
    @RequiresApi(Build.VERSION_CODES.M)
    fun getPreviewDdkInt() = romDataSource.getPreviewDdkInt()

    // region [Fingerprint]
    fun getFingerprint() = fingerprintDataSource.getFingerprint()
    fun getPreviewSdkFingerprint() = fingerprintDataSource.getPreviewSdkFingerprint()

    fun getPartitions() = fingerprintDataSource.getPartitions()
    fun partitionFingerprint(name: String) = fingerprintDataSource.getPartitionFingerprint(name)
    fun getPartitionFingerprintProperty(partitionFingerprintProperty: String) =
        fingerprintDataSource.getPartitionFingerprintProperty(partitionFingerprintProperty)
    // endregion [Fingerprint]

    fun getDefaultUserAgent(context: Context) = romDataSource.getDefaultUserAgent(context)

    // region [Kernel]
    fun getKernelVersion() = kernelDataSource.getKernelVersion()
    fun getKernelAll() = kernelDataSource.getKernelAll()
    // endregion [Kernel]
    // endregion [ROM]

    // region [Others]
    fun getBootloader() = othersDataSource.getBootloader()
    fun getRadioVersionOrNull() = othersDataSource.getRadioVersionOrNull()
    // endregion [Others]
}