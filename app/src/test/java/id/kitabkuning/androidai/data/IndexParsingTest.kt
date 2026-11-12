package id.kitabkuning.androidai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the REAL bundled index asset. Unit tests run with the module dir as the
 * working directory, so the asset is read via a relative file path.
 */
class IndexParsingTest {

    private fun loadAsset(): String {
        val candidates = listOf(
            File("src/main/assets/quran.json"),
            File("app/src/main/assets/quran.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("quran.json not found; cwd=${File(".").absolutePath}")
        return file.readText()
    }

    @Test
    fun index_has114Surahs_inOrder() {
        val surahs = QuranJson.parseIndex(loadAsset())
        assertEquals(114, surahs.size)
        assertEquals((1..114).toList(), surahs.map { it.number })
    }

    @Test
    fun surah1_fieldsAreCorrect() {
        val surah = QuranJson.parseIndex(loadAsset()).first { it.number == 1 }
        assertEquals("Al-Fatiha", surah.name)
        assertEquals("الفاتحة", surah.arabicName)
        assertEquals("Pembukaan", surah.indonesianMeaning)
        assertEquals(7, surah.numberOfAyah)
        assertEquals("Makkiyah", surah.type)
        assertTrue(surah.recitation.endsWith("001.mp3"))
    }

    @Test
    fun surah114_fieldsAreCorrect() {
        val surah = QuranJson.parseIndex(loadAsset()).first { it.number == 114 }
        assertEquals("An-Nas", surah.name)
        assertEquals("الناس", surah.arabicName)
        assertEquals("Manusia", surah.indonesianMeaning)
        assertEquals(6, surah.numberOfAyah)
        assertEquals("Makkiyah", surah.type)
        assertTrue(surah.recitation.endsWith("114.mp3"))
    }
}