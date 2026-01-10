package com.sonique.lyrics.models.request

import kotlinx.serialization.Serializable

@Serializable
data class VoteBody(
    val id: String,
    val vote: Int,
     
)


