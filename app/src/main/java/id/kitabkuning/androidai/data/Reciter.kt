package id.kitabkuning.androidai.data

private const val QURAN_AUDIO_BASE = "https://download.quranicaudio.com/quran"

/**
 * The three qari offered by QuranJSON. Full-surah mp3 URLs follow verified,
 * uniform patterns (3-digit zero-padded {nnn}.mp3). All three patterns were
 * verified with HTTP HEAD on surah 114 -> HTTP 200, audio/mpeg.
 *
 * [pathSegment] for Saad al-Ghamdi includes the `/complete` sub-directory.
 */
enum class Reciter(val displayName: String, private val pathSegment: String) {
    MISHARY("Mishary Rashid al-Afasy", "mishaari_raashid_al_3afaasee"),
    SUDAIS("Abdur-Rahman as-Sudais", "abdurrahmaan_as-sudays"),
    GHAMDI("Saad al-Ghamdi", "sa3d_al-ghaamidi/complete");

    /** Builds the full-surah recitation URL for [surahNumber] (1..114). */
    fun url(surahNumber: Int): String {
        val padded = surahNumber.toString().padStart(3, '0')
        return "$QURAN_AUDIO_BASE/$pathSegment/$padded.mp3"
    }

    companion object {
        val DEFAULT: Reciter = MISHARY

        /** Restores a persisted qari; unknown/missing values use the default. */
        fun fromKey(key: String?): Reciter =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: DEFAULT
    }
}