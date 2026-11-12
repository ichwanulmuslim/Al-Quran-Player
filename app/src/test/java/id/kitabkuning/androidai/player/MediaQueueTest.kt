package id.kitabkuning.androidai.player

import id.kitabkuning.androidai.data.QuranJson
import id.kitabkuning.androidai.data.Reciter
import id.kitabkuning.androidai.data.Surah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies the shared queue builder against the REAL bundled index. Runs under
 * Robolectric because MediaItem.Builder parses android.net.Uri, which the plain
 * JVM stub jar does not implement.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaQueueTest {

    private val surahs: List<Surah> by lazy {
        val candidates = listOf(
            File("src/main/assets/quran.json"),
            File("app/src/main/assets/quran.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("quran.json not found; cwd=${File(".").absolutePath}")
        QuranJson.parseIndex(file.readText())
    }

    @Test
    fun defaultQueue_has114Items_withSurahNumberMediaIds() {
        val queue = buildSurahQueue(surahs, Reciter.DEFAULT)
        assertEquals(114, queue.size)
        assertEquals((1..114).map { it.toString() }, queue.map { it.mediaId })
    }

    @Test
    fun defaultQueue_urisMatchIndexRecitation() {
        val queue = buildSurahQueue(surahs, Reciter.DEFAULT)
        surahs.zip(queue).forEach { (surah, item) ->
            assertEquals(surah.recitation, item.localConfiguration?.uri?.toString())
        }
    }

    @Test
    fun perReciter_queuesUseThatRecitersUrls() {
        for (reciter in listOf(Reciter.SUDAIS, Reciter.GHAMDI)) {
            val queue = buildSurahQueue(surahs, reciter)
            assertEquals(114, queue.size)
            surahs.zip(queue).forEach { (surah, item) ->
                assertEquals(reciter.url(surah.number), item.localConfiguration?.uri?.toString())
            }
        }
    }

    @Test
    fun metadata_carriesTitleArtistAndAlbum() {
        val first = buildSurahQueue(surahs, Reciter.SUDAIS).first().mediaMetadata
        assertEquals("Al-Fatiha", first.title)
        assertEquals("Al-Fatiha", first.displayTitle)
        assertEquals(Reciter.SUDAIS.displayName, first.artist)
        assertEquals("Quran Player", first.albumTitle)
        assertEquals(surahs.first().arabicName, first.subtitle)
    }

    @Test
    fun emptyIndex_producesEmptyQueue() {
        assertTrue(buildSurahQueue(emptyList(), Reciter.DEFAULT).isEmpty())
    }
}