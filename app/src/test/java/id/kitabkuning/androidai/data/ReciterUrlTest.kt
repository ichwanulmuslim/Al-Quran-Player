package id.kitabkuning.androidai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the per-reciter full-surah URL patterns and 3-digit zero padding. */
class ReciterUrlTest {

    @Test
    fun mishary_urls() {
        assertEquals(
            "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/001.mp3",
            Reciter.MISHARY.url(1),
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/114.mp3",
            Reciter.MISHARY.url(114),
        )
    }

    @Test
    fun sudais_urls() {
        assertEquals(
            "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/001.mp3",
            Reciter.SUDAIS.url(1),
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/abdurrahmaan_as-sudays/114.mp3",
            Reciter.SUDAIS.url(114),
        )
    }

    @Test
    fun ghamdi_urls_includeCompleteSubdir() {
        assertEquals(
            "https://download.quranicaudio.com/quran/sa3d_al-ghaamidi/complete/001.mp3",
            Reciter.GHAMDI.url(1),
        )
        assertEquals(
            "https://download.quranicaudio.com/quran/sa3d_al-ghaamidi/complete/114.mp3",
            Reciter.GHAMDI.url(114),
        )
    }

    @Test
    fun padding_isThreeDigits() {
        assertTrue(Reciter.MISHARY.url(1).endsWith("/001.mp3"))
        assertTrue(Reciter.MISHARY.url(10).endsWith("/010.mp3"))
        assertTrue(Reciter.MISHARY.url(99).endsWith("/099.mp3"))
        assertTrue(Reciter.MISHARY.url(114).endsWith("/114.mp3"))
    }

    @Test
    fun defaultReciter_isMishary() {
        assertEquals(Reciter.MISHARY, Reciter.DEFAULT)
    }
}