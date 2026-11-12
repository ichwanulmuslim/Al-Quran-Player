package id.kitabkuning.androidai.data

/**
 * Offline search over the surah index. Matches by number, latin name, Arabic
 * name or Indonesian meaning. Extracted as a pure function for unit testing.
 */
object SurahSearch {
    fun filter(surahs: List<Surah>, query: String): List<Surah> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return surahs
        val lower = trimmed.lowercase()
        return surahs.filter { surah ->
            surah.number.toString().contains(trimmed) ||
                surah.name.contains(lower, ignoreCase = true) ||
                surah.arabicName.contains(trimmed) ||
                surah.indonesianMeaning.contains(lower, ignoreCase = true)
        }
    }
}