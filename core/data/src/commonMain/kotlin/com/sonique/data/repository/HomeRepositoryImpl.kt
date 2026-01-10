package com.sonique.data.repository

import com.sonique.data.parser.parseChart
import com.sonique.data.parser.parseGenreObject
import com.sonique.data.parser.parseMixedContent
import com.sonique.data.parser.parseMoodsMomentObject
import com.sonique.data.parser.parseNewRelease
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.data.model.home.chart.Chart
import com.sonique.domain.data.model.mood.Genre
import com.sonique.domain.data.model.mood.Mood
import com.sonique.domain.data.model.mood.MoodsMoment
import com.sonique.domain.data.model.mood.genre.GenreObject
import com.sonique.domain.data.model.mood.moodmoments.MoodsMomentObject
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.HomeRepository
import com.sonique.domain.utils.Resource
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class HomeRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val youTube: YouTube,
) : HomeRepository {
    override fun getHomeData(
        params: String?,
        viewString: String,
        songString: String,
    ): Flow<Resource<Pair<String?, List<HomeItem>>>> =
        flow {
            runCatching {
                val limit = dataStoreManager.homeLimit.first()
                youTube
                    .customQuery(browseId = "FEmusic_home", params = params)
                    .onSuccess { result ->
                        val list: ArrayList<HomeItem> = arrayListOf()
                        if (result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicCarouselShelfRenderer
                                ?.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.strapline
                                ?.runs
                                ?.get(
                                    0,
                                )?.text != null
                        ) {
                            val accountName =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.strapline
                                    ?.runs
                                    ?.get(
                                        0,
                                    )?.text ?: ""
                            val accountThumbUrl =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                                    ?.get(
                                        0,
                                    )?.url
                                    ?.replace("s88", "s352") ?: ""
                            if (accountName != "" && accountThumbUrl != "") {
                                dataStoreManager.putString("AccountName", accountName)
                                dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                            }
                        }
                        val continueParam =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.continuations
                                ?.get(
                                    0,
                                )?.nextContinuationData
                                ?.continuation
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                        list.addAll(
                            parseMixedContent(
                                data,
                                viewString,
                                songString,
                            ),
                        )
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
                        Logger.d("Repository", "List size: ${list.size}")
                        emit(Resource.Success(continueParam to list.toList()))
                    }.onFailure { error ->
                        emit(Resource.Error<Pair<String?, List<HomeItem>>>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getHomeDataContinue(
        continueParam: String,
        viewString: String,
        songString: String
    ): Flow<Resource<Pair<String?, List<HomeItem>>>> = flow {
        youTube
            .customQuery(browseId = "", continuation = continueParam)
            .onSuccess { response ->
                val newContinueParam =
                    response.continuationContents
                        ?.sectionListContinuation
                        ?.continuations
                        ?.get(
                            0,
                        )?.nextContinuationData
                        ?.continuation
                Logger.d("Repository", "continueParam: $continueParam")
                val dataContinue =
                    response.continuationContents?.sectionListContinuation?.contents
                val list =
                    parseMixedContent(
                        dataContinue,
                        viewString,
                        songString,
                    )
                emit(Resource.Success(newContinueParam to list))
            }.onFailure {
                emit(Resource.Error<Pair<String?, List<HomeItem>>>(it.message.toString()))
            }
    }.flowOn(Dispatchers.IO)

    override fun getNewRelease(
        newReleaseString: String,
        musicVideoString: String,
    ): Flow<Resource<List<HomeItem>>> =
        flow {
            youTube
                .newRelease()
                .onSuccess { result ->
                    emit(Resource.Success<List<HomeItem>>(parseNewRelease(result, newReleaseString, musicVideoString)))
                }.onFailure { error ->
                    emit(Resource.Error<List<HomeItem>>(error.message.toString()))
                }
        }.flowOn(Dispatchers.IO)

    override fun getChartData(countryCode: String): Flow<Resource<Chart>> =
        flow {
            runCatching {
                youTube
                    .customQuery("FEmusic_charts", country = countryCode)
                    .onSuccess { result ->
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                        val chart = parseChart(data)
                        if (chart != null) {
                            emit(Resource.Success<Chart>(chart))
                        } else {
                            emit(Resource.Error<Chart>("Error"))
                        }
                    }.onFailure { error ->
                        emit(Resource.Error<Chart>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getMoodAndMomentsData(): Flow<Resource<Mood>> =
        flow {
            runCatching {
                youTube
                    .moodAndGenres()
                    .onSuccess { result ->
                        val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                        val listGenre: ArrayList<Genre> = arrayListOf()
                        result[0].let { moodsmoment ->
                            for (item in moodsmoment.items) {
                                listMoodMoments.add(
                                    MoodsMoment(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        result[1].let { genres ->
                            for (item in genres.items) {
                                listGenre.add(
                                    Genre(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))
                    }.onFailure { e ->
                        emit(Resource.Error<Mood>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> =
        flow {
            runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseMoodsMomentObject(result)
                        if (data != null) {
                            emit(Resource.Success<MoodsMomentObject>(data))
                        } else {
                            emit(Resource.Error<MoodsMomentObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getGenreData(params: String): Flow<Resource<GenreObject>> =
        flow {
            runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseGenreObject(result)
                        if (data != null) {
                            emit(Resource.Success<GenreObject>(data))
                        } else {
                            emit(Resource.Error<GenreObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<GenreObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}

