package com.sonique.domain.lyrics.utils

/**
 * Converts Devanagari (Hindi) script to phonetic Roman (Hinglish) script.
 *
 * Uses rule-based transliteration:
 *  - Each consonant has an inherent "a" vowel (unless suppressed by virama ्)
 *  - Matras (vowel signs) modify the preceding consonant vowel
 *  - Anusvara (ं) renders as "n", visarga (ः) as "h"
 *
 * The mapping follows a simplified IAST-inspired scheme tuned for natural
 * Hinglish readability (e.g. क→k, ख→kh, ग→g, etc.)
 */
object HindiTransliterator {

    // ── Unicode ranges ────────────────────────────────────────────────────────
    private const val VIRAMA = '\u094D'      // ् halant / virama
    private const val ANUSVARA = '\u0902'    // ं
    private const val CHANDRABINDU = '\u0901'// ँ
    private const val VISARGA = '\u0903'     // ः
    private const val NUKTA = '\u093C'       // ़

    private val CONSONANT: Map<Char, String> = mapOf(
        'क' to "k",  'ख' to "kh", 'ग' to "g",  'घ' to "gh", 'ङ' to "ng",
        'च' to "ch", 'छ' to "chh",'ज' to "j",  'झ' to "jh", 'ञ' to "ny",
        'ट' to "t",  'ठ' to "th", 'ड' to "d",  'ढ' to "dh", 'ण' to "n",
        'त' to "t",  'थ' to "th", 'द' to "d",  'ध' to "dh", 'न' to "n",
        'प' to "p",  'फ' to "ph", 'ब' to "b",  'भ' to "bh", 'म' to "m",
        'य' to "y",  'र' to "r",  'ल' to "l",  'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s",  'ह' to "h",
        'ळ' to "l",
        '\u0958' to "q",  // क़
        '\u0959' to "kh", // ख़
        '\u095A' to "g",  // ग़
        '\u095B' to "z",  // ज़
        '\u095C' to "r",  // ड़
        '\u095D' to "rh", // ढ़
        '\u095E' to "f",  // फ़
        '\u095F' to "y"   // य़
    )

    // ── Independent vowels ────────────────────────────────────────────────────
    private val VOWEL = mapOf(
        'अ' to "a",  'आ' to "aa", 'इ' to "i",  'ई' to "ee",
        'उ' to "u",  'ऊ' to "oo", 'ए' to "e",  'ऐ' to "ai",
        'ओ' to "o",  'औ' to "au", 'ऋ' to "ri", 'ॠ' to "ree",
        'ऑ' to "o",  'ऍ' to "ae",
    )

    // ── Dependent vowel signs (matras) ────────────────────────────────────────
    private val MATRA = mapOf(
        '\u093E' to "aa",  // ा  aa
        '\u093F' to "i",   // ि  i
        '\u0940' to "ee",  // ी  ee
        '\u0941' to "u",   // ु  u
        '\u0942' to "oo",  // ू  oo
        '\u0943' to "ri",  // ृ  ri
        '\u0944' to "ree", // ॄ  ree
        '\u0947' to "e",   // े  e
        '\u0948' to "ai",  // ै  ai
        '\u094B' to "o",   // ो  o
        '\u094C' to "au",  // ौ  au
        '\u0945' to "ae",  // ॅ  ae
        '\u094F' to "oe",  // ॏ  oe
        '\u0949' to "o",   // ॉ  o
    )

    // ── Per-char consonant lookup incl. nukta compound variants ───────────────
    private fun consonantRoman(c: Char, next: Char?): String? {
        // Standard consonant map
        CONSONANT[c]?.let { return it }
        return null
    }

    /** Returns true if [c] is a Devanagari consonant */
    private fun isConsonant(c: Char) = c in '\u0915'..'\u0939' || c in '\u0958'..'\u095F' || c == '\u0933'

    /** Returns true if [c] is any Devanagari character (vowel, consonant, sign) */
    private fun isDevanagari(c: Char) = c in '\u0900'..'\u097F'

    /**
     * Transliterate a single word / token that may be a mix of Devanagari and
     * non-Devanagari characters.
     */
    fun transliterate(input: String): String {
        if (input.none { isDevanagari(it) }) return input   // no Hindi → pass-through

        val out = StringBuilder(input.length * 2)
        val chars = input.toList()
        var i = 0

        while (i < chars.size) {
            val c = chars[i]
            val next = chars.getOrNull(i + 1)
            val next2 = chars.getOrNull(i + 2)

            when {
                // ── Independent vowel ─────────────────────────────────────
                VOWEL.containsKey(c) -> {
                    out.append(VOWEL[c])
                    i++
                }

                // ── Consonant ─────────────────────────────────────────────
                isConsonant(c) -> {
                    val roman = consonantRoman(c, next) ?: c.toString()
                    out.append(roman)
                    i++
                    // What follows the consonant?
                    when {
                        // Virama → suppress inherent-a
                        next == VIRAMA -> {
                            i++ // consume virama
                        }
                        // Matra → replace inherent-a with the matra vowel
                        next != null && MATRA.containsKey(next) -> {
                            out.append(MATRA[next])
                            i++ // consume matra
                        }
                        // Anusvara after matra — handled next iteration
                        // Next is consonant, anusvara, visarga, end → add inherent-a
                        else -> {
                            out.append("a")
                        }
                    }
                    // Anusvara / chandrabindu following (ं/ँ)
                    if (i < chars.size && (chars[i] == ANUSVARA || chars[i] == CHANDRABINDU)) {
                        out.append("n")
                        i++
                    }
                    // Visarga (ः)
                    if (i < chars.size && chars[i] == VISARGA) {
                        out.append("h")
                        i++
                    }
                    // Nukta — already consumed as part of compound (skip)
                    if (i < chars.size && chars[i] == NUKTA) i++
                }

                // ── Standalone anusvara (e.g. after independent vowel) ────
                c == ANUSVARA || c == CHANDRABINDU -> { out.append("n"); i++ }
                c == VISARGA -> { out.append("h"); i++ }
                c == VIRAMA -> i++ // stray virama → skip

                // ── Non-Devanagari (punctuation, spaces, Latin, digits) ───
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    /**
     * Transliterate every word in a full lyrics line.
     * Preserves original spacing and punctuation between words.
     */
    fun transliterateLine(line: String): String {
        if (line.none { isDevanagari(it) }) return line
        return line.split(" ").joinToString(" ") { transliterate(it) }
    }

    /** Returns true if the string contains significant Devanagari script */
    fun isDevanagari(text: String): Boolean =
        text.count { isDevanagari(it) } > text.length * 0.3
}
