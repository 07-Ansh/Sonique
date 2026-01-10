package com.sonique.app.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.Logger
import com.sonique.app.extension.copy
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.GridLibraryPlaylist
import com.sonique.app.ui.component.LibraryItem
import com.sonique.app.ui.component.LibraryItemState
import com.sonique.app.ui.component.LibraryItemType
import com.sonique.app.ui.component.LibraryTilingBox
import com.sonique.app.ui.component.RippleIconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
    openDownloads: Boolean = false,
) {
    LaunchedEffect(openDownloads) {
        if (openDownloads) {
            viewModel.setCurrentScreen(LibraryChipType.DOWNLOADED_PLAYLIST)
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

    var showAddSheet by remember { mutableStateOf(false) }
    var isScrollingUp by remember { mutableStateOf(true) }

     
    LaunchedEffect(Unit) {
         
    }

    LaunchedEffect(nowPlaying) {
        Logger.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    val currentFilter by viewModel.currentScreen.collectAsStateWithLifecycle()

    LaunchedEffect(currentFilter) {
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

            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getCanvasSong()
                viewModel.getRecentlyAdded()
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

     
    val handleScrolling: (Boolean) -> Unit = { scrollingUp ->
        isScrollingUp = scrollingUp
        onScrolling(scrollingUp)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = currentFilter,
        ) { filter ->
            when (filter) {
                LibraryChipType.YOUR_LIBRARY -> {
                    val state = rememberLazyListState()
                    val scrollingUp by state.isScrollingUp()
                    LaunchedEffect(state) {
                        snapshotFlow { state.firstVisibleItemIndex }
                            .collect { index ->
                                val isAtTop = index <= 1
                                val shouldBeVisible = if (isAtTop) true else scrollingUp
                                handleScrolling(shouldBeVisible)
                            }
                    }
                    LazyColumn(
                        contentPadding =
                            innerPadding.copy(
                                top = 25.dp,
                            ),
                        state = state,
                    ) {
                        item {
                            LibraryTilingBox(
                                navController = navController,
                                onNavigate = { type ->
                                    viewModel.setCurrentScreen(type)
                                }
                            )
                        }

                        if (!listCanvasSong.data.isNullOrEmpty()) {
                            item {
                                LibraryItem(
                                    state =
                                        LibraryItemState(
                                            type = LibraryItemType.CanvasSong,
                                            data = listCanvasSong.data ?: emptyList(),
                                            isLoading = listCanvasSong is LocalResource.Loading,
                                        ),
                                    navController = navController,
                                )
                            }
                        }


                        if (activeDownloads > 0) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF242424)  
                                    ),
                                    onClick = {
                                        viewModel.setCurrentScreen(LibraryChipType.DOWNLOADED_PLAYLIST)
                                    },
                                    shape = RoundedCornerShape(12.dp)
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
                            )
                        }
                        item {
                            EndOfPage()
                        }
                    }
                }

                LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                    GridLibraryPlaylist(
                        navController,
                        innerPadding.copy(top = 30.dp),
                        youTubePlaylist,
                        emptyText = Res.string.no_YouTube_playlists,
                        title = Res.string.your_youtube_playlists,
                        onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                        onScrolling = handleScrolling,
                    ) {
                        viewModel.getYouTubePlaylist()
                    }
                }

                LibraryChipType.YOUTUBE_MIX_FOR_YOU -> {
                    GridLibraryPlaylist(
                        navController,
                        innerPadding.copy(top = 30.dp),
                        youTubeMixForYou,
                        emptyText = Res.string.no_mixes_found,
                        title = Res.string.mix_for_you,
                        onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                        onScrolling = handleScrolling,
                    ) {
                        viewModel.getYouTubeMixedForYou()
                    }
                }

                LibraryChipType.LOCAL_PLAYLIST -> {
                    GridLibraryPlaylist(
                        navController,
                        innerPadding.copy(top = 30.dp),
                        yourLocalPlaylist,
                        onScrolling = handleScrolling,
                        emptyText = Res.string.no_playlists_added,
                        title = Res.string.your_playlists,
                        onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                        createNewPlaylist = {
                            showAddSheet = true
                        },
                    ) {
                        viewModel.getLocalPlaylist()
                    }
                }

                LibraryChipType.FAVORITE_PLAYLIST -> {
                    FavoriteCompositeScreen(
                        navController = navController,
                        contentPadding = innerPadding.copy(top = 30.dp),
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
                        innerPadding.copy(top = 30.dp),
                        downloadedPlaylist,
                        emptyText = Res.string.no_playlists_downloaded,
                        title = Res.string.downloaded_playlists,
                        onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                        onScrolling = handleScrolling,
                    ) {
                        viewModel.getDownloadedPlaylist()
                    }
                }

                LibraryChipType.FAVORITE_PODCAST -> {
                    GridLibraryPlaylist(
                        navController,
                        innerPadding.copy(top = 30.dp),
                        favoritePodcasts,
                        emptyText = Res.string.no_favorite_podcasts,
                        title = Res.string.favorite_podcasts,
                        onBack = { viewModel.setCurrentScreen(LibraryChipType.YOUR_LIBRARY) },
                        onScrolling = handleScrolling,
                    ) {
                        viewModel.getFavoritePodcasts()
                    }
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
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
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

