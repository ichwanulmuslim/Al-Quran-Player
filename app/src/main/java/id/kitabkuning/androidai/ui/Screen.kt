package id.kitabkuning.androidai.ui

import androidx.compose.runtime.saveable.Saver

/**
 * Lightweight navigation destinations. navigation-compose is intentionally not
 * used; the root composable switches on this sealed type and keeps each
 * destination's saveable state via [androidx.compose.runtime.saveable.SaveableStateHolder].
 */
sealed interface Screen {
    data object List : Screen
    data class Player(val surahNumber: Int) : Screen
    data object Settings : Screen
}

private const val KEY_LIST = "list"
private const val KEY_SETTINGS = "settings"
private const val KEY_PLAYER_PREFIX = "player:"

/** Survives configuration changes and process death. */
internal val ScreenSaver: Saver<Screen, String> = Saver(
    save = { screen ->
        when (screen) {
            Screen.List -> KEY_LIST
            is Screen.Player -> "$KEY_PLAYER_PREFIX${screen.surahNumber}"
            Screen.Settings -> KEY_SETTINGS
        }
    },
    restore = { saved ->
        when {
            saved == KEY_SETTINGS -> Screen.Settings
            saved.startsWith(KEY_PLAYER_PREFIX) -> {
                val number = saved.removePrefix(KEY_PLAYER_PREFIX).toIntOrNull()
                if (number != null) Screen.Player(number) else Screen.List
            }

            else -> Screen.List
        }
    },
)
