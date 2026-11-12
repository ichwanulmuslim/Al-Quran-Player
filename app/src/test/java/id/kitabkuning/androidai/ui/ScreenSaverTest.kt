package id.kitabkuning.androidai.ui

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies state-based navigation survives configuration changes/process death:
 * every destination round-trips and malformed payloads fall back to the list.
 */
class ScreenSaverTest {

    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private fun save(screen: Screen): String? =
        with(ScreenSaver) { scope.save(screen) }

    private fun restore(saved: String?): Screen? =
        with(ScreenSaver) { saved?.let { restore(it) } }

    @Test
    fun destinations_roundTrip() {
        listOf(Screen.List, Screen.Player(1), Screen.Player(114), Screen.Settings).forEach { screen ->
            assertEquals(screen, restore(save(screen)))
        }
    }

    @Test
    fun malformedPlayerPayload_fallsBackToList() {
        assertEquals(Screen.List, restore("player:"))
        assertEquals(Screen.List, restore("player:abc"))
    }

    @Test
    fun unknownPayload_fallsBackToList() {
        assertEquals(Screen.List, restore("somewhere-else"))
    }
}
