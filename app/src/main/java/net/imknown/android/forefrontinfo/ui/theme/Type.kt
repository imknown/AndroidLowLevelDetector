package net.imknown.android.forefrontinfo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Match the legacy my_view_holder.xml look:
 * - The legacy TextView defaults to includeFontPadding=true, while recent Compose flipped
 *   the default to false (verified in the ui-text PlatformParagraphStyle private
 *   constructor), so both slots turn it back on explicitly to restore the lost font padding;
 * - No lineHeight set: font metrics + font padding = same as the legacy TextView without
 *   an explicit line height;
 * - The material3 Typography constructor replaces slots entirely (defaults come from
 *   TypographyTokens, no merging), so every kept field must be spelled out;
 * - Re-tune when Material 3 Expressive becomes available.
 */
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    ),
)
