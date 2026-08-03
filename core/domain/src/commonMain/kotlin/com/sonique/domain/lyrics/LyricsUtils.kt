package com.sonique.domain.lyrics

import com.sonique.domain.data.model.metadata.LyricsEntry
import com.sonique.domain.data.model.metadata.WordTimestamp

val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.*)".toRegex()
val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

private val RICH_SYNC_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
private val RICH_SYNC_WORD_REGEX = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>([^<]+)".toRegex()
private val PAXSENIX_AGENT_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](v\\d+):\\s*(.*)".toRegex()
private val PAXSENIX_BG_LINE_REGEX = "^\\[bg:\\s*(.*)\\]$".toRegex()
private val AGENT_REGEX = "\\{agent:([^}]+)\\}".toRegex()
private val BACKGROUND_REGEX = "^\\{bg\\}".toRegex()

private const val MINUTE_IN_MILLIS = 60_000L
private const val SECOND_IN_MILLIS = 1_000L

object LyricsUtils {
    fun cleanTitleForSearch(title: String): String {
        return title.replace(Regex("\\s*[(\\[].*?[)\\]]"), "").trim()
    }

    fun filterLyricsCreditLines(lyrics: String): String {
        return lyrics.lines().filter { line ->
            var textContent = line.trim()
            var stripping = true
            while (stripping) {
                val prevLength = textContent.length
                textContent = textContent
                    .replaceFirst(Regex("^\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "")
                    .replaceFirst(Regex("^\\{agent:[^}]+\\}"), "")
                    .replaceFirst(Regex("^\\{bg\\}"), "")
                    .replaceFirst(Regex("^\\[bg:.*\\]"), "")
                    .replaceFirst(Regex("^v\\d+:"), "")
                    .trim()
                stripping = textContent.length < prevLength
            }

            val lowerText = textContent.lowercase()
            val isCredit = lowerText.startsWith("synced by") ||
                    lowerText.startsWith("lyrics by") ||
                    lowerText.startsWith("music by") ||
                    lowerText.startsWith("arranged by") ||
                    (lowerText.startsWith("[") && lowerText.endsWith("]") && lowerText.length < 40 && lowerText.contains("synced by"))
            
            !isCredit
        }.joinToString("\n")
    }

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        if (lyrics.isBlank()) return emptyList()

        val unescapedLyrics = if (lyrics.contains('\\') || lyrics.startsWith("\"")) {
            val s = lyrics.trim().removePrefix("\"").removeSuffix("\"")
            val sb = StringBuilder(s.length)
            var j = 0
            while (j < s.length) {
                val c = s[j]
                if (c == '\\' && j + 1 < s.length) {
                    when (val next = s[j + 1]) {
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(c).append(next)
                    }
                    j += 2
                } else {
                    sb.append(c)
                    j++
                }
            }
            sb.toString()
        } else lyrics

        val decodedLyrics = decodeHtmlEntities(unescapedLyrics)
        val lines = decodedLyrics.lines()
            .filter { 
                it.isNotBlank() || it.trim().startsWith("[") || it.trim().startsWith("<")
            }
            .filter { !it.trim().startsWith("[offset:") }

        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) &&
            RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }

        return if (isRichSync) {
            parseRichSyncLyrics(lines)
        } else {
            parseStandardLyrics(lines)
        }
    }

    private fun decodeHtmlEntities(input: String): String {
        return input.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }

    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        var lastNonBgAgent: String? = null

        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            val bgMatch = PAXSENIX_BG_LINE_REGEX.find(trimmedLine)
            if (bgMatch != null) {
                val content = bgMatch.groupValues[1]
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"), "").trim()
                val lineTimeMs = wordTimings?.firstOrNull()?.startTime?.let { (it * 1000).toLong() } ?: 0L
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = lastNonBgAgent ?: "bg", isBackground = true))
                return@forEachIndexed
            }
            
            val agentMatch = PAXSENIX_AGENT_LINE_REGEX.find(trimmedLine)
            if (agentMatch != null) {
                val minutes = agentMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = agentMatch.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = agentMatch.groupValues[3].toLongOrNull() ?: 0L
                val agent = agentMatch.groupValues[4]
                val content = agentMatch.groupValues[5]
                val millisPart = if (agentMatch.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * MINUTE_IN_MILLIS + seconds * SECOND_IN_MILLIS + millisPart
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"), "").trim()
                if (!agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = agent, isBackground = false))
                return@forEachIndexed
            }
            
            val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(trimmedLine)
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L
                val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * MINUTE_IN_MILLIS + seconds * SECOND_IN_MILLIS + millisPart
                var content = matchResult.groupValues[4].trimStart()

                val oldAgentMatch = AGENT_REGEX.find(content)
                val agent = oldAgentMatch?.groupValues?.get(1)
                if (oldAgentMatch != null) {
                    content = content.replaceFirst(AGENT_REGEX, "")
                }

                val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
                if (isBackground) {
                    content = content.replaceFirst(BACKGROUND_REGEX, "")
                }

                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"), "").trim()
                if (!isBackground && !agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = if (isBackground) lastNonBgAgent ?: "bg" else agent, isBackground = isBackground))
            }
        }

        return result.sorted()
    }

    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<WordTimestamp>? {
        val wordMatches = RICH_SYNC_WORD_REGEX.findAll(content).toList()
        if (wordMatches.isEmpty()) return null

        val lastMatchEnd = wordMatches.last().range.last
        val trailingContent = content.substring(lastMatchEnd + 1).trim()
        val angleTrailingMatch = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>".toRegex().find(trailingContent)
        val squareTrailingMatch = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\]".toRegex().find(trailingContent)
        val trailingTimeMatch = angleTrailingMatch ?: squareTrailingMatch
        val trailingEndTime: Double? = if (trailingTimeMatch != null && trailingContent.substring(trailingTimeMatch.range.last + 1).removeSuffix("]").isBlank()) {
            val tMin = trailingTimeMatch.groupValues[1].toLongOrNull() ?: 0L
            val tSec = trailingTimeMatch.groupValues[2].toLongOrNull() ?: 0L
            val tFrac = trailingTimeMatch.groupValues[3].toLongOrNull() ?: 0L
            val tFracPart = if (trailingTimeMatch.groupValues[3].length == 3) tFrac / 1000.0 else tFrac / 100.0
            tMin * 60.0 + tSec + tFracPart
        } else null

        val wordTimings = mutableListOf<WordTimestamp>()
        wordMatches.forEachIndexed { index, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].toLongOrNull() ?: 0L
            val fractionPart = if (match.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            val startTimeSeconds = minutes * 60.0 + seconds + fractionPart
            val rawText = match.groupValues[4]
            val hasTrailingSpace = rawText.endsWith(" ")
            val words = rawText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            val nextTimestamp: Double
            val nextLineTime: Double?
            if (index < wordMatches.size - 1) {
                val nextMatch = wordMatches[index + 1]
                val nextMin = nextMatch.groupValues[1].toLongOrNull() ?: 0L
                val nextSec = nextMatch.groupValues[2].toLongOrNull() ?: 0L
                val nextFrac = nextMatch.groupValues[3].toLongOrNull() ?: 0L
                val nextFracPart = if (nextMatch.groupValues[3].length == 3) nextFrac / 1000.0 else nextFrac / 100.0
                nextTimestamp = nextMin * 60.0 + nextSec + nextFracPart
                nextLineTime = null
            } else {
                nextLineTime = getNextLineStartTime(currentIndex, allLines)
                nextTimestamp = trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
            }

            words.forEachIndexed { wordIndex, word ->
                val isLastWordInGroup = wordIndex == words.lastIndex
                val isLastWordOverall = index == wordMatches.lastIndex && isLastWordInGroup
                val wordStartTime = startTimeSeconds + (nextTimestamp - startTimeSeconds) * wordIndex / words.size
                val wordEndTime = if (!isLastWordInGroup) {
                    startTimeSeconds + (nextTimestamp - startTimeSeconds) * (wordIndex + 1) / words.size
                } else if (!isLastWordOverall) {
                    nextTimestamp
                } else {
                    trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
                }

                val wordHasTrailingSpace = if (!isLastWordInGroup) {
                    true
                } else if (!isLastWordOverall) {
                    hasTrailingSpace
                } else {
                    val textAfterMatch = if (trailingTimeMatch != null) {
                        trailingContent.substring(0, trailingTimeMatch.range.first)
                    } else {
                        trailingContent
                    }
                    textAfterMatch.isNotBlank()
                }

                if (word.isNotBlank()) {
                    wordTimings.add(WordTimestamp(word, wordStartTime, wordEndTime, wordHasTrailingSpace))
                }
            }
        }
        return if (wordTimings.isNotEmpty()) wordTimings else null
    }

    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Double? {
        if (currentIndex + 1 >= allLines.size) return null
        val nextLine = allLines[currentIndex + 1].trim()
        
        val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(nextLine)
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLongOrNull() ?: return null
            val seconds = matchResult.groupValues[2].toLongOrNull() ?: return null
            val fraction = matchResult.groupValues[3].toLongOrNull() ?: 0L
            val fractionPart = if (matchResult.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }
        
        val bgMatch = PAXSENIX_BG_LINE_REGEX.matchEntire(nextLine)
        if (bgMatch != null) {
            val content = bgMatch.groupValues[1]
            val wordMatch = RICH_SYNC_WORD_REGEX.find(content) ?: return null
            val minutes = wordMatch.groupValues[1].toLongOrNull() ?: return null
            val seconds = wordMatch.groupValues[2].toLongOrNull() ?: return null
            val fraction = wordMatch.groupValues[3].toLongOrNull() ?: 0L
            val fractionPart = if (wordMatch.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }
        return null
    }

    private fun parseStandardLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.trim().startsWith("<") || !line.trim().endsWith(">")) {
                val entries = parseLine(line, null)
                if (entries != null) {
                    val wordTimestamps = if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (nextLine.trim().startsWith("<") && nextLine.trim().endsWith(">")) {
                            parseWordTimestamps(nextLine.trim().removeSurrounding("<", ">"))
                        } else null
                    } else null

                    if (wordTimestamps != null) {
                        result.addAll(entries.map { entry ->
                            LyricsEntry(entry.time, entry.text, wordTimestamps, agent = entry.agent, isBackground = entry.isBackground)
                        })
                    } else {
                        result.addAll(entries)
                    }
                }
            }
            i++
        }
        return result.sorted()
    }

    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size >= 3) {
                    val text = parts.dropLast(2).joinToString(":")
                    val startTime = parts[parts.size - 2].toDoubleOrNull() ?: 0.0
                    val endTime = parts[parts.size - 1].toDoubleOrNull() ?: 0.0
                    val isLast = wordData == data.split("|").last()
                    WordTimestamp(
                        text = text,
                        startTime = startTime,
                        endTime = endTime,
                        hasTrailingSpace = !isLast
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLine(line: String, words: List<WordTimestamp>? = null): List<LyricsEntry>? {
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        var text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) {
            text = text.replaceFirst(AGENT_REGEX, "")
        }

        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) {
            text = text.replaceFirst(BACKGROUND_REGEX, "")
        }

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * MINUTE_IN_MILLIS + sec * SECOND_IN_MILLIS + mil
                LyricsEntry(time, text, words, agent = agent, isBackground = isBackground)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        val threshold = 100L
        for (index in lines.indices) {
            if (lines[index].time >= position + threshold) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    fun findActiveLineIndices(
        lines: List<LyricsEntry>,
        position: Long,
    ): Set<Int> {
        val active = mutableSetOf<Int>()
        val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

        for (index in lines.indices) {
            val line = lines[index]
            if (line.time > position) break

            val lineEndMs: Long = if (!line.words.isNullOrEmpty()) {
                (line.words.last().endTime * 1000).toLong()
            } else {
                if (index + 1 < lines.size) lines[index + 1].time else Long.MAX_VALUE
            }

            if (position <= lineEndMs) {
                active.add(index)
            }
        }

        if (!hasWordTimings && active.size > 1) {
            val mainActive = active.filter { lines[it].isBackground == false }
            if (mainActive.size > 1) {
                val maxTime = mainActive.maxOf { lines[it].time }
                active.removeAll { it in mainActive && lines[it].time < maxTime }
            }
        }

        return active
    }

    fun isHindi(text: String): Boolean {
        return text.any { char -> char in '\u0900'..'\u097F' }
    }

    fun isPunjabi(text: String): Boolean {
        return text.any { char -> char in '\u0A00'..'\u0A7F' }
    }

    suspend fun romanizeHindi(text: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        com.sonique.domain.lyrics.utils.HindiTransliterator.transliterateLine(text)
    }

    suspend fun romanizePunjabi(text: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        // Punjabi Gurmukhi mapping algorithm matching Metrolist
        val gurmukhiMap = mapOf(
            'ੳ' to "o", 'ਅ' to "a", 'ੲ' to "e", 'ਸ' to "s", 'ਹ' to "h",
            'ਕ' to "k", 'ਖ' to "kh", 'ਗ' to "g", 'ਘ' to "gh", 'ਙ' to "ng",
            'ਚ' to "ch", 'ਛ' to "chh", 'ਜ' to "j", 'ਝ' to "jh", 'ਞ' to "ny",
            'ਟ' to "t", 'ਠ' to "th", 'ਡ' to "d", 'ਢ' to "dh", 'ਣ' to "n",
            'ਤ' to "t", 'ਥ' to "th", 'ਦ' to "d", 'ਧ' to "dh", 'ਨ' to "n",
            'ਪ' to "p", 'ਫ' to "ph", 'ਬ' to "b", 'ਭ' to "bh", 'ਮ' to "m",
            'ਯ' to "y", 'ਰ' to "r", 'ਲ' to "l", 'ਵ' to "v", 'ੜ' to "r",
            'ਸ਼' to "sh", 'ਖ਼' to "kh", 'ਗ਼' to "g", 'ਜ਼' to "z", 'ਫ਼' to "f",
            'ਾ' to "aa", 'ਿ' to "i", 'ੀ' to "ee", 'ੁ' to "u", 'ੂ' to "oo",
            'ੇ' to "e", 'ੈ' to "ai", 'ੋ' to "o", 'ੌ' to "au", 'ਂ' to "n", 'ੰ' to "n"
        )
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (char == '\u0A71') { // Adhak / gemination
                if (i + 1 < text.length) {
                    val nextCharStr = text[i + 1]
                    val nextMapped = gurmukhiMap[nextCharStr] ?: ""
                    if (nextMapped.isNotEmpty()) {
                        sb.append(nextMapped[0])
                    }
                }
                i++
                continue
            }
            val mapped = gurmukhiMap[char]
            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.append(char)
            }
            i++
        }
        sb.toString()
    }
}

