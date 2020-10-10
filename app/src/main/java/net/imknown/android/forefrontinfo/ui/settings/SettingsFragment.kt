package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.EventObserver
import net.imknown.android.forefrontinfo.ui.base.IFragmentView

class SettingsFragment : PreferenceFragmentCompat(), IFragmentView {

    companion object {
        suspend fun newInstance() = withContext(Dispatchers.Main) {
            SettingsFragment()
        }
    }

    override val visualContext by lazy { context }

    private val settingsViewModel by activityViewModels<SettingsViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            Looper.myLooper() ?: Looper.prepare()
            setPreferencesFromResource(R.xml.preferences, rootKey)
            Looper.myLooper()?.quit()

            whenCreated {
                initViews()
            }
        }
    }

    private fun initViews() {
        settingsViewModel.scrollBarModeChangeEvent.observe(viewLifecycleOwner, EventObserver {
            it?.let { themesValue ->
                settingsViewModel.setScrollBarMode(themesValue)
            }
        })

        settingsViewModel.changeScrollBarModeEvent.observe(viewLifecycleOwner, EventObserver {
            listView.isVerticalScrollBarEnabled = it
        })

        val versionPref =
            findPreference<Preference>(MyApplication.getMyString(R.string.about_version_key))!!
        settingsViewModel.version.observe(viewLifecycleOwner) {
            versionPref.summary = MyApplication.getMyString(
                it.id,
                it.versionName,
                it.versionCode,
                it.assetLldVersion,
                it.distributor,
                it.installer,
                it.firstInstallTime,
                it.lastUpdateTime
            )
        }

        settingsViewModel.versionClick.observe(viewLifecycleOwner) {
            toast(R.string.about_version_click)
        }

        settingsViewModel.themesPrefChangeEvent.observe(viewLifecycleOwner, EventObserver {
            it?.let { themesValue ->
                settingsViewModel.setMyTheme(themesValue)
            }
        })

        versionPref.setOnPreferenceClickListener {
            settingsViewModel.versionClicked()

            true
        }

        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        settingsViewModel.setScrollBarMode(scrollBarModePref.value)

        settingsViewModel.setBuiltInDataVersion(
            MyApplication.instance.packageName,
            MyApplication.instance.packageManager
        )
    }
}