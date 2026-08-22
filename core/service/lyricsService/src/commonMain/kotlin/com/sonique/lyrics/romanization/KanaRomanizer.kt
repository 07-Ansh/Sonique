package com.sonique.lyrics.romanization

/**
 * Kana → Hepburn romaji.
 *
 * Deliberately in commonMain even though only the JVM targets can currently reach it: nothing here
 * needs a library. What DOES need one is turning kanji into kana in the first place, and that is
 * the only part behind an expect/actual.
 *
 * Three things make this more than a character map:
 *
 *  - **Youon** — きゃ is one sound "kya", written as two characters. Two-character sequences are
 *    therefore matched BEFORE single ones.
 *  - **Sokuon** — っ/ッ has no sound of its own; it doubles the next consonant (きって → "kitte").
 *    That needs look-ahead, which the issue flags as the trap it is.
 *  - **Chouon** — ー lengthens the previous vowel (ラーメン → "raamen").
 */
internal object KanaRomanizer {
    private val YOUON: Map<String, String> =
        mapOf(
            "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
            "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
            "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
            "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
            "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
            "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
            "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
            "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
            "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
            "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
            "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
            "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
            "うぃ" to "wi", "うぇ" to "we", "てぃ" to "ti", "でぃ" to "di",
            "とぅ" to "tu", "どぅ" to "du", "ゔぁ" to "va", "ゔぃ" to "vi",
            "ゔぇ" to "ve", "ゔぉ" to "vo", "しぇ" to "she", "ちぇ" to "che",
            "じぇ" to "je",
        )

    private val KANA: Map<Char, String> =
        mapOf(
            'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
            'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
            'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
            'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
            'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
            'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
            'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
            'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
            'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
            'わ' to "wa", 'を' to "o", 'ん' to "n",
            'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
            'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
            'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
            'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
            'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
            'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
            'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゔ' to "vu",
        )

    private const val KATAKANA_OFFSET = 0x60
    private const val SOKUON_HIRAGANA = 'っ'
    private const val CHOUON = 'ー'

    /** Katakana and hiragana are the same 96 sounds 0x60 apart, so one table serves both. */
    private fun toHiragana(text: String): String =
        buildString {
            text.forEach { char ->
                if (char in 'ァ'..'ヶ') append((char.code - KATAKANA_OFFSET).toChar()) else append(char)
            }
        }

    fun romanize(text: String): String {
        val kana = toHiragana(text)
        val builder = StringBuilder()
        var index = 0
        while (index < kana.length) {
            val char = kana[index]

            if (char == SOKUON_HIRAGANA) {
                // Doubles the FOLLOWING consonant, so it emits nothing itself and instead peeks at
                // what the next syllable will romanize to. Reading it as its own sound is the
                // classic mistake — きって would come out "kitsute".
                val nextRomaji = peekNext(kana, index + 1)
                if (nextRomaji != null && nextRomaji.isNotEmpty() && nextRomaji[0] !in "aiueo") {
                    builder.append(nextRomaji[0])
                }
                index++
                continue
            }

            if (char == CHOUON) {
                // Repeats the vowel already written. If nothing was written yet there is nothing to
                // lengthen, and the mark is dropped rather than printed.
                builder.lastOrNull()?.let { if (it in "aiueo") builder.append(it) }
                index++
                continue
            }

            val pair = if (index + 1 < kana.length) kana.substring(index, index + 2) else null
            val youon = pair?.let { YOUON[it] }
            if (youon != null) {
                builder.append(youon)
                index += 2
                continue
            }

            val single = KANA[char]
            if (single != null) {
                builder.append(single)
                index++
                continue
            }

            builder.append(char)
            index++
        }
        return builder.toString()
    }

    /** What the syllable starting at [index] romanizes to — youon first, exactly as the loop does. */
    private fun peekNext(
        kana: String,
        index: Int,
    ): String? {
        if (index >= kana.length) return null
        val pair = if (index + 1 < kana.length) kana.substring(index, index + 2) else null
        return pair?.let { YOUON[it] } ?: KANA[kana[index]]
    }
}
