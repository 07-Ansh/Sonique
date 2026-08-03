package com.sonique.data.lyrics.providers

import com.sonique.domain.lyrics.LyricsProvider
import com.sonique.ktorext.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.util.decodeBase64String

@Serializable
private data class KuGouSearchResponse(
    val status: Int? = null,
    val errcode: Int? = null,
    val error: String? = null,
    val data: SearchData? = null,
) {
    @Serializable
    data class SearchData(
        val info: List<Info> = emptyList()
    )

    @Serializable
    data class Info(
        @SerialName("songname") val songName: String? = null,
        @SerialName("singername") val singerName: String? = null,
        val duration: Int? = null,
        val hash: String? = null,
        @SerialName("album_name") val albumName: String? = null,
    )
}

@Serializable
private data class KuGouLyricsResponse(
    val status: Int? = null,
    val errcode: Int? = null,
    val decimals: Int? = null,
    val info: String? = null,
)

object KuGouLyricsProvider : LyricsProvider {
    override val name = "KuGou"

    private val client by lazy {
        HttpClient(getEngine()) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
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
        val search = client.get("http://mobilecdn.kugou.com/api/v3/search/song") {
            parameter("format", "json")
            parameter("keyword", "$title $artist")
            parameter("page", 1)
            parameter("pagesize", 10)
        }.body<KuGouSearchResponse>()

        val infoList = search.data?.info ?: throw IllegalStateException("No search results on KuGou")
        val matched = infoList.firstOrNull() ?: throw IllegalStateException("Empty search results")
        val hash = matched.hash ?: throw IllegalStateException("No hash returned for track")

        val lyricsRes = client.get("http://krcs.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "mobi")
            parameter("hash", hash)
        }.body<KuGouLyricsResponse>()

        val base64Lrc = lyricsRes.info ?: throw IllegalStateException("No lyrics content found")
        val decoded = base64Lrc.decodeBase64String()
        if (decoded.isEmpty()) {
            throw IllegalStateException("Empty decoded lyrics")
        }
        decoded
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
