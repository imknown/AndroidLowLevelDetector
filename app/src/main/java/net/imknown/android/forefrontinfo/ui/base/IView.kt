package net.imknown.android.forefrontinfo.ui.base

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

interface IView

inline fun <T : ViewBinding> Activity.viewBinding(
    crossinline inflate: (LayoutInflater) -> T
) = lazy { inflate(layoutInflater) }

inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline inflate: (LayoutInflater, ViewGroup?, Boolean) -> T,
    container: ViewGroup?
) = inflate(layoutInflater, container, false)

inline fun <T : ViewBinding> ViewGroup.viewBinding(
    crossinline inflate: (LayoutInflater, ViewGroup?, Boolean) -> T
) = inflate(LayoutInflater.from(context), this, false)

inline fun <T : ViewBinding> View.viewBinding(
    crossinline inflate: (LayoutInflater, ViewGroup?, Boolean) -> T
) = lazy { inflate(LayoutInflater.from(context), null, false) }

inline fun <T : ViewBinding> View.viewBinding(
    crossinline bind: (View) -> T
) = lazy { bind(this) }