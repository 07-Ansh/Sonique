package com.sonique.spotify.model.body

import kotlinx.serialization.Serializable

@Serializable
data class CanvasBody(
    val tracks: List<Track>,
) {
    @Serializable
    data class Track(
        val track_uri: String,
    )
}

 
 
 
 
 
 

