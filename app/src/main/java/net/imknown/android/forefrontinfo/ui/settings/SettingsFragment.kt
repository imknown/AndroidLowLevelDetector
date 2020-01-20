package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.IFragmentView

class SettingsFragment : PreferenceFragmentCompat(), IFragmentView {

    companion object {
        suspend fun newInstance() = withContext(Dispatchers.Main) {
            SettingsFragment()
        }
    }

    private val versionPref by lazy {
        findPreference<Preference>(MyApplication.getMyString(R.string.about_version_key))!!
    }

    private val settingsViewModel by activityViewModels<SettingsViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        MainScope().launch(Dispatchers.IO) {
            Looper.myLooper() ?: Looper.prepare()
            setPreferencesFromResource(R.xml.preferences, rootKey)
            Looper.myLooper()?.quit()

            withContext(Dispatchers.Main) {
                initViews()
            }
        }
    }

    private fun initViews() {
        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        scrollBarModePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            settingsViewModel.setScrollBarMode(newValue.toString())

            true
        }

        // TODO: MultiWindow/FreeForm raises error 'ViewModel can be accessed only when Fragment is attached'. Maybe a framework bug?
        settingsViewModel.changeScrollBarModeEvent.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { isVerticalScrollBarEnabled ->
                listView.isVerticalScrollBarEnabled = isVerticalScrollBarEnabled
            }
        })

        settingsViewModel.setScrollBarMode(scrollBarModePref.value)

        settingsViewModel.version.observe(viewLifecycleOwner, Observer {
            versionPref.summary = it
        })

        settingsViewModel.versionClick.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let {
                toast(R.string.about_version_click)
            }
        })

        // TODO: Use LiveData
        val themesPref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_themes_key))!!
        themesPref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            settingsViewModel.setMyTheme(newValue)

            true
        }

        settingsViewModel.setBuiltInDataVersion()

        // TODO: Use LiveData
        versionPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            settingsViewModel.versionClicked()

            true
        }
    }
}