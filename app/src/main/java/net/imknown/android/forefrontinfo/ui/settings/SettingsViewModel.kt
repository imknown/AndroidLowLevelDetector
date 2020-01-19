package net.imknown.android.forefrontinfo.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.JsonIo
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.BaseViewModel
import net.imknown.android.forefrontinfo.base.IAndroidVersion
import net.imknown.android.forefrontinfo.base.SingleEvent

class SettingsViewModel : BaseViewModel(), IAndroidVersion {

    private var counter = 5

    val version by lazy { MutableLiveData<String>() }
    val versionClick by lazy { MutableLiveData<SingleEvent<Int>>() }

    fun setMyTheme(themesValue: Any) = viewModelScope.launch(Dispatchers.IO) {
        MyApplication.instance.setMyTheme(themesValue.toString())
    }

    fun setBuiltInDataVersion() = viewModelScope.launch(Dispatchers.IO) {
        val assetLldVersion = try {
            JsonIo.getAssetLldVersion(MyApplication.instance.assets)
        } catch (e: Exception) {
            e.printStackTrace()

            MyApplication.getMyString(android.R.string.unknownName)
        }

        withContext(Dispatchers.Main) {
            version.value = MyApplication.getMyString(
                R.string.about_version_summary,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                assetLldVersion
            )
        }
    }

    fun versionClicked() = viewModelScope.launch(Dispatchers.Default) {
        if (counter > 0) {
            counter -= 1
        } else if (counter == 0) {
            counter -= 100

            withContext(Dispatchers.Main) {
                versionClick.value = SingleEvent(0)
            }
        }
    }
}