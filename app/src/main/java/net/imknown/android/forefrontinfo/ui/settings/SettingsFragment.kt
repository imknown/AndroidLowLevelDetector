package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.JsonIo
import net.imknown.android.forefrontinfo.MyApplication
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.IFragmentView
import net.imknown.android.forefrontinfo.ui.GetRawPropEventViewModel

class SettingsFragment : PreferenceFragmentCompat(), IFragmentView, CoroutineScope by MainScope() {

    companion object {
        suspend fun newInstance() = withContext(Dispatchers.Main) {
            SettingsFragment()
        }
    }

    private var counter = 5

    private val rawBuildPropPref by lazy {
        findPreference<SwitchPreferenceCompat>(MyApplication.getMyString(R.string.function_raw_build_prop_key))!!
    }

    private val getRawPropEventViewModel by activityViewModels<GetRawPropEventViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        launch(Dispatchers.IO) {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            setPreferencesFromResource(R.xml.preferences, rootKey)
            Looper.myLooper()?.quit()

            initViews()
        }
    }

    private suspend fun initViews() = withContext(Dispatchers.Main) {
        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        scrollBarModePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            onScrollBarChanged(newValue.toString())

            true
        }

        setScrollBarMode(listView, scrollBarModePref.value)

        rawBuildPropPref.setOnPreferenceChangeListener { _: Preference, _: Any ->
            onRawBuildPropChanged()

            true
        }

        getRawPropEventViewModel.isGetPropFinish.observe(viewLifecycleOwner, Observer {
            rawBuildPropPref.isEnabled = true
        })

        val themesPref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_themes_key))!!
        themesPref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            MyApplication.instance.setMyTheme(newValue.toString())

            true
        }

        val versionPref =
            findPreference<Preference>(MyApplication.getMyString(R.string.about_version_key))!!
        setBuiltInDataVersion(versionPref)
        versionPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            versionClicked()

            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cancel()
    }

    // region [Scroll bar]
    private fun onScrollBarChanged(value: String) = launch(Dispatchers.Default) {
        setScrollBarMode(listView, value)
    }
    // endregion [Scroll bar]

    // region [Raw build]
    private fun onRawBuildPropChanged() = launch(Dispatchers.IO) {
        val fragment =
            activity?.supportFragmentManager?.findFragmentByTag(R.id.navigation_others.toString())
        if (fragment != null) {
            withContext(Dispatchers.Main) {
                rawBuildPropPref.isEnabled = false
            }
        }
    }
    // endregion [Raw build]

    // region [Check for update]
    private suspend fun setBuiltInDataVersion(versionPref: Preference) =
        withContext(Dispatchers.IO) {
            val assetLldVersion = try {
                JsonIo.getAssetLldVersion(MyApplication.instance.assets)
            } catch (e: Exception) {
                e.printStackTrace()

                MyApplication.getMyString(android.R.string.unknownName)
            }

            withContext(Dispatchers.Main) {
                versionPref.summary =
                    MyApplication.getMyString(
                        R.string.about_version_summary,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        assetLldVersion
                    )
            }
        }

    // region [Version click]
    private fun versionClicked() = launch(Dispatchers.Default) {
        if (counter > 0) {
            counter -= 1
        } else if (counter == 0) {
            counter -= 100

            toast(R.string.about_version_click)
        }
    }
    // endregion [Version click]
}