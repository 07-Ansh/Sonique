package com.sonique.domain.data.model.mood.genre

import com.sonique.domain.data.model.searchResult.songs.Thumbnail
import com.sonique.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val thumbnail: List<Thumbnail>?,
    val title: Title,
) : HomeContentType

