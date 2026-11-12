package id.kitabkuning.androidai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import id.kitabkuning.androidai.data.AppSettings
import id.kitabkuning.androidai.data.ColorPalette
import id.kitabkuning.androidai.data.ThemeMode
import id.kitabkuning.androidai.data.clampArabicTextSizeSp
import id.kitabkuning.androidai.data.clampTranslationTextSizeSp

/** The effective dark/light decision, exposed for palette previews. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Root theme. Owns palette + mode selection, system bar appearance, and the
 * resolved reading typography shared by every screen.
 */
@Composable
fun AndroidAITheme(
    appSettings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (appSettings.themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        appSettings.colorPalette == ColorPalette.DINAMIS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> colorSchemeFor(appSettings.colorPalette, darkTheme)
    }

    val availableFonts = remember(context) { ArabicFontResources.availableFonts(context) }
    val effectiveFont = appSettings.effectiveArabicFont(availableFonts)
    val arabicFontFamily = remember(context, effectiveFont) {
        ArabicFontResources.fontFamily(context, effectiveFont)
    }
    val readingStyle = ReadingStyle(
        arabicFontFamily = arabicFontFamily,
        arabicSizeSp = clampArabicTextSizeSp(appSettings.arabicTextSizeSp),
        translationSizeSp = clampTranslationTextSizeSp(appSettings.translationTextSizeSp),
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalReadingStyle provides readingStyle,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
