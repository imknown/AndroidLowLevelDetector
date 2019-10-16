package net.imknown.android.forefrontinfo.ui.main

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.topjohnwu.superuser.BuildConfig
import com.topjohnwu.superuser.Shell
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.main_fragment_item.view.*
import net.imknown.android.forefrontinfo.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()

        init {
            Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
            Shell.Config.verboseLogging(BuildConfig.DEBUG)
            Shell.Config.setTimeout(10)
        }

        private const val CMD_BOOT_PARTITION = "ls /dev/block/bootdevice/by-name | grep boot_"
        private const val CMD_AB_UPDATE = "getprop ro.build.ab_update"
        private const val CMD_SLOT_SUFFIX = "getprop ro.boot.slot_suffix"

        private const val CMD_TREBLE_ENABLED = "getprop ro.treble.enabled"

        private const val CMD_VNDK_LITE = "getprop ro.vndk.lite"
        private const val CMD_VNDK_VERSION = "getprop ro.vndk.version"

        private const val CMD_SAR = "mount | grep 'rootfs on / type rootfs'"

        private const val CMD_APEX_MOUNT = "mount | grep 'tmpfs on /apex type tmpfs'"
        private const val CMD_APEX_TZDATA = "mount | grep /apex/com.android.tzdata"
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

        showResult(collectionDataset())
    }

    private class MyAdapter(private val myDataset: List<MyModel>) :
        RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.main_fragment_item,
                parent,
                false
            )
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.itemView.card.setCardBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    myDataset[position].color
                )
            )
            holder.itemView.result.text = myDataset[position].result
        }

        override fun getItemCount() = myDataset.size
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private data class MyModel(val result: String, @ColorRes val color: Int)

    private fun collectionDataset(): ArrayList<MyModel> {
        val myDataset = ArrayList<MyModel>()

        // region [A/B]
        // val bootPartitions = sh(CMD_BOOT_PARTITION)[0]
        val isAbUpdateSupported = sh(CMD_AB_UPDATE)[0]!!.toBoolean()
        myDataset.add(
            MyModel(
                getString(
                    R.string.ab_seamless_update_enabled_result,
                    translate(isAbUpdateSupported)
                ),
                getResultColor(isAbUpdateSupported)
            )
        )
        if (isAbUpdateSupported) {
            val slotSuffixUsing = sh(CMD_SLOT_SUFFIX)[0]
            myDataset.add(
                MyModel(
                    getString(R.string.current_using_ab_slot_result, slotSuffixUsing),
                    R.color.colorSupport
                )
            )
        }
        // endregion [A/B]

        // region [Treble]
        val isTrebleEnabled = sh(CMD_TREBLE_ENABLED)[0]!!.toBoolean()
        myDataset.add(
            MyModel(
                getString(R.string.treble_enabled_result, translate(isTrebleEnabled)),
                getResultColor(isTrebleEnabled)
            )
        )
        // endregion [Treble]

        // region [VNDK]
        val isVndkLite = sh(CMD_VNDK_LITE)[0]!!.toBoolean()
        val vndkVersion = sh(CMD_VNDK_VERSION)[0]
        val vndkVersionInt = vndkVersion.toInt()
        val isVndkBuiltIn = (isVndkLite || vndkVersionInt >= Build.VERSION_CODES.O)
        var isVndkBuiltInResult = translate(isVndkBuiltIn)
        if (isVndkBuiltIn) {
            isVndkBuiltInResult += getString(R.string.built_in_vndk_version_result, vndkVersion)
        }
        myDataset.add(
            MyModel(
                getString(R.string.vndk_built_in_result, isVndkBuiltInResult),
                getResultColor(isVndkBuiltIn)
            )
        )
        // endregion [VNDK]

        // region [SAR]
        val sarResult = sh(CMD_SAR)
        val isSar = sarResult.isEmpty() || sarResult[0].isEmpty()
        myDataset.add(
            MyModel(
                getString(R.string.sar_enabled_result, translate(isSar)),
                getResultColor(isSar)
            )
        )
        // endregion [SAR]

        // region [APEX]
        val isApexMounted = sh(CMD_APEX_MOUNT)[0].isNotEmpty()
        val isApexUsed = sh(CMD_APEX_TZDATA)[0].isNotEmpty()
        val isApex = isApexMounted && isApexUsed
        myDataset.add(
            MyModel(
                getString(R.string.apex_enabled_result, translate(isApex)),
                getResultColor(isApex)
            )
        )
        // endregion [APEX]

        return myDataset
    }

    private fun showResult(myDataset: ArrayList<MyModel>) {
        list.apply {
            setHasFixedSize(true)

            layoutManager = LinearLayoutManager(context)

            itemAnimator = DefaultItemAnimator()

            addItemDecoration(MyItemDecoration(resources.getDimensionPixelSize(R.dimen.item_divider_space)))

            adapter = MyAdapter(myDataset)
        }
    }

    private class MyItemDecoration(val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = spaceHeight
                }
                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }

    private fun sh(cmd: String) = Shell.sh(cmd).exec().out

    private fun translate(condition: Boolean) = getString(
        if (condition) {
            R.string.result_yes
        } else {
            R.string.result_no
        }
    )

    private fun getResultColor(condition: Boolean) =
        if (condition) {
            R.color.colorSupport
        } else {
            R.color.colorNotSupport
        }

}
