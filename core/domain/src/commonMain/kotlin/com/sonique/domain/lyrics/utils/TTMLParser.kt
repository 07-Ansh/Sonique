package com.sonique.domain.lyrics.utils

import kotlin.text.RegexOption

object TTMLParser {

    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList()
    )
    
    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean = true
    )
    
    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean
    )

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        try {
            // Find global offset if present
            var globalOffset = 0.0
            val audioRegex = Regex("""<audio[^>]*lyricOffset=["']([^"']+)["']""")
            audioRegex.find(ttml)?.let { match ->
                globalOffset = match.groupValues[1].toDoubleOrNull() ?: 0.0
            }

            // Extract all <p> elements
            // Using a simple stateful parser to handle nested or consecutive tags
            val pRegex = Regex("""<p\b([^>]*)>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
            pRegex.findAll(ttml).forEach { pMatch ->
                val attrsStr = pMatch.groupValues[1]
                val content = pMatch.groupValues[2]

                val attrs = parseAttributes(attrsStr)
                val begin = attrs["begin"] ?: findFirstSpanBegin(content) ?: return@forEach
                val startTime = parseTime(begin) + globalOffset
                
                val agent = attrs["agent"] ?: attrs["ttm:agent"]
                val isPBackground = attrs["role"] == "x-bg" || attrs["ttm:role"] == "x-bg"

                val spanInfos = mutableListOf<SpanInfo>()
                val backgroundLines = mutableListOf<ParsedLine>()

                // Extract all <span> elements
                val spanRegex = Regex("""<span\b([^>]*)>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
                var lastIndex = 0
                spanRegex.findAll(content).forEach { spanMatch ->
                    val spanAttrs = parseAttributes(spanMatch.groupValues[1])
                    val spanText = spanMatch.groupValues[2]
                    
                    val role = spanAttrs["role"] ?: spanAttrs["ttm:role"]
                    val spanBegin = spanAttrs["begin"]
                    val spanEnd = spanAttrs["end"]

                    if (role == "x-bg") {
                        if (isPBackground) {
                            parseWordSpan(spanText, spanBegin, spanEnd, globalOffset, spanInfos)
                        } else {
                            parseBackgroundSpan(spanText, spanBegin, spanEnd, startTime, globalOffset)?.let {
                                backgroundLines.add(it)
                            }
                        }
                    } else if (role != "x-translation" && role != "x-roman") {
                        parseWordSpan(spanText, spanBegin, spanEnd, globalOffset, spanInfos)
                    }
                }

                val words = mergeSpansIntoWords(spanInfos)
                val lineText = if (words.isEmpty()) stripTags(content).trim() else buildLineText(words)

                if (lineText.isNotEmpty()) {
                    val bgLines = if (backgroundLines.isNotEmpty()) {
                        listOf(ParsedLine(
                            text = backgroundLines.joinToString(" ") { it.text },
                            startTime = backgroundLines.minOf { it.startTime },
                            words = backgroundLines.flatMap { it.words },
                            isBackground = true
                        ))
                    } else emptyList()
                    lines.add(ParsedLine(lineText, startTime, words, agent, isPBackground, bgLines))
                } else if (backgroundLines.isNotEmpty()) {
                    lines.add(ParsedLine(
                        text = backgroundLines.joinToString(" ") { it.text },
                        startTime = backgroundLines.minOf { it.startTime },
                        words = backgroundLines.flatMap { it.words },
                        isBackground = true
                    ))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return lines
    }

    private fun parseAttributes(attrsStr: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""\b([a-zA-Z_:][a-zA-Z0-9_.:-]*)\s*=\s*["']([^"']*)["']""")
        regex.findAll(attrsStr).forEach { match ->
            map[match.groupValues[1]] = match.groupValues[2]
        }
        return map
    }

    private fun findFirstSpanBegin(content: String): String? {
        val spanRegex = Regex("""<span\b([^>]*)>""")
        var bestSeconds = Double.POSITIVE_INFINITY
        var best: String? = null
        spanRegex.findAll(content).forEach { match ->
            val attrs = parseAttributes(match.groupValues[1])
            attrs["begin"]?.let { b ->
                val s = parseTime(b)
                if (s < bestSeconds) {
                    bestSeconds = s
                    best = b
                }
            }
        }
        return best
    }

    private fun parseWordSpan(text: String, begin: String?, end: String?, offset: Double, spanInfos: MutableList<SpanInfo>) {
        if (!begin.isNullOrEmpty() && !end.isNullOrEmpty()) {
            val cleanText = stripTags(text)
            val space = cleanText.isNotEmpty() && cleanText.last().isWhitespace()
            spanInfos.add(SpanInfo(cleanText, parseTime(begin) + offset, parseTime(end) + offset, space))
        }
    }

    private fun parseBackgroundSpan(text: String, begin: String?, end: String?, parentStart: Double, offset: Double): ParsedLine? {
        val start = if (!begin.isNullOrEmpty()) parseTime(begin) + offset else parentStart
        val spanInfos = mutableListOf<SpanInfo>()
        
        val spanRegex = Regex("""<span\b([^>]*)>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        val matches = spanRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            val cleanText = stripTags(text).trim()
            return ParsedLine(cleanText, start, emptyList(), isBackground = true)
        }
        
        matches.forEach { match ->
            val attrs = parseAttributes(match.groupValues[1])
            val spanText = match.groupValues[2]
            val role = attrs["role"] ?: attrs["ttm:role"]
            if (role != "x-translation" && role != "x-roman") {
                parseWordSpan(spanText, attrs["begin"], attrs["end"], offset, spanInfos)
            }
        }
        
        val words = mergeSpansIntoWords(spanInfos)
        val lineText = if (words.isEmpty()) stripTags(text).trim() else buildLineText(words)
        return ParsedLine(lineText, start, words, isBackground = true)
    }

    private fun stripTags(xml: String): String {
        return xml.replace(Regex("<[^>]*>"), "")
    }

    private fun buildLineText(words: List<ParsedWord>) = buildString {
        words.forEachIndexed { i, w ->
            append(w.text)
            if (w.hasTrailingSpace && !w.text.endsWith('-') && i < words.lastIndex) append(" ")
        }
    }.trim()

    private fun mergeSpansIntoWords(spanInfos: List<SpanInfo>): List<ParsedWord> {
        if (spanInfos.isEmpty()) return emptyList()
        val words = mutableListOf<ParsedWord>()
        var text = StringBuilder(spanInfos[0].text)
        var start = spanInfos[0].startTime
        var end = spanInfos[0].endTime
        
        for (i in 1 until spanInfos.size) {
            val prev = spanInfos[i - 1]
            val curr = spanInfos[i]
            if (prev.hasTrailingSpace && !prev.text.endsWith('-')) {
                words.add(ParsedWord(text.toString(), start, end, true))
                text = StringBuilder(curr.text)
                start = curr.startTime
                end = curr.endTime
            } else {
                text.append(curr.text)
                end = curr.endTime
            }
        }
        words.add(ParsedWord(text.toString(), start, end, spanInfos.last().hasTrailingSpace))
        return words.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
    }

    fun toLRC(lines: List<ParsedLine>): String {
        val agentMap = mutableMapOf<String, String>()
        lines.forEach { line ->
            line.agent?.lowercase()?.let { raw ->
                if (raw == "v1" || raw == "v2" || raw == "v1000") {
                    agentMap[raw] = raw
                }
            }
        }
        
        var nextNum = 1
        lines.forEach { line ->
            line.agent?.lowercase()?.let { raw ->
                if (!agentMap.containsKey(raw)) {
                    while (nextNum <= 2 && (agentMap.containsKey("v$nextNum") || agentMap.values.contains("v$nextNum"))) {
                        nextNum++
                    }
                    agentMap[raw] = if (nextNum <= 2) "v$nextNum" else "v1"
                }
            }
        }

        if (agentMap.containsKey("v1000") && agentMap.containsKey("v1")) {
            agentMap["v1000"] = "v2"
        }

        val hasBackgroundLine = lines.any { it.isBackground }
        val multi = agentMap.size > 1 ||
                (agentMap.size == 1 && !agentMap.containsKey("v1")) ||
                (hasBackgroundLine && agentMap.size == 1 && agentMap.containsKey("v1"))
        
        val sb = StringBuilder(lines.size * 128)
        var lastBg = false
        lines.forEach { line ->
            val time = formatLrcTime(line.startTime)
            val isBg = line.isBackground
            if (!isBg) lastBg = false
            
            val agentId = agentMap[line.agent?.lowercase()]
            val tag = when {
                isBg -> if (lastBg) "" else "{bg}"
                multi && agentId != null -> "{agent:$agentId}"
                else -> ""
            }
            if (isBg) lastBg = true

            sb.append(time).append(tag).append(line.text).append('\n')
            if (line.words.isNotEmpty()) {
                sb.append('<')
                line.words.forEachIndexed { i, w ->
                    sb.append(w.text).append(':').append(w.startTime).append(':').append(w.endTime)
                    if (i < line.words.lastIndex) sb.append('|')
                }
                sb.append(">\n")
            }
            line.backgroundLines.forEach { bg ->
                val bTag = if (lastBg) "" else "{bg}"
                sb.append(formatLrcTime(bg.startTime)).append(bTag).append(bg.text).append('\n')
                lastBg = true
                if (bg.words.isNotEmpty()) {
                    sb.append('<')
                    bg.words.forEachIndexed { i, w ->
                        sb.append(w.text).append(':').append(w.startTime).append(':').append(w.endTime)
                        if (i < bg.words.lastIndex) sb.append('|')
                    }
                    sb.append(">\n")
                }
            }
        }
        return sb.toString()
    }

    private fun formatLrcTime(time: Double): String {
        val ms = (time * 1000).toLong()
        val m = ms / 60000
        val s = (ms % 60000) / 1000
        val c = (ms % 1000) / 10
        val sb = StringBuilder(10)
        sb.append('[')
        if (m < 10) sb.append('0')
        sb.append(m).append(':')
        if (s < 10) sb.append('0')
        sb.append(s).append('.')
        if (c < 10) sb.append('0')
        sb.append(c).append(']')
        return sb.toString()
    }

    private fun parseTime(time: String): Double {
        val t = time.trim()
        val c1 = t.indexOf(':')
        if (c1 != -1) {
            val c2 = t.lastIndexOf(':')
            return if (c1 == c2) {
                (t.substring(0, c1).toIntOrNull() ?: 0) * 60.0 + (t.substring(c1 + 1).toDoubleOrNull() ?: 0.0)
            } else {
                (t.substring(0, c1).toIntOrNull() ?: 0) * 3600.0 + (t.substring(c1 + 1, c2).toIntOrNull() ?: 0) * 60.0 + (t.substring(c2 + 1).toDoubleOrNull() ?: 0.0)
            }
        }
        if (t.endsWith("ms")) return (t.substring(0, t.length - 2).toDoubleOrNull() ?: 0.0) / 1000.0
        val s = if (t.endsWith("s") || t.endsWith("m") || t.endsWith("h")) t.substring(0, t.length - 1) else t
        val v = s.toDoubleOrNull() ?: 0.0
        return when {
            t.endsWith("m") -> v * 60.0
            t.endsWith("h") -> v * 3600.0
            else -> v
        }
    }
}
