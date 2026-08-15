package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.sonique.domain.data.entities.AlbumEntity
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.PodcastsEntity
import com.sonique.domain.data.model.searchResult.albums.AlbumsResult
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.data.type.PlaylistType
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.Logger
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.copy
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.LocalPlaylistDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.navigation.destination.list.PodcastDestination
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.viewModel.LibraryViewModel
import org.koin.compose.viewmodel.koinViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.ui.component.painterPlaylistThumbnail
import com.sonique.domain.utils.connectArtists
import com.sonique.domain.utils.toListName
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.artists
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_search_24
import sonique.composeapp.generated.resources.create
import sonique.composeapp.generated.resources.playlist
import sonique.composeapp.generated.resources.podcasts
import sonique.composeapp.generated.resources.search
import sonique.composeapp.generated.resources.sonique_lyrics
import sonique.composeapp.generated.resources.you

@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalMaterial3Api::class)
@Composable
internal inline fun <reified T> GridLibraryPlaylist(
    navController: NavController,
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    title: StringResource? = null,
    noinline onBack: (() -> Unit)? = null,
    noinline onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    noinline createNewPlaylist: (() -> Unit)? = null,
    noinline onLocalPlaylistClick: (Long) -> Unit = {},
    noinline onAlbumClick: ((String) -> Unit)? = null,
    noinline onArtistClick: ((String) -> Unit)? = null,
    noinline onPlaylistClick: ((String, Boolean) -> Unit)? = null,
    noinline actions: (@Composable () -> Unit)? = null,
    noinline onReload: () -> Unit = {},
) {
    Logger.w("GridLibraryPlaylist", "Generic Type: ${T::class.java}")
    val viewModel: LibraryViewModel = koinViewModel()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val state = rememberLazyListState()
    val isScrollingUp by state.isScrollingUp()
    val typography = typo()
    val displayTitle = title?.let { stringResource(it) }
    val createString = stringResource(Res.string.create)

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val hazeState = rememberHazeState(blurEnabled = true)

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                onScrolling.invoke(isAtTop)
            }
    }
    val pullToRefreshState = rememberPullToRefreshState()
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = onReload,
            isRefreshing = data is LocalResource.Loading,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = data is LocalResource.Loading,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = contentPadding.calculateTopPadding(),
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            Crossfade(targetState = data) { data ->
                val list = (data as? LocalResource.Success)?.data ?: emptyList()
                val filteredList = remember(list, query, showSearchBar) {
                    if (query.isNotEmpty() && showSearchBar) {
                        list.filter { item ->
                            val itemTitle = when (item) {
                                is PlaylistEntity -> item.title
                                is LocalPlaylistEntity -> item.title
                                is AlbumEntity -> item.title
                                is PlaylistsResult -> item.title
                                is AlbumsResult -> item.title
                                is PodcastsEntity -> item.title
                                is com.sonique.domain.data.entities.ArtistEntity -> item.name
                                else -> ""
                            }
                            itemTitle.contains(query, ignoreCase = true)
                        }
                    } else {
                        list
                    }
                }

                if (data is LocalResource.Success && (filteredList.isNotEmpty() || list.isNotEmpty()) || createNewPlaylist != null) {
                    if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.hazeSource(hazeState).fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = contentPadding.calculateTopPadding() + 65.dp,
                            bottom = 100.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = rememberLazyGridState(),
                    ) {
                        if (showSearchBar) {
                            item(span = { GridItemSpan(3) }) {
                                Spacer(Modifier.height(55.dp))
                            }
                        }

                        if (createNewPlaylist != null) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { createNewPlaylist() }
                                        .padding(4.dp),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(32.dp),
                                            imageVector = Icons.Rounded.Add,
                                            tint = Color.White,
                                            contentDescription = null,
                                        )
                                    }
                                    Text(
                                        text = createString,
                                        style = typo().bodyMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                    )
                                }
                            }
                        }

                        items(filteredList) { item ->
                            var title = ""
                            var thumbUrl: String? = null
                            var subtitle: String? = null
                            var isArtist = false

                            when (item) {
                                is AlbumEntity -> {
                                    title = item.title
                                    thumbUrl = item.thumbnails
                                    subtitle = item.artistName?.connectArtists() ?: stringResource(Res.string.album)
                                }

                                is PlaylistEntity -> {
                                    title = item.title
                                    thumbUrl = item.thumbnails
                                    val author = item.author ?: stringResource(Res.string.playlist)
                                    subtitle = if (author == stringResource(Res.string.sonique_lyrics)) "Sonique" else author
                                }

                                is LocalPlaylistEntity -> {
                                    title = item.title
                                    thumbUrl = item.thumbnail
                                    subtitle = stringResource(Res.string.you)
                                }

                                is PlaylistsResult -> {
                                    title = item.title
                                    thumbUrl = item.thumbnails.lastOrNull()?.url
                                    subtitle = item.author ?: stringResource(Res.string.playlist)
                                }

                                is AlbumsResult -> {
                                    title = item.title
                                    thumbUrl = item.thumbnails.lastOrNull()?.url
                                    subtitle = item.artists.toListName().connectArtists()
                                }

                                is PodcastsEntity -> {
                                    title = item.title
                                    thumbUrl = item.thumbnail
                                    subtitle = item.authorName ?: stringResource(Res.string.podcasts)
                                }

                                is com.sonique.domain.data.entities.ArtistEntity -> {
                                    title = item.name
                                    thumbUrl = item.thumbnails
                                    subtitle = stringResource(Res.string.artists)
                                    isArtist = true
                                }
                            }

                            val onClick: () -> Unit = {
                                when (item) {
                                    is LocalPlaylistEntity -> {
                                        onLocalPlaylistClick(item.id)
                                    }

                                    is PlaylistsResult -> {
                                        if (item.browseId.startsWith("MPRE")) {
                                            onAlbumClick?.invoke(item.browseId) ?: navController.navigate(
                                                AlbumDestination(item.browseId),
                                            )
                                        } else {
                                            onPlaylistClick?.invoke(item.browseId, true) ?: navController.navigate(
                                                PlaylistDestination(
                                                    item.browseId,
                                                    isYourYouTubePlaylist = true,
                                                ),
                                            )
                                        }
                                    }

                                    is AlbumEntity -> {
                                        onAlbumClick?.invoke(item.browseId) ?: navController.navigate(
                                            AlbumDestination(item.browseId),
                                        )
                                    }

                                    is PlaylistEntity -> {
                                        onPlaylistClick?.invoke(item.id, false) ?: navController.navigate(
                                            PlaylistDestination(item.id),
                                        )
                                    }

                                    is PodcastsEntity -> {
                                        navController.navigate(
                                            PodcastDestination(podcastId = item.podcastId),
                                        )
                                    }

                                    is com.sonique.domain.data.entities.ArtistEntity -> {
                                        onArtistClick?.invoke(item.channelId) ?: navController.navigate(
                                            com.sonique.app.ui.navigation.destination.list.ArtistDestination(
                                                channelId = item.channelId,
                                            )
                                        )
                                    }
                                }
                            }

                            var showOptionsDialog by remember { mutableStateOf(false) }
                            var showRenameDialog by remember { mutableStateOf(false) }
                            var showDeleteDialog by remember { mutableStateOf(false) }
                            var renameTitleText by remember { mutableStateOf("") }

                            if (showOptionsDialog && item is LocalPlaylistEntity && item.id != -999L && item.youtubePlaylistId == null) {
                                AlertDialog(
                                    onDismissRequest = { showOptionsDialog = false },
                                    title = { Text(item.title, color = Color.White, style = typo().titleMedium) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = {
                                                    showOptionsDialog = false
                                                    renameTitleText = item.title
                                                    showRenameDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Start,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("Rename Playlist", color = Color.White)
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    showOptionsDialog = false
                                                    showDeleteDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Start,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Red)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("Delete Playlist", color = Color.Red)
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {
                                        TextButton(onClick = { showOptionsDialog = false }) {
                                            Text("Cancel", color = Color.Gray)
                                        }
                                    }
                                )
                            }

                            if (showRenameDialog && item is LocalPlaylistEntity) {
                                AlertDialog(
                                    onDismissRequest = { showRenameDialog = false },
                                    title = { Text("Rename Playlist", color = Color.White, style = typo().titleMedium) },
                                    text = {
                                        OutlinedTextField(
                                            value = renameTitleText,
                                            onValueChange = { renameTitleText = it },
                                            label = { Text("Playlist Title", color = Color.Gray) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.White,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedLabelColor = Color.White,
                                                cursorColor = Color.White,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                if (renameTitleText.isNotBlank()) {
                                                    viewModel.renameLocalPlaylist(item.id, renameTitleText.trim())
                                                    showRenameDialog = false
                                                }
                                            }
                                        ) {
                                            Text("Rename", color = Color.White)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showRenameDialog = false }) {
                                            Text("Cancel", color = Color.Gray)
                                        }
                                    }
                                )
                            }

                            if (showDeleteDialog && item is LocalPlaylistEntity) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("Delete Playlist", color = Color.White, style = typo().titleMedium) },
                                    text = { Text("Are you sure you want to delete '${item.title}'?", color = Color.White) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                viewModel.deleteLocalPlaylist(item.id)
                                                showDeleteDialog = false
                                            }
                                        ) {
                                            Text("Delete", color = Color.Red)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Cancel", color = Color.Gray)
                                        }
                                    }
                                )
                            }

                            Box {
                                HomeGridCardItem(
                                    title = title,
                                    thumbUrl = thumbUrl,
                                    subtitle = subtitle,
                                    isArtist = isArtist,
                                    onClick = onClick,
                                    onLongClick = if (item is LocalPlaylistEntity && item.id != -999L && item.youtubePlaylistId == null) {
                                        { showOptionsDialog = true }
                                    } else null,
                                )
                                if (item is LocalPlaylistEntity && item.id != -999L && item.youtubePlaylistId == null) {
                                    IconButton(
                                        onClick = { showOptionsDialog = true },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            EndOfPage()
                        }
                    }
                    } else {
                        LazyColumn(
                            modifier = Modifier.hazeSource(hazeState).fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding() + 65.dp,
                                bottom = 100.dp,
                            ),
                        ) {
                            items(filteredList) { item ->
                                var showOptionsDialogList by remember { mutableStateOf(false) }
                                var showRenameDialogList by remember { mutableStateOf(false) }
                                var showDeleteDialogList by remember { mutableStateOf(false) }
                                var renameTitleTextList by remember { mutableStateOf("") }

                                if (showOptionsDialogList && item is LocalPlaylistEntity && item.id != -999L && item.youtubePlaylistId == null) {
                                    AlertDialog(
                                        onDismissRequest = { showOptionsDialogList = false },
                                        title = { Text(item.title, color = Color.White, style = typo().titleMedium) },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        showOptionsDialogList = false
                                                        renameTitleTextList = item.title
                                                        showRenameDialogList = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Start,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White)
                                                        Spacer(Modifier.width(12.dp))
                                                        Text("Rename Playlist", color = Color.White)
                                                    }
                                                }
                                                TextButton(
                                                    onClick = {
                                                        showOptionsDialogList = false
                                                        showDeleteDialogList = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Start,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Red)
                                                        Spacer(Modifier.width(12.dp))
                                                        Text("Delete Playlist", color = Color.Red)
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {},
                                        dismissButton = {
                                            TextButton(onClick = { showOptionsDialogList = false }) {
                                                Text("Cancel", color = Color.Gray)
                                            }
                                        }
                                    )
                                }

                                if (showRenameDialogList && item is LocalPlaylistEntity) {
                                    AlertDialog(
                                        onDismissRequest = { showRenameDialogList = false },
                                        title = { Text("Rename Playlist", color = Color.White, style = typo().titleMedium) },
                                        text = {
                                            OutlinedTextField(
                                                value = renameTitleTextList,
                                                onValueChange = { renameTitleTextList = it },
                                                label = { Text("Playlist Title", color = Color.Gray) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.White,
                                                    unfocusedBorderColor = Color.Gray,
                                                    focusedLabelColor = Color.White,
                                                    cursorColor = Color.White,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    if (renameTitleTextList.isNotBlank()) {
                                                        viewModel.renameLocalPlaylist(item.id, renameTitleTextList.trim())
                                                        showRenameDialogList = false
                                                    }
                                                }
                                            ) {
                                                Text("Rename", color = Color.White)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showRenameDialogList = false }) {
                                                Text("Cancel", color = Color.Gray)
                                            }
                                        }
                                    )
                                }

                                if (showDeleteDialogList && item is LocalPlaylistEntity) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteDialogList = false },
                                        title = { Text("Delete Playlist", color = Color.White, style = typo().titleMedium) },
                                        text = { Text("Are you sure you want to delete '${item.title}'?", color = Color.White) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteLocalPlaylist(item.id)
                                                    showDeleteDialogList = false
                                                }
                                            ) {
                                                Text("Delete", color = Color.Red)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteDialogList = false }) {
                                                Text("Cancel", color = Color.Gray)
                                            }
                                        }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (item is PlaylistType) {
                                        PlaylistFullWidthItems(
                                            modifier = Modifier.weight(1f),
                                            onClickListener = {
                                                when (item) {
                                                    is LocalPlaylistEntity -> onLocalPlaylistClick(item.id)
                                                    is PlaylistsResult -> {
                                                        if (item.browseId.startsWith("MPRE")) {
                                                            onAlbumClick?.invoke(item.browseId) ?: navController.navigate(AlbumDestination(item.browseId))
                                                        } else {
                                                            onPlaylistClick?.invoke(item.browseId, true) ?: navController.navigate(PlaylistDestination(item.browseId, isYourYouTubePlaylist = true))
                                                        }
                                                    }
                                                    is AlbumEntity -> onAlbumClick?.invoke(item.browseId) ?: navController.navigate(AlbumDestination(item.browseId))
                                                    is PlaylistEntity -> onPlaylistClick?.invoke(item.id, false) ?: navController.navigate(PlaylistDestination(item.id))
                                                    is PodcastsEntity -> navController.navigate(PodcastDestination(podcastId = item.podcastId))
                                                }
                                            },
                                            data = item,
                                        )
                                    } else if (item is com.sonique.domain.data.entities.ArtistEntity) {
                                        ArtistFullWidthItems(
                                            modifier = Modifier.weight(1f),
                                            data = item,
                                            onClickListener = {
                                                onArtistClick?.invoke(item.channelId) ?: navController.navigate(
                                                    com.sonique.app.ui.navigation.destination.list.ArtistDestination(channelId = item.channelId)
                                                )
                                            }
                                        )
                                    }

                                    if (item is LocalPlaylistEntity && item.id != -999L && item.youtubePlaylistId == null) {
                                        IconButton(onClick = { showOptionsDialogList = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "Options",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            item { EndOfPage() }
                        }
                    }
                } else if (data is LocalResource.Loading) {
                    CenterLoadingBox(
                        Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(emptyText),
                            style = typo().bodySmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = displayTitle ?: "",
                        style = typography.titleMedium,
                    )
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(start = 5.dp, end = 4.dp)) {
                            RippleIconButton(
                                Res.drawable.baseline_arrow_back_ios_new_24,
                                Modifier.size(32.dp),
                                true,
                            ) {
                                onBack?.invoke() ?: navController.navigateUp()
                            }
                        }
                        actions?.invoke()
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLayoutView() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle Layout",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            if (showSearchBar) Res.drawable.baseline_close_24 else Res.drawable.baseline_search_24,
                            Modifier.size(32.dp),
                            true,
                        ) {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) query = ""
                        }
                    }
                },
                modifier =
                    Modifier.hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
            androidx.compose.animation.AnimatedVisibility(visible = showSearchBar) {
                SearchBar(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(horizontal = 12.dp),
                    inputField = {
                        CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                            SearchBarDefaults.InputField(
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = { showSearchBar = false },
                                expanded = showSearchBar,
                                onExpandedChange = { showSearchBar = it },
                                placeholder = {
                                    Text(
                                        stringResource(Res.string.search),
                                        style = typo().bodySmall,
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            )
                        }
                    },
                    expanded = false,
                    onExpandedChange = {},
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGridCardItem(
    title: String,
    thumbUrl: String?,
    subtitle: String?,
    isArtist: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            val isDownloadedTile = title.contains("Downloaded", ignoreCase = true) ||
                title.equals("Downloads", ignoreCase = true) ||
                thumbUrl == "sonique://downloaded_songs_thumbnail"
            val placeholderPainter = if (isDownloadedTile) {
                painterDownloadedSongsThumbnail(
                    title = title,
                    style = typo().bodySmall,
                    sizeDp = 120.dp to 120.dp,
                )
            } else {
                painterPlaylistThumbnail(
                    title = title,
                    style = typo().bodySmall,
                    sizeDp = 120.dp to 120.dp,
                )
            }

            if (!thumbUrl.isNullOrEmpty() && !isDownloadedTile) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(thumbUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(550)
                        .build(),
                    placeholder = placeholderPainter,
                    error = placeholderPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painter = placeholderPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = title,
            style = typo().bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                style = typo().bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = typo()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBack() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.baseline_arrow_back_ios_new_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
        Text(
            text = title,
            style = typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
