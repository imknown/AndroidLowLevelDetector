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
import net.imknown.android.forefrontinfo.ui.base.detect.detectAll
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.model.PropEntryId
import net.imknown.android.forefrontinfo.ui.prop.model.rawDetector
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

// Metro wiring: the three annotations replace the former companion Factory.
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

    override suspend fun collectModels(): List<MyModel> = withContext(Dispatchers.IO) {
        // The entry list is data (ids are the PropEntryId semantic ids;
        // list order is the display order); every source exists on every device, so there
        // are no preconditions.
        val detectors = listOf(
            rawDetector(PropEntryId.PROP_SYSTEM_PROP) { propRepository.getSystemProp() },
            rawDetector(PropEntryId.PROP_SETTINGS_SYSTEM) {
                propRepository.getSettings(Settings.System::class)
            },
            rawDetector(PropEntryId.PROP_SETTINGS_SECURE) {
                propRepository.getSettings(Settings.Secure::class)
            },
            rawDetector(PropEntryId.PROP_SETTINGS_GLOBAL) {
                propRepository.getSettings(Settings.Global::class)
            },
            rawDetector(PropEntryId.PROP_BUILD_PROP) { propRepository.getBuildProp() }
        )

        detectors.detectAll()
    }
}
