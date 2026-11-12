package id.kitabkuning.androidai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.kitabkuning.androidai.ui.QuranPlayerApp
import id.kitabkuning.androidai.ui.settings.SettingsViewModel
import id.kitabkuning.androidai.ui.theme.AndroidAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            AndroidAITheme(appSettings = settings) {
                QuranPlayerApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}
