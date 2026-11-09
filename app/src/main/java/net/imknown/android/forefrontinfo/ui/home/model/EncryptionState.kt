package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectEncryption: the device's storage encryption status as reported by
 * [android.app.admin.DevicePolicyManager]. [Status.Active] and [Status.ActivePerUser] are two
 * distinct platform facts rendered by the same wording; [Status.Unknown] covers every
 * unrecognized or transitioning status (the old else branch, activated and inactivating
 * included) plus a missing DevicePolicyManager, and renders as not supported, left to the mapper.
 */
data class EncryptionState(
    val status: Status
) {
    enum class Status { Active, ActivePerUser, ActiveDefaultKey, Inactive, Unsupported, Unknown }
}
