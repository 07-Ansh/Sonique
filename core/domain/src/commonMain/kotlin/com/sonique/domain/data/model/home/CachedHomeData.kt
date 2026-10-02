package com.sonique.domain.data.model.home

import kotlinx.serialization.Serializable

@Serializable
data class CachedHomeData(
    val homeItems: List<HomeItem>,
    val continuation: String? = null,
    val timestamp: Long = 0L,
)
