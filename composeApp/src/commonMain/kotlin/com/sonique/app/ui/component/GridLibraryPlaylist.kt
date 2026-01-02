package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.create
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_search_24
import sonique.composeapp.generated.resources.search



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
    noinline onReload: () -> Unit,
) {
    Logger.w("GridLibraryPlaylist", "Generic Type: ${T::class.java}")
    val state = rememberLazyListState()
    val isScrollingUp by state.isScrollingUp()
    val typography = typo()
    val displayTitle = title?.let { stringResource(it) }
    val createString = stringResource(Res.string.create)

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
                                else -> ""
                            }
                            itemTitle.contains(query, ignoreCase = true)
                        }
                    } else {
                        list
                    }
                }

                if (data is LocalResource.Success && (filteredList.isNotEmpty() || list.isNotEmpty()) || createNewPlaylist != null) {
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

                        if (createNewPlaylist != null) {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                createNewPlaylist()
                                            },
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .padding(vertical = 10.dp, horizontal = 15.dp)
                                                .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(white.copy(alpha = 0.1f)),
                                            Alignment.Center,
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(32.dp),
                                                imageVector = Icons.Rounded.Add,
                                                tint = white,
                                                contentDescription = null,
                                            )
                                        }
                                        Text(
                                            text = createString,
                                            style = typography.bodyMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .padding(start = 12.dp)
                                                    .weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                        items(filteredList) { item ->
                            if (item !is PlaylistType) {
                                return@items
                            }
                            PlaylistFullWidthItems(
                                onClickListener = {
                                    when (item) {
                                        is LocalPlaylistEntity -> {
                                            navController.navigate(
                                                LocalPlaylistDestination(
                                                    item.id,
                                                ),
                                            )
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
                                data = item,
                            )
                        }

                        item {
                            EndOfPage()
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
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            Res.drawable.baseline_arrow_back_ios_new_24,
                            Modifier.size(32.dp),
                            true,
                        ) {
                            onBack?.invoke() ?: navController.navigateUp()
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

