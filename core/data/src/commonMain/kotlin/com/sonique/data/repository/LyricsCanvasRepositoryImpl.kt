@file:OptIn(ExperimentalTime::class)

package com.sonique.data.repository

import com.sonique.data.db.LocalDataSource
import com.sonique.data.mapping.toCanvasResult
import com.sonique.data.mapping.toLyrics
import com.sonique.domain.data.entities.LyricsEntity
import com.sonique.domain.data.entities.TranslatedLyricsEntity
import com.sonique.domain.data.model.canvas.CanvasResult
import com.sonique.domain.data.model.metadata.Lyrics
import com.sonique.domain.extension.now
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.LyricsCanvasRepository
import com.sonique.domain.utils.Resource
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.logger.Logger
import com.sonique.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.sonique.lyrics.SoniqueLyricsClient
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class LyricsCanvasRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val spotify: Spotify,
    private val lyricsClient: SoniqueLyricsClient,
) : LyricsCanvasRepository {
    override fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> = flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    override suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertLyrics(lyricsEntity)
        }

    override suspend fun insertTranslatedLyrics(translatedLyrics: TranslatedLyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertTranslatedLyrics(translatedLyrics)
        }

    override fun getSavedTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<TranslatedLyricsEntity?> = flow { emit(localDataSource.getTranslatedLyrics(videoId, language)) }.flowOn(Dispatchers.IO)

    override suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.removeTranslatedLyrics(videoId, language)
    }

    override fun getYouTubeCaption(
        preferLang: String,
        videoId: String,
    ): Flow<Resource<Pair<Lyrics, Lyrics?>>> =
        flow {
            runCatching {
                youTube
                    .getYouTubeCaption(videoId, preferLang)
                    .onSuccess { lyrics ->
                        emit(
                            Resource.Success<Pair<Lyrics, Lyrics?>>(
                                Pair(lyrics.first.toLyrics(), lyrics.second?.toLyrics()),
                            ),
                        )
                    }.onFailure { e ->
                        Logger.d("Lyrics", "Error: ${e.message}")
                        emit(Resource.Error<Pair<Lyrics, Lyrics?>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getCanvas(
        dataStoreManager: DataStoreManager,
        videoId: String,
        duration: Int,
    ): Flow<Resource<CanvasResult>> =
        flow {
            runCatching {
                localDataSource.getSong(videoId).let { song ->
                    val q =
                        "${song?.title} ${song?.artistName?.firstOrNull() ?: ""}"
                            .replace(
                                Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                " ",
                            ).replace(
                                Regex("( và | & | и | e | und |, |和| dan)"),
                                " ",
                            ).replace("  ", " ")
                            .replace(Regex("([()])"), "")
                            .replace(".", " ")
                            .replace("  ", " ")
                    var spotifyPersonalToken = ""
                    var spotifyClientToken = ""
                    Logger.w("Lyrics", "getSpotifyLyrics: ${dataStoreManager.spotifyPersonalTokenExpires.first()}")
                    Logger.w("Lyrics", "getSpotifyLyrics ${dataStoreManager.spotifyClientTokenExpires.first()}")
                    Logger.w("Lyrics", "getSpotifyLyrics now: ${now()}")
                    if (dataStoreManager.spotifyPersonalToken
                            .first()
                            .isNotEmpty() &&
                        dataStoreManager.spotifyClientToken.first().isNotEmpty() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                        dataStoreManager.spotifyClientTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                        dataStoreManager.spotifyClientTokenExpires.first() != 0L
                    ) {
                        spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                        spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                        Logger.d("Canvas", "spotifyPersonalToken: $spotifyPersonalToken")
                        Logger.d("Canvas", "spotifyClientToken: $spotifyClientToken")
                    } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Logger.d("Canvas", "Request clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L) + Clock.System.now().toEpochMilliseconds(),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                            }
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Logger.d("Canvas", "Request spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                            }
                    }
                    if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                        val authToken = spotifyPersonalToken
                        spotify
                            .searchSpotifyTrack(q, authToken, spotifyClientToken)
                            .onSuccess { searchResponse ->
                                Logger.w("Canvas", "searchSpotifyResponse: $searchResponse")
                                val track =
                                    if (duration != 0) {
                                        searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                            abs(
                                                (
                                                    (
                                                        (
                                                            it.item
                                                                ?.data
                                                                ?.duration
                                                                ?.totalMilliseconds ?: (0 / 1000)
                                                        ) - duration
                                                    )
                                                ),
                                            ) < 1
                                        }
                                            ?: searchResponse.data
                                                ?.searchV2
                                                ?.tracksV2
                                                ?.items
                                                ?.firstOrNull()
                                    } else {
                                        searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                    }
                                if (track != null) {
                                    Logger.w("Canvas", "track: $track")
                                    spotify
                                        .getSpotifyCanvas(
                                            track.item?.data?.id ?: "",
                                            spotifyPersonalToken,
                                            spotifyClientToken,
                                        ).onSuccess {
                                            Logger.w("Canvas", "canvas: $it")
                                            it.toCanvasResult()?.let {
                                                emit(Resource.Success(it))
                                            } ?: run {
                                                emit(Resource.Error<CanvasResult>("Not found"))
                                            }
                                        }.onFailure {
                                            it.printStackTrace()
                                            emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                                        }
                                } else {
                                    emit(Resource.Error<CanvasResult>("Not found"))
                                }
                            }.onFailure { throwable ->
                                throwable.printStackTrace()
                                emit(Resource.Error<CanvasResult>(throwable.message ?: "Not found"))
                            }
                    } else {
                        emit(Resource.Error<CanvasResult>("Not found"))
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasUrl(videoId, canvasUrl)
    }

    override suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasThumbUrl(videoId, canvasThumbUrl)
    }

    override fun getSpotifyLyrics(
        dataStoreManager: DataStoreManager,
        query: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                val q =
                    query
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                        .replace("  ", " ")
                Logger.d("Lyrics", "query: $q")
                var spotifyPersonalToken = ""
                var spotifyClientToken = ""
                Logger.w("Lyrics", "getSpotifyLyrics: ${dataStoreManager.spotifyPersonalTokenExpires.first()}")
                if (dataStoreManager.spotifyPersonalToken
                        .first()
                        .isNotEmpty() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                    dataStoreManager.spotifyClientTokenExpires.first() > Clock.System.now().toEpochMilliseconds() &&
                    dataStoreManager.spotifyClientTokenExpires.first() != 0L
                ) {
                    spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                    spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                    Logger.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                    Logger.d("Lyrics", "spotifyClientToken: $spotifyClientToken")
                } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                    runBlocking {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Logger.d("Canvas", "Request clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L) + Clock.System.now().toEpochMilliseconds(),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                    runBlocking {
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Logger.d("Lyrics", "REQUEST spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                }
                if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                    val authToken = spotifyPersonalToken
                    Logger.d("Lyrics", "authToken: $authToken")
                    spotify
                        .searchSpotifyTrack(q, authToken, spotifyClientToken)
                        .onSuccess { searchResponse ->
                            val track =
                                if (duration != 0 && duration != null) {
                                    searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                        abs(
                                            (
                                                (
                                                    (
                                                        it.item
                                                            ?.data
                                                            ?.duration
                                                            ?.totalMilliseconds ?: (0 / 1000)
                                                    ) - duration
                                                )
                                            ),
                                        ) < 1
                                    }
                                        ?: searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                } else {
                                    searchResponse.data
                                        ?.searchV2
                                        ?.tracksV2
                                        ?.items
                                        ?.firstOrNull()
                                }
                            Logger.d("Lyrics", "track: $track")
                            if (track != null) {
                                spotify
                                    .getSpotifyLyrics(track.item?.data?.id ?: "", spotifyPersonalToken, spotifyClientToken)
                                    .onSuccess {
                                        emit(Resource.Success<Lyrics>(it.toLyrics()))
                                    }.onFailure {
                                        it.printStackTrace()
                                        emit(Resource.Error<Lyrics>("Not found"))
                                    }
                            } else {
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Resource.Error<Lyrics>("Not found"))
                        }
                }
            }
        }

    override fun getLrclibLyricsData(
        sartist: String,
        strack: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow<Resource<Lyrics>> {
            Logger.w("Lyrics", "getLrclibLyricsData: $sartist $strack $duration")
            val qartist =
                sartist
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
            val qtrack =
                strack
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")

            val result = lyricsClient.searchLrclibLyrics(qtrack, qartist, duration)
            if (result.isSuccess) {
                result.getOrNull()?.let {
                    emit(Resource.Success(it.toLyrics()))
                }
            } else {
                result.exceptionOrNull()?.printStackTrace()
                emit(Resource.Error("Not found"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getAITranslationLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
    ): Flow<Resource<Lyrics>> =
        flow {
            emit(Resource.Error<Lyrics>("AI Translation is no longer supported."))
        }.flowOn(Dispatchers.IO)


}


