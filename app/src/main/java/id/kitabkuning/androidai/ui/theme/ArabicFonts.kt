package id.kitabkuning.androidai.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import id.kitabkuning.androidai.data.ArabicFont

/**
 * Resolves res/font files at runtime. The font binaries are supplied by the
 * user after this module is compiled, so there is deliberately no compile-time
 * R.font reference: a missing file reports id 0 and the UI disables that option.
 */
object ArabicFontResources {

    fun availableFonts(context: Context): Set<ArabicFont> =
        ArabicFont.entries
            .filter { font -> font.resourceName != null && resolveFontId(context, font) != 0 }
            .toSet()

    fun fontFamily(context: Context, font: ArabicFont): FontFamily {
        if (font.resourceName == null) return FontFamily.Default
        val fontId = resolveFontId(context, font)
        if (fontId == 0) return FontFamily.Default
        return try {
            FontFamily(Font(fontId))
        } catch (error: RuntimeException) {
            FontFamily.Default
        }
    }

    private fun resolveFontId(context: Context, font: ArabicFont): Int {
        val name = font.resourceName ?: return 0
        return try {
            context.resources.getIdentifier(name, "font", context.packageName)
        } catch (error: RuntimeException) {
            0
        }
    }
}
