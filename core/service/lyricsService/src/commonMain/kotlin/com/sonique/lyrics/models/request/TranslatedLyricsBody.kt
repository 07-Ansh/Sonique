package com.sonique.lyrics.models.request

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedLyricsBody(
    val videoId: String,
    val translatedLyric: String,  
    val language: String,  
    val contributor: String,
    val contributorEmail: String,
)


