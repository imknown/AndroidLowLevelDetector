package net.imknown.android.forefrontinfo.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.base.IView
import net.imknown.android.forefrontinfo.ui.settings.model.GithubReleaseInfo
import java.io.File

class SettingsFragment : PreferenceFragmentCompat(), IView {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var counter = 5

    private lateinit var allowNetworkDataPref: SwitchPreferenceCompat
    private lateinit var checkUpdatePref: Preference
    private lateinit var clearApkFolderPref: Preference

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

    override var fastScroller: Any? = null

    private suspend fun initViews() = withContext(Dispatchers.Main) {
        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        scrollBarModePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            if (isActivityAndFragmentOk(this@SettingsFragment)) {
                setScrollBarMode(listView, newValue.toString())
            }
            true
        }

        setScrollBarMode(listView, scrollBarModePref.value)

        allowNetworkDataPref =
            findPreference(MyApplication.getMyString(R.string.network_allow_network_data_key))!!

        val themesPref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_themes_key))!!
        themesPref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
                GlobalScope.launch(Dispatchers.IO) {
                    MyApplication.setMyTheme(newValue.toString())
                }

                true
            }

        val versionPref =
            findPreference<Preference>(MyApplication.getMyString(R.string.about_version_key))!!
        versionPref.let {
            val assetLldVersion = JsonIo.getAssetLldVersion(MyApplication.instance.assets)

            it.summary =
                MyApplication.getMyString(
                    R.string.about_version_summary,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    assetLldVersion
                )
        }

        versionPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                versionClicked()
            }

            true
        }

        checkUpdatePref =
            findPreference(MyApplication.getMyString(R.string.about_check_for_update_key))!!
        checkUpdatePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                checkForUpdate()
            }

            true
        }

        clearApkFolderPref =
            findPreference(MyApplication.getMyString(R.string.about_clear_apk_folder_key))!!
        clearApkFolderPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                clearApkFolder()
            }

            true
        }
    }

    private suspend fun setPreferenceStatus(
        preference: Preference,
        isEnabled: Boolean,
        @StringRes normalResId: Int,
        @StringRes checkingResId: Int
    ) = withContext(Dispatchers.Main) {
        preference.isEnabled = isEnabled

        preference.title = MyApplication.getMyString(
            if (isEnabled) {
                normalResId
            } else {
                checkingResId
            }
        )
    }

    // region [check for update]
    private suspend fun setCheckUpdatePreferenceStatus(isEnabled: Boolean) {
        setPreferenceStatus(
            checkUpdatePref,
            isEnabled,
            R.string.about_check_for_update_title,
            R.string.about_check_for_update_title_checking
        )
    }

    private suspend fun checkForUpdate() {
        setCheckUpdatePreferenceStatus(false)

        if (allowNetworkDataPref.isChecked) {
            GatewayApi.checkForUpdate(GatewayApiCheckForUpdate@{ data ->
                if (!isActivityAndFragmentOk(this)) {
                    return@GatewayApiCheckForUpdate
                }

                isLatestVersion(data)
            }, { error ->
                showError(error)
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
    private lateinit var progressDialog: android.app.ProgressDialog

    private suspend fun showDownloadConfirmDialog(githubReleaseInfo: GithubReleaseInfo) {
        val version = githubReleaseInfo.tag_name
        val asset = githubReleaseInfo.assets[0]
        val size = asset.size
        val sizeInMb = size / 1_048_576F
        val sizeInMbString = "%.2f".format(sizeInMb)
        val date = githubReleaseInfo.published_at.replace("T", " ").replace("Z", "")
        val desc = githubReleaseInfo.name
        val log = githubReleaseInfo.body

        val title = MyApplication.getMyString(
            R.string.about_check_for_update_new_version,
            version,
            sizeInMbString
        )
        val message = "$date\n$desc\n$log"

        val builder = AlertDialog.Builder(MyApplication.instance)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
            GlobalScope.launch(Dispatchers.IO) {
                dialog.dismiss()

                showProgressDialog(title, size, sizeInMb)

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
        size: Int,
        sizeInMb: Float
    ) = withContext(Dispatchers.Main) {
        progressDialog = android.app.ProgressDialog(context)
        with(progressDialog) {
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setTitle(title)
            // setMessage(message)
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
                progressDialog.setProgressNumberFormat(
                    String.format("%.2fM/%.2fM", readBytes / 1_048_576F, sizeInMb)
                )
                progressDialog.progress = readBytes.toInt()
            }, { data ->
                if (data.isEmpty()) {
                    showError(Exception(MyApplication.getMyString(R.string.about_check_for_update_network_empty_file)))
                } else {
                    install(fileName)
                }
            }, { error ->
                showError(error)
            })
        } catch (t: Throwable) {
            showError(t)
        }
    }

    private fun install(fileName: String) = GlobalScope.launch(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(MyApplication.getApkDir(), fileName)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FileProvider.getUriForFile(
                MyApplication.instance,
                MyApplication.instance.packageName + ".provider",
                file
            )
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Uri.fromFile(file)
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive")
        startActivity(intent)

        dismissProgressDialog()
    }

    private suspend fun dismissProgressDialog() = withContext(Dispatchers.Main) {
        if (::progressDialog.isInitialized) {
            if (progressDialog.isShowing) {
                progressDialog.dismiss()
            }
        }
    }
    // endregion [check for update]

    // region [clear apk folder]
    private suspend fun setClearApkFolderStatus(isEnabled: Boolean) {
        setPreferenceStatus(
            clearApkFolderPref,
            isEnabled,
            R.string.about_clear_apk_folder_title,
            R.string.about_clear_apk_folder_title_clearing
        )
    }

    private suspend fun clearApkFolder() {
        setClearApkFolderStatus(false)

        val result = GatewayApi.clearFolder(MyApplication.getApkDir())

        // For better UX
        delay(500)

        toast(
            if (result) {
                R.string.about_clear_apk_folder_successfully
            } else {
                R.string.about_clear_apk_folder_failed
            }
        )

        setClearApkFolderStatus(true)
    }
    // endregion [clear apk folder]

    // region [version click]
    private suspend fun versionClicked() {
        if (counter > 0) {
            counter -= 1
        } else if (counter == 0) {
            counter -= 100

            toast(R.string.about_version_click)
        }
    }
    // endregion [version click]

    override fun showError(error: Throwable) {
        super.showError(error)

        runBlocking {
            setCheckUpdatePreferenceStatus(true)

            dismissProgressDialog()

            toast(
                MyApplication.getMyString(
                    R.string.about_check_for_update_network_error,
                    error.message
                )
            )
        }
    }
}