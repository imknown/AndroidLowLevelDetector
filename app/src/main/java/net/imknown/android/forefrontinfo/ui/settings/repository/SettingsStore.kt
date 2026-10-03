package net.imknown.android.forefrontinfo.ui.settings.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.AppThemeMode
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.ScrollBarMode

// The single observable owner of the settings (issues-cn #08): keys defined in one place, one
// entry point each for reads and writes. SharedPreferences stays the persisted truth on disk;
// this class is the single in-memory read/write front -- the five read points that each reach
// into SharedPreferences directly (ST-02..04) will switch to observing these flows one by one.
// Flow-vs-storage consistency runs on listener callback: a write touches SP exactly once, and
// OnSharedPreferenceChangeListener's same-process synchronous callback pushes the new value into
// the flows -- one update path, no two-step "wrote SP, forgot to refresh the flow" shape.
// First resolution is construction, and seeding happens synchronously there; pinning that point
// is the consumer's contract (ST-02 resolves the store once in MyApplication.onCreate, where
// initTheme sits today) -- nothing before that point may read the flows. Pure addition for now:
// no consumer resolves this class yet, so behavior is unchanged.
@SingleIn(AppScope::class)
@Inject
class SettingsStore(private val prefs: SharedPreferences) {

    // region [Keys]
    // Keys and stored values resolved once, centrally (replacing each read point's in-place
    // getMyString): all translatable="false", the stored values are stable, so one
    // construction-time resolution is enough
    private val themeKey = MyApplication.getMyString(R.string.interface_themes_key)
    private val followSystemValue = MyApplication.getMyString(R.string.interface_themes_follow_system_value)
    private val alwaysLightValue = MyApplication.getMyString(R.string.interface_themes_always_light_value)
    private val alwaysDarkValue = MyApplication.getMyString(R.string.interface_themes_always_dark_value)
    private val powerSaverValue = MyApplication.getMyString(R.string.interface_themes_power_saver_value)

    private val scrollBarKey = MyApplication.getMyString(R.string.interface_scroll_bar_key)
    private val noScrollBarValue = MyApplication.getMyString(R.string.interface_no_scroll_bar_value)
    private val normalScrollBarValue = MyApplication.getMyString(R.string.interface_normal_scroll_bar_value)
    private val fastScrollBarValue = MyApplication.getMyString(R.string.interface_fast_scroll_bar_value)

    private val allowNetworkKey = MyApplication.getMyString(R.string.function_allow_network_data_key)
    private val outdatedOrderKey = MyApplication.getMyString(R.string.function_outdated_target_order_by_package_name_first_key)
    // endregion [Keys]

    // Raw stored-value flow: the settings dialog matches its selection against the raw string,
    // kept alongside the enum flow (the pair is updated together in the same callback, no drift)
    val themeValue: StateFlow<String>
        field = MutableStateFlow(followSystemValue)

    // Current-value flow: non-null enum, seeded synchronously at construction -- consumers
    // (AppTheme / MainActivity.isAppDark) read .value before composition and still get the
    // persisted value; no window where a subscriber sees the default (the timing constraint
    // issues-cn #08 hinges on)
    val themeMode: StateFlow<AppThemeMode>
        field = MutableStateFlow(AppThemeMode.FollowSystem)

    val scrollBarValue: StateFlow<String>
        field = MutableStateFlow(noScrollBarValue)

    val scrollBarMode: StateFlow<ScrollBarMode>
        field = MutableStateFlow(ScrollBarMode.None)

    // The boolean switches have no enum shape; the seeded defaults match every read point's
    // default today (false)
    val allowNetworkData: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val outdatedOrderFirst: StateFlow<Boolean>
        field = MutableStateFlow(false)

    // Parsing is verbatim-identical to the replaced setMyTheme / setMyScrollBar when()s:
    // unrecognized values (including the retired "power saver" and null) fall back to follow
    // system / none, and a stored Draggable value still parses as itself (the UI does not draw
    // it yet). Transient params/locals are uniformly named xxxStored, distinct from the public
    // persisted flows xxxValue
    private fun parseThemeMode(themeStored: String?): AppThemeMode = when (themeStored) {
        alwaysLightValue -> AppThemeMode.AlwaysLight
        alwaysDarkValue -> AppThemeMode.AlwaysDark
        else -> AppThemeMode.FollowSystem
    }

    private fun parseScrollBarMode(scrollBarStored: String?): ScrollBarMode = when (scrollBarStored) {
        normalScrollBarValue -> ScrollBarMode.Normal
        fastScrollBarValue -> ScrollBarMode.Draggable
        else -> ScrollBarMode.None
    }

    // SharedPreferences registers listeners through a weak reference internally: this class
    // must hold the listener in a field, or it gets GC'd and the flows silently stop updating
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                themeKey -> {
                    val themeStored = prefs.getString(themeKey, null) ?: followSystemValue
                    themeValue.value = themeStored
                    themeMode.value = parseThemeMode(themeStored)
                }

                scrollBarKey -> {
                    val scrollBarStored = prefs.getString(scrollBarKey, null) ?: noScrollBarValue
                    scrollBarValue.value = scrollBarStored
                    scrollBarMode.value = parseScrollBarMode(scrollBarStored)
                }

                allowNetworkKey -> allowNetworkData.value = prefs.getBoolean(allowNetworkKey, false)

                outdatedOrderKey -> outdatedOrderFirst.value = prefs.getBoolean(outdatedOrderKey, false)
            }
        }

    init {
        // Construction-time seeding: the SP reads complete synchronously, so the first frame and
        // every pre-composition read see the persisted value, not the default
        var themeStored = prefs.getString(themeKey, null) ?: followSystemValue

        // One-time migration of the retired "power saver" mode (moved in from initTheme): the
        // tombstone value stays in strings.xml, never recycled; the startup write-back to follow
        // system lets every later read (settings page included) see a clean value instead of
        // relying on the parse fallback
        if (themeStored == powerSaverValue) {
            prefs.edit { putString(themeKey, followSystemValue) }
            themeStored = followSystemValue
        }
        themeValue.value = themeStored
        themeMode.value = parseThemeMode(themeStored)

        val scrollBarStored = prefs.getString(scrollBarKey, null) ?: noScrollBarValue
        scrollBarValue.value = scrollBarStored
        scrollBarMode.value = parseScrollBarMode(scrollBarStored)

        allowNetworkData.value = prefs.getBoolean(allowNetworkKey, false)
        outdatedOrderFirst.value = prefs.getBoolean(outdatedOrderKey, false)

        // The normalization write happens before registration, so it never triggers the store's
        // own listener -- seeding and callback each own their phase. Process-lifetime singleton:
        // the listener lives as long as the process, no unregister needed
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    // Write entry points: touch SP exactly once and let the listener's synchronous callback
    // (same-process writes notify synchronously, effective the same frame) refresh the flows --
    // never write a flow here, preserving the single update path: SP is the persisted truth and
    // flows are only ever updated via the callback
    fun setTheme(value: String) {
        prefs.edit { putString(themeKey, value) }
    }

    fun setScrollBarMode(value: String) {
        prefs.edit { putString(scrollBarKey, value) }
    }

    fun setAllowNetworkData(value: Boolean) {
        prefs.edit { putBoolean(allowNetworkKey, value) }
    }

    fun setOutdatedOrderFirst(value: Boolean) {
        prefs.edit { putBoolean(outdatedOrderKey, value) }
    }
}
