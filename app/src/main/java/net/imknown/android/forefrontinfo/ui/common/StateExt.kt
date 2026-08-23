package net.imknown.android.forefrontinfo.ui.common

sealed interface State<out T> {
    data class Done<out T>(val value: T) : State<T>
    data object NotInitialized : State<Nothing>
}
