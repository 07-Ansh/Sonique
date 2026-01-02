package com.sonique.lyrics.parser

import com.sonique.lyrics.domain.Lyrics

fun parseSyncedLyrics(data: String): Lyrics {
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.+)")
    val lines = data.lines()
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    lines.map { line ->
        val matchResult = regex.matchEntire(line)
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLong()
            val seconds = matchResult.groupValues[2].toLong()
            val milliseconds = matchResult.groupValues[3].toLong()
            val timeInMillis = minutes * 60_000L + seconds * 1000L + milliseconds
            val content = (if (matchResult.groupValues[4] == " ") " ♫" else matchResult.groupValues[4]).removeRange(0, 1)
            linesLyrics.add(
                Lyrics.LyricsX.Line(
                    endTimeMs = "0",
                    startTimeMs = timeInMillis.toString(),
                    syllables = listOf(),
                    words = content,
                ),
            )
        }
    }
    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "LINE_SYNCED",
            ),
    )
}

fun parseRichSyncLyrics(data: String): Lyrics {
    // Unescape JSON string if needed (remove quotes and replace \n with actual newlines)
    val unescapedData =
        data
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")

    // Handle different line separators (Unix \n, Windows \r\n, Mac \r)
    val lines = unescapedData.lines()
    // Skip offset line if present (starts with [offset:)
    val lyricsLines =
        lines.filter { line ->
            line.isNotBlank() && !line.trim().startsWith("[offset:")
        }

    println("[parseRichSyncLyrics] Total lines: ${lines.size}, Filtered lines: ${lyricsLines.size}")
    if (lyricsLines.isNotEmpty()) {
        println("[parseRichSyncLyrics] First line sample: ${lyricsLines.first()}")
    }

    // Regex to match [MM:SS.mm] format (flexible with 1-2 digits)
    val regex = Regex("\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.+)")
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()

    lyricsLines.forEachIndexed { index, line ->
        val matchResult = regex.matchEntire(line.trim())
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
            val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
            val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L

            // Convert to milliseconds
            // If centiseconds has 3 digits (milliseconds), use directly
            // If 2 digits (centiseconds), multiply by 10
            val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
            val timeInMillis = minutes * 60_000L + seconds * 1000L + millisPart

            // Keep the rich sync content as-is (with <MM:SS.mm> word format)
            val content = matchResult.groupValues[4].trimStart()

            if (content.isNotBlank()) {
                linesLyrics.add(
                    Lyrics.LyricsX.Line(
                        endTimeMs = "0",
                        startTimeMs = timeInMillis.toString(),
                        syllables = listOf(),
                        words = content,
                    ),
                )
            }
        } else {
            if (index < 3) { // Only log first 3 failed matches to avoid spam
                println("[parseRichSyncLyrics] Line $index failed to match: '${line.take(100)}'")
            }
        }
    }

    println("[parseRichSyncLyrics] Parsed ${linesLyrics.size} lines successfully")

    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "RICH_SYNCED",
            ),
    )
}

fun parseUnsyncedLyrics(data: String): Lyrics {
    val lines = data.lines()
    val linesLyrics = ArrayList<Lyrics.LyricsX.Line>()
    lines.map { line ->
        linesLyrics.add(
            Lyrics.LyricsX.Line(
                endTimeMs = "0",
                startTimeMs = "0",
                syllables = listOf(),
                words = line,
            ),
        )
    }
    return Lyrics(
        lyrics =
            Lyrics.LyricsX(
                lines = linesLyrics,
                syncType = "UNSYNCED",
            ),
    )
}


