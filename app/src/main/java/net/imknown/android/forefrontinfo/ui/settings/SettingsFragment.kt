package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.base.mvvm.EventObserver
import net.imknown.android.forefrontinfo.base.mvvm.IFragmentView
import com.google.android.material.R as materialR

class SettingsFragment : PreferenceFragmentCompat(), IFragmentView {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    override val visualContext by lazy { context }

    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.clipToPadding = false
        val bottomR = materialR.dimen.design_bottom_navigation_height
        val bottom = resources.getDimensionPixelSize(bottomR)
        ViewCompat.setOnApplyWindowInsetsListener(listView) { insetView, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            insetView.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom + (insets.bottom + bottom)
            )

            windowInsets
        }

        initViews()
    }

    private fun initViews() {
        // region [Theme]
        settingsViewModel.themesPrefChangeEvent.observe(viewLifecycleOwner, EventObserver {
            it?.let { themesValue ->
                MyApplication.setMyTheme(themesValue)
            }
        })
        // endregion [Theme]

        // region [Scroll Bar Mode]
        settingsViewModel.scrollBarModeChangedEvent.observe(viewLifecycleOwner, EventObserver {
            it?.let { scrollBarMode ->
                settingsViewModel.setScrollBarMode(scrollBarMode)
            }
        })

        settingsViewModel.changeScrollBarModeEvent.observe(viewLifecycleOwner, EventObserver {
            listView.isVerticalScrollBarEnabled = it
        })

        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        settingsViewModel.setScrollBarMode(scrollBarModePref.value)
        // endregion [Scroll Bar Mode]

        settingsViewModel.showMessageEvent.observe(viewLifecycleOwner, EventObserver {
            toast(it)
        })

        val aboutShopPref = findPreference(R.string.about_shop_key)
        setOnOpenInExternalListener(
            aboutShopPref, if (isChinaMainlandTimezone()) {
                R.string.about_shop_china_mainland_uri
            } else {
                R.string.about_shop_uri
            }
        )

        val aboutSourcePref = findPreference(R.string.about_source_key)
        setOnOpenInExternalListener(aboutSourcePref, R.string.about_source_uri)

        val aboutPrivacyPolicyPref = findPreference(R.string.about_privacy_policy_key)
        setOnOpenInExternalListener(aboutPrivacyPolicyPref, R.string.about_privacy_policy_uri)

        val aboutLicensesPref = findPreference(R.string.about_licenses_key)
        setOnOpenInExternalListener(aboutLicensesPref, R.string.about_licenses_uri)

        val aboutTranslatorMoreInfoPref = findPreference(R.string.about_translator_more_info_key)
        setOnOpenInExternalListener(
            aboutTranslatorMoreInfoPref,
            R.string.translator_website
        )

        // region [Version Info]
        val versionPref = findPreference(R.string.about_version_key)
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

        settingsViewModel.setBuiltInDataVersion(
            MyApplication.instance.packageName,
            MyApplication.instance.packageManager
        )
        // endregion [Version Info]

        // region [Version Click]
        versionPref.setOnPreferenceClickListener {
            settingsViewModel.versionClicked()

            true
        }
        // endregion [Version Click]
    }

    private fun findPreference(@StringRes resId: Int) =
        findPreference<Preference>(MyApplication.getMyString(resId))!!

    private fun setOnOpenInExternalListener(pref: Preference, @StringRes uriResId: Int) {
        pref.setOnPreferenceClickListener {
            settingsViewModel.openInExternal(uriResId)

            true
        }
    }
}