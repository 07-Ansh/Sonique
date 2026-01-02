package com.sonique.domain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "your_youtube_playlist_list")
data class YourYouTubePlaylistList(
    @PrimaryKey val emailPageId: String, // {email}_{pageId}
    val listBrowseIds: List<String>,
)

