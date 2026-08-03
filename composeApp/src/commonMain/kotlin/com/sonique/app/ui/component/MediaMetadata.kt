package com.sonique.app.ui.component

import androidx.compose.runtime.Immutable

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int = 0,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
) {
    data class Artist(
        val id: String?,
        val name: String,
    )

    data class Album(
        val id: String,
        val title: String,
    )
}
