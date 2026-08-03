package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.domain.lyrics.utils.TTMLParser
import com.sonique.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BetterLyricsResponse(
    val ttml: String? = null,
)

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

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
        val response = client.get("https://lyrics-api.boidu.dev/lyrics") {
            parameter("title", title)
            parameter("artist", artist)
        }.body<BetterLyricsResponse>()

        val ttml = response.ttml ?: throw IllegalStateException("TTML content is null")
        val parsed = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsed)
        if (lrc.isEmpty()) {
            throw IllegalStateException("Empty LRC parsed")
        }
        lrc
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
