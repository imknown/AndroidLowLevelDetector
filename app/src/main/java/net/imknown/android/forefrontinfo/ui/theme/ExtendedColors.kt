package net.imknown.android.forefrontinfo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic key for a list row's status dot. Replaces the old @AttrRes Int handles (R.attr.color*),
// so the color token no longer depends on any XML resource / R.attr.
enum class StatusColor {
    NONE,
    NO_PROBLEM,
    WARNING,
    CRITICAL
}

/**
 * Project-specific status colors outside the Material 3 palette.
 * The six ARGB literals (light + dark) are defined here in Kotlin; the old XML color resources are retired.
 */
@Immutable
data class ExtendedColors(
    val noProblem: Color,
    val warning: Color,
    val critical: Color,
)

internal val ExtendedLightColors = ExtendedColors(
    noProblem = Color(0xD0_ACDDB7),
    warning = Color(0xD0_FDD18F),
    critical = Color(0xD0_FFB1AC),
)

internal val ExtendedDarkColors = ExtendedColors(
    noProblem = Color(0xD0_2B5128),
    warning = Color(0xD0_7E581F),
    critical = Color(0xD0_812F2F),
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedLightColors }

/** Maps a [StatusColor] onto the extended palette; NONE yields Color.Unspecified (caller may also filter it out). */
@Composable
fun ExtendedColors.of(status: StatusColor): Color = when (status) {
    StatusColor.NO_PROBLEM -> noProblem
    StatusColor.WARNING -> warning
    StatusColor.CRITICAL -> critical
    StatusColor.NONE -> Color.Unspecified
}
