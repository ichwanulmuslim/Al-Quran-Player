package id.kitabkuning.androidai.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.kitabkuning.androidai.data.AppSettings
import id.kitabkuning.androidai.data.ArabicFont
import id.kitabkuning.androidai.data.ColorPalette
import id.kitabkuning.androidai.data.SettingsRepository
import id.kitabkuning.androidai.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes persisted settings to the UI and serializes writes. Kept separate from
 * [id.kitabkuning.androidai.player.QuranViewModel] so settings edits never
 * disturb playback state.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings(),
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch { repository.setColorPalette(palette) }
    }

    fun setArabicFont(font: ArabicFont) {
        viewModelScope.launch { repository.setArabicFont(font) }
    }

    fun setArabicTextSizeSp(sizeSp: Float) {
        viewModelScope.launch { repository.setArabicTextSizeSp(sizeSp) }
    }

    fun setTranslationTextSizeSp(sizeSp: Float) {
        viewModelScope.launch { repository.setTranslationTextSizeSp(sizeSp) }
    }
}
