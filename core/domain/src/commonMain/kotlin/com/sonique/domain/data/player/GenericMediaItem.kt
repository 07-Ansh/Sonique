package com.sonique.domain.data.player

 
data class GenericMediaItem(
    val mediaId: String,
    val uri: String?,
    val metadata: GenericMediaMetadata,
    val customCacheKey: String? = null,
) {
    companion object {
        val EMPTY =
            GenericMediaItem(
                mediaId = "",
                uri = null,
                metadata = GenericMediaMetadata.EMPTY,
            )
    }
}

