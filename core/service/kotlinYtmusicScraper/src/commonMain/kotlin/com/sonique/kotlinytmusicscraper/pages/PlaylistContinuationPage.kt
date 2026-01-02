package com.sonique.kotlinytmusicscraper.pages

import com.sonique.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)

