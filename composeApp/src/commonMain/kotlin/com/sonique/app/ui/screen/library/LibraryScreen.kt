package com.sonique.app.ui.screen.library

import androidx.compose.foundation.focusable
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import com.sonique.common.Config
import com.sonique.app.extension.NonLazyGrid
import com.sonique.app.ui.component.HomeGridCardItem
import com.sonique.app.ui.component.PlaylistFullWidthItems
import com.sonique.common.LOCAL_PLAYLIST_ID_DOWNLOADED
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import com.sonique.app.ui.theme.backgroundCard
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.common.LibraryChipType
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.Logger
import com.sonique.app.extension.copy
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.expect.ui.BackHandler
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.GridLibraryPlaylist
import com.sonique.app.ui.component.LibraryItem
import com.sonique.app.ui.component.LibraryItemState
import com.sonique.app.ui.component.LibraryItemType
import com.sonique.app.ui.component.LibraryTilingBox
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.screen.other.AlbumScreen
import com.sonique.app.ui.screen.other.ArtistScreen
import com.sonique.app.ui.screen.other.PlaylistScreen
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.transparent
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.LibraryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.create
import sonique.composeapp.generated.resources.cancel
import sonique.composeapp.generated.resources.downloaded_playlists
import sonique.composeapp.generated.resources.favorite_playlists
import sonique.composeapp.generated.resources.favorite_podcasts
import sonique.composeapp.generated.resources.library
import sonique.composeapp.generated.resources.mix_for_you
import sonique.composeapp.generated.resources.no_YouTube_playlists
import sonique.composeapp.generated.resources.no_favorite_playlists
import sonique.composeapp.generated.resources.no_favorite_podcasts
import sonique.composeapp.generated.resources.no_mixes_found
import sonique.composeapp.generated.resources.no_playlists_added
import sonique.composeapp.generated.resources.no_playlists_downloaded
import sonique.composeapp.generated.resources.playlist_name
import sonique.composeapp.generated.resources.playlist_name_cannot_be_empty
import sonique.composeapp.generated.resources.your_playlists
import sonique.composeapp.generated.resources.your_youtube_playlists
import sonique.composeapp.generated.resources.youtube_albums
import sonique.composeapp.generated.resources.no_youtube_albums
import sonique.composeapp.generated.resources.followed
import sonique.composeapp.generated.resources.no_artist_found

enum class LibrarySubScreen {
    MAIN,
    DYNAMIC_PLAYLIST,
    LOCAL_PLAYLIST_DETAILS,
    ALBUM_DETAILS,
    ARTIST_DETAILS,
    PLAYLIST_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
    openDownloads: Boolean = false,
    initialChip: LibraryChipType? = null,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val showMostPlayed by sharedViewModel.showMostPlayed.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    var activeSubScreen by rememberSaveable { mutableStateOf(LibrarySubScreen.MAIN) }
    var activeDynamicType by rememberSaveable { mutableStateOf("") }
    var activeLocalPlaylistId by rememberSaveable { mutableStateOf(-1L) }
    var activeBrowseId by rememberSaveable { mutableStateOf("") }
    var activeChannelId by rememberSaveable { mutableStateOf("") }
    var activePlaylistId by rememberSaveable { mutableStateOf("") }
    var activeIsYourYouTubePlaylist by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openDownloads, initialChip) {
        if (openDownloads) {
            viewModel.setCurrentScreen(LibraryChipType.DOWNLOADED_PLAYLIST)
        } else if (initialChip != null) {
            viewModel.setCurrentScreen(initialChip)
        }
    }
    val density = LocalDensity.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val youTubeMixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val youTubeAlbums by viewModel.youTubeAlbums.collectAsStateWithLifecycle()
    val followedArtists by viewModel.followedArtists.collectAsStateWithLifecycle()
    val pinnedItems by viewModel.pinnedItems.collectAsStateWithLifecycle()
    val isPinnedGridView by viewModel.isPinnedGridView.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var isScrollingUp by remember { mutableStateOf(true) }

    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()

    LaunchedEffect(currentFilter, loggedIn) {
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                if (youTubePlaylist.data.isNullOrEmpty()) {
                    viewModel.getYouTubePlaylist()
                }
            }

            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                if (youTubeMixForYou.data.isNullOrEmpty()) {
                    viewModel.getYouTubeMixedForYou()
                }
            }

            LibraryChipType.FOLLOWED_ARTISTS -> {
                if (loggedIn) {
                    viewModel.syncFollowedArtists()
                }
            }

            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getCanvasSong()
                viewModel.getLocalPlaylist()
                viewModel.getYouTubePlaylist()
                if (loggedIn) {
                    viewModel.syncFollowedArtists()
                }
            }

            LibraryChipType.YOUTUBE_ALBUMS -> {
                if (youTubeAlbums.data.isNullOrEmpty()) {
                    viewModel.getYouTubeAlbums()
                }
            }

            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }

            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }

            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }

            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }
        }
    }

    BackHandler(enabled = activeSubScreen != LibrarySubScreen.MAIN || currentFilter != LibraryChipType.YOUR_LIBRARY) {
        if (activeSubScreen != LibrarySubScreen.MAIN) {
            activeSubScreen = LibrarySubScreen.MAIN
        } else {
            viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY)
        }
    }

    val handleScrolling: (Boolean) -> Unit = { scrollingUp ->
        isScrollingUp = scrollingUp
        onScrolling(scrollingUp)
    }

    Box(
        modifier = Modifier.fillMaxSize().then(
            when (activeSubScreen) {
                LibrarySubScreen.PLAYLIST_DETAILS,
                LibrarySubScreen.ALBUM_DETAILS,
                LibrarySubScreen.ARTIST_DETAILS,
                LibrarySubScreen.LOCAL_PLAYLIST_DETAILS -> Modifier
                else -> Modifier.windowInsetsPadding(WindowInsets.statusBars)
            }
        )
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = activeSubScreen,
        ) { subScreen ->
            when (subScreen) {
                LibrarySubScreen.MAIN -> {
                    Crossfade(
                        modifier = Modifier.fillMaxSize(),
                        targetState = currentFilter,
                    ) { filter ->
                        when (filter) {
                            LibraryChipType.YOUR_LIBRARY -> {
                                val state = rememberLazyListState()
                                LaunchedEffect(state) {
                                    snapshotFlow { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 }
                                        .collect { isAtTop ->
                                            handleScrolling(isAtTop)
                                        }
                                }
                                LazyColumn(
                                    contentPadding =
                                        innerPadding.copy(
                                            top = 12.dp,
                                        ),
                                    state = state,
                                ) {
                                    if (showMostPlayed && !listCanvasSong.data.isNullOrEmpty()) {
                                        item {
                                            LibraryItem(
                                                state =
                                                    LibraryItemState(
                                                        type = LibraryItemType.CanvasSong,
                                                        data = listCanvasSong.data ?: emptyList(),
                                                        isLoading = listCanvasSong is LocalResource.Loading,
                                                    ),
                                                navController = navController,
                                                onLocalPlaylistClick = { id ->
                                                    activeLocalPlaylistId = id
                                                    activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS
                                                },
                                                onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                                onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                                onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                            )
                                        }
                                    }

                                    if (pinnedItems.isNotEmpty()) {
                                        item {
                                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                Row(
                                                    modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(
                                                        text = "Quick Access",
                                                        style = typo().titleMedium,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        modifier = Modifier.fillMaxWidth().height(35.dp)
                                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                                            .weight(1f).focusable(),
                                                    )
                                                    IconButton(onClick = { viewModel.togglePinnedLayoutView() }) {
                                                        Icon(
                                                            imageVector = if (isPinnedGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                                            contentDescription = "Toggle Quick Access Layout",
                                                            tint = Color.White,
                                                        )
                                                    }
                                                }
                                                if (isPinnedGridView) {
                                                    NonLazyGrid(
                                                        columns = 3,
                                                        itemCount = pinnedItems.size,
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                                    ) { index ->
                                                        val pinned = pinnedItems[index]
                                                        HomeGridCardItem(
                                                            title = pinned.title,
                                                            thumbUrl = pinned.thumbnails,
                                                            subtitle = pinned.author,
                                                            isArtist = false,
                                                            onClick = {
                                                                when (pinned.id) {
                                                                    Config.PIN_YT_PLAYLISTS -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_MUSIC_PLAYLIST)
                                                                    Config.PIN_YT_ALBUMS -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_ALBUMS)
                                                                    Config.PIN_YT_MIX -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_MIX_FOR_YOU)
                                                                    "LM" -> {
                                                                        activePlaylistId = "LM"
                                                                        activeIsYourYouTubePlaylist = false
                                                                        activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS
                                                                    }
                                                                    else -> {
                                                                        activePlaylistId = pinned.id
                                                                        activeIsYourYouTubePlaylist = false
                                                                        activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                                } else {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        pinnedItems.forEach { pinned ->
                                                            PlaylistFullWidthItems(
                                                                data = pinned,
                                                                onClickListener = {
                                                                    when (pinned.id) {
                                                                        Config.PIN_YT_PLAYLISTS -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_MUSIC_PLAYLIST)
                                                                        Config.PIN_YT_ALBUMS -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_ALBUMS)
                                                                        Config.PIN_YT_MIX -> viewModel.setCurrentScreen(LibraryChipType.YOUTUBE_MIX_FOR_YOU)
                                                                        "LM" -> {
                                                                            activePlaylistId = "LM"
                                                                            activeIsYourYouTubePlaylist = false
                                                                            activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS
                                                                        }
                                                                        else -> {
                                                                            activePlaylistId = pinned.id
                                                                            activeIsYourYouTubePlaylist = false
                                                                            activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS
                                                                        }
                                                                    }
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                         var selectedPlaylistTab by rememberSaveable { mutableStateOf(0) }
                                         val currentPlaylistData: LocalResource<List<com.sonique.domain.data.type.PlaylistType>> = if (selectedPlaylistTab == 0) {
                                             (yourLocalPlaylist as? LocalResource.Success)?.let { success ->
                                                 LocalResource.Success(success.data?.map { p -> p as com.sonique.domain.data.type.PlaylistType } ?: emptyList())
                                             } ?: LocalResource.Loading()
                                         } else {
                                             (youTubePlaylist as? LocalResource.Success)?.let { success ->
                                                 LocalResource.Success(success.data?.map { p -> p as com.sonique.domain.data.type.PlaylistType } ?: emptyList())
                                             } ?: LocalResource.Loading()
                                         }

                                         var showCreateDialogTab by remember { mutableStateOf(false) }
                                         var newPlaylistTitleTab by remember { mutableStateOf("") }

                                         if (showCreateDialogTab) {
                                             androidx.compose.material3.AlertDialog(
                                                 onDismissRequest = {
                                                     showCreateDialogTab = false
                                                     newPlaylistTitleTab = ""
                                                 },
                                                 title = { Text("New Playlist", style = typo().titleMedium, color = Color.White) },
                                                 text = {
                                                     androidx.compose.material3.OutlinedTextField(
                                                         value = newPlaylistTitleTab,
                                                         onValueChange = { newPlaylistTitleTab = it },
                                                         label = { Text("Playlist Title", color = Color.Gray) },
                                                         singleLine = true,
                                                         colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                                                     androidx.compose.material3.TextButton(
                                                         onClick = {
                                                             if (newPlaylistTitleTab.isNotBlank()) {
                                                                 viewModel.createPlaylist(newPlaylistTitleTab.trim())
                                                                 showCreateDialogTab = false
                                                                 newPlaylistTitleTab = ""
                                                             }
                                                         }
                                                     ) { Text("Create", color = Color.White) }
                                                 },
                                                 dismissButton = {
                                                     androidx.compose.material3.TextButton(
                                                         onClick = {
                                                             showCreateDialogTab = false
                                                             newPlaylistTitleTab = ""
                                                         }
                                                     ) { Text("Cancel", color = Color.Gray) }
                                                 }
                                             )
                                         }

                                         Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = "Playlists",
                                                     style = typo().titleMedium,
                                                     color = Color.White
                                                 )
                                                 Row(
                                                     horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     androidx.compose.material3.FilterChip(
                                                         selected = selectedPlaylistTab == 0,
                                                         onClick = { selectedPlaylistTab = 0 },
                                                         label = { Text("Local", style = typo().labelMedium) },
                                                         leadingIcon = if (selectedPlaylistTab == 0) {
                                                             { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                         } else null
                                                     )
                                                     androidx.compose.material3.FilterChip(
                                                         selected = selectedPlaylistTab == 1,
                                                         onClick = { selectedPlaylistTab = 1 },
                                                         label = { Text("YouTube", style = typo().labelMedium) },
                                                         leadingIcon = if (selectedPlaylistTab == 1) {
                                                             { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                         } else null
                                                     )
                                                 }
                                             }
                                             if (selectedPlaylistTab == 0) {
                                                 androidx.compose.material3.Card(
                                                     onClick = { showCreateDialogTab = true },
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .padding(horizontal = 20.dp, vertical = 4.dp),
                                                     shape = MaterialTheme.shapes.large,
                                                     colors = androidx.compose.material3.CardDefaults.cardColors(
                                                         containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                                     )
                                                 ) {
                                                     Row(
                                                         modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                     ) {
                                                         Icon(
                                                             Icons.Rounded.Add,
                                                             contentDescription = "New Playlist",
                                                             tint = MaterialTheme.colorScheme.primary
                                                         )
                                                         Text(
                                                             text = "New Playlist",
                                                             style = MaterialTheme.typography.bodyLarge,
                                                             color = MaterialTheme.colorScheme.onSurface
                                                         )
                                                     }
                                                 }
                                             }

                                             LibraryItem(
                                                 state = LibraryItemState(
                                                     type = if (selectedPlaylistTab == 0) {
                                                         LibraryItemType.YourLocalPlaylist
                                                     } else {
                                                         LibraryItemType.YouTubePlaylist(isLoggedIn = loggedIn)
                                                     },
                                                     data = (currentPlaylistData as? LocalResource.Success)?.data ?: emptyList(),
                                                     isLoading = currentPlaylistData is LocalResource.Loading,
                                                 ),
                                                 navController = navController,
                                                 onLocalPlaylistClick = { id ->
                                                     if (id == -999L) {
                                                         activePlaylistId = LOCAL_PLAYLIST_ID_DOWNLOADED
                                                         activeIsYourYouTubePlaylist = false
                                                         activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS
                                                     } else {
                                                         activeLocalPlaylistId = id
                                                         activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS
                                                     }
                                                 },
                                                 onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                                 onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                                 onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                             )
                                         }
                                    }

                                    if (activeDownloads > 0) {
                                        item {
                                            val cardModifier = if (enableLiquidGlass) {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                                    .clip(RoundedCornerShape(28.dp))
                                                    .background(Color.White.copy(alpha = 0.06f))
                                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(28.dp))
                                                    .liquidGlass(backdrop, shape = RoundedCornerShape(28.dp), interactive = true)
                                            } else {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                            }
                                            Card(
                                                modifier = cardModifier,
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (enableLiquidGlass) Color.Transparent else backgroundCard
                                                ),
                                                onClick = {
                                                    viewModel.setCurrentScreen(LibraryChipType.DOWNLOADED_PLAYLIST)
                                                },
                                                shape = if (enableLiquidGlass) RoundedCornerShape(28.dp) else RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = "Downloading $activeDownloads items...",
                                                        style = typo().bodyMedium,
                                                        color = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.cancelActiveDownloads()
                                                        }
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.cancel),
                                                            style = typo().bodyMedium,
                                                            color = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        LibraryItem(
                                            state =
                                                LibraryItemState(
                                                    type =
                                                        LibraryItemType.RecentlyAdded(
                                                            playingVideoId = nowPlaying,
                                                        ),
                                                    data = recentlyAdded.data ?: emptyList(),
                                                    isLoading = recentlyAdded is LocalResource.Loading,
                                                ),
                                            navController = navController,
                                            onLocalPlaylistClick = { id ->
                                                activeLocalPlaylistId = id
                                                activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS
                                            },
                                            onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                            onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                            onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                        )
                                    }
                                    item {
                                        EndOfPage()
                                    }
                                }
                            }

                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                                var selectedPlaylistTab by rememberSaveable { mutableStateOf(0) }
                                val currentPlaylistData: LocalResource<List<com.sonique.domain.data.type.PlaylistType>> = if (selectedPlaylistTab == 0) {
                                    (youTubePlaylist as? LocalResource.Success)?.let { success ->
                                        LocalResource.Success(success.data?.map { p -> p as com.sonique.domain.data.type.PlaylistType } ?: emptyList())
                                    } ?: LocalResource.Loading()
                                } else {
                                    (yourLocalPlaylist as? LocalResource.Success)?.let { success ->
                                        LocalResource.Success(success.data?.map { p -> p as com.sonique.domain.data.type.PlaylistType } ?: emptyList())
                                    } ?: LocalResource.Loading()
                                }

                                var showCreateDialogTab by remember { mutableStateOf(false) }
                                var newPlaylistTitleTab by remember { mutableStateOf("") }

                                if (showCreateDialogTab) {
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = {
                                            showCreateDialogTab = false
                                            newPlaylistTitleTab = ""
                                        },
                                        title = { Text("New Playlist", style = typo().titleMedium, color = Color.White) },
                                        text = {
                                            androidx.compose.material3.OutlinedTextField(
                                                value = newPlaylistTitleTab,
                                                onValueChange = { newPlaylistTitleTab = it },
                                                label = { Text("Playlist Title", color = Color.Gray) },
                                                singleLine = true,
                                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                                            androidx.compose.material3.TextButton(
                                                onClick = {
                                                    if (newPlaylistTitleTab.isNotBlank()) {
                                                        viewModel.createPlaylist(newPlaylistTitleTab.trim())
                                                        showCreateDialogTab = false
                                                        newPlaylistTitleTab = ""
                                                    }
                                                }
                                            ) { Text("Create", color = Color.White) }
                                        },
                                        dismissButton = {
                                            androidx.compose.material3.TextButton(
                                                onClick = {
                                                    showCreateDialogTab = false
                                                    newPlaylistTitleTab = ""
                                                }
                                            ) { Text("Cancel", color = Color.Gray) }
                                        }
                                    )
                                }

                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    currentPlaylistData,
                                    emptyText = if (selectedPlaylistTab == 0) Res.string.no_YouTube_playlists else Res.string.no_playlists_added,
                                    title = null,
                                    createNewPlaylist = if (selectedPlaylistTab == 1) { { showCreateDialogTab = true } } else null,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onScrolling = handleScrolling,
                                    onLocalPlaylistClick = { id -> activeLocalPlaylistId = id; activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS },
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS },
                                    actions = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.FilterChip(
                                                selected = selectedPlaylistTab == 0,
                                                onClick = { selectedPlaylistTab = 0 },
                                                label = { Text("YouTube Playlists", style = typo().labelMedium) },
                                                leadingIcon = if (selectedPlaylistTab == 0) {
                                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                } else null
                                            )
                                            androidx.compose.material3.FilterChip(
                                                selected = selectedPlaylistTab == 1,
                                                onClick = { selectedPlaylistTab = 1 },
                                                label = { Text("Your Local Playlists", style = typo().labelMedium) },
                                                leadingIcon = if (selectedPlaylistTab == 1) {
                                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                ) {
                                    if (selectedPlaylistTab == 0) {
                                        viewModel.getYouTubePlaylist()
                                    } else {
                                        viewModel.getCanvasSong()
                                    }
                                }
                            }

                            LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    youTubeMixForYou,
                                    emptyText = Res.string.no_mixes_found,
                                    title = Res.string.mix_for_you,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onScrolling = handleScrolling,
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.getYouTubeMixedForYou()
                                }
                            }

                            LibraryChipType.LOCAL_PLAYLIST -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    yourLocalPlaylist,
                                    onScrolling = handleScrolling,
                                    emptyText = Res.string.no_playlists_added,
                                    title = Res.string.your_playlists,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    createNewPlaylist = {
                                        showAddSheet = true
                                    },
                                    onLocalPlaylistClick = { id ->
                                        activeLocalPlaylistId = id
                                        activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS
                                    },
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.getLocalPlaylist()
                                }
                            }

                            LibraryChipType.FOLLOWED_ARTISTS -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    followedArtists,
                                    onScrolling = handleScrolling,
                                    emptyText = Res.string.no_artist_found,
                                    title = Res.string.followed,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.syncFollowedArtists()
                                }
                            }

                            LibraryChipType.FAVORITE_PLAYLIST -> {
                                FavoriteCompositeScreen(
                                    navController = navController,
                                    contentPadding = innerPadding.copy(top = 24.dp),
                                    favoritePlaylistData = favoritePlaylist,
                                    onScrolling = handleScrolling,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onReload = {
                                        viewModel.getPlaylistFavorite()
                                    },
                                )
                            }

                            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    downloadedPlaylist,
                                    emptyText = Res.string.no_playlists_downloaded,
                                    title = Res.string.downloaded_playlists,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onScrolling = handleScrolling,
                                    onLocalPlaylistClick = { id ->
                                        activeLocalPlaylistId = id
                                        activeSubScreen = LibrarySubScreen.LOCAL_PLAYLIST_DETAILS
                                    },
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.getDownloadedPlaylist()
                                }
                            }

                            LibraryChipType.FAVORITE_PODCAST -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    favoritePodcasts,
                                    emptyText = Res.string.no_favorite_podcasts,
                                    title = Res.string.favorite_podcasts,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onScrolling = handleScrolling,
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.getFavoritePodcasts()
                                }
                            }

                            LibraryChipType.YOUTUBE_ALBUMS -> {
                                GridLibraryPlaylist(
                                    navController,
                                    innerPadding.copy(top = 24.dp),
                                    youTubeAlbums,
                                    emptyText = Res.string.no_youtube_albums,
                                    title = Res.string.youtube_albums,
                                    onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                                    onScrolling = handleScrolling,
                                    onAlbumClick = { id -> activeBrowseId = id; activeSubScreen = LibrarySubScreen.ALBUM_DETAILS },
                                    onArtistClick = { id -> activeChannelId = id; activeSubScreen = LibrarySubScreen.ARTIST_DETAILS },
                                    onPlaylistClick = { id, isYt -> activePlaylistId = id; activeIsYourYouTubePlaylist = isYt; activeSubScreen = LibrarySubScreen.PLAYLIST_DETAILS }
                                ) {
                                    viewModel.getYouTubeAlbums()
                                }
                            }
                        }
                    }
                }

                LibrarySubScreen.DYNAMIC_PLAYLIST -> {
                    LibraryDynamicPlaylistScreen(
                        innerPadding = innerPadding,
                        navController = navController,
                        type = activeDynamicType,
                        onScrolling = handleScrolling,
                        onBack = { activeSubScreen = LibrarySubScreen.MAIN }
                    )
                }

                LibrarySubScreen.LOCAL_PLAYLIST_DETAILS -> {
                    LocalPlaylistScreen(
                        id = activeLocalPlaylistId,
                        navController = navController,
                        onScrolling = handleScrolling,
                        onBack = { activeSubScreen = LibrarySubScreen.MAIN }
                    )
                }

                LibrarySubScreen.ALBUM_DETAILS -> {
                    AlbumScreen(
                        browseId = activeBrowseId,
                        navController = navController,
                        onScrolling = handleScrolling,
                        onBack = { activeSubScreen = LibrarySubScreen.MAIN }
                    )
                }

                LibrarySubScreen.ARTIST_DETAILS -> {
                    ArtistScreen(
                        channelId = activeChannelId,
                        navController = navController,
                        onScrolling = handleScrolling,
                        onBack = { activeSubScreen = LibrarySubScreen.MAIN }
                    )
                }

                LibrarySubScreen.PLAYLIST_DETAILS -> {
                    PlaylistScreen(
                        playlistId = activePlaylistId,
                        isYourYouTubePlaylist = activeIsYourYouTubePlaylist,
                        navController = navController,
                        onScrolling = handleScrolling,
                        onBack = { activeSubScreen = LibrarySubScreen.MAIN }
                    )
                }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showAddSheetState.hide()
                    showAddSheet = false
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = md_theme_dark_background.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = backgroundCard),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.playlist_name))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                viewModel.makeToast(runBlocking { getString(Res.string.playlist_name_cannot_be_empty) })
                            } else {
                                viewModel.createPlaylist(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.create))
                    }
                }
            }
        }
    }
}
