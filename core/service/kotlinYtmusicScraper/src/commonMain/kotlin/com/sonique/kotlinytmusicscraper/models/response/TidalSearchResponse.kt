package com.sonique.kotlinytmusicscraper.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TidalSearchResponse(
    @SerialName("tracks")
    val tracks: TracksResult? = null,
) {
    @Serializable
    data class TracksResult(
        @SerialName("items")
        val items: List<Item?>? = null,
        @SerialName("totalNumberOfItems")
        val totalNumberOfItems: Int? = null,
    )

    @Serializable
    data class Item(
        @SerialName("id")
        val id: Int? = null,
        @SerialName("title")
        val title: String? = null,
        @SerialName("duration")
        val duration: Int? = null,
        @SerialName("artists")
        val artists: List<Artist?>? = null,
        @SerialName("audioAnalysisAttributes")
        val audioAnalysisAttributes: AudioAnalysisAttributes? = null,
        @SerialName("isrc")
        val isrc: String? = null,
    )

    @Serializable
    data class Artist(
        @SerialName("id")
        val id: Int? = null,
        @SerialName("name")
        val name: String? = null,
        @SerialName("type")
        val type: String? = null,
    )

    @Serializable
    data class AudioAnalysisAttributes(
        @SerialName("bpm")
        val bpm: String? = null,
        @SerialName("key")
        val key: String? = null,
        @SerialName("keyScale")
        val keyScale: String? = null,
        @SerialName("scale")
        val scale: String? = null,
    )
}
