package com.sonique.spotify

import com.sonique.spotify.auth.SpotifyAuth
import com.sonique.spotify.model.response.spotify.CanvasResponse
import com.sonique.spotify.model.response.spotify.ClientTokenResponse
import com.sonique.spotify.model.response.spotify.PersonalTokenResponse
import com.sonique.spotify.model.response.spotify.SpotifyLyricsResponse
import com.sonique.spotify.model.response.spotify.search.SpotifySearchResponse
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http

class Spotify {
    private val spotifyClient = SpotifyClient()
    private val spotifyAuth = SpotifyAuth(spotifyClient)

     
    fun removeProxy() {
        spotifyClient.proxy = null
    }

     
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        val verifiedHost =
            if (!host.contains("http")) {
                "http://$host"
            } else {
                host
            }
        runCatching {
            if (isHttp) ProxyBuilder.http("$verifiedHost:$port") else ProxyBuilder.socks(verifiedHost, port)
        }.onSuccess {
            spotifyClient.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

     
    suspend fun getPersonalToken(spdc: String) =
        runCatching {
            spotifyClient.getSpotifyLyricsToken(spdc).body<PersonalTokenResponse>()
        }

     
    suspend fun getPersonalTokenWithTotp(spdc: String) = spotifyAuth.refreshToken(spdc)

    suspend fun getClientToken() =
        runCatching {
            spotifyClient
                .getSpotifyClientToken()
                .body<ClientTokenResponse>()
        }

    suspend fun searchSpotifyTrack(
        query: String,
        authToken: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient
            .searchSpotifyTrack(query, authToken, clientToken)
            .body<SpotifySearchResponse>()
    }

    suspend fun getSpotifyLyrics(
        trackId: String,
        token: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient
            .getSpotifyLyrics(
                token = token,
                clientToken = clientToken,
                trackId,
            ).body<SpotifyLyricsResponse>()
    }

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
        clientToken: String,
    ) = runCatching {
        spotifyClient.getSpotifyCanvas(trackId, token, clientToken).body<CanvasResponse>()
    }
}

