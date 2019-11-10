package net.imknown.android.forefrontinfo.ui.settings

import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.JsonIo
import net.imknown.android.forefrontinfo.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var counter = 5

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            setPreferencesFromResource(R.xml.preferences, rootKey)
            Looper.myLooper()?.quit()

            initViews()
        }
    }

    private suspend fun initViews() {
        val versionPref = findPreference<Preference>(getString(R.string.about_version_key))
        versionPref?.let {
            val assetLldVersion = JsonIo.getAssetLldVersion(context?.assets!!)

            it.summary =
                getString(
                    R.string.about_version_summary,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    assetLldVersion
                )
        }

        versionPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (counter > 0) {
                    counter -= 1
                } else if (counter == 0) {
                    counter -= 100

                    toast(R.string.about_version_click)
                }
            }

            true
        }
    }

    private suspend fun toast(@StringRes resId: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    private suspend fun toast(text: String?) = withContext(Dispatchers.Main) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}