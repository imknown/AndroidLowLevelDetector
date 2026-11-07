package net.imknown.android.forefrontinfo.ui.base.detect

/**
 * Detection result container: every detection first says whether a value was read,
 * and the fact itself is stated by the entry's own type.
 *
 * [Found] carries the fact; [Missing] means this item does not exist on this device;
 * [Failed] means the read itself went wrong. Missing and Failed both land on "unknown"
 * in the UI and every entry handles them the same way, so they live in this shared layer.
 * Entry semantics such as "meets the bar or lags behind" do not fit a shared container;
 * they travel out through [Found.value] in the entry's own type.
 * [Missing]'s reason comes from the repository; display text and color are the mapper's job.
 */
sealed interface DetectResult<out T> {
    data class Found<T>(val value: T) : DetectResult<T>

    data class Missing(val reason: String) : DetectResult<Nothing>

    data class Failed(val cause: Exception) : DetectResult<Nothing>
}
