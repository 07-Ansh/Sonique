package com.sonique.app.extension

 
data class WordTiming(
    val text: String,
    val startTimeMs: Long,
)

 
data class ParsedRichSyncLine(
    val words: List<WordTiming>,
    val lineStartTimeMs: Long,
    val lineEndTimeMs: Long,
)

 
fun parseRichSyncWords(
    words: String,
    lineStartTimeMs: String,
    lineEndTimeMs: String,
): ParsedRichSyncLine? {
     
    if (words.isBlank()) {
        println("[parseRichSyncWords] Input is blank")
        return null
    }

    println("[parseRichSyncWords] Input preview: ${words.take(100)}")

     
     
    val timestampRegex = Regex("""<(\d{2}):(\d{2})\.(\d{2})>""")

    val wordTimings = mutableListOf<WordTiming>()

     
    val timestamps = timestampRegex.findAll(words).toList()

    timestamps.forEachIndexed { index, match ->
        val (minutes, seconds, centiseconds) = match.destructured

         
        val timeMs =
            (minutes.toLongOrNull() ?: 0L) * 60000L +
                (seconds.toLongOrNull() ?: 0L) * 1000L +
                (centiseconds.toLongOrNull() ?: 0L) * 10L

         
        val startPos = match.range.last + 1
        val endPos =
            if (index < timestamps.size - 1) {
                timestamps[index + 1].range.first
            } else {
                words.length
            }

         
        val textBetween = words.substring(startPos, endPos).trim()

         
        if (textBetween.isNotBlank()) {
            wordTimings.add(WordTiming(text = textBetween, startTimeMs = timeMs))
        }
    }

     
    if (wordTimings.isEmpty()) {
        println("[parseRichSyncWords] No words matched the regex")
        return null
    }

    println("[parseRichSyncWords] Successfully parsed ${wordTimings.size} words")

     
    val lineStart = lineStartTimeMs.toLongOrNull() ?: 0L
    val lineEnd = lineEndTimeMs.toLongOrNull() ?: Long.MAX_VALUE

    return ParsedRichSyncLine(
        words = wordTimings,
        lineStartTimeMs = lineStart,
        lineEndTimeMs = lineEnd,
    )
}


