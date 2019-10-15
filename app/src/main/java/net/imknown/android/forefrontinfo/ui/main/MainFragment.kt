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
            Shell.Config.setFlags(FLAG_REDIRECT_STDERR);
            Shell.Config.verboseLogging(BuildConfig.DEBUG);
            Shell.Config.setTimeout(10);
        }
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
        // val bootPartitions = sh("ls /dev/block/bootdevice/by-name | grep boot_")[0]!!
        val isAbUpdateSupported = sh("getprop ro.build.ab_update")[0]!!.toBoolean()
        totalResult += "启用了 A/B 无缝升级? ${translate(isAbUpdateSupported)}\n"
        if (isAbUpdateSupported) {
            val slotSuffixUsing = sh("getprop ro.boot.slot_suffix")[0]!!
            totalResult += "当前使用的 A/B 槽位: $slotSuffixUsing\n"
        }
        // endregion [A/B]

        // region [Treble]
        val isTrebleEnabled = sh("getprop ro.treble.enabled")[0]!!.toBoolean()
        totalResult += "启用了 Project Treble? ${translate(isTrebleEnabled)}\n"
        // endregion [Treble]

        // region [VNDK]
        val isVndkLite = sh("getprop ro.vndk.lite")[0]!!.toBoolean()
        val vndkVersion = sh("getprop ro.vndk.version")[0]!!.toInt()
        val isVndkBuiltIn = (isVndkLite || vndkVersion >= Build.VERSION_CODES.O)
        totalResult += "内建了 Vendor NDK? ${translate(isVndkBuiltIn)}"
        if (isVndkBuiltIn) {
            totalResult += " (等级: $vndkVersion)\n"
        } else {
            totalResult += "\n"
        }
        // endregion [VNDK]

        // region [SAR]
        val sarResult = sh("mount | grep 'rootfs on / type rootfs'")
        val isSar = sarResult.isEmpty() || sarResult[0].isEmpty()
        totalResult += "启用了 System-as-root? ${translate(isSar)}\n"
        // endregion [SAR]

        // region [APEX]
        val isApexMounted = sh("mount | grep 'tmpfs on /apex type tmpfs'")[0]!!.isNotEmpty()
        val isApexUsed = sh("mount | grep /apex/com.android.tzdata")[0]!!.isNotEmpty()
        totalResult += "启用了 APEX? ${translate(isApexMounted && isApexUsed)}"
        // endregion [APEX]

        message.text = totalResult
    }

    private fun sh(cmd: String) = Shell.sh(cmd).exec().out

    private fun translate(value: Boolean) = if (value) "✅" else "❌"

}
