package net.imknown.android.forefrontinfo.ui.others.datasource

import android.os.Build
import androidx.annotation.RequiresApi
import dev.zacsweers.metro.Inject
import net.imknown.android.forefrontinfo.ui.common.PropertyReader
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource

@Inject
class BasicDataSource(private val propertyReader: PropertyReader) {
    fun getBrand(): String = Build.BRAND
    fun getManufacturer(): String = Build.MANUFACTURER
    fun getModel(): String = Build.MODEL
    fun getDevice(): String = Build.DEVICE
    fun getProduct(): String = Build.PRODUCT
    fun getHardware(): String = Build.HARDWARE
    fun getBoard(): String = Build.BOARD

    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocModel(): String = Build.SOC_MODEL
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSocManufacturer(): String = Build.SOC_MANUFACTURER
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSku(): String = Build.SKU
    fun getVendorSku() = propertyReader.getString(AndroidDataSource.PROP_VENDOR_SKU)
    @RequiresApi(Build.VERSION_CODES.S)
    fun getOdmSku(): String = Build.ODM_SKU
}