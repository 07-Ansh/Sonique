package com.sonique.kotlinytmusicscraper.pages

import com.sonique.kotlinytmusicscraper.models.PlaylistItem
import com.sonique.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<PlaylistItem>,
    val musicVideo: List<VideoItem>,
)

