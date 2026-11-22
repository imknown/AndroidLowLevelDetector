package net.imknown.android.forefrontinfo.ui.others

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import net.imknown.android.forefrontinfo.ui.base.list.BaseListViewModel
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.others.detect.OthersDetectorCatalog

// Metro wiring: the three annotations replace the former companion Factory.
// @ViewModelKey + @ContributesIntoMap land this class in the ViewModel multibinding map that
// AppViewModelFactory consults; binding<ViewModel>() is REQUIRED because Metro binds to the
// direct supertype by default (BaseListViewModel here) and a wrong map key only surfaces at
// runtime. Deliberately not @SingleIn: the lifetime belongs to the Nav3 entry's ViewModelStore.
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class OthersViewModel(
    private val detectorCatalog: OthersDetectorCatalog
) : BaseListViewModel() {

    override suspend fun collectModels(): List<MyModel> = detectorCatalog.detect()
}
