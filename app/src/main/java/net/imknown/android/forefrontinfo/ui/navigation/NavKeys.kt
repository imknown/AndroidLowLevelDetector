package net.imknown.android.forefrontinfo.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// In Navigation 3 a destination is a "key", not a page: NavKey is just a marker interface
// that must be paired with @Serializable (the back stack is serialized to survive process
// death and restored afterwards). All four keys are data objects: the cheapest way to
// declare argument-free destinations (singleton + friendly toString).
@Serializable
data object HomeKey : NavKey

@Serializable
data object OthersKey : NavKey

@Serializable
data object PropKey : NavKey

@Serializable
data object SettingsKey : NavKey
