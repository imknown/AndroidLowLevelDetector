package net.imknown.android.forefrontinfo.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.isChinaMainlandTimezone
import net.imknown.android.forefrontinfo.ui.MainActivity
import net.imknown.android.forefrontinfo.ui.base.ext.toast
import net.imknown.android.forefrontinfo.ui.base.ext.windowInsetsCompatTypes
import net.imknown.android.forefrontinfo.ui.common.State
import net.imknown.android.forefrontinfo.ui.common.setScrollBarMode
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
        val themeModePref = findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_themes_key))
        themeModePref?.setOnPreferenceChangeListener { preference, newValue ->
            MyApplication.setMyTheme(newValue as? String)
            true
        }
        // endregion [Theme]

        // region [ScrollBar Mode]
        val scrollBarModePref = findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))
        listView.setScrollBarMode(scrollBarModePref?.value)

        scrollBarModePref?.setOnPreferenceChangeListener { preference, newValue ->
            val scrollBarMode = newValue as? String
            listView.setScrollBarMode(scrollBarMode)
            settingsViewModel.emitScrollBarModeChangedSharedFlow(scrollBarMode)
            true
        }
        // endregion [ScrollBar Mode]

        val outdatedOrderPref = findPreference<SwitchPreferenceCompat>(MyApplication.getMyString(R.string.function_outdated_target_order_by_package_name_first_key))
        outdatedOrderPref?.setOnPreferenceChangeListener { preference, newValue ->
            settingsViewModel.emitOutdatedOrderChangedSharedFlow()
            true
        }

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
        viewLifecycleOwner.lifecycleScope.launch {
            settingsViewModel.version.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { stateVersion ->
                if (stateVersion is State.NotInitialized) {
                    return@collect
                }

                val version = stateVersion.toValue()

                versionPref?.summary = MyApplication.getMyString(
                    version.id,
                    version.versionName,
                    version.versionCode,
                    version.assetLldVersion,
                    version.distributor,
                    version.installer,
                    version.firstInstallTime,
                    version.lastUpdateTime
                )
            }
        }

        val context = MyApplication.instance
        settingsViewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
        // endregion [Version Info]

        // region [Version Click]
        versionPref?.setOnPreferenceClickListener {
            settingsViewModel.getVersionClickedMessage()?.let {
                this.context?.toast(it)
            }

            true
        }
        // endregion [Version Click]
    }

    private fun findPreferenceOrNull(@StringRes resId: Int) =
        findPreference<Preference>(MyApplication.getMyString(resId))

    private fun setOnOpenInExternalListener(pref: Preference?, @StringRes uriResId: Int) {
        pref?.setOnPreferenceClickListener {
            openInExternal(uriResId)

            true
        }
    }

    fun openInExternal(@StringRes uriResId: Int) {
        val uri = MyApplication.getMyString(uriResId).toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val component = intent.resolveActivity(MyApplication.instance.packageManager)
        if (component != null) {
            MyApplication.instance.startActivity(intent)
        } else {
            context?.toast(R.string.no_browser_found)
        }
    }
}