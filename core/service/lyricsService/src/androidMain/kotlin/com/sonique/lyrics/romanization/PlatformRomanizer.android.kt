package com.sonique.lyrics.romanization

import com.atilika.kuromoji.ipadic.Tokenizer
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * Japanese and Chinese romanization on Android.
 *
 * No longer identical to the jvm actual: desktop keeps building the plain classpath `Tokenizer()`,
 * while here the ipadic dictionary is not in the APK at all — it arrives by download (see
 * [KuromojiDictionary]) and the analyzer can only be built once every file is on disk. Until then
 * [japanese] answers null, which the pipeline already treats as "show nothing extra".
 */
internal actual object PlatformRomanizer {
    // Loading the ipadic dictionary costs real time and memory, so it happens once, on first use —
    // never at startup, since most listeners never play a Japanese track. Only a SUCCESSFUL build
    // is cached: a failed attempt (mid-download, corrupted file) stays null and the next Japanese
    // line simply tries again, which is also how the analyzer comes alive right after a download
    // finishes, with no restart.
    @Volatile
    private var tokenizer: Tokenizer? = null

    private fun tokenizerOrNull(): Tokenizer? {
        tokenizer?.let { return it }
        val directory = RomanizationDictionaryPack.dictionaryDirectory ?: return null
        if (!KuromojiDictionary.isReady(directory)) return null
        return synchronized(this) {
            tokenizer ?: runCatching { KuromojiDictionary.buildTokenizer(directory) }
                .getOrNull()
                ?.also { tokenizer = it }
        }
    }

    actual fun japanese(line: String): String? {
        val analyzer = tokenizerOrNull() ?: return null
        return runCatching {
            val reading =
                buildString {
                    analyzer.tokenize(line).forEach { token ->
                        // getReading() is katakana, and "*" is kuromoji's way of saying it does not
                        // know the word — Latin text, numbers, or a name outside the dictionary.
                        // Falling back to the surface form keeps those readable instead of printing
                        // an asterisk into the lyrics.
                        val katakana = token.reading
                        if (katakana.isNullOrEmpty() || katakana == "*") append(token.surface) else append(katakana)
                    }
                }
            KanaRomanizer.romanize(reading)
        }.getOrNull()
    }

    actual fun chinese(line: String): String? =
        runCatching {
            // WITH_TONE_MARK is the reason for the other two settings, not a free choice: pinyin4j
            // throws BadHanyuPinyinOutputFormatCombination if tone marks are asked for while
            // vCharType is anything but WITH_U_UNICODE. That exception is the single most common
            // way this library is mis-wired.
            val format =
                HanyuPinyinOutputFormat().apply {
                    caseType = HanyuPinyinCaseType.LOWERCASE
                    toneType = HanyuPinyinToneType.WITH_TONE_MARK
                    vCharType = HanyuPinyinVCharType.WITH_U_UNICODE
                }
            buildString {
                line.forEach { char ->
                    // isHan(), not the library's own predicate: the same test the script detector
                    // upstream already used, so the two can never disagree about what is Chinese.
                    if (char.isHan()) {
                        // A heteronym returns several readings and the array is NOT ranked, so the
                        // first is a guess. Disambiguating properly needs the surrounding word,
                        // which per-character lookup cannot see — the limitation the issue calls
                        // out, and one no character-level library escapes.
                        val readings = PinyinHelper.toHanyuPinyinStringArray(char, format)
                        if (readings.isNullOrEmpty()) {
                            append(char)
                        } else {
                            // Syllables are separated, the way pinyin is written. Without it
                            // 你好 reads as one word rather than two.
                            if (isNotEmpty() && last() != ' ') append(' ')
                            append(readings.first())
                            append(' ')
                        }
                    } else {
                        append(char)
                    }
                }
            }.replace(Regex(" {2,}"), " ").trim()
        }.getOrNull()
}
