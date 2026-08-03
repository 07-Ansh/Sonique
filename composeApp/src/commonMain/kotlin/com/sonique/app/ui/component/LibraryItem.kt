package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.common.Config
import com.sonique.common.LibraryChipType
import com.sonique.app.extension.NonLazyGrid
import com.sonique.app.ui.component.HomeGridCardItem
import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.PodcastsEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.type.LibraryType
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.artists
import sonique.composeapp.generated.resources.playlist
import sonique.composeapp.generated.resources.podcasts
import sonique.composeapp.generated.resources.sonique_lyrics
import sonique.composeapp.generated.resources.you
import com.sonique.domain.data.type.PlaylistType
import com.sonique.domain.data.type.RecentlyType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.utils.connectArtists
import com.sonique.domain.utils.toTrack
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.navigation.destination.list.LocalPlaylistDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.navigation.destination.list.PodcastDestination
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.LibraryViewModel
import com.sonique.app.viewModel.SharedViewModel
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.most_played
import sonique.composeapp.generated.resources.no_favorite_playlists
import sonique.composeapp.generated.resources.no_playlists_downloaded
import sonique.composeapp.generated.resources.radio
import sonique.composeapp.generated.resources.recently_added
import sonique.composeapp.generated.resources.followed_artists
import com.sonique.domain.mediaservice.handler.PlaylistType as DomainPlaylistType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItem(
    state: LibraryItemState,
    viewModel: LibraryViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onLocalPlaylistClick: (Long) -> Unit = {},
    onAlbumClick: ((String) -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    onPlaylistClick: ((String, Boolean) -> Unit)? = null,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var songEntity by remember { mutableStateOf<SongEntity?>(null) }
    val title =
        when (state.type) {
            is LibraryItemType.RecentlyAdded -> stringResource(Res.string.recently_added)
            is LibraryItemType.CanvasSong -> stringResource(Res.string.most_played)
            is LibraryItemType.FollowedArtists -> stringResource(Res.string.followed_artists)
            is LibraryItemType.YourLocalPlaylist, is LibraryItemType.YouTubePlaylist -> ""
            else -> return
        }
    val noPlaylistTitle =
        when (state.type) {
            LibraryItemType.DownloadedPlaylist -> stringResource(Res.string.no_playlists_downloaded)
            LibraryItemType.FavoritePlaylist -> stringResource(Res.string.no_favorite_playlists)
            is LibraryItemType.RecentlyAdded -> stringResource(Res.string.recently_added)
            is LibraryItemType.CanvasSong -> stringResource(Res.string.most_played)
            is LibraryItemType.FollowedArtists -> stringResource(Res.string.followed_artists)
            is LibraryItemType.YourLocalPlaylist, is LibraryItemType.YouTubePlaylist -> ""
            else -> return
        }
    Box {
        if (showBottomSheet) {
            NowPlayingBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    songEntity = null
                },
                navController = navController,
                song = songEntity ?: return,
                onLibraryDelete = {
                    songEntity?.videoId?.let { viewModel.deleteSong(it) }
                },
            )
        }
        val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
        val pinnedItems by viewModel.pinnedItems.collectAsStateWithLifecycle()
        val isPinnedGridView by viewModel.isPinnedGridView.collectAsStateWithLifecycle()
        Column {
            Crossfade(targetState = state.isLoading, label = "Loading") { isLoading ->
                if (!isLoading) {
                    if (state.type is LibraryItemType.YourLocalPlaylist || state.type is LibraryItemType.YouTubePlaylist) {
                        val playlistList = remember(state.data) { state.data.filterIsInstance<PlaylistType>() }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(playlistList) { item ->
                                val itemTitle: String = when (item) {
                                    is PlaylistEntity -> item.title
                                    is LocalPlaylistEntity -> item.title
                                    is PlaylistsResult -> item.title
                                    else -> ""
                                }
                                val thumbUrl: String? = when (item) {
                                    is PlaylistEntity -> item.thumbnails
                                    is LocalPlaylistEntity -> item.thumbnail
                                    is PlaylistsResult -> item.thumbnails.lastOrNull()?.url ?: item.thumbnails.firstOrNull()?.url
                                    else -> null
                                }
                                HomeGridCardItem(
                                    title = itemTitle,
                                    thumbUrl = thumbUrl,
                                    subtitle = (item as? PlaylistsResult)?.author,
                                    isArtist = false,
                                    onClick = {
                                        when (item) {
                                            is PlaylistEntity -> onPlaylistClick?.invoke(item.id, false) ?: navController.navigate(PlaylistDestination(item.id))
                                            is LocalPlaylistEntity -> onLocalPlaylistClick(item.id)
                                            is PlaylistsResult -> onPlaylistClick?.invoke(item.browseId, true) ?: navController.navigate(PlaylistDestination(item.browseId, isYourYouTubePlaylist = true))
                                        }
                                    },
                                    modifier = Modifier.width(115.dp),
                                )
                            }
                        }
                    } else if (state.type is LibraryItemType.FollowedArtists) {
                        LazyRow(
                            Modifier.padding(
                                top = 10.dp,
                            ),
                        ) {
                            items(state.data) { item ->
                                val artist = item as? ArtistEntity ?: return@items
                                ArtistCircularItem(
                                    artist = artist,
                                    onClick = {
                                        onArtistClick?.invoke(artist.channelId) ?: navController.navigate(
                                            ArtistDestination(
                                                channelId = artist.channelId,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    } else if (state.type is LibraryItemType.CanvasSong) {
                        LazyRow(
                            Modifier.padding(
                                top = 10.dp,
                            ),
                        ) {
                            items(state.data) { item ->
                                val song = item as? SongEntity ?: return@items
                                Box(
                                    Modifier
                                        .padding(horizontal = 10.dp)
                                        .height(300.dp)
                                        .width(170.dp)
                                        .clickable {
                                            val firstQueue: Track = song.toTrack()
                                            viewModel.setQueueData(
                                                QueueData.Data(
                                                    listTracks = arrayListOf(firstQueue),
                                                    firstPlayedTrack = firstQueue,
                                                    playlistId = "RDAMVM${firstQueue.videoId}",
                                                    playlistName = "\"${song.title}\" ${runBlocking { getString(Res.string.radio) }}",
                                                    playlistType = DomainPlaylistType.RADIO,
                                                    continuation = null,
                                                ),
                                            )
                                            viewModel.loadMediaItem(
                                                firstQueue,
                                                type = Config.SONG_CLICK,
                                            )
                                        },
                                ) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(item.canvasThumbUrl)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(item.canvasThumbUrl)
                                                .crossfade(true)
                                                .build(),
                                        placeholder = painterResource(Res.drawable.holder),
                                        error = painterResource(Res.drawable.holder),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .clip(
                                                    RoundedCornerShape(8.dp),
                                                ),
                                    )
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .align(Alignment.BottomStart),
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = typo().bodySmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(
                                                        align = Alignment.CenterVertically,
                                                    ).focusable(),
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.animation.AnimatedVisibility(visible = song.isExplicit) {
                                                ExplicitBadge(
                                                    modifier =
                                                        Modifier
                                                            .size(20.dp)
                                                            .padding(end = 4.dp)
                                                            .weight(1f),
                                                )
                                            }
                                            Text(
                                                text = (song.artistName?.connectArtists() ?: ""),
                                                style = typo().labelSmall,
                                                maxLines = 1,
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .wrapContentHeight(
                                                            align = Alignment.CenterVertically,
                                                        ).focusable(),
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        if (state.data.isNotEmpty()) {
                            LazyRow {
                                items(items = state.data) { item ->
                                    Box(modifier = Modifier.animateItem()) {
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                when (item) {
                                                    is LocalPlaylistEntity -> {
                                                        onLocalPlaylistClick(item.id)
                                                    }

                                                    is PlaylistsResult -> {
                                                        navController.navigate(
                                                            PlaylistDestination(
                                                                item.browseId,
                                                                isYourYouTubePlaylist = true,
                                                            ),
                                                        )
                                                    }

                                                    is AlbumEntity -> {
                                                        navController.navigate(
                                                            AlbumDestination(
                                                                item.browseId,
                                                            ),
                                                        )
                                                    }

                                                    is PlaylistEntity -> {
                                                        navController.navigate(
                                                            PlaylistDestination(
                                                                item.id,
                                                            ),
                                                        )
                                                    }

                                                    is PodcastsEntity -> {
                                                        navController.navigate(
                                                            PodcastDestination(
                                                                podcastId = item.podcastId,
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            data = item as? PlaylistType ?: return@items,
                                            thumbSize = 125.dp,
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(noPlaylistTitle, style = typo().bodyMedium)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CenterLoadingBox(Modifier.wrapContentSize())
                    }
                }
            }
        }
    }
}

sealed class LibraryItemType {
    data object CanvasSong : LibraryItemType()

    data class YouTubePlaylist(
        val isLoggedIn: Boolean,
        val onReload: () -> Unit = {},
    ) : LibraryItemType()

    data object YourLocalPlaylist : LibraryItemType()

    data class LocalPlaylist(
         
        val onAddClick: (String) -> Unit,
    ) : LibraryItemType()

    data object FavoritePlaylist : LibraryItemType()

    data object DownloadedPlaylist : LibraryItemType()

    data object FavoritePodcasts : LibraryItemType()

    data class RecentlyAdded(
        val playingVideoId: String,
    ) : LibraryItemType()

    data object FollowedArtists : LibraryItemType()
}

data class LibraryItemState(
    val type: LibraryItemType,
    val data: List<LibraryType>,
    val isLoading: Boolean = true,
)




