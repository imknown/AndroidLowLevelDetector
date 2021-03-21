package net.imknown.android.forefrontinfo.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.Event
import net.imknown.android.forefrontinfo.ui.base.booleanEventLiveData
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.LldManager
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseListViewModel() {

    companion object {
        val MY_REPOSITORY_KEY = object : CreationExtras.Key<HomeRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as HomeRepository
                val savedStateHandle = createSavedStateHandle()
                HomeViewModel(repository, savedStateHandle)
            }
        }
    }

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

    override fun collectModels()  {
        val allowNetwork = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_allow_network_data_key), false
        )

        if (allowNetwork) {
            tryDetectOnline()
        } else {
            tryDetectOffline()
        }
    }

    // region [Lld]
    private fun tryDetectOnline() {
        viewModelScope.launch {
            val lldString = try {
                withContext(Dispatchers.Default) {
                    homeRepository.fetchOnlineLldJsonStringOrThrow()
                }
            } catch (e: Exception) {
                showError(R.string.lld_json_fetch_failed, e)
                tryDetectOffline()
                return@launch
            }

            val lld = try {
                lldString.toObjectOrThrow<Lld>()
            } catch (e: Exception) {
                showError(R.string.lld_json_parse_failed, e)
                tryDetectOffline()
                return@launch
            }

            tryDetect(lld, R.string.lld_json_online)

            try {
                withContext(Dispatchers.IO) {
                    LldManager.saveLldJsonFile(lldString)
                }
            } catch (e: Exception) {
                showError(R.string.lld_json_save_failed, e)
            }
        }
    }

    private fun tryDetectOffline() {
        viewModelScope.launch {
            val lld = fetchOfflineLldOrNull()
            tryDetect(lld, R.string.lld_json_offline)
        }
    }

    private suspend fun fetchOfflineLldOrNull(): Lld? {
        try {
            withContext(Dispatchers.IO) {
                LldManager.copyJsonIfNeeded()
            }
        } catch (e: Exception) {
            showError(R.string.lld_json_save_failed, e)

            val lld = LldManager.getAssetLld(MyApplication.instance.assets)
            return lld
        }

        val lld = try {
            withContext(Dispatchers.IO) {
                homeRepository.fetchOfflineLldFileOrThrow().toObjectOrThrow<Lld>()
            }
        } catch (e: Exception) {
            showError(R.string.lld_json_parse_failed, e)

            withContext(Dispatchers.IO) {
                LldManager.getAssetLld(MyApplication.instance.assets)
            }
        }

        return lld
    }
    // endregion [Lld]

    private fun tryDetect(lld: Lld?, @StringRes modeResId: Int) {
        return try {
            detectOrThrow(lld, modeResId)
        } catch (e: Exception) {
            showError(R.string.lld_json_detect_failed, e)
        }
    }

    private fun detectOrThrow(lld: Lld?, @StringRes modeResId: Int) {
        viewModelScope.launch {
            val tempModels = mutableListOf<MyModel>()

            withContext(Dispatchers.Default) {
                tempModels += homeRepository.detectMode(lld, modeResId)
            }

            if (lld == null) {
                setModels(tempModels)
                return@launch
            }

            withContext(Dispatchers.Default) {
                tempModels += homeRepository.detectAndroid(lld)
                tempModels += homeRepository.detectBuildId(lld)
                tempModels += homeRepository.detectSecurityPatches(lld)
                tempModels += homeRepository.detectPerformanceClass()
                tempModels += homeRepository.detectKernel(lld)
                tempModels += homeRepository.detectAb()
                tempModels += homeRepository.detectSar()
                tempModels += homeRepository.detectDynamicPartitions()
                tempModels += homeRepository.detectTrebleAndGsiCompatibility()
                tempModels += homeRepository.detectDsu()
                tempModels += homeRepository.detectMainline(lld)
                tempModels += homeRepository.detectVndk(lld)
                tempModels += homeRepository.detectApex()
                tempModels += homeRepository.detectDeveloperOptions()
                tempModels += homeRepository.detectAdb()
                tempModels += homeRepository.detectAdbAuthentication()
                tempModels += homeRepository.detectEncryption()
                tempModels += homeRepository.detectSELinux()
                tempModels += homeRepository.detectToybox(lld)
                tempModels += homeRepository.detectWebView(lld)
                tempModels += homeRepository.getOutdatedTargetSdkVersionApkModel(lld)
            }

            setModels(tempModels)
        }
    }

    fun payloadOutdatedTargetSdkVersionApk(myModels: List<MyModel>) {
        if (myModels.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val lld = fetchOfflineLldOrNull()
                ?: return@launch
            myModels.last().detail = withContext(Dispatchers.Default) {
                homeRepository.getOutdatedTargetSdkVersionApkModel(lld).detail
            }
            _showOutdatedOrderEvent.value = Event(Unit)
        }
    }
}