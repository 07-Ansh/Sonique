package com.sonique.lyrics.romanization

import com.sonique.domain.data.model.lyrics.RomanizationLanguage

/**
 * Turns one lyric line into a Latin-script reading of itself.
 *
 * Works line by line, never on the whole sheet, because a lyric sheet routinely alternates between
 * an original line and an English one — romanizing the English half produces gibberish, and a
 * per-song language would have no way to tell them apart.
 */
object LyricsRomanizer {
    /**
     * @return the romanized line, or null when there is nothing to do — the line is already Latin,
     *   its language is switched off, the platform has no library for it, or the result came back
     *   identical to the input. Null means the caller should show nothing rather than a duplicate
     *   of the line it already has.
     */
    fun romanize(
        line: String,
        enabled: Set<RomanizationLanguage>,
    ): String? {
        if (line.isBlank() || enabled.isEmpty()) return null

        val romanized =
            when (detectScript(line)) {
                LineScript.LATIN -> null

                LineScript.JAPANESE ->
                    if (RomanizationLanguage.JAPANESE in enabled) PlatformRomanizer.japanese(line) else null

                LineScript.HANGUL ->
                    if (RomanizationLanguage.KOREAN in enabled) HangulRomanizer.romanize(line) else null

                LineScript.HAN ->
                    if (RomanizationLanguage.CHINESE in enabled) PlatformRomanizer.chinese(line) else null

                LineScript.DEVANAGARI ->
                    if (RomanizationLanguage.HINDI in enabled) IndicRomanizer.romanizeDevanagari(line) else null

                LineScript.GURMUKHI ->
                    if (RomanizationLanguage.PUNJABI in enabled) IndicRomanizer.romanizeGurmukhi(line) else null

                // The only branch that has to decide WHICH language it is looking at, because the
                // seven Cyrillic languages are indistinguishable by Unicode block.
                LineScript.CYRILLIC ->
                    guessCyrillicLanguage(line, enabled)?.let { CyrillicRomanizer.romanize(line, it) }
            } ?: return null

        // A line of pure punctuation, or one whose script has no mapping, comes back unchanged.
        // Showing it under the original would just print the same thing twice.
        return romanized.takeIf { it.isNotBlank() && it != line }
    }
}
