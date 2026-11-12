package id.kitabkuning.androidai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses a small representative fixture of surah/1.json: metadata + 2 verses +
 * 3 recitations + a `tafsir` object (which must be ignored via ignoreUnknownKeys).
 */
class DetailParsingTest {

    private val fixture = """
        {
          "name": "Al-Fatiha",
          "name_translations": {"ar": "الفاتحة", "en": "The Opening", "id": "Pembukaan"},
          "number_of_ayah": 7,
          "number_of_surah": 1,
          "place": "Mecca",
          "type": "Makkiyah",
          "recitations": [
            {"name": "Mishari Rashid al-Afasy", "audio_url": "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/001.mp3"},
            {"name": "Abdur-Rahman as-Sudais", "audio_url": "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/001.mp3"},
            {"name": "Saad al-Ghamdi", "audio_url": "https://download.quranicaudio.com/quran/sa3d_al-ghaamidi/complete/001.mp3"}
          ],
          "verses": [
            {"number": 1, "text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "translation_en": "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "translation_id": "Dengan menyebut nama Allah Yang Maha Pemurah lagi Maha Penyayang."},
            {"number": 2, "text": "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "translation_en": "[All] praise is [due] to Allah, Lord of the worlds -", "translation_id": "Segala puji bagi Allah, Tuhan semesta alam."}
          ],
          "tafsir": {"1": {"text": "ignored tafsir one"}, "2": {"text": "ignored tafsir two"}}
        }
    """.trimIndent()

    @Test
    fun detail_metadataIsMapped() {
        val detail = QuranJson.parseDetail(fixture)
        assertEquals(1, detail.number)
        assertEquals("Al-Fatiha", detail.name)
        assertEquals("الفاتحة", detail.nameTranslations.ar)
        assertEquals("Pembukaan", detail.nameTranslations.id)
        assertEquals(7, detail.numberOfAyah)
        assertEquals("Makkiyah", detail.type)
    }

    @Test
    fun detail_versesAndTranslationsAreMapped() {
        val detail = QuranJson.parseDetail(fixture)
        assertEquals(2, detail.verses.size)
        assertEquals(1, detail.verses[0].number)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", detail.verses[0].text)
        assertEquals(
            "Dengan menyebut nama Allah Yang Maha Pemurah lagi Maha Penyayang.",
            detail.verses[0].translationId,
        )
        assertEquals("Segala puji bagi Allah, Tuhan semesta alam.", detail.verses[1].translationId)
    }

    @Test
    fun detail_recitationsAreMapped() {
        val detail = QuranJson.parseDetail(fixture)
        assertEquals(3, detail.recitations.size)
        assertEquals("Abdur-Rahman as-Sudais", detail.recitations[1].name)
        assertTrue(detail.recitations[2].audioUrl.endsWith("sa3d_al-ghaamidi/complete/001.mp3"))
    }

    @Test
    fun detail_ignoresUnknownTafsirObject() {
        // The fixture contains a `tafsir` object; parsing must succeed regardless.
        val detail = QuranJson.parseDetail(fixture)
        assertEquals(2, detail.verses.size)
        assertEquals(3, detail.recitations.size)
    }
}