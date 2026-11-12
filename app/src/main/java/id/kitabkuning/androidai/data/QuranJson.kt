package id.kitabkuning.androidai.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Pure JSON (de)serialization helpers so parsing can be unit-tested on the JVM
 * without an Android Context. Unknown keys (e.g. `tafsir`) are ignored.
 */
object QuranJson {
    val format: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseIndex(text: String): List<Surah> =
        format.decodeFromString<List<Surah>>(text)

    fun parseDetail(text: String): SurahDetail =
        format.decodeFromString<SurahDetail>(text)
}