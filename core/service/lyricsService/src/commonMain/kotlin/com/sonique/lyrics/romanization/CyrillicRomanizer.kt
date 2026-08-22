package com.sonique.lyrics.romanization

import com.sonique.domain.data.model.lyrics.RomanizationLanguage

/**
 * Cyrillic → Latin for the seven Cyrillic languages this feature supports.
 *
 * They share one alphabet and romanize it differently, so each gets its own table. The shared
 * letters are factored into [BASE] and every language declares only what it does DIFFERENTLY —
 * which is also the honest way to read this file: the overrides are exactly the disagreements.
 *
 * Standards followed, one per language, all of them the national or de-facto official system:
 *  - Russian: BGN/PCGN, the system used on maps and passports.
 *  - Ukrainian: the 2010 national system (и → y, г → h — the two that most often come out wrong).
 *  - Serbian: Gaj's Latin, which is not a transliteration at all but Serbia's OTHER alphabet, so
 *    it is exactly one-to-one and never ambiguous.
 *  - Bulgarian: the Streamlined System, official since 2009 (ъ → a, х → h).
 *  - Belarusian: the 2007 national system (ў → u with a breve, г → h).
 *  - Kyrgyz: BGN/PCGN.
 *  - Macedonian: the national system, which shares Serbian's letters but not its ѓ/ќ.
 */
internal object CyrillicRomanizer {
    private val BASE: Map<Char, String> =
        mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
            'е' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y",
            'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o",
            'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
            'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh",
            'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e",
            'ю' to "yu", 'я' to "ya", 'ё' to "yo",
        )

    private val OVERRIDES: Map<RomanizationLanguage, Map<Char, String>> =
        mapOf(
            RomanizationLanguage.RUSSIAN to emptyMap(),
            RomanizationLanguage.UKRAINIAN to
                mapOf(
                    'г' to "h", 'ґ' to "g", 'и' to "y", 'і' to "i", 'ї' to "yi",
                    'є' to "ye", 'х' to "kh", 'щ' to "shch", 'й' to "y",
                ),
            RomanizationLanguage.SERBIAN to
                mapOf(
                    'ђ' to "đ", 'ј' to "j", 'љ' to "lj", 'њ' to "nj", 'ћ' to "ć",
                    'џ' to "dž", 'ж' to "ž", 'ч' to "č", 'ш' to "š", 'х' to "h",
                    'ц' to "c",
                ),
            RomanizationLanguage.BULGARIAN to
                mapOf(
                    'х' to "h", 'ъ' to "a", 'щ' to "sht", 'ь' to "y",
                ),
            RomanizationLanguage.BELARUSIAN to
                mapOf(
                    'г' to "h", 'ў' to "ŭ", 'і' to "i", 'х' to "kh", 'ч' to "ch",
                    'ж' to "zh", 'э' to "e",
                ),
            RomanizationLanguage.KYRGYZ to
                mapOf(
                    'ң' to "ng", 'ө' to "ö", 'ү' to "ü", 'х' to "kh",
                ),
            RomanizationLanguage.MACEDONIAN to
                mapOf(
                    'ѓ' to "gj", 'ќ' to "kj", 'ѕ' to "dz", 'ј' to "j", 'љ' to "lj",
                    'њ' to "nj", 'џ' to "dž", 'ж' to "ž", 'ч' to "č", 'ш' to "š",
                    'х' to "h", 'ц' to "c",
                ),
        )

    // Russian is the only one of the seven where a letter's romanization depends on WHERE it sits.
    // е, ё, ю, я take a leading y after a vowel, after the two signs, and at the start of a word;
    // elsewhere е is a bare e. Ignoring this is what turns Дмитриев into "Dmitriev" instead of
    // "Dmitriyev".
    private val RUSSIAN_INITIAL_FORMS: Map<Char, String> =
        mapOf('е' to "ye", 'ё' to "ye", 'ю' to "yu", 'я' to "ya")

    private const val RUSSIAN_VOWELS = "аеёиоуыэюя"

    fun romanize(
        line: String,
        language: RomanizationLanguage,
    ): String {
        val table = BASE + (OVERRIDES[language] ?: emptyMap())
        val builder = StringBuilder()
        line.forEachIndexed { index, char ->
            val lower = char.lowercaseChar()
            val mapped =
                if (language == RomanizationLanguage.RUSSIAN && lower in RUSSIAN_INITIAL_FORMS) {
                    val previous = line.getOrNull(index - 1)?.lowercaseChar()
                    val afterVowelOrSign = previous != null && (previous in RUSSIAN_VOWELS || previous == 'ь' || previous == 'ъ')
                    val wordInitial = previous == null || !previous.isCyrillic()
                    if (wordInitial || afterVowelOrSign) RUSSIAN_INITIAL_FORMS.getValue(lower) else table[lower]
                } else {
                    table[lower]
                }
            if (mapped == null) {
                builder.append(char)
            } else {
                builder.append(if (char.isUpperCase()) mapped.replaceFirstChar { it.uppercaseChar() } else mapped)
            }
        }
        return builder.toString()
    }
}
