package id.kitabkuning.androidai.data

/**
 * User preferences persisted in DataStore. All values have product-approved
 * defaults so a fresh install and a corrupt/missing preference file behave
 * identically.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val colorPalette: ColorPalette = ColorPalette.DEFAULT,
    val arabicFont: ArabicFont = ArabicFont.DEFAULT,
    val arabicTextSizeSp: Float = ARABIC_TEXT_SIZE_DEFAULT_SP,
    val translationTextSizeSp: Float = TRANSLATION_TEXT_SIZE_DEFAULT_SP,
    val reciter: Reciter = Reciter.DEFAULT,
) {

    /** Values read from disk are untrusted; keep the UI inside the approved ranges. */
    fun withClampedSizes(): AppSettings = copy(
        arabicTextSizeSp = clampArabicTextSizeSp(arabicTextSizeSp),
        translationTextSizeSp = clampTranslationTextSizeSp(translationTextSizeSp),
    )

    /**
     * Fonts are delivered as res/font files after the app is built, so the
     * selected font may not exist yet. Fall back to the system font until the
     * user's file is present.
     */
    fun effectiveArabicFont(availableFonts: Set<ArabicFont>): ArabicFont =
        if (arabicFont == ArabicFont.SYSTEM || arabicFont in availableFonts) {
            arabicFont
        } else {
            ArabicFont.SYSTEM
        }
}

/** How the app chooses between the light and dark color schemes. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT: ThemeMode = SYSTEM

        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key?.lowercase() } ?: DEFAULT
    }
}

/** Reading palettes offered in Settings. DINAMIS uses wallpaper colors on API 31+. */
enum class ColorPalette(val key: String) {
    HIJAU("hijau"),
    SEPIA("sepia"),
    BIRU("biru"),
    UNGU("ungu"),
    DINAMIS("dinamis");

    companion object {
        val DEFAULT: ColorPalette = HIJAU

        fun fromKey(key: String?): ColorPalette =
            entries.firstOrNull { it.key == key?.lowercase() } ?: DEFAULT
    }
}

/**
 * Arabic reading fonts. [resourceName] is the res/font name (without extension)
 * and is resolved at runtime, so this module compiles before the font files are
 * supplied. No compile-time R reference is needed (or possible).
 */
enum class ArabicFont(val key: String, val resourceName: String?) {
    SYSTEM("system", null),
    LPMQ("lpmq", "lpmq_isep_misbah"),
    MAJEED("majeed", "al_qalam_quran_majeed"),
    NOOREHIRA("noorehira", "noorehira");

    val isFileBacked: Boolean get() = resourceName != null

    companion object {
        val DEFAULT: ArabicFont = SYSTEM

        fun fromKey(key: String?): ArabicFont =
            entries.firstOrNull { it.key == key?.lowercase() } ?: DEFAULT
    }
}

const val ARABIC_TEXT_SIZE_MIN_SP = 20f
const val ARABIC_TEXT_SIZE_MAX_SP = 34f
const val ARABIC_TEXT_SIZE_DEFAULT_SP = 22f

const val TRANSLATION_TEXT_SIZE_MIN_SP = 12f
const val TRANSLATION_TEXT_SIZE_MAX_SP = 22f
const val TRANSLATION_TEXT_SIZE_DEFAULT_SP = 16f

/** Clamps to the approved slider range; NaN (corrupt float) uses the default. */
fun clampArabicTextSizeSp(value: Float): Float =
    if (value.isNaN()) {
        ARABIC_TEXT_SIZE_DEFAULT_SP
    } else {
        value.coerceIn(ARABIC_TEXT_SIZE_MIN_SP, ARABIC_TEXT_SIZE_MAX_SP)
    }

fun clampTranslationTextSizeSp(value: Float): Float =
    if (value.isNaN()) {
        TRANSLATION_TEXT_SIZE_DEFAULT_SP
    } else {
        value.coerceIn(TRANSLATION_TEXT_SIZE_MIN_SP, TRANSLATION_TEXT_SIZE_MAX_SP)
    }
