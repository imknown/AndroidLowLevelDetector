package net.imknown.android.forefrontinfo.ui.common

import net.imknown.android.forefrontinfo.base.shell.getShellResultByCondition

fun sh(cmd: String, condition: Boolean = true) = getShellResultByCondition(cmd, condition)