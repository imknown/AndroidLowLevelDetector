package net.imknown.android.forefrontinfo.ui.home

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.LldManager
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository
import net.imknown.android.forefrontinfo.ui.settings.SettingsViewModel

private typealias LldAndError = Pair<Lld?, String?>

@Stable
class HomeViewModel(
    private val application: Application,
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseListViewModel() {

    companion object {
        val MY_REPOSITORY_KEY = object : CreationExtras.Key<HomeRepository> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val repository = this[MY_REPOSITORY_KEY] as HomeRepository
                val savedStateHandle = createSavedStateHandle()
                val application = this[APPLICATION_KEY] as Application
                HomeViewModel(application, repository, savedStateHandle)
            }
        }
    }

    init {
        viewModelScope.launch {
            snapshotFlow { SettingsViewModel.outdatedOrderChangedCount }
                .drop(1) // Ignore default value 0
                .collect {
                    val state = modelsState
                    if (state is State.Done) {
                        val models = state.toValue()
                        payloadOutdatedTargetSdkVersionApk(models)
                        setModels(ArrayList(models))
                    }
                }
        }
    }

    override suspend fun collectModels(): List<MyModel> {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        val allowNetwork = sharedPreferences.getBoolean(
            application.getString(R.string.function_allow_network_data_key), false
        )

        return if (allowNetwork) {
            tryDetectOnline()
        } else {
            tryDetectOffline(null)
        }
    }

    // region [Lld]
    private suspend fun tryDetectOnline(): List<MyModel> {
        val lldString = try {
            withContext(Dispatchers.IO) {
                homeRepository.fetchOnlineLldJsonStringOrThrow()
            }
        } catch (e: Exception) {
            val errorMessage = errorMessage(R.string.lld_json_fetch_failed, e)
            return tryDetectOffline(errorMessage)
        }

        val lld = try {
            withContext(Dispatchers.IO) {
                lldString.toObjectOrThrow<Lld>()
            }
        } catch (e: Exception) {
            val errorMessage = errorMessage(R.string.lld_json_parse_failed, e)
            return tryDetectOffline(errorMessage)
        }

        val errorMessage = try {
            withContext(Dispatchers.IO) {
                LldManager.saveLldJsonFileOrThrow(lldString)
            }
            null
        } catch (e: Exception) {
            errorMessage(R.string.lld_json_save_failed, e)
        }

        return detect(lld, listOf(errorMessage), R.string.lld_json_online)
    }

    private suspend fun tryDetectOffline(errorMessage: String?): List<MyModel> {
        val lldAndError = fetchOfflineLldOrNull()
        val errorMessages = listOf(errorMessage, lldAndError.second)
        return detect(lldAndError.first, errorMessages, R.string.lld_json_offline)
    }

    private suspend fun fetchOfflineLldOrNull(): LldAndError {
        try {
            withContext(Dispatchers.IO) {
                LldManager.copyJsonIfNeededOrThrow()
            }
        } catch (e: Exception) {
            val errorMessage = errorMessage(R.string.lld_json_save_failed, e)

            val lld = LldManager.getAssetLld(application.assets)
            return lld to errorMessage
        }

        val lldAndError = try {
            withContext(Dispatchers.IO) {
                homeRepository.fetchOfflineLldFileOrThrow().toObjectOrThrow<Lld>()
            } to null
        } catch (e: Exception) {
            val lld = withContext(Dispatchers.IO) {
                LldManager.getAssetLld(application.assets)
            }

            val errorMessage = errorMessage(R.string.lld_json_parse_failed, e)

            lld to errorMessage
        }

        return lldAndError
    }
    // endregion [Lld]

    private suspend fun detect(
        lld: Lld?, errorMessage: List<String?>, @StringRes modeResId: Int
    ): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        withContext(Dispatchers.Default) {
            tempModels += homeRepository.detectMode(lld, errorMessage, modeResId)
        }

        withContext(Dispatchers.Default) {
            tempModels += homeRepository.detectAndroid(lld)
            tempModels += homeRepository.detectSdkExtension(lld)
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

        return tempModels
    }

    private fun errorMessage(@StringRes messageId: Int, cause: Exception): String {
        if (BuildConfig.DEBUG) {
            cause.printStackTrace()
        }
        return application.getString(messageId, cause.fullMessage)
    }

    @MainThread
    suspend fun payloadOutdatedTargetSdkVersionApk(myModels: List<MyModel>) {
        if (myModels.isEmpty()) {
            return
        }

        val lld = fetchOfflineLldOrNull().first
            ?: return
        myModels.last().detail = withContext(Dispatchers.Default) {
            homeRepository.getOutdatedTargetSdkVersionApkModel(lld).detail
        }
    }
}
