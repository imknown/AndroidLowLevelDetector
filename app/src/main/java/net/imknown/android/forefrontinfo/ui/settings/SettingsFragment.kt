package net.imknown.android.forefrontinfo.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.ui.settings.model.GithubReleaseInfo
import java.io.File

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var counter = 5

    private lateinit var checkUpdatePref: Preference
    private lateinit var allowNetworkDataPref: SwitchPreferenceCompat

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

    private suspend fun initViews() = withContext(Dispatchers.Main) {
        allowNetworkDataPref = findPreference(getString(R.string.network_allow_network_data_key))!!

        val versionPref = findPreference<Preference>(getString(R.string.about_version_key))!!
        versionPref.let {
            val assetLldVersion = JsonIo.getAssetLldVersion(context?.assets!!)

            it.summary =
                getString(
                    R.string.about_version_summary,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    assetLldVersion
                )
        }

        versionPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
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

        checkUpdatePref = findPreference(getString(R.string.about_check_for_update_key))!!
        checkUpdatePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                checkForUpdate()
            }

            true
        }
    }

    private suspend fun setCheckUpdatePreferenceStatus(isEnabled: Boolean) =
        withContext(Dispatchers.Main) {
            checkUpdatePref.isEnabled = isEnabled

            checkUpdatePref.title = getString(
                if (isEnabled) {
                    R.string.about_check_for_update_title
                } else {
                    R.string.about_check_for_update_title_checking
                }
            )
        }

    private suspend fun checkForUpdate() {
        setCheckUpdatePreferenceStatus(false)

        if (allowNetworkDataPref.isChecked) {
            GatewayApi.checkForUpdate({ data ->
                isLatestVersion(data)
            }, { error ->
                showNetError(error)
            })
        } else {
            toast(R.string.about_check_for_update_allow_network_data_first)

            setCheckUpdatePreferenceStatus(true)
        }
    }

    private fun isLatestVersion(data: String) = GlobalScope.launch(Dispatchers.IO) {
        val githubReleaseInfo = data.fromJson<GithubReleaseInfo>()
        val remoteVersion = githubReleaseInfo.tag_name
        val currentVersion = BuildConfig.VERSION_NAME

        if (remoteVersion > currentVersion) {
            showDownloadConfirmDialog(githubReleaseInfo)
        } else {
            toast(R.string.about_check_for_update_already_latest)
        }

        setCheckUpdatePreferenceStatus(true)
    }

    @Suppress("DEPRECATION")
    private var progressDialog: android.app.ProgressDialog? = null

    private suspend fun showDownloadConfirmDialog(githubReleaseInfo: GithubReleaseInfo) {
        val version = githubReleaseInfo.tag_name
        val asset = githubReleaseInfo.assets[0]
        val size = asset.size
        val sizeInMb = size / 1_048_576F
        val sizeInMbString = "%.2f".format(sizeInMb)
        val date = githubReleaseInfo.published_at.replace("T", " ").replace("Z", "")
        val desc = githubReleaseInfo.name
        val log = githubReleaseInfo.body

        val title = getString(
            R.string.about_check_for_update_new_version,
            version,
            sizeInMbString
        )
        val message = "$date\n$desc\n$log"

        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
            GlobalScope.launch(Dispatchers.IO) {
                dialog.dismiss()

                showProgressDialog(title, message, size, sizeInMb)

                val url = asset.browser_download_url
                val fileName = asset.name
                downloadApk(url, fileName, sizeInMb)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }

        withContext(Dispatchers.Main) {
            builder.show()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun showProgressDialog(
        title: String,
        message: String,
        size: Int,
        sizeInMb: Float
    ) = withContext(Dispatchers.Main) {
        progressDialog = android.app.ProgressDialog(context)
        with(progressDialog!!) {
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setTitle(title)
            setMessage(message)
            progress = 0
            max = size
            setProgressNumberFormat(String.format("0M/%.2fM", sizeInMb))
            show()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun downloadApk(url: String, fileName: String, sizeInMb: Float) {
        try {
            GatewayApi.downloadApk(url, fileName, { readBytes, _ ->
                progressDialog?.setProgressNumberFormat(
                    String.format("%.2fM/%.2fM", readBytes / 1_048_576F, sizeInMb)
                )
                progressDialog?.progress = readBytes.toInt()
            }, { data ->
                if (data.isEmpty()) {
                    showNetError(Exception(getString(R.string.about_check_for_update_network_empty_file)))
                } else {
                    install(fileName)
                }
            }, { error ->
                showNetError(error)
            })
        } catch (t: Throwable) {
            showNetError(t)
        }
    }

    private fun install(fileName: String) = GlobalScope.launch(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(MyApplication.getApkDir(), fileName)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(context!!, context!!.packageName + ".provider", file)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Uri.fromFile(file)
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive")
        startActivity(intent)

        dismissProgressDialog()
    }

    private fun showNetError(error: Throwable) {
        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }

        runBlocking {
            setCheckUpdatePreferenceStatus(true)

            dismissProgressDialog()

            toast(getString(R.string.about_check_for_update_network_error, error.message))
        }
    }

    private suspend fun dismissProgressDialog() = withContext(Dispatchers.Main) {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    private suspend fun toast(@StringRes resId: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    private suspend fun toast(text: String?) = withContext(Dispatchers.Main) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}