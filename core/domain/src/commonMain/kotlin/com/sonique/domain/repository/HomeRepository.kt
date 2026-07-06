package com.sonique.domain.repository

import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.data.model.home.chart.Chart
import com.sonique.domain.data.model.mood.Mood
import com.sonique.domain.data.model.mood.genre.GenreObject
import com.sonique.domain.data.model.mood.moodmoments.MoodsMomentObject
import com.sonique.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
     
    fun getHomeData(
        params: String? = null,
        viewString: String,
        songString: String,
        forceRefresh: Boolean = false,
    ): Flow<Resource<Pair<String?, List<HomeItem>>>>

    fun getHomeDataContinue(
        continueParam: String,
        viewString: String,
        songString: String,
    ): Flow<Resource<Pair<String?, List<HomeItem>>>>

    fun getNewRelease(
        newReleaseString: String,
        musicVideoString: String,
        forceRefresh: Boolean = false,
    ): Flow<Resource<List<HomeItem>>>

    fun getChartData(
        countryCode: String = "KR",
        forceRefresh: Boolean = false,
    ): Flow<Resource<Chart>>

    fun getMoodAndMomentsData(
        forceRefresh: Boolean = false,
    ): Flow<Resource<Mood>>

    fun getGenreData(params: String): Flow<Resource<GenreObject>>

    fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>>
}

