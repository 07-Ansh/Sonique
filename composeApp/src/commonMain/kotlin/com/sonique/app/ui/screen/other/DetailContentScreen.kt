package com.sonique.app.ui.screen.other

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

enum class DetailType {
    ALBUM,
    ARTIST,
    LOCAL_PLAYLIST,
    MORE_ALBUMS,
    YOUTUBE_PLAYLIST,
    PODCAST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContentScreen(
    type: DetailType,
    id: String,
    secondaryId: String? = null,
    isYourYouTubePlaylist: Boolean = false,
    navController: NavController,
    onScrolling: (Boolean) -> Unit = {},
    innerPadding: PaddingValues = PaddingValues()
) {
    // Dynamic switching based on type selected
    when (type) {
        DetailType.ALBUM -> {
            AlbumScreen(
                browseId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.ARTIST -> {
            ArtistScreen(
                channelId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.LOCAL_PLAYLIST -> {
            com.sonique.app.ui.screen.library.LocalPlaylistScreen(
                id = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.MORE_ALBUMS -> {
            MoreAlbumsScreen(
                innerPadding = innerPadding,
                navController = navController,
                type = id,
                id = secondaryId ?: ""
            )
        }
        DetailType.YOUTUBE_PLAYLIST -> {
            PlaylistScreen(
                playlistId = id,
                isYourYouTubePlaylist = isYourYouTubePlaylist,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.PODCAST -> {
            PodcastScreen(
                podcastId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
    }
}
