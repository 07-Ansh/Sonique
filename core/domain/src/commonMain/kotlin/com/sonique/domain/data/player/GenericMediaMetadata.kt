package com.sonique.domain.data.player

 
data class GenericMediaMetadata(
    val title: String? = null,
    val artist: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val description: String? = null,
) {
    companion object {
        val EMPTY = GenericMediaMetadata()
    }
}

