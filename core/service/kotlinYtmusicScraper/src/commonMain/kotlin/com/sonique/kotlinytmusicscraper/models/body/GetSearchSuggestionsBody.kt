package com.sonique.kotlinytmusicscraper.models.body

import com.sonique.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)

