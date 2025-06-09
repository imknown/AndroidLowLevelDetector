package net.imknown.android.forefrontinfo.ui.common

/** Remove this after Kotlin 2.4 Rich Errors were introduced */
sealed class State<out T> {
    data class Done<out T>(val value: T) : State<T>()
    data object NotInitialized : State<Nothing>()

    fun toValue() = (this as Done).value
}