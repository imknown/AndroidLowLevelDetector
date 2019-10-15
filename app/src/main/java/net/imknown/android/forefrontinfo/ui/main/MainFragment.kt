package net.imknown.android.forefrontinfo.ui.main

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.topjohnwu.superuser.BuildConfig
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.Shell.FLAG_REDIRECT_STDERR
import kotlinx.android.synthetic.main.main_fragment.*
import net.imknown.android.forefrontinfo.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        init {
            Shell.Config.setFlags(FLAG_REDIRECT_STDERR)
            Shell.Config.verboseLogging(BuildConfig.DEBUG)
            Shell.Config.setTimeout(10)
        }

        const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        const val CMD_AB_UPDATE = "getprop ro.build.ab_update"
        const val CMD_SLOT_SUFFIX = "getprop ro.boot.slot_suffix"

        const val CMD_TREBLE_ENABLED = "getprop ro.treble.enabled"

        const val CMD_VNDK_LITE = "getprop ro.vndk.lite"
        const val CMD_VNDK_VERSION = "getprop ro.vndk.version"

        const val CMD_SAR = "mount | grep 'rootfs on / type rootfs'"

        const val CMD_APEX_MOUNT = "mount | grep 'tmpfs on /apex type tmpfs'"
        const val CMD_APEX_TZDATA = "mount | grep /apex/com.android.tzdata"
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        showResult()
    }

    private fun showResult() {
        var totalResult = ""

        // region [A/B]
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]
        val isAbUpdateSupported = sh(CMD_AB_UPDATE)[0]!!.toBoolean()
        totalResult += getString(
            R.string.ab_seamless_update_enabled_result,
            translate(isAbUpdateSupported)
        )
        if (isAbUpdateSupported) {
            val slotSuffixUsing = sh(CMD_SLOT_SUFFIX)[0]
            totalResult += getString(
                R.string.current_using_ab_slot_result,
                slotSuffixUsing
            )
        }
        // endregion [A/B]

        // region [Treble]
        val isTrebleEnabled = sh(CMD_TREBLE_ENABLED)[0]!!.toBoolean()
        totalResult += getString(
            R.string.treble_enabled_result,
            translate(isTrebleEnabled)
        )
        // endregion [Treble]

        // region [VNDK]
        val isVndkLite = sh(CMD_VNDK_LITE)[0]!!.toBoolean()
        val vndkVersion = sh(CMD_VNDK_VERSION)[0]
        val vndkVersionInt = vndkVersion.toInt()
        val isVndkBuiltIn = (isVndkLite || vndkVersionInt >= Build.VERSION_CODES.O)
        totalResult += getString(
            R.string.vndk_built_in_result,
            translate(isVndkBuiltIn)
        )
        if (isVndkBuiltIn) {
            totalResult += getString(
                R.string.built_in_vndk_version_result,
                vndkVersion
            )
        }
        // endregion [VNDK]

        // region [SAR]
        val sarResult = sh(CMD_SAR)
        val isSar = sarResult.isEmpty() || sarResult[0].isEmpty()
        totalResult += getString(
            R.string.sar_enabled_result,
            translate(isSar)
        )
        // endregion [SAR]

        // region [APEX]
        val isApexMounted = sh(CMD_APEX_MOUNT)[0].isNotEmpty()
        val isApexUsed = sh(CMD_APEX_TZDATA)[0].isNotEmpty()
        totalResult += getString(
            R.string.apex_enabled_result,
            translate(isApexMounted && isApexUsed)
        )
        // endregion [APEX]

        message.text = totalResult
    }

    private fun sh(cmd: String) = Shell.sh(cmd).exec().out

    private fun translate(condition: Boolean) = getString(
        if (condition) {
            R.string.result_yes
        } else {
            R.string.result_no
        }
    )

}
