package id.kitabkuning.androidai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the offline search filter (number / latin / Arabic / Indonesian). */
class SurahSearchTest {

    private val sample = listOf(
        Surah(1, "Al-Fatiha", NameTranslations("الفاتحة", "The Opening", "Pembukaan"), 7, "Mecca", "Makkiyah", Reciter.MISHARY.url(1)),
        Surah(2, "Al-Baqara", NameTranslations("البقرة", "The Cow", "Sapi Betina"), 286, "Medina", "Madaniyah", Reciter.MISHARY.url(2)),
        Surah(114, "An-Nas", NameTranslations("الناس", "Mankind", "Manusia"), 6, "Mecca", "Makkiyah", Reciter.MISHARY.url(114)),
    )

    @Test
    fun emptyQuery_returnsAll() {
        assertEquals(sample, SurahSearch.filter(sample, ""))
        assertEquals(sample, SurahSearch.filter(sample, "   "))
    }

    @Test
    fun byNumber_exact() {
        val result = SurahSearch.filter(sample, "114")
        assertEquals(listOf(114), result.map { it.number })
    }

    @Test
    fun byNumber_substring() {
        val result = SurahSearch.filter(sample, "1")
        assertEquals(listOf(1, 114), result.map { it.number })
    }

    @Test
    fun byLatinName_caseInsensitive() {
        val result = SurahSearch.filter(sample, "baqara")
        assertEquals(listOf(2), result.map { it.number })
    }

    @Test
    fun byIndonesianMeaning() {
        val result = SurahSearch.filter(sample, "manusia")
        assertEquals(listOf(114), result.map { it.number })
    }

    @Test
    fun byArabicName() {
        val result = SurahSearch.filter(sample, "الفاتحة")
        assertEquals(listOf(1), result.map { it.number })
    }

    @Test
    fun noMatch_returnsEmpty() {
        assertTrue(SurahSearch.filter(sample, "zzzzz").isEmpty())
    }
}