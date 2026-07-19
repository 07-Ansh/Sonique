package com.sonique.app.ui.navigation.graph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.navigation.destination.list.LocalPlaylistDestination
import com.sonique.app.ui.navigation.destination.list.MoreAlbumsDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.navigation.destination.list.PodcastDestination
import com.sonique.app.ui.screen.library.LocalPlaylistScreen
import com.sonique.app.ui.screen.other.AlbumScreen
import com.sonique.app.ui.screen.other.ArtistScreen
import com.sonique.app.ui.screen.other.MoreAlbumsScreen
import com.sonique.app.ui.screen.other.PlaylistScreen
import com.sonique.app.ui.screen.other.PodcastScreen

import com.sonique.app.ui.screen.other.DetailContentScreen
import com.sonique.app.ui.screen.other.DetailType

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun NavGraphBuilder.listScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    onScrolling: (Boolean) -> Unit = {},
) {
    composable<AlbumDestination> { entry ->
        val data = entry.toRoute<AlbumDestination>()
        DetailContentScreen(
            type = DetailType.ALBUM,
            id = data.browseId,
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
    composable<ArtistDestination> { entry ->
        val data = entry.toRoute<ArtistDestination>()
        DetailContentScreen(
            type = DetailType.ARTIST,
            id = data.channelId,
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
    composable<LocalPlaylistDestination> { entry ->
        val data = entry.toRoute<LocalPlaylistDestination>()
        DetailContentScreen(
            type = DetailType.LOCAL_PLAYLIST,
            id = data.id.toString(),
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
    composable<MoreAlbumsDestination> { entry ->
        val data = entry.toRoute<MoreAlbumsDestination>()
        DetailContentScreen(
            type = DetailType.MORE_ALBUMS,
            id = data.type,
            secondaryId = data.id,
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
    composable<PlaylistDestination> { entry ->
        val data = entry.toRoute<PlaylistDestination>()
        DetailContentScreen(
            type = DetailType.YOUTUBE_PLAYLIST,
            id = data.playlistId,
            isYourYouTubePlaylist = data.isYourYouTubePlaylist,
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
    composable<PodcastDestination> { entry ->
        val data = entry.toRoute<PodcastDestination>()
        DetailContentScreen(
            type = DetailType.PODCAST,
            id = data.podcastId,
            navController = navController,
            onScrolling = onScrolling,
            innerPadding = innerPadding
        )
    }
}

