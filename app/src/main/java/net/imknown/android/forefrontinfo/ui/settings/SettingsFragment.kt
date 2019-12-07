package net.imknown.android.forefrontinfo.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import net.imknown.android.forefrontinfo.*
import net.imknown.android.forefrontinfo.base.IFragmentView
import net.imknown.android.forefrontinfo.base.SharedViewModel
import net.imknown.android.forefrontinfo.ui.settings.model.GithubReleaseInfo
import java.io.File

class SettingsFragment : PreferenceFragmentCompat(), IFragmentView, CoroutineScope by MainScope() {

    companion object {
        fun newInstance() = SettingsFragment()
    }

    private var counter = 5

    private lateinit var allowNetworkDataPref: SwitchPreferenceCompat
    private lateinit var rawBuildPropPref: SwitchPreferenceCompat
    private lateinit var checkUpdatePref: Preference
    private lateinit var clearApkFolderPref: Preference

    private val sharedViewModel: SharedViewModel by activityViewModels()

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

    private fun initViews() = launch(Dispatchers.Main) {
        if (!isActivityAndFragmentOk(this@SettingsFragment)) {
            return@launch
        }

        val scrollBarModePref =
            findPreference<ListPreference>(MyApplication.getMyString(R.string.interface_scroll_bar_key))!!
        scrollBarModePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            launch {
                if (isActivityAndFragmentOk(this@SettingsFragment)) {
                    setScrollBarMode(listView, newValue.toString())
                }
            }

            true
        }

        setScrollBarMode(listView, scrollBarModePref.value)

        rawBuildPropPref =
            findPreference<SwitchPreferenceCompat>(MyApplication.getMyString(R.string.interface_raw_build_prop_key))!!
        rawBuildPropPref.setOnPreferenceChangeListener { _: Preference, _: Any ->
            onRawBuildPropChanged()

            true
        }

        sharedViewModel.isSucceed.observe(viewLifecycleOwner, Observer<Boolean> {
            if (::rawBuildPropPref.isInitialized) {
                rawBuildPropPref.isEnabled = true
            }
        })

        allowNetworkDataPref =
            findPreference(MyApplication.getMyString(R.string.network_allow_network_data_key))!!

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

        checkUpdatePref =
            findPreference(MyApplication.getMyString(R.string.about_check_for_update_key))!!
        checkUpdatePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            checkForUpdate()

            true
        }

        clearApkFolderPref =
            findPreference(MyApplication.getMyString(R.string.about_clear_apk_folder_key))!!
        clearApkFolderPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            clearApkFolder()

            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cancel()
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
    private suspend fun setCheckUpdatePreferenceStatus(isEnabled: Boolean) {
        setPreferenceStatus(
            checkUpdatePref,
            isEnabled,
            R.string.about_check_for_update_title,
            R.string.about_check_for_update_title_checking
        )
    }

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

    private fun checkForUpdate() = launch(Dispatchers.IO) {
        setCheckUpdatePreferenceStatus(false)

        if (allowNetworkDataPref.isChecked) {
            GatewayApi.checkForUpdate(GatewayApiCheckForUpdate@{ data ->
                launch success@{
                    if (!isActivityAndFragmentOk(this@SettingsFragment)) {
                        return@success
                    }

                    isLatestVersion(data)
                }
            }, { error ->
                showError(error)
            })
        } else {
            toast(R.string.about_check_for_update_allow_network_data_first)

            setCheckUpdatePreferenceStatus(true)
        }
    }

    private fun isLatestVersion(data: String) = launch(Dispatchers.IO) {
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

    private fun showDownloadConfirmDialog(githubReleaseInfo: GithubReleaseInfo) {
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

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
            launch(Dispatchers.IO) {
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

        launch(Dispatchers.Main) {
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

    private suspend fun downloadApk(url: String, fileName: String, sizeInMb: Float) {
        try {
            GatewayApi.downloadApk(url, fileName, { readBytes, _ ->
                setProgress(readBytes, sizeInMb)
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

    @Suppress("DEPRECATION")
    private fun setProgress(readBytes: Long, sizeInMb: Float) {
        progressDialog.setProgressNumberFormat(
            String.format("%.2fM/%.2fM", readBytes / 1_048_576F, sizeInMb)
        )
        progressDialog.progress = readBytes.toInt()
    }

    private fun install(fileName: String) = launch(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(MyApplication.getApkDir(), fileName)
        val data = if (isAtLeastAndroid7()) {
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
    // endregion [Check for update]

    // region [Clear apk folder]
    private suspend fun setClearApkFolderStatus(isEnabled: Boolean) {
        setPreferenceStatus(
            clearApkFolderPref,
            isEnabled,
            R.string.about_clear_apk_folder_title,
            R.string.about_clear_apk_folder_title_clearing
        )
    }

    private fun clearApkFolder() = launch(Dispatchers.IO) {
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
    // endregion [Clear apk folder]

    // region [Version click]
    private fun versionClicked() = launch(Dispatchers.IO) {
        if (counter > 0) {
            counter -= 1
        } else if (counter == 0) {
            counter -= 100

            toast(R.string.about_version_click)
        }
    }
    // endregion [Version click]

    override fun showError(error: Throwable) {
        super.showError(error)

        launch(Dispatchers.IO) {
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