package net.imknown.android.forefrontinfo.ui.home

import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.home.datasource.LldDataSource
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.LldManager
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository

private data class LldAndError(val lld: Lld?, val message: String?)

// Stable (not Immutable): instance identity never changes and UI-visible state lives in the
// observed StateFlow; SavedStateHandle is restore-only storage, never read for composition,
// so promising stability is safe (same as BaseListViewModel).
@Stable
class HomeViewModel(
    private val homeRepository: HomeRepository
) : BaseListViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    HomeRepository(LldDataSource(), MountDataSource(), AppInfoDataSource())
                )
            }
        }
    }

    override suspend fun collectModels(): List<MyModel> {
        // Stamp this load with the current preference generation (compared in onModelsLoaded)
        loadStartGeneration = outdatedOrderChanges.value

        val allowNetwork = MyApplication.sharedPreferences.getBoolean(
            MyApplication.getMyString(R.string.function_allow_network_data_key), false
        )

        return if (allowNetwork) {
            tryDetectOnline()
        } else {
            tryDetectOffline(null)
        }
    }

    // region [Outdated order switch]
    // SharedPreferences is the single source of truth (Settings only writes it); Home observes
    // its own key — no static event bus a writer could forget to fire. The counter only means
    // "the key changed"; StateFlow conflation is exactly right here: rapid toggles collapse
    // into one recompute, which reads the latest stored value anyway.
    private val outdatedOrderChanges = MutableStateFlow(0)

    private val outdatedOrderChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MyApplication.getMyString(
                    R.string.function_outdated_target_order_by_package_name_first_key
                )
            ) {
                outdatedOrderChanges.update { it + 1 }
            }
        }

    // The preference generation a load started building its list with; compared when that
    // list lands (onModelsLoaded).
    private var loadStartGeneration = 0

    init {
        MyApplication.sharedPreferences
            .registerOnSharedPreferenceChangeListener(outdatedOrderChangeListener)

        // Rule 1 — live update: a toggle while any list is on screen (the initial data, or the
        // still-visible previous data during a pull-to-refresh) re-syncs that entry at once.
        // Before the first load lands there is nothing to patch, and the load itself reads the
        // current preference anyway.
        viewModelScope.launch {
            outdatedOrderChanges.collect {
                if (modelsStateFlow.value != null) {
                    payloadOutdatedTargetSdkVersionApk()
                }
            }
        }
    }

    override fun onCleared() {
        MyApplication.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(outdatedOrderChangeListener)
        super.onCleared()
    }
    // endregion [Outdated order switch]

    // region [Lld]
    private suspend fun tryDetectOnline(): List<MyModel> {
        val lldString = try {
            withContext(Dispatchers.IO) {
                homeRepository.fetchOnlineLldJsonStringOrThrow()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = errorMessage(R.string.lld_json_fetch_failed, e)
            return tryDetectOffline(errorMessage)
        }

        val lld = try {
            withContext(Dispatchers.IO) {
                lldString.toObjectOrThrow<Lld>()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = errorMessage(R.string.lld_json_parse_failed, e)
            return tryDetectOffline(errorMessage)
        }

        val errorMessage = try {
            withContext(Dispatchers.IO) {
                LldManager.saveLldJsonFileOrThrow(lldString)
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage(R.string.lld_json_save_failed, e)
        }

        return detect(lld, listOf(errorMessage), R.string.lld_json_online)
    }

    private suspend fun tryDetectOffline(errorMessage: String?): List<MyModel> {
        val lldAndError = fetchOfflineLldOrNull()
        val errorMessages = listOf(errorMessage, lldAndError.message)
        return detect(lldAndError.lld, errorMessages, R.string.lld_json_offline)
    }

    private suspend fun fetchOfflineLldOrNull(): LldAndError {
        try {
            withContext(Dispatchers.IO) {
                LldManager.copyJsonIfNeededOrThrow()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val lld = withContext(Dispatchers.IO) {
                LldManager.getAssetLld(MyApplication.instance.assets)
            }

            val errorMessage = errorMessage(R.string.lld_json_save_failed, e)

            return LldAndError(lld, errorMessage)
        }

        val lldAndError = try {
            val lld = withContext(Dispatchers.IO) {
                homeRepository.fetchOfflineLldFileOrThrow().toObjectOrThrow<Lld>()
            }

            LldAndError(lld, null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val lld = withContext(Dispatchers.IO) {
                LldManager.getAssetLld(MyApplication.instance.assets)
            }

            val errorMessage = errorMessage(R.string.lld_json_parse_failed, e)

            LldAndError(lld, errorMessage)
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
            val mounts = withContext(Dispatchers.IO) {
                homeRepository.getMounts()
            }
            tempModels += homeRepository.detectSar(mounts)
            tempModels += homeRepository.detectDynamicPartitions()
            tempModels += homeRepository.detectTrebleAndGsiCompatibility()
            tempModels += homeRepository.detectDsu()
            tempModels += homeRepository.detectMainline(lld)
            tempModels += homeRepository.detectVndk(lld)
            tempModels += homeRepository.detectApex(mounts)
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
        return MyApplication.getMyString(messageId, cause.fullMessage)
    }

    // Rule 2 — load landing: a toggle while the list was being built (initial load or
    // pull-to-refresh) is not covered by Rule 1's patch of the old list — the builder read the
    // preference at some point mid-build, so the freshly landed entry can lag one toggle
    // behind. Re-sync it with the current stored value in that case.
    override fun onModelsLoaded() {
        if (outdatedOrderChanges.value != loadStartGeneration) {
            viewModelScope.launch {
                payloadOutdatedTargetSdkVersionApk()
            }
        }
    }

    @MainThread
    suspend fun payloadOutdatedTargetSdkVersionApk() {
        val lld = fetchOfflineLldOrNull().lld
            ?: return

        val newDetail = withContext(Dispatchers.Default) {
            homeRepository.getOutdatedTargetSdkVersionApkModel(lld).detail
        }

        val list = modelsStateFlow.value ?: return
        val targetIndex = list.indexOfFirst {
            it.type == OutdatedTargetSdkApk
        }
        if (targetIndex == -1) {
            return
        }

        updateModelDetail(targetIndex, newDetail)
    }
}