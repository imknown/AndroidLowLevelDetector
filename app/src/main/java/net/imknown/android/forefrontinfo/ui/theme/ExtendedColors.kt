package net.imknown.android.forefrontinfo.ui.theme

import androidx.annotation.AttrRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import net.imknown.android.forefrontinfo.R

/**
 * Project-specific status colors outside the Material 3 palette.
 * Values mirror the six ARGB literals in values/ and values-night/colors.xml.
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

/** Maps legacy ?attr references onto the extended palette; RES_ID_NONE must be filtered by callers. */
@Composable
fun ExtendedColors.of(@AttrRes attr: Int): Color = when (attr) {
    R.attr.colorNoProblem -> noProblem
    R.attr.colorWarning -> warning
    R.attr.colorCritical -> critical
    else -> Color.Unspecified
}
