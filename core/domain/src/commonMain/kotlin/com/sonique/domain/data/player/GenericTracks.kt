package com.sonique.domain.data.player

 
data class GenericTracks(
    val groups: List<GenericTrackGroup>,
) {
    data class GenericTrackGroup(
        val trackCount: Int,
    )
}

