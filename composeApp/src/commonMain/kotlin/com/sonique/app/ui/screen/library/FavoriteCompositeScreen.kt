package com.sonique.app.ui.screen.library

import androidx.compose.foundation.layout.*
import com.sonique.app.ui.screen.library.LibraryDynamicPlaylistType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.domain.data.entities.PlaylistEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.type.PlaylistType
import com.sonique.domain.utils.LocalResource
import com.sonique.domain.utils.toTrack
import com.sonique.app.extension.copy
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.PlaylistFullWidthItems
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.component.SongFullWidthItems
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.LibraryDynamicPlaylistViewModel
import com.sonique.app.viewModel.LibraryViewModel
import com.sonique.app.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.favorite_playlists
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_search_24
import sonique.composeapp.generated.resources.liked
import sonique.composeapp.generated.resources.search


@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoriteCompositeScreen(
    navController: NavController,
    contentPadding: PaddingValues,
    favoritePlaylistData: LocalResource<List<PlaylistType>>,
    onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    onBack: () -> Unit,
    onReload: () -> Unit,
    libraryViewModel: LibraryViewModel = koinViewModel(),
    dynamicPlaylistViewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
) {
    val state = rememberLazyListState()
    val isScrollingUp by state.isScrollingUp()
    val nowPlayingVideoId by dynamicPlaylistViewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val favoriteSongs by dynamicPlaylistViewModel.listFavoriteSong.collectAsStateWithLifecycle()
    val sharedViewModel: SharedViewModel = koinInject()
    val typography = typo()
    val likedString = stringResource(Res.string.liked)
    val favoritePlaylistsString = stringResource(Res.string.favorite_playlists)

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val hazeState = rememberHazeState(blurEnabled = true)

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = onReload,
            isRefreshing = favoritePlaylistData is LocalResource.Loading,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = favoritePlaylistData is LocalResource.Loading,
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
            val filteredSongs = remember(favoriteSongs, query, showSearchBar) {
                if (query.isNotEmpty() && showSearchBar) {
                    favoriteSongs.filter { it.title.contains(query, ignoreCase = true) }
                } else {
                    favoriteSongs
                }
            }
            val playlistList = (favoritePlaylistData as? LocalResource.Success)?.data ?: emptyList()
            val filteredPlaylists = remember(playlistList, query, showSearchBar) {
                if (query.isNotEmpty() && showSearchBar) {
                    playlistList.filter { item ->
                        val itemTitle = when (item) {
                            is PlaylistEntity -> item.title
                            else -> ""
                        }
                        itemTitle.contains(query, ignoreCase = true)
                    }
                } else {
                    playlistList
                }
            }

            LazyColumn(
                modifier = Modifier.hazeSource(hazeState),
                contentPadding = contentPadding.copy(top = contentPadding.calculateTopPadding() + 25.dp),
                state = state,
            ) {
                item {
                    Spacer(Modifier.height(64.dp))
                }
                item {
                    androidx.compose.animation.AnimatedVisibility(showSearchBar) {
                        Spacer(Modifier.height(55.dp))
                    }
                }

                 
                if (filteredSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Liked Songs",
                            style = typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    items(
                        filteredSongs,
                        key = { it.hashCode() },
                    ) { song ->
                        SongFullWidthItems(
                            songEntity = song,
                            isPlaying = song.videoId == nowPlayingVideoId,
                            modifier = Modifier.fillMaxWidth(),
                            onMoreClickListener = {
                                 
                            },
                            onClickListener = { videoId ->
                                dynamicPlaylistViewModel.playSong(videoId, type = LibraryDynamicPlaylistType.Favorite)
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(song.toTrack()),
                                )
                            },
                        )
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }

                 
                if (filteredPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = favoritePlaylistsString,
                            style = typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    items(filteredPlaylists) { item ->
                        PlaylistFullWidthItems(
                            onClickListener = {
                                when (item) {
                                    is PlaylistEntity -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                item.id,
                                            ),
                                        )
                                    }
                                    else -> {}
                                }
                            },
                            data = item,
                        )
                    }
                }

                item {
                    EndOfPage()
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = likedString,
                        style = typography.titleMedium,
                    )
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            Res.drawable.baseline_arrow_back_ios_new_24,
                            Modifier.size(32.dp),
                            true,
                        ) {
                            onBack()
                        }
                    }
                },
                actions = {
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



