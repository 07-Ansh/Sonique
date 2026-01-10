package com.sonique.app.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.domain.data.entities.ArtistEntity
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.utils.toTrack
import com.sonique.logger.Logger
import com.sonique.app.ui.component.ArtistFullWidthItems
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.component.SongFullWidthItems
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.LibraryDynamicPlaylistViewModel
import com.sonique.app.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.baseline_search_24
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.favorite
import sonique.composeapp.generated.resources.followed
import sonique.composeapp.generated.resources.most_played
import sonique.composeapp.generated.resources.search

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
@ExperimentalMaterial3Api
fun LibraryDynamicPlaylistScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    type: String,
    viewModel: LibraryDynamicPlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val nowPlayingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    var chosenSong: SongEntity? by remember { mutableStateOf(null) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val favorite by viewModel.listFavoriteSong.collectAsStateWithLifecycle()
    var tempFavorite by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val followed by viewModel.listFollowedArtist.collectAsStateWithLifecycle()
    var tempFollowed by rememberSaveable { mutableStateOf(emptyList<ArtistEntity>()) }
    val mostPlayed by viewModel.listMostPlayedSong.collectAsStateWithLifecycle()
    var tempMostPlayed by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val downloaded by viewModel.listDownloadedSong.collectAsStateWithLifecycle()
    var tempDownloaded by rememberSaveable { mutableStateOf(emptyList<SongEntity>()) }
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    LaunchedEffect(query) {
        Logger.w("LibraryDynamicPlaylistScreen", "Check query: $query")
        tempFavorite = favorite.filter { it.title.contains(query, ignoreCase = true) }
        Logger.w("LibraryDynamicPlaylistScreen", "Check tempFavorite: $tempFavorite")
        tempFollowed = followed.filter { it.name.contains(query, ignoreCase = true) }
        Logger.w("LibraryDynamicPlaylistScreen", "Check tempFollowed: $tempFollowed")
        tempMostPlayed = mostPlayed.filter { it.title.contains(query, ignoreCase = true) }
        Logger.w("LibraryDynamicPlaylistScreen", "Check tempMostPlayed: $tempMostPlayed")
        tempDownloaded = downloaded.filter { it.title.contains(query, ignoreCase = true) }
        Logger.w("LibraryDynamicPlaylistScreen", "Check tempDownloaded: $tempDownloaded")
    }

    LazyColumn(
        modifier = Modifier.hazeSource(hazeState),
        contentPadding = innerPadding,
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        item {
            AnimatedVisibility(showSearchBar) {
                Spacer(Modifier.height(55.dp))
            }
        }
        val type = LibraryDynamicPlaylistType.toType(type)
        if (type == LibraryDynamicPlaylistType.Followed) {
            items(
                if (query.isNotEmpty() && showSearchBar) {
                    tempFollowed
                } else {
                    followed
                },
                key = { it.channelId },
            ) { artist ->
                ArtistFullWidthItems(
                    artist,
                    onClickListener = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = artist.channelId,
                            ),
                        )
                    },
                )
            }
        } else {
            items(
                when (type) {
                    LibraryDynamicPlaylistType.Downloaded ->
                        if (query.isNotEmpty() && showSearchBar) {
                            tempDownloaded
                        } else {
                            downloaded
                        }

                    LibraryDynamicPlaylistType.Favorite ->
                        if (query.isNotEmpty() && showSearchBar) {
                            tempFavorite
                        } else {
                            favorite
                        }

                    LibraryDynamicPlaylistType.MostPlayed ->
                        if (query.isNotEmpty() && showSearchBar) {
                            tempMostPlayed
                        } else {
                            mostPlayed
                        }

                    else -> emptyList()
                },
                key = { it.hashCode() },
            ) { song ->
                SongFullWidthItems(
                    songEntity = song,
                    isPlaying = song.videoId == nowPlayingVideoId,
                    modifier = Modifier.fillMaxWidth(),
                    onMoreClickListener = {
                        chosenSong = song
                        showBottomSheet = true
                    },
                    onClickListener = { videoId ->
                        viewModel.playSong(videoId, type = type)
                    },
                    onAddToQueue = {
                        sharedViewModel.addListToQueue(
                            arrayListOf(song.toTrack()),
                        )
                    },
                )
            }
        }
        item {
            EndOfPage()
        }
    }
    if (showBottomSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showBottomSheet = false
                chosenSong = null
            },
            navController = navController,
            song = chosenSong ?: return,
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val type = LibraryDynamicPlaylistType.toType(type)
        TopAppBar(
            title = {
                Text(
                    text =
                        stringResource(
                            type.name(),
                        ),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        Res.drawable.baseline_arrow_back_ios_new_24,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        if (showSearchBar) Res.drawable.baseline_close_24 else Res.drawable.baseline_search_24,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        showSearchBar = !showSearchBar
                    }
                }
            },
            modifier =
                Modifier
                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
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

sealed class LibraryDynamicPlaylistType {
    data object Favorite : LibraryDynamicPlaylistType()

    data object Followed : LibraryDynamicPlaylistType()

    data object MostPlayed : LibraryDynamicPlaylistType()

    data object Downloaded : LibraryDynamicPlaylistType()

    fun name(): StringResource =
        when (this) {
            Favorite -> Res.string.favorite
            Followed -> Res.string.followed
            MostPlayed -> Res.string.most_played
            Downloaded -> Res.string.downloaded
        }

     
    fun toStringParams(): String =
        when (this) {
            Favorite -> "favorite"
            Followed -> "followed"
            MostPlayed -> "most_played"
            Downloaded -> "downloaded"
        }

    companion object {
        fun toType(input: String): LibraryDynamicPlaylistType =
            when (input) {
                "favorite" -> Favorite
                "followed" -> Followed
                "most_played" -> MostPlayed
                "downloaded" -> Downloaded
                else -> throw IllegalArgumentException("Unknown type: $this")
            }
    }
}

