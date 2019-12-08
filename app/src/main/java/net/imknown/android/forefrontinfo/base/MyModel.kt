package net.imknown.android.forefrontinfo.base

import androidx.annotation.ColorInt
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_CRITICAL
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_DEFAULT_STYLE
import net.imknown.android.forefrontinfo.MainActivity.Companion.COLOR_STATE_LIST_NO_PROBLEM

data class MyModel(val title: String, val detail: String, @ColorInt val color: Int) {
    constructor(result: String, detail: String) : this(
        result,
        detail,
        COLOR_STATE_LIST_DEFAULT_STYLE
    )

    constructor(result: String, detail: String, condition: Boolean) : this(
        result, detail, if (condition) {
            COLOR_STATE_LIST_NO_PROBLEM
        } else {
            COLOR_STATE_LIST_CRITICAL
        }
    )
}