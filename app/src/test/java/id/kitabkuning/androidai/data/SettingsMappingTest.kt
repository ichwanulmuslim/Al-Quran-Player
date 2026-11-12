package id.kitabkuning.androidai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the persisted-settings contract: stable keys, unknown/missing values
 * fall back to product defaults, sizes are clamped to the approved slider range,
 * and a selected font degrades to the system font while its res/font file is
 * absent.
 */
class SettingsMappingTest {

    @Test
    fun defaults_matchProductSpec() {
        val settings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(ColorPalette.HIJAU, settings.colorPalette)
        assertEquals(ArabicFont.SYSTEM, settings.arabicFont)
        assertEquals(22f, settings.arabicTextSizeSp)
        assertEquals(16f, settings.translationTextSizeSp)
        assertEquals(Reciter.MISHARY, settings.reciter)
    }

    @Test
    fun themeMode_fromKey_mapsKnownKeys() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
    }

    @Test
    fun themeMode_isCaseInsensitiveAndUnknownFallsBack() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("sepia"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(""))
    }

    @Test
    fun colorPalette_fromKey_mapsKnownKeys() {
        assertEquals(ColorPalette.HIJAU, ColorPalette.fromKey("hijau"))
        assertEquals(ColorPalette.SEPIA, ColorPalette.fromKey("sepia"))
        assertEquals(ColorPalette.BIRU, ColorPalette.fromKey("biru"))
        assertEquals(ColorPalette.UNGU, ColorPalette.fromKey("ungu"))
        assertEquals(ColorPalette.DINAMIS, ColorPalette.fromKey("dinamis"))
    }

    @Test
    fun colorPalette_unknownOrNullFallsBackToDefault() {
        assertEquals(ColorPalette.HIJAU, ColorPalette.fromKey(null))
        assertEquals(ColorPalette.HIJAU, ColorPalette.fromKey("merah"))
    }

    @Test
    fun arabicFont_fromKey_mapsKnownKeys() {
        assertEquals(ArabicFont.SYSTEM, ArabicFont.fromKey("system"))
        assertEquals(ArabicFont.LPMQ, ArabicFont.fromKey("lpmq"))
        assertEquals(ArabicFont.MAJEED, ArabicFont.fromKey("majeed"))
        assertEquals(ArabicFont.NOOREHIRA, ArabicFont.fromKey("noorehira"))
    }

    @Test
    fun arabicFont_unknownOrNullFallsBackToSystem() {
        assertEquals(ArabicFont.SYSTEM, ArabicFont.fromKey(null))
        assertEquals(ArabicFont.SYSTEM, ArabicFont.fromKey("amiri"))
    }

    @Test
    fun arabicFont_resourceNames_matchExpectedFileNames() {
        assertEquals("lpmq_isep_misbah", ArabicFont.LPMQ.resourceName)
        assertEquals("al_qalam_quran_majeed", ArabicFont.MAJEED.resourceName)
        assertEquals("noorehira", ArabicFont.NOOREHIRA.resourceName)
        assertEquals(null, ArabicFont.SYSTEM.resourceName)
        assertTrue(ArabicFont.LPMQ.isFileBacked)
        assertFalse(ArabicFont.SYSTEM.isFileBacked)
    }

    @Test
    fun effectiveFont_usesSelectedFontWhenFileExists() {
        val settings = AppSettings().copy(arabicFont = ArabicFont.LPMQ)
        assertEquals(
            ArabicFont.LPMQ,
            settings.effectiveArabicFont(setOf(ArabicFont.LPMQ)),
        )
    }

    @Test
    fun effectiveFont_fallsBackToSystemWhenFileMissing() {
        val settings = AppSettings().copy(arabicFont = ArabicFont.NOOREHIRA)
        assertEquals(ArabicFont.SYSTEM, settings.effectiveArabicFont(emptySet()))
        assertEquals(
            ArabicFont.SYSTEM,
            settings.effectiveArabicFont(setOf(ArabicFont.LPMQ, ArabicFont.MAJEED)),
        )
    }

    @Test
    fun effectiveFont_systemStaysSystemEvenWhenOthersAreAvailable() {
        val settings = AppSettings().copy(arabicFont = ArabicFont.SYSTEM)
        assertEquals(
            ArabicFont.SYSTEM,
            settings.effectiveArabicFont(setOf(ArabicFont.LPMQ, ArabicFont.NOOREHIRA)),
        )
    }

    @Test
    fun arabicSize_clampsToApprovedRange() {
        assertEquals(ARABIC_TEXT_SIZE_MIN_SP, clampArabicTextSizeSp(0f))
        assertEquals(ARABIC_TEXT_SIZE_MIN_SP, clampArabicTextSizeSp(19.4f))
        assertEquals(24f, clampArabicTextSizeSp(24f))
        assertEquals(ARABIC_TEXT_SIZE_MAX_SP, clampArabicTextSizeSp(34.6f))
        assertEquals(ARABIC_TEXT_SIZE_MAX_SP, clampArabicTextSizeSp(100f))
    }

    @Test
    fun arabicSize_nanUsesDefault() {
        assertEquals(ARABIC_TEXT_SIZE_DEFAULT_SP, clampArabicTextSizeSp(Float.NaN))
    }

    @Test
    fun translationSize_clampsToApprovedRange() {
        assertEquals(TRANSLATION_TEXT_SIZE_MIN_SP, clampTranslationTextSizeSp(4f))
        assertEquals(18f, clampTranslationTextSizeSp(18f))
        assertEquals(TRANSLATION_TEXT_SIZE_MAX_SP, clampTranslationTextSizeSp(40f))
        assertEquals(TRANSLATION_TEXT_SIZE_DEFAULT_SP, clampTranslationTextSizeSp(Float.NaN))
    }

    @Test
    fun withClampedSizes_normalizesBothValues() {
        val normalized = AppSettings(
            arabicTextSizeSp = 1f,
            translationTextSizeSp = 99f,
        ).withClampedSizes()

        assertEquals(ARABIC_TEXT_SIZE_MIN_SP, normalized.arabicTextSizeSp)
        assertEquals(TRANSLATION_TEXT_SIZE_MAX_SP, normalized.translationTextSizeSp)
    }

    @Test
    fun reciter_fromKey_restoresPersistedQari() {
        assertEquals(Reciter.SUDAIS, Reciter.fromKey("SUDAIS"))
        assertEquals(Reciter.GHAMDI, Reciter.fromKey("Ghamdi"))
        assertEquals(Reciter.MISHARY, Reciter.fromKey("mishary"))
    }

    @Test
    fun reciter_unknownOrNullFallsBackToDefault() {
        assertEquals(Reciter.DEFAULT, Reciter.fromKey(null))
        assertEquals(Reciter.DEFAULT, Reciter.fromKey("unknown"))
    }
}
