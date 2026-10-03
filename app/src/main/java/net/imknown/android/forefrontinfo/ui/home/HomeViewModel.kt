package net.imknown.android.forefrontinfo.ui.home

import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.extension.isLldDatetime
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.home.datasource.MountDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.repository.HomeRepository

private data class LldAndError(val lld: Lld?, val message: String?)

// Stable (not Immutable): instance identity never changes and UI-visible state lives in the
// observed StateFlow; SavedStateHandle is restore-only storage, never read for composition,
// so promising stability is safe (same as BaseListViewModel).
// Metro wiring: the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseListViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Stable
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val sharedPreferences: SharedPreferences
) : BaseListViewModel() {

    override suspend fun collectModels(): List<MyModel> {
        // Stamp this load with the current preference generation (compared in onModelsLoaded)
        loadStartGeneration = outdatedOrderChanges.value

        val allowNetwork = sharedPreferences.getBoolean(
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
        sharedPreferences
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
        sharedPreferences
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

        // The upstream `version` string is outside this app's control; the format contract is
        // machine-checked (LLD_DATETIME_FORMATTER). An unexpected format means the payload
        // cannot be trusted — degrade to the built-in offline data instead of blowing up in
        // the middle of detect().
        if (!lld.version.isLldDatetime()) {
            val errorMessage = errorMessage(
                R.string.lld_json_parse_failed,
                IOException("Unsupported lld.version format: ${lld.version}")
            )
            return tryDetectOffline(errorMessage)
        }

        val errorMessage = try {
            withContext(Dispatchers.IO) {
                homeRepository.saveLldJsonFileOrThrow(lldString)
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
                homeRepository.copyJsonIfNeededOrThrow()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val lld = getAssetLldOrNull()

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
            val lld = getAssetLldOrNull()

            val errorMessage = errorMessage(R.string.lld_json_parse_failed, e)

            LldAndError(lld, errorMessage)
        }

        return lldAndError
    }

    // Last link of the offline fallback chain: even the built-in asset read failing must not
    // escape — a null lld renders as the "unknown" row, not a crash.
    private suspend fun getAssetLldOrNull(): Lld? = try {
        withContext(Dispatchers.IO) {
            homeRepository.getAssetLld(MyApplication.instance.assets)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
        null
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