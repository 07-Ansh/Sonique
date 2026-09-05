package com.sonique.lyrics.romanization

/**
 * The two scripts that cannot be transliterated by rule, and therefore need a platform library.
 *
 * Everything else in this package is arithmetic or a table and lives in commonMain. These two are
 * different in kind:
 *
 *  - **Japanese** — a kanji's reading depends on the words around it (生 is *nama*, *sei*, *i* or
 *    *u* depending on context), so this needs a morphological analyzer, not a lookup.
 *  - **Chinese** — heteronyms need the surrounding WORD to disambiguate (行 is *xíng* or *háng*),
 *    which a per-character table cannot see.
 *
 * Both return null when the platform has no implementation, and null means "leave the line as it
 * is" rather than "error". That is what lets iOS compile with no library at all.
 */
internal expect object PlatformRomanizer {
    /** Kanji and kana → romaji, or null where no analyzer is available. */
    fun japanese(line: String): String?

    /** Hanzi → pinyin, or null where no lexicon is available. */
    fun chinese(line: String): String?
}
