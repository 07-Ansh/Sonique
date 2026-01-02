package com.sonique.domain.data.model.mood.moodmoments

import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val subtitle: String,
    val thumbnails: List<Thumbnail>?,
    val title: String,
) : HomeContentType

