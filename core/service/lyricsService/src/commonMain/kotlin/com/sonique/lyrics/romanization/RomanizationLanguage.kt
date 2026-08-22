package com.sonique.lyrics.romanization

import com.sonique.domain.data.model.lyrics.RomanizationLanguage

/**
 * What a single line is written in. Detected per LINE rather than per song, because a lyric sheet
 * routinely mixes an original line with an English one, and romanizing the English half produces
 * nonsense.
 */
internal enum class LineScript {
    LATIN,
    JAPANESE,
    HANGUL,
    HAN,
    DEVANAGARI,
    GURMUKHI,
    CYRILLIC,
}

private const val HIRAGANA_START = '぀'
private const val HIRAGANA_END = 'ゟ'
private const val KATAKANA_START = '゠'
private const val KATAKANA_END = 'ヿ'
private const val HAN_START = '一'
private const val HAN_END = '鿿'
private const val HANGUL_SYLLABLE_START = '가'
private const val HANGUL_SYLLABLE_END = '힣'
private const val HANGUL_JAMO_START = 'ᄀ'
private const val HANGUL_JAMO_END = 'ᇿ'
private const val DEVANAGARI_START = 'ऀ'
private const val DEVANAGARI_END = 'ॿ'
private const val GURMUKHI_START = '਀'
private const val GURMUKHI_END = '੿'
private const val CYRILLIC_START = 'Ѐ'
private const val CYRILLIC_END = 'ӿ'

internal fun Char.isKana(): Boolean =
    this in HIRAGANA_START..HIRAGANA_END || this in KATAKANA_START..KATAKANA_END

internal fun Char.isHan(): Boolean = this in HAN_START..HAN_END

internal fun Char.isHangul(): Boolean =
    this in HANGUL_SYLLABLE_START..HANGUL_SYLLABLE_END || this in HANGUL_JAMO_START..HANGUL_JAMO_END

internal fun Char.isDevanagari(): Boolean = this in DEVANAGARI_START..DEVANAGARI_END

internal fun Char.isGurmukhi(): Boolean = this in GURMUKHI_START..GURMUKHI_END

internal fun Char.isCyrillic(): Boolean = this in CYRILLIC_START..CYRILLIC_END

/**
 * The script [line] is predominantly written in.
 *
 * Kana wins over Han outright rather than by count: a Japanese line is mostly kanji with a few
 * kana particles, so counting characters would call it Chinese. One kana anywhere is proof the
 * line is Japanese, and Chinese never contains any.
 */
internal fun detectScript(line: String): LineScript {
    if (line.any { it.isKana() }) return LineScript.JAPANESE
    if (line.any { it.isHangul() }) return LineScript.HANGUL
    if (line.any { it.isDevanagari() }) return LineScript.DEVANAGARI
    if (line.any { it.isGurmukhi() }) return LineScript.GURMUKHI
    if (line.any { it.isCyrillic() }) return LineScript.CYRILLIC
    if (line.any { it.isHan() }) return LineScript.HAN
    return LineScript.LATIN
}

/**
 * Which Cyrillic language a line is in.
 *
 * Unicode cannot answer this — all seven share one block — so the guess is made from the letters
 * each language has that the others do not, and falls back to Russian, which is both the most
 * common and the one whose alphabet is a subset of the rest.
 *
 * Only consulted when the user has enabled MORE THAN ONE Cyrillic language; with exactly one
 * enabled, that choice is the answer and no guessing happens at all.
 */
internal fun guessCyrillicLanguage(
    line: String,
    enabled: Set<RomanizationLanguage>,
): RomanizationLanguage? {
    val cyrillicEnabled = enabled.filter { it in CYRILLIC_LANGUAGES }
    if (cyrillicEnabled.isEmpty()) return null
    if (cyrillicEnabled.size == 1) return cyrillicEnabled.single()

    // Ordered most-distinctive first. Serbian and Macedonian share ј/љ/њ/џ, so the letters unique
    // to Macedonian (ѓ ќ ѕ) are tested before the pair they have in common.
    val distinctive =
        listOf(
            RomanizationLanguage.MACEDONIAN to "ѓќѕ",
            RomanizationLanguage.SERBIAN to "ђћџљњј",
            RomanizationLanguage.KYRGYZ to "ңөү",
            RomanizationLanguage.BELARUSIAN to "ўі",
            RomanizationLanguage.UKRAINIAN to "їєґ",
        )
    for ((language, markers) in distinctive) {
        if (language in cyrillicEnabled && line.any { it.lowercaseChar() in markers }) return language
    }
    return RomanizationLanguage.RUSSIAN.takeIf { it in cyrillicEnabled } ?: cyrillicEnabled.first()
}

internal val CYRILLIC_LANGUAGES =
    setOf(
        RomanizationLanguage.RUSSIAN,
        RomanizationLanguage.UKRAINIAN,
        RomanizationLanguage.SERBIAN,
        RomanizationLanguage.BULGARIAN,
        RomanizationLanguage.BELARUSIAN,
        RomanizationLanguage.KYRGYZ,
        RomanizationLanguage.MACEDONIAN,
    )
