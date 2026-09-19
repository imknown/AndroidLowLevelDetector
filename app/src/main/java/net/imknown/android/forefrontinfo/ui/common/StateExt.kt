package net.imknown.android.forefrontinfo.ui.common

import androidx.compose.runtime.Immutable

// @Immutable: Done holds an immutable value and Loading/NotInitialized are data objects, so composables taking this type can be skipped entirely
@Immutable
sealed interface State<out T> {
    data class Done<out T>(val value: T) : State<T>
    data object Loading : State<Nothing>
    data object NotInitialized : State<Nothing>
}
