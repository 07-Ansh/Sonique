package com.sonique.kotlinytmusicscraper.pages

import com.sonique.kotlinytmusicscraper.models.AlbumItem
import com.sonique.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<AlbumItem>,
    val musicVideo: List<VideoItem>,
)

