package com.sonique.domain.data.model.home

import com.sonique.domain.data.model.home.chart.Chart
import com.sonique.domain.data.model.mood.Mood
import com.sonique.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)

