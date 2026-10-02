package net.imknown.android.forefrontinfo.ui.prop

import android.provider.Settings
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

// Metro wiring (issues-cn #05): the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseListViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class PropViewModel(
    private val propRepository: PropRepository
) : BaseListViewModel() {

    override suspend fun collectModels(): List<MyModel> {
        val tempModels = mutableListOf<MyModel>()

        withContext(Dispatchers.Default) {
            tempModels += propRepository.getSystemProp()

            tempModels += propRepository.getSettings(Settings.System::class)
            tempModels += propRepository.getSettings(Settings.Secure::class)
            tempModels += propRepository.getSettings(Settings.Global::class)

            tempModels += propRepository.getBuildProp()
        }

        return tempModels
    }
}