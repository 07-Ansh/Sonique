package com.sonique.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedLyricsResponse(
    val id: String,
    val videoId: String,
    val translatedLyric: String,  
    val language: String,  
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
)


