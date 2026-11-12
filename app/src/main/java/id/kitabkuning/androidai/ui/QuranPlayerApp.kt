package id.kitabkuning.androidai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.kitabkuning.androidai.player.QuranViewModel
import id.kitabkuning.androidai.ui.list.SurahListScreen
import id.kitabkuning.androidai.ui.nowplaying.NowPlayingScreen
import id.kitabkuning.androidai.ui.settings.SettingsScreen
import id.kitabkuning.androidai.ui.settings.SettingsViewModel

/**
 * Root composable. State-based navigation between the surah list (start), Now
 * Playing, and Settings. Each destination keeps its saveable state (list scroll,
 * search field focus, settings scroll) while another is shown, and all playback
 * state stays in the ViewModel so visiting Settings never resets it.
 */
@Composable
fun QuranPlayerApp(
    viewModel: QuranViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playerState.error) {
        val message = playerState.error
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    val context = LocalContext.current
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Playback proceeds whether or not notifications are granted. */ }

    val ensureNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionAsked) {
            permissionAsked = true
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.List) }
    val saveableStateHolder = rememberSaveableStateHolder()

    BackHandler(enabled = screen !is Screen.List) { screen = Screen.List }

    when (val current = screen) {
        Screen.List -> saveableStateHolder.SaveableStateProvider(KEY_LIST_STATE) {
            SurahListScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onSurahClick = { number ->
                    ensureNotificationPermission()
                    viewModel.playSurah(number)
                    screen = Screen.Player(number)
                },
                onMiniPlayerClick = {
                    playerState.currentSurah?.let { screen = Screen.Player(it.number) }
                },
                onOpenSettings = { screen = Screen.Settings },
            )
        }

        is Screen.Player -> saveableStateHolder.SaveableStateProvider(
            "$KEY_PLAYER_STATE${current.surahNumber}"
        ) {
            NowPlayingScreen(
                surahNumber = current.surahNumber,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onBack = { screen = Screen.List },
            )
        }

        Screen.Settings -> saveableStateHolder.SaveableStateProvider(KEY_SETTINGS_STATE) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { screen = Screen.List },
            )
        }
    }
}

private const val KEY_LIST_STATE = "screen:list"
private const val KEY_PLAYER_STATE = "screen:player:"
private const val KEY_SETTINGS_STATE = "screen:settings"
