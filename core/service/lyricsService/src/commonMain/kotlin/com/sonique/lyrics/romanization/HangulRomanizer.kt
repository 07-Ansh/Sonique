package com.sonique.lyrics.romanization

/**
 * Revised Romanization of Korean, the South Korean government standard since 2000.
 *
 * Hangul needs no dictionary: a syllable block is composed ARITHMETICALLY from three slots, so it
 * decomposes the same way. Every one of the 11 172 blocks is
 * `0xAC00 + (initial * 21 + medial) * 28 + final`, which is why the three tables below — 19, 21
 * and 28 entries — cover the entire script exactly.
 *
 * What it does NOT do is the full set of assimilation rules. RR specifies how a final consonant
 * and the next initial change EACH OTHER, and the common cases are handled in [applyLiaison];
 * the rarer ones (palatalisation before 이, tensing after obstruents) are left alone. They change
 * a consonant here and there, not legibility, and getting them all right needs the morpheme
 * boundaries that only a Korean analyzer knows.
 */
internal object HangulRomanizer {
    private const val SYLLABLE_BASE = 0xAC00
    private const val MEDIAL_COUNT = 21
    private const val FINAL_COUNT = 28

    private val INITIALS =
        listOf(
            "g", "kk", "n", "d", "tt", "r", "m", "b", "pp",
            "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h",
        )

    private val MEDIALS =
        listOf(
            "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o",
            "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu",
            "eu", "ui", "i",
        )

    // Index 0 is "no final consonant". The romanized form here is the one used when the syllable
    // is NOT followed by a vowel — 밥 is "bap" even though ㅂ is "b" as an initial, because a
    // Korean final stop is unreleased.
    private val FINALS =
        listOf(
            "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m",
            "l", "l", "l", "p", "l", "m", "p", "p", "t", "t",
            "ng", "t", "t", "k", "t", "p", "t",
        )

    // The SAME final consonant, but as it sounds when it moves onto the next syllable's empty
    // initial slot. This is the liaison: 한국어 is "hanguk-eo" written apart, but "hangugeo" as one
    // word, because the ㄱ becomes an initial g rather than a final k.
    private val FINALS_AS_INITIAL =
        listOf(
            "", "g", "kk", "ks", "n", "nj", "nh", "d", "l", "lg", "lm",
            "lb", "ls", "lt", "lp", "lh", "m", "b", "bs", "s", "ss",
            "ng", "j", "ch", "k", "t", "p", "h",
        )

    private data class Jamo(
        val initial: Int,
        val medial: Int,
        val final: Int,
    )

    private fun decompose(char: Char): Jamo? {
        val offset = char.code - SYLLABLE_BASE
        if (offset < 0 || offset >= MEDIAL_COUNT * FINAL_COUNT * INITIALS.size) return null
        return Jamo(
            initial = offset / (MEDIAL_COUNT * FINAL_COUNT),
            medial = (offset % (MEDIAL_COUNT * FINAL_COUNT)) / FINAL_COUNT,
            final = offset % FINAL_COUNT,
        )
    }

    fun romanize(line: String): String {
        val builder = StringBuilder()
        var index = 0
        while (index < line.length) {
            val jamo = decompose(line[index])
            if (jamo == null) {
                builder.append(line[index])
                index++
                continue
            }
            val next = line.getOrNull(index + 1)?.let { decompose(it) }
            builder.append(INITIALS[jamo.initial])
            builder.append(MEDIALS[jamo.medial])
            builder.append(applyLiaison(jamo.final, next))
            index++
        }
        return builder.toString()
    }

    /**
     * How this syllable's final consonant is written, given what follows it.
     *
     * Three cases, in order:
     *  - nothing follows, or a non-Hangul character does — the plain final form.
     *  - the next syllable starts with ㅇ (initial index 11), which carries no sound of its own —
     *    the final moves across and is written as an initial instead. This is why the empty string
     *    is returned here and the consonant appears at the start of the NEXT syllable's output.
     *  - anything else — assimilation, where both consonants can change.
     */
    private fun applyLiaison(
        final: Int,
        next: Jamo?,
    ): String {
        if (final == 0) return ""
        if (next == null) return FINALS[final]
        if (next.initial == SILENT_INITIAL) return FINALS_AS_INITIAL[final]
        return ASSIMILATIONS[final to next.initial] ?: FINALS[final]
    }

    private const val SILENT_INITIAL = 11

    // The assimilations RR actually spells out, keyed by (final index, next initial index). Only
    // the final's spelling is changed here; the next initial keeps its own table entry, which is
    // correct for every pair listed.
    private val ASSIMILATIONS: Map<Pair<Int, Int>, String> =
        buildMap {
            // ㄱ/ㄲ/ㅋ before ㄴ or ㅁ -> ng (학년 hangnyeon, 국물 gungmul)
            for (final in listOf(1, 2, 24)) {
                put(final to 2, "ng")
                put(final to 6, "ng")
            }
            // ㅂ/ㅍ before ㄴ or ㅁ -> m (입니다 imnida)
            for (final in listOf(17, 26)) {
                put(final to 2, "m")
                put(final to 6, "m")
            }
            // ㄷ/ㅅ/ㅆ/ㅈ/ㅊ/ㅌ/ㅎ before ㄴ or ㅁ -> n (닫는 danneun)
            for (final in listOf(7, 19, 20, 22, 23, 25, 27)) {
                put(final to 2, "n")
                put(final to 6, "n")
            }
            // ㄴ before ㄹ -> l (신라 Silla); ㄹ before ㄴ -> l (설날 seollal)
            put(4 to 5, "l")
            put(8 to 2, "l")
        }
}
