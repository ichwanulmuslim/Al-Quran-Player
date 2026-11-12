package id.kitabkuning.androidai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import id.kitabkuning.androidai.data.ARABIC_TEXT_SIZE_DEFAULT_SP
import id.kitabkuning.androidai.data.TRANSLATION_TEXT_SIZE_DEFAULT_SP

/** Arabic needs extra vertical room so harakat and waqf marks are never clipped. */
const val ARABIC_LINE_HEIGHT_MULTIPLIER = 1.8f
const val TRANSLATION_LINE_HEIGHT_MULTIPLIER = 1.5f

/** Extra size used for Arabic surah titles so they stay visually dominant. */
const val ARABIC_HEADER_EXTRA_SP = 8f
const val ARABIC_HEADER_MAX_SP = 42f

private val ReadingLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Resolved reading typography for every Arabic/translation render site.
 * Provided once by [AndroidAITheme] so all screens share the same font and
 * user-chosen sizes.
 */
data class ReadingStyle(
    val arabicFontFamily: FontFamily = FontFamily.Default,
    val arabicSizeSp: Float = ARABIC_TEXT_SIZE_DEFAULT_SP,
    val translationSizeSp: Float = TRANSLATION_TEXT_SIZE_DEFAULT_SP,
) {
    val arabicLineHeightSp: Float get() = arabicSizeSp * ARABIC_LINE_HEIGHT_MULTIPLIER
    val translationLineHeightSp: Float get() = translationSizeSp * TRANSLATION_LINE_HEIGHT_MULTIPLIER
    val headerArabicSizeSp: Float
        get() = (arabicSizeSp + ARABIC_HEADER_EXTRA_SP).coerceAtMost(ARABIC_HEADER_MAX_SP)
}

val LocalReadingStyle = staticCompositionLocalOf { ReadingStyle() }

/**
 * Style for Arabic text: user font, user size, 1.8x line height with full
 * ascent/descent, no letter spacing, and Start alignment (pair with
 * `LocalLayoutDirection = Rtl` so Start means the right edge).
 */
@Composable
fun arabicReadingStyle(
    base: TextStyle = MaterialTheme.typography.bodyLarge,
    sizeSp: Float? = null,
): TextStyle {
    val reading = LocalReadingStyle.current
    val size = sizeSp ?: reading.arabicSizeSp
    return base.copy(
        fontFamily = reading.arabicFontFamily,
        fontSize = size.sp,
        lineHeight = (size * ARABIC_LINE_HEIGHT_MULTIPLIER).sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        lineHeightStyle = ReadingLineHeightStyle,
    )
}

/** Style for Indonesian translations: user size and 1.5x line height. */
@Composable
fun translationReadingStyle(
    base: TextStyle = MaterialTheme.typography.bodyMedium,
): TextStyle {
    val reading = LocalReadingStyle.current
    return base.copy(
        fontSize = reading.translationSizeSp.sp,
        lineHeight = reading.translationLineHeightSp.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ReadingLineHeightStyle,
    )
}
