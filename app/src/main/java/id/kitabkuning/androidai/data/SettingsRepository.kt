package id.kitabkuning.androidai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// Single DataStore per process for the settings file. Defined once so multiple
// repository instances (player + settings screens) never open the same file twice.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists [AppSettings] in Preferences DataStore.
 *
 * Reads are defensive: an unreadable file falls back to empty preferences (all
 * product defaults), and every value read is mapped/normalised rather than
 * trusted verbatim.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toAppSettings() }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = mode.key }

    suspend fun setColorPalette(palette: ColorPalette) = dataStore.edit { it[Keys.COLOR_PALETTE] = palette.key }

    suspend fun setArabicFont(font: ArabicFont) = dataStore.edit { it[Keys.ARABIC_FONT] = font.key }

    suspend fun setArabicTextSizeSp(sizeSp: Float) = dataStore.edit {
        it[Keys.ARABIC_TEXT_SIZE_SP] = clampArabicTextSizeSp(sizeSp)
    }

    suspend fun setTranslationTextSizeSp(sizeSp: Float) = dataStore.edit {
        it[Keys.TRANSLATION_TEXT_SIZE_SP] = clampTranslationTextSizeSp(sizeSp)
    }

    suspend fun setReciter(reciter: Reciter) = dataStore.edit { it[Keys.RECITER] = reciter.name }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        themeMode = ThemeMode.fromKey(this[Keys.THEME_MODE]),
        colorPalette = ColorPalette.fromKey(this[Keys.COLOR_PALETTE]),
        arabicFont = ArabicFont.fromKey(this[Keys.ARABIC_FONT]),
        arabicTextSizeSp = this[Keys.ARABIC_TEXT_SIZE_SP] ?: ARABIC_TEXT_SIZE_DEFAULT_SP,
        translationTextSizeSp = this[Keys.TRANSLATION_TEXT_SIZE_SP]
            ?: TRANSLATION_TEXT_SIZE_DEFAULT_SP,
        reciter = Reciter.fromKey(this[Keys.RECITER]),
    ).withClampedSizes()

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val ARABIC_FONT = stringPreferencesKey("arabic_font")
        val ARABIC_TEXT_SIZE_SP = floatPreferencesKey("arabic_text_size_sp")
        val TRANSLATION_TEXT_SIZE_SP = floatPreferencesKey("translation_text_size_sp")
        val RECITER = stringPreferencesKey("reciter")
    }
}
