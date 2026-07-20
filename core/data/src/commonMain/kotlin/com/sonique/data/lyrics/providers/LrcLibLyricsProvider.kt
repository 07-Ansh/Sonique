package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
private data class LrcLibResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
) {
    val displayName: String get() = trackName ?: name ?: ""
}

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    private val client by lazy {
        HttpClient(getEngine()) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
    }

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val searchResults = client.get("https://lrclib.net/api/search") {
            parameter("q", "$title $artist")
        }.body<List<LrcLibResponse>>()

        val filtered = scoreAndFilterResults(searchResults, title, artist, duration)
        val best = filtered.firstOrNull()?.first ?: throw IllegalStateException("No matching track found")

        val lyrics = best.syncedLyrics ?: best.plainLyrics ?: throw IllegalStateException("No lyrics content")
        lyrics
    }

    private fun scoreAndFilterResults(
        results: List<LrcLibResponse>,
        title: String,
        artist: String,
        duration: Int
    ): List<Pair<LrcLibResponse, Double>> {
        val durationMs = duration * 1000
        val targetIsMixed = title.contains("mixed", ignoreCase = true)
        val targetIsRemix = title.contains("remix", ignoreCase = true)

        return results.map { result ->
            var score = 0.0
            val resTitle = result.displayName
            val resArtist = result.artistName ?: ""

            result.duration?.let { d ->
                val diff = abs(d * 1000 - durationMs)
                when {
                    diff <= 2000 -> score += 100
                    diff <= 5000 -> score += 50
                    diff <= 10000 -> score += 10
                    else -> score -= 50
                }
            }

            val cleanedResTitle = resTitle.lowercase().trim()
            val cleanedTargetTitle = title.lowercase().trim()
            when {
                cleanedResTitle == cleanedTargetTitle -> score += 80
                cleanedResTitle.contains(cleanedTargetTitle) || cleanedTargetTitle.contains(cleanedResTitle) -> score += 40
            }

            val resIsMixed = resTitle.contains("mixed", ignoreCase = true)
            val resIsRemix = resTitle.contains("remix", ignoreCase = true)
            if (resIsMixed && !targetIsMixed) score -= 60
            if (resIsRemix && !targetIsRemix) score -= 40

            val resArtistLower = resArtist.lowercase()
            val targetArtistLower = artist.lowercase()
            if (resArtistLower.contains(targetArtistLower) || targetArtistLower.contains(resArtistLower)) {
                score += 50
            }

            result to score
        }.sortedByDescending { it.second }.filter { it.second > 0 }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, duration, album).onSuccess { callback(it) }
    }
}
