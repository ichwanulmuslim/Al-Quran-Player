package id.kitabkuning.androidai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An entry from the bundled index (assets/quran.json). Works fully offline.
 * Note the index uses the singular field `recitation` (a full-surah mp3 URL).
 */
@Serializable
data class Surah(
    @SerialName("number_of_surah") val number: Int,
    val name: String,
    @SerialName("name_translations") val nameTranslations: NameTranslations,
    @SerialName("number_of_ayah") val numberOfAyah: Int,
    val place: String,
    val type: String,
    val recitation: String,
) {
    val arabicName: String get() = nameTranslations.ar
    val indonesianMeaning: String get() = nameTranslations.id
}

@Serializable
data class NameTranslations(
    val ar: String,
    val en: String,
    val id: String,
)

/**
 * Lazily-fetched detail (surah/{n}.json). Adds `recitations` (array), `verses`
 * and a `tafsir` object which we deliberately ignore via ignoreUnknownKeys.
 */
@Serializable
data class SurahDetail(
    @SerialName("number_of_surah") val number: Int,
    val name: String,
    @SerialName("name_translations") val nameTranslations: NameTranslations,
    @SerialName("number_of_ayah") val numberOfAyah: Int,
    val place: String,
    val type: String,
    val recitations: List<Recitation> = emptyList(),
    val verses: List<Verse> = emptyList(),
)

@Serializable
data class Recitation(
    val name: String,
    @SerialName("audio_url") val audioUrl: String,
)

@Serializable
data class Verse(
    val number: Int,
    val text: String,
    @SerialName("translation_en") val translationEn: String,
    @SerialName("translation_id") val translationId: String,
)