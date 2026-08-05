package com.sonique.app.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import com.sonique.app.ui.theme.overlayMedium
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.utils.toSongEntity
import com.sonique.domain.utils.toTrack
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.DescriptionView
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HeartCheckBox
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.PodcastEpisodeFullWidthItem
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.PodcastUIEvent
import com.sonique.app.viewModel.PodcastUIState
import com.sonique.app.viewModel.PodcastViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album_length
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_play_circle_24
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.baseline_shuffle_24
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.no_description
import sonique.composeapp.generated.resources.podcasts

import com.sonique.app.extension.isScrollingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel = koinViewModel(),
    podcastId: String,
    navController: NavController,
    onScrolling: (Boolean) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }

    var currentTrack by rememberSaveable {
        mutableStateOf<Track?>(null)
    }
    var shouldShowMoreBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val isScrollingUp by lazyState.isScrollingUp()
    LaunchedEffect(lazyState) {
        snapshotFlow { lazyState.firstVisibleItemIndex == 0 && lazyState.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                onScrolling.invoke(isAtTop)
            }
    }

     
    var gradientColors by remember { mutableStateOf(listOf(md_theme_dark_background, md_theme_dark_background)) }

    val paletteState = rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null) {
            paletteState.generate(bm)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                gradientColors = listOf(it.getColorFromPalette(), md_theme_dark_background)
            }
    }

    LaunchedEffect(key1 = podcastId) {
        if ((uiState as? PodcastUIState.Success)?.id == podcastId) {
            return@LaunchedEffect
        }
        viewModel.getPodcastBrowse(podcastId)
    }

    Crossfade(targetState = uiState) { state ->
        when (state) {
            is PodcastUIState.Success -> {
                val data = state.data
                val id = state.id
                SharedDetailTemplate(
                    title = data.title,
                    subtitle = data.author.name + " • " + stringResource(Res.string.podcasts),
                    description = data.description,
                    thumbnailUrl = data.thumbnail.lastOrNull()?.url,
                    listColors = gradientColors,
                    onBack = { navController.navigateUp() },
                    playButtonContent = {
                        RippleIconButton(
                            resId = Res.drawable.baseline_play_circle_24,
                            fillMaxSize = true,
                            tint = seed,
                            modifier = Modifier.size(48.dp),
                        ) {
                            viewModel.onUIEvent(PodcastUIEvent.PlayAll(id))
                        }
                    },
                    onShuffleClick = {
                        viewModel.onUIEvent(PodcastUIEvent.Shuffle(id))
                    },
                    onHeartClick = {
                        HeartCheckBox(
                            size = 32,
                            checked = isFavorite,
                            onStateChange = {
                                viewModel.onUIEvent(PodcastUIEvent.ToggleFavorite(id, !isFavorite))
                            },
                        )
                    },
                    additionalActions = {
                        RippleIconButton(
                            modifier = Modifier.size(36.dp),
                            resId = Res.drawable.baseline_share_24,
                            fillMaxSize = true,
                        ) {
                            viewModel.onUIEvent(PodcastUIEvent.Share(id))
                        }
                    },
                    onPaletteGenerated = { colors ->
                        gradientColors = colors
                    },
                    lazyState = lazyState
                ) {
                    items(count = data.listEpisode.size, key = { index ->
                        val item = data.listEpisode.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val episode = data.listEpisode.getOrNull(index)
                        if (episode != null) {
                            PodcastEpisodeFullWidthItem(
                                episode = episode,
                                onClick = {
                                    viewModel.onUIEvent(PodcastUIEvent.EpisodeClick(episode.videoId, id))
                                },
                                onMoreClickListener = {
                                    currentTrack = episode.toTrack()
                                    shouldShowMoreBottomSheet = true
                                },
                            )
                        }
                    }

                    item {
                        EndOfPage()
                    }
                }

                if (shouldShowMoreBottomSheet) {
                    val song = currentTrack?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = { shouldShowMoreBottomSheet = false },
                        navController = navController,
                        song = song,
                    )
                }
            }

            is PodcastUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    )
                }
            }

            is PodcastUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }
    }
}

