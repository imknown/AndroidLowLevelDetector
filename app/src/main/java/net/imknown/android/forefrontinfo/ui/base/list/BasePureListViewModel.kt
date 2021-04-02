package net.imknown.android.forefrontinfo.ui.base.list

import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import java.util.*

abstract class BasePureListViewModel : BaseListViewModel() {
    protected fun add(tempModels: ArrayList<MyModel>, title: String, detail: String?) {
        val translatedDetail = if (detail.isNullOrEmpty()) {
            MyApplication.getMyString(R.string.build_not_filled)
        } else {
            detail.toString()
        }

        tempModels.add(MyModel(title, translatedDetail))
    }
}