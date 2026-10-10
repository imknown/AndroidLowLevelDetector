package net.imknown.android.forefrontinfo.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

// The factory MetroX requires, replacing the four hand-written companion
// `Factory`s: MetroViewModelFactory's create() resolves ViewModels by the
// KClasses in the three maps below. The three constructor parameters cover the
// abstract class's protected vals and must all be supplied even though the two
// assisted maps are empty today, since ViewModelGraph declares them as
// @Multibinds(allowEmpty = true) precisely for that.
//
// The day some ViewModel adopts @AssistedInject (a CreationExtras/
// SavedStateHandle style of construction), the assisted maps light up
// automatically and this class needs zero changes.
//
// @ContributesBinding binds this class as *the* MetroViewModelFactory, which is
// what AppGraph's metroViewModelFactory accessor resolves; @SingleIn caches one
// factory instance with the graph. ViewModels themselves deliberately get no
// @SingleIn (see AppGraph).
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class AppViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
