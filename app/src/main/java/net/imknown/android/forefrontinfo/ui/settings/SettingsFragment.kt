package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.ui.MainActivity
import net.imknown.android.forefrontinfo.ui.base.EventObserver
import net.imknown.android.forefrontinfo.ui.base.ext.toast
import net.imknown.android.forefrontinfo.ui.base.ext.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.ui.settings.datasource.AppInfoDataSource
import net.imknown.android.forefrontinfo.ui.settings.datasource.FingerprintDataSource
import net.imknown.android.forefrontinfo.ui.settings.repository.SettingsRepository

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private val settingsViewModel by viewModels<SettingsViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = SettingsRepository(AppInfoDataSource(), FingerprintDataSource())
                this[SettingsViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = {
            SettingsViewModel.Factory
        }
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initWindowInsets()

        initViews()
    }

    private fun initWindowInsets() {
        listView.clipToPadding = false

        ViewCompat.setOnApplyWindowInsetsListener(listView) { rv, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
            (activity as? MainActivity)?.binding?.bottomNavigationView?.doOnLayout { bnv ->
                rv.updatePadding(
                    left = insets.left,
                    right = insets.right,
                    bottom = bnv.height
                )
            }

            windowInsetsCompat
        }
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

        val scrollBarModePref = findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))
        scrollBarModePref?.let {
            settingsViewModel.setScrollBarMode(it.value)
        }
        // endregion [Scroll Bar Mode]

        settingsViewModel.showMessageEvent.observe(viewLifecycleOwner, EventObserver {
            context?.toast(it)
        })

        val aboutShopPref = findPreferenceOrNull(R.string.about_shop_key)
        setOnOpenInExternalListener(
            aboutShopPref, if (isChinaMainlandTimezone()) {
                R.string.about_shop_china_mainland_uri
            } else {
                R.string.about_shop_uri
            }
        )

        val aboutSourcePref = findPreferenceOrNull(R.string.about_source_key)
        setOnOpenInExternalListener(aboutSourcePref, R.string.about_source_uri)

        val aboutPrivacyPolicyPref = findPreferenceOrNull(R.string.about_privacy_policy_key)
        setOnOpenInExternalListener(aboutPrivacyPolicyPref, R.string.about_privacy_policy_uri)

        val aboutLicensesPref = findPreferenceOrNull(R.string.about_licenses_key)
        setOnOpenInExternalListener(aboutLicensesPref, R.string.about_licenses_uri)

        val aboutTranslatorMoreInfoPref = findPreferenceOrNull(R.string.about_translator_more_info_key)
        setOnOpenInExternalListener(aboutTranslatorMoreInfoPref, R.string.translator_website)

        // region [Version Info]
        val versionPref = findPreferenceOrNull(R.string.about_version_key)
        settingsViewModel.version.observe(viewLifecycleOwner) {
            versionPref?.summary = MyApplication.getMyString(
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

        val context = MyApplication.instance
        settingsViewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
        // endregion [Version Info]

        // region [Version Click]
        versionPref?.setOnPreferenceClickListener {
            settingsViewModel.versionClicked()

            true
        }
        // endregion [Version Click]
    }

    private fun findPreferenceOrNull(@StringRes resId: Int) =
        findPreference<Preference>(MyApplication.getMyString(resId))

    private fun setOnOpenInExternalListener(pref: Preference?, @StringRes uriResId: Int) {
        pref?.setOnPreferenceClickListener {
            settingsViewModel.openInExternal(uriResId)

            true
        }
    }
}