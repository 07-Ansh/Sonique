package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.kotlinytmusicscraper.models.WatchEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object YouTubeLyricsProvider : LyricsProvider {
    override val name = "YouTube Music"

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
            val nextResult = youTube.next(WatchEndpoint(videoId = id)).getOrThrow()
            val endpoint = nextResult.lyricsEndpoint
                ?: throw IllegalStateException("Lyrics endpoint not found")
            val lyrics = youTube.lyrics(endpoint).getOrThrow()
                ?: throw IllegalStateException("Lyrics unavailable")
            Result.success(lyrics)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
