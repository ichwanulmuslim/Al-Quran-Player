package id.kitabkuning.androidai.ui.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.kitabkuning.androidai.R
import id.kitabkuning.androidai.data.ARABIC_TEXT_SIZE_MAX_SP
import id.kitabkuning.androidai.data.ARABIC_TEXT_SIZE_MIN_SP
import id.kitabkuning.androidai.data.ArabicFont
import id.kitabkuning.androidai.data.ColorPalette
import id.kitabkuning.androidai.data.TRANSLATION_TEXT_SIZE_MAX_SP
import id.kitabkuning.androidai.data.TRANSLATION_TEXT_SIZE_MIN_SP
import id.kitabkuning.androidai.data.ThemeMode
import id.kitabkuning.androidai.data.clampArabicTextSizeSp
import id.kitabkuning.androidai.data.clampTranslationTextSizeSp
import id.kitabkuning.androidai.ui.theme.ArabicFontResources
import id.kitabkuning.androidai.ui.theme.LocalDarkTheme
import id.kitabkuning.androidai.ui.theme.LocalReadingStyle
import id.kitabkuning.androidai.ui.theme.ReadingStyle
import id.kitabkuning.androidai.ui.theme.arabicReadingStyle
import id.kitabkuning.androidai.ui.theme.colorSchemeFor
import id.kitabkuning.androidai.ui.theme.translationReadingStyle
import kotlin.math.roundToInt

/**
 * Settings: live reading preview, Arabic font selection (file-backed options are
 * disabled until the user supplies the res/font file), text sizes, palette, and
 * theme mode. Every control writes straight to DataStore, so the root theme and
 * all reading screens update immediately and the choice survives restarts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val availableFonts = remember(context) { ArabicFontResources.availableFonts(context) }
    val effectiveFont = settings.effectiveArabicFont(availableFonts)

    var arabicSize by remember(settings.arabicTextSizeSp) {
        mutableFloatStateOf(clampArabicTextSizeSp(settings.arabicTextSizeSp))
    }
    var translationSize by remember(settings.translationTextSizeSp) {
        mutableFloatStateOf(clampTranslationTextSizeSp(settings.translationTextSizeSp))
    }

    val previewFontFamily = remember(context, effectiveFont) {
        ArabicFontResources.fontFamily(context, effectiveFont)
    }
    val previewStyle = ReadingStyle(
        arabicFontFamily = previewFontFamily,
        arabicSizeSp = arabicSize,
        translationSizeSp = translationSize,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreviewCard(readingStyle = previewStyle)

            SettingsSectionTitle(stringResource(R.string.settings_section_font))
            if (effectiveFont != settings.arabicFont) {
                Text(
                    stringResource(R.string.settings_font_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            ArabicFont.entries.forEach { font ->
                FontOptionRow(
                    font = font,
                    selected = settings.arabicFont == font,
                    available = font == ArabicFont.SYSTEM || font in availableFonts,
                    onSelect = { viewModel.setArabicFont(font) },
                )
            }

            SettingsSectionTitle(stringResource(R.string.settings_section_arabic_size))
            SizeSlider(
                value = arabicSize,
                valueRange = ARABIC_TEXT_SIZE_MIN_SP..ARABIC_TEXT_SIZE_MAX_SP,
                onChange = { arabicSize = clampArabicTextSizeSp(it) },
                onChangeFinished = {
                    viewModel.setArabicTextSizeSp(clampArabicTextSizeSp(arabicSize))
                },
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_translation_size))
            SizeSlider(
                value = translationSize,
                valueRange = TRANSLATION_TEXT_SIZE_MIN_SP..TRANSLATION_TEXT_SIZE_MAX_SP,
                onChange = { translationSize = clampTranslationTextSizeSp(it) },
                onChangeFinished = {
                    viewModel.setTranslationTextSizeSp(clampTranslationTextSizeSp(translationSize))
                },
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_palette))
            PaletteGrid(
                selected = settings.colorPalette,
                onSelect = viewModel::setColorPalette,
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_theme_mode))
            ThemeModeChips(
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PreviewCard(readingStyle: ReadingStyle) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.settings_preview_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompositionLocalProvider(
                LocalReadingStyle provides readingStyle,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Text(
                    stringResource(R.string.settings_preview_arabic),
                    modifier = Modifier.fillMaxWidth(),
                    style = arabicReadingStyle(base = MaterialTheme.typography.bodyLarge),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            CompositionLocalProvider(LocalReadingStyle provides readingStyle) {
                Text(
                    stringResource(R.string.settings_preview_translation),
                    style = translationReadingStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun FontOptionRow(
    font: ArabicFont,
    selected: Boolean,
    available: Boolean,
    onSelect: () -> Unit,
) {
    val label = stringResource(fontLabelRes(font))
    val status = when {
        font == ArabicFont.SYSTEM -> stringResource(R.string.settings_font_system_status)
        available -> stringResource(R.string.settings_font_ready)
        else -> stringResource(R.string.settings_font_missing, font.resourceName.orEmpty())
    }
    val contentAlpha = if (available) 1f else 0.55f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                enabled = available,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = available)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                // Decorative: the row already announces its selected state.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SizeSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = value,
            onValueChange = { onChange(it.roundToInt().toFloat()) },
            onValueChangeFinished = onChangeFinished,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.settings_size_value, value.roundToInt()),
            modifier = Modifier.widthIn(min = 48.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PaletteGrid(
    selected: ColorPalette,
    onSelect: (ColorPalette) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ColorPalette.entries.chunked(2).forEach { rowPalettes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowPalettes.forEach { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = selected == palette,
                        onSelect = { onSelect(palette) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowPalettes.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ColorPalette,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val darkTheme = LocalDarkTheme.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supported = palette != ColorPalette.DINAMIS || dynamicSupported
    val previewScheme = remember(palette, darkTheme, dynamicSupported, context) {
        if (palette == ColorPalette.DINAMIS && dynamicSupported) {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            colorSchemeFor(palette, darkTheme)
        }
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .selectable(
                selected = selected,
                enabled = supported,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                previewScheme.primary,
                previewScheme.secondary,
                previewScheme.tertiary,
            ).forEach { color ->
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(paletteLabelRes(palette)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (supported) 1f else 0.55f,
                ),
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (palette == ColorPalette.DINAMIS) {
            Text(
                stringResource(
                    if (dynamicSupported) {
                        R.string.settings_palette_dinamis_note
                    } else {
                        R.string.settings_palette_dinamis_unsupported
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeModeChips(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(themeModeLabelRes(mode))) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@StringRes
private fun fontLabelRes(font: ArabicFont): Int = when (font) {
    ArabicFont.SYSTEM -> R.string.font_system
    ArabicFont.LPMQ -> R.string.font_lpmq
    ArabicFont.MAJEED -> R.string.font_majeed
    ArabicFont.NOOREHIRA -> R.string.font_noorehira
}

@StringRes
private fun paletteLabelRes(palette: ColorPalette): Int = when (palette) {
    ColorPalette.HIJAU -> R.string.palette_hijau
    ColorPalette.SEPIA -> R.string.palette_sepia
    ColorPalette.BIRU -> R.string.palette_biru
    ColorPalette.UNGU -> R.string.palette_ungu
    ColorPalette.DINAMIS -> R.string.palette_dinamis
}

@StringRes
private fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}
