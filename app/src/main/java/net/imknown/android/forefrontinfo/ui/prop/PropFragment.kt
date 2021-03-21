package net.imknown.android.forefrontinfo.ui.prop

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.BaseListFragment
import net.imknown.android.forefrontinfo.ui.prop.datasource.PropertiesDataSource
import net.imknown.android.forefrontinfo.ui.prop.datasource.SettingsDataSource
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

class PropFragment : BaseListFragment() {

    companion object {
        fun newInstance() = PropFragment()
    }

    override val listViewModel by viewModels<PropViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = PropRepository(PropertiesDataSource(), SettingsDataSource())
                this[PropViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            PropViewModel.Factory
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeLanguageEvent(MyApplication.propLanguageEvent)
    }
}