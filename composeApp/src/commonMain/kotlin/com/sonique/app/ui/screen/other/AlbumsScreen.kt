package com.sonique.app.ui.screen.other

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HomeGridCardItem
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.AlbumsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: AlbumsViewModel = koinViewModel(),
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val albumsForYou by viewModel.albumsForYou.collectAsStateWithLifecycle()
    val playlistsForYou by viewModel.playlistsForYou.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val isScrollingUp by gridState.isScrollingUp()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                onScrolling(isAtTop)
            }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = { viewModel.fetchAlbumsData(forceRefresh = true) },
            isRefreshing = isLoading,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isLoading,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                )
            },
        ) {
            if (isLoading && albumsForYou.isEmpty() && playlistsForYou.isEmpty()) {
                CenterLoadingBox(modifier = Modifier.fillMaxSize())
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (albumsForYou.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = "Albums for you",
                                style = typo().titleMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        items(
                            items = albumsForYou,
                            key = { it.browseId ?: it.playlistId ?: it.album?.id ?: it.title.hashCode() }
                        ) { content ->
                            val browseId = content.browseId ?: content.playlistId ?: content.album?.id ?: ""
                            val title = content.title
                            val thumbUrl = content.thumbnails.lastOrNull()?.url
                            val artistName = content.artists?.firstOrNull()?.name ?: "Album"

                            HomeGridCardItem(
                                title = title,
                                thumbUrl = thumbUrl,
                                subtitle = artistName,
                                onClick = {
                                    if (browseId.isNotEmpty()) {
                                        navController.navigate(AlbumDestination(browseId))
                                    }
                                }
                            )
                        }
                    }
                    if (playlistsForYou.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = "Playlists for you",
                                style = typo().titleMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(
                            items = playlistsForYou,
                            key = { it.playlistId ?: it.browseId ?: it.title.hashCode() }
                        ) { content ->
                            val playlistId = content.playlistId ?: content.browseId ?: ""
                            val title = content.title
                            val thumbUrl = content.thumbnails.lastOrNull()?.url
                            val subtitle = content.artists?.firstOrNull()?.name ?: "Playlist"

                            HomeGridCardItem(
                                title = title,
                                thumbUrl = thumbUrl,
                                subtitle = subtitle,
                                onClick = {
                                    if (playlistId.isNotEmpty()) {
                                        navController.navigate(PlaylistDestination(playlistId))
                                    }
                                }
                            )
                        }
                    }

                    item(span = { GridItemSpan(3) }) {
                        EndOfPage()
                    }
                }
            }
        }
    }
}
