package com.sonique.lyrics.romanization

/**
 * Devanagari (Hindi) and Gurmukhi (Punjabi) → Latin, in the Hunterian system — India's official
 * one, and the one without diacritics, which matters here because the reader is trying to sing
 * along rather than study phonology.
 *
 * These two are **abugidas**, not alphabets, and that is the whole difficulty: a consonant letter
 * already carries an inherent `a`, so क alone is "ka", not "k". Three things can happen to that
 * inherent vowel and a plain lookup table handles none of them:
 *
 *  - a MATRA (dependent vowel sign) replaces it — कि is "ki", not "kai".
 *  - a VIRAMA (्) deletes it — क् is "k", which is how consonant clusters are written.
 *  - nothing follows, and it stays.
 *
 * So the transliteration is a small state machine over the string, not a map.
 */
internal object IndicRomanizer {
    private const val DEVANAGARI_VIRAMA = '\u094D'
    private const val GURMUKHI_VIRAMA = '\u0A4D'
    private const val DEVANAGARI_NUKTA = '\u093C'
    private const val GURMUKHI_NUKTA = '\u0A3C'

    private val DEVANAGARI_CONSONANTS: Map<Char, String> =
        mapOf(
            'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
            'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
            'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
            'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
            'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
            'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v", 'श' to "sh",
            'ष' to "sh", 'स' to "s", 'ह' to "h", 'ळ' to "l",
        )

    // Perso-Arabic sounds in Hindi — very common in film lyrics — are written as a base consonant
    // plus a NUKTA (U+093C), i.e. TWO code units. They cannot be Char keys: 'क़' is not a character
    // literal, it is क followed by a combining mark, and Kotlin rejects it outright. So the nukta
    // is handled the way matras and viramas are, as a sign that modifies the consonant already
    // read.
    private val DEVANAGARI_NUKTA_FORMS: Map<Char, String> =
        mapOf(
            'क' to "q", 'ख' to "kh", 'ग' to "gh", 'ज' to "z",
            'ड' to "r", 'ढ' to "rh", 'फ' to "f",
        )

    private val DEVANAGARI_INDEPENDENT_VOWELS: Map<Char, String> =
        mapOf(
            'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u",
            'ऊ' to "oo", 'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o",
            'औ' to "au",
        )

    private val DEVANAGARI_MATRAS: Map<Char, String> =
        mapOf(
            'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
            'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au",
        )

    // Anusvara/candrabindu nasalise the preceding vowel; visarga is an h. All three attach to the
    // syllable already emitted, so they are appended rather than treated as letters.
    private val DEVANAGARI_SIGNS: Map<Char, String> =
        mapOf('ं' to "n", 'ँ' to "n", 'ः' to "h")

    private val GURMUKHI_CONSONANTS: Map<Char, String> =
        mapOf(
            'ਕ' to "k", 'ਖ' to "kh", 'ਗ' to "g", 'ਘ' to "gh", 'ਙ' to "n",
            'ਚ' to "ch", 'ਛ' to "chh", 'ਜ' to "j", 'ਝ' to "jh", 'ਞ' to "n",
            'ਟ' to "t", 'ਠ' to "th", 'ਡ' to "d", 'ਢ' to "dh", 'ਣ' to "n",
            'ਤ' to "t", 'ਥ' to "th", 'ਦ' to "d", 'ਧ' to "dh", 'ਨ' to "n",
            'ਪ' to "p", 'ਫ' to "ph", 'ਬ' to "b", 'ਭ' to "bh", 'ਮ' to "m",
            'ਯ' to "y", 'ਰ' to "r", 'ਲ' to "l", 'ਵ' to "v",
            'ਸ' to "s", 'ਹ' to "h", 'ੜ' to "r",
        )

    /** Same story as Devanagari's, with Gurmukhi's own nukta at U+0A3C. */
    private val GURMUKHI_NUKTA_FORMS: Map<Char, String> =
        mapOf(
            'ਸ' to "sh", 'ਲ' to "l", 'ਖ' to "kh",
            'ਗ' to "gh", 'ਜ' to "z", 'ਫ' to "f",
        )

    private val GURMUKHI_INDEPENDENT_VOWELS: Map<Char, String> =
        mapOf('ਅ' to "a", 'ਆ' to "aa", 'ਇ' to "i", 'ਈ' to "ee", 'ਉ' to "u", 'ਊ' to "oo", 'ਏ' to "e", 'ਐ' to "ai", 'ਓ' to "o", 'ਔ' to "au")

    private val GURMUKHI_MATRAS: Map<Char, String> =
        mapOf(
            'ਾ' to "aa", 'ਿ' to "i", 'ੀ' to "ee", 'ੁ' to "u", 'ੂ' to "oo",
            'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au",
        )

    private val GURMUKHI_SIGNS: Map<Char, String> =
        mapOf('ਂ' to "n", 'ੰ' to "n", 'ਃ' to "h")

    fun romanizeDevanagari(line: String): String =
        romanize(
            line,
            DEVANAGARI_CONSONANTS,
            DEVANAGARI_INDEPENDENT_VOWELS,
            DEVANAGARI_MATRAS,
            DEVANAGARI_SIGNS,
            DEVANAGARI_VIRAMA,
            DEVANAGARI_NUKTA,
            DEVANAGARI_NUKTA_FORMS,
        )

    fun romanizeGurmukhi(line: String): String =
        romanize(
            line,
            GURMUKHI_CONSONANTS,
            GURMUKHI_INDEPENDENT_VOWELS,
            GURMUKHI_MATRAS,
            GURMUKHI_SIGNS,
            GURMUKHI_VIRAMA,
            GURMUKHI_NUKTA,
            GURMUKHI_NUKTA_FORMS,
        )

    private fun romanize(
        line: String,
        consonants: Map<Char, String>,
        vowels: Map<Char, String>,
        matras: Map<Char, String>,
        signs: Map<Char, String>,
        virama: Char,
        nukta: Char,
        nuktaForms: Map<Char, String>,
    ): String {
        val builder = StringBuilder()
        var index = 0
        while (index < line.length) {
            val char = line[index]
            val consonant = consonants[char]
            if (consonant != null) {
                // The nukta is consumed FIRST, before the inherent vowel is decided: it changes
                // which consonant this is (ज -> z, not j), and the vowel logic below then applies
                // to whatever it became. Reading them in the other order writes the wrong
                // consonant and then correctly vowels it.
                var cursor = index
                if (line.getOrNull(cursor + 1) == nukta) {
                    builder.append(nuktaForms[char] ?: consonant)
                    cursor++
                } else {
                    builder.append(consonant)
                }
                index = cursor
                // Look ONE character ahead to decide what happened to the inherent vowel. The
                // order matters: a virama kills it, a matra replaces it, and only if neither is
                // there does the inherent `a` get written.
                val next = line.getOrNull(index + 1)
                when {
                    next == virama -> index++
                    next != null && matras.containsKey(next) -> {
                        builder.append(matras.getValue(next))
                        index++
                    }
                    else -> builder.append("a")
                }
                index++
                continue
            }
            val vowel = vowels[char]
            if (vowel != null) {
                builder.append(vowel)
                index++
                continue
            }
            val sign = signs[char]
            if (sign != null) {
                builder.append(sign)
                index++
                continue
            }
            // Stray matra with no consonant before it, punctuation, Latin text — passed through.
            builder.append(char)
            index++
        }
        return builder.toString()
    }
}
