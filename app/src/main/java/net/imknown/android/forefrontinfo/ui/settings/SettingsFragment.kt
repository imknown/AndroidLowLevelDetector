package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var counter = 5

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val versionPref = findPreference<Preference>("version")
        versionPref?.let {
            it.summary =
                getString(
                    R.string.about_version_summary,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
        }

        versionPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (counter > 0) {
                counter -= 1
            } else if (counter == 0) {
                counter -= 100

                Toast.makeText(
                    this@SettingsFragment.context,
                    R.string.about_version_click,
                    Toast.LENGTH_LONG
                ).show()
            }

            true
        }
    }
}