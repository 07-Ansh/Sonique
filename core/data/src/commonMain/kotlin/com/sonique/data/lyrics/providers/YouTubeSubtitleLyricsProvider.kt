package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    private val youTube by lazy { YouTube() }

    override fun isEnabled() = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val captionPair = youTube.getYouTubeCaption(id, "en").getOrThrow()
            val transcript = captionPair.first
            val lrc = transcript.text.joinToString(separator = "\n") { item ->
                val time = (item.start.toDoubleOrNull() ?: 0.0) * 1000.0
                val ms = time.toLong()
                val m = ms / 60000
                val s = (ms % 60000) / 1000
                val c = (ms % 1000) / 10
                val timeStr = buildString {
                    append('[')
                    if (m < 10) append('0')
                    append(m).append(':')
                    if (s < 10) append('0')
                    append(s).append('.')
                    if (c < 10) append('0')
                    append(c).append(']')
                }
                val text = item.content.replace(Regex("<[^>]*>"), "").trim('♪', ' ')
                "$timeStr$text"
            }
            if (lrc.isBlank()) {
                throw IllegalStateException("Empty transcript")
            }
            Result.success(lrc)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
