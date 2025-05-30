package net.imknown.android.forefrontinfo.ui.base.list

import androidx.annotation.MainThread
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.ui.base.BaseViewModel
import net.imknown.android.forefrontinfo.ui.common.State

@Stable
abstract class BaseListViewModel : BaseViewModel() {
    var modelsState by mutableStateOf<State<List<MyModel>>>(State.NotInitialized)
        private set

    var loading by mutableStateOf(false)
        private set

    private var refreshJob: Job? = null

    abstract suspend fun collectModels(): List<MyModel>

    fun init() {
        if (hasNoData() && !loading) {
            refresh()
        }
    }

    fun refresh() {
        if (loading) return

        loading = true

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val list = collectModels()
                setModels(list)
            } finally {
                loading = false
            }
        }
    }

    fun hasNoData() = modelsState == State.NotInitialized

    @MainThread
    fun setModels(tempModels: List<MyModel>) {
        modelsState = State.Done(tempModels)
    }
}
