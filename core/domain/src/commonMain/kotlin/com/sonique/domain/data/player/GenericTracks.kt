package com.sonique.domain.data.player

/**
 * Generic tracks information
 */
data class GenericTracks(
    val groups: List<GenericTrackGroup>,
) {
    data class GenericTrackGroup(
        val trackCount: Int,
    )
}

