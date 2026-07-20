package com.sonique.app.ui.screen.other

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
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
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.common.LOCAL_PLAYLIST_ID_DOWNLOADED
import com.sonique.domain.utils.toSongEntity
import com.sonique.logger.Logger
import com.sonique.app.Platform
import com.sonique.app.expect.ui.layerBackdrop
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.expect.ui.toImageBitmap
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.extension.getScreenSizeInfo
import com.sonique.app.extension.getStringBlocking
import com.sonique.app.extension.toImmersiveBackground
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.DescriptionView
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HeartCheckBox
import com.sonique.app.ui.component.LoadingDialog
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.PlaylistBottomSheet
import com.sonique.app.ui.component.LiquidGlassIconButton
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.ui.component.SongFullWidthItems
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.ListState
import com.sonique.app.viewModel.PlaylistUIEvent
import com.sonique.app.viewModel.PlaylistUIState
import com.sonique.app.viewModel.PlaylistViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album_length
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_downloaded
import sonique.composeapp.generated.resources.baseline_more_vert_24
import sonique.composeapp.generated.resources.baseline_pause_circle_24
import sonique.composeapp.generated.resources.baseline_play_circle_24
import sonique.composeapp.generated.resources.baseline_sensors_24
import sonique.composeapp.generated.resources.baseline_shuffle_24
import sonique.composeapp.generated.resources.download_button
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.downloading
import sonique.composeapp.generated.resources.error
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.no_description
import sonique.composeapp.generated.resources.playlist
import sonique.composeapp.generated.resources.radio
import sonique.composeapp.generated.resources.search
import sonique.composeapp.generated.resources.unlimited

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
    onScrolling: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val tag = "PlaylistScreen"

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val listColors by viewModel.listColors.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val tracksListState by viewModel.tracksListState.collectAsStateWithLifecycle()

    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchBarHeightPx by remember { mutableStateOf(0) }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val filteredTrack by remember {
        derivedStateOf {
            if (query.isEmpty() || !showSearchBar) {
                tracks
            } else {
                tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        Logger.d(tag, "uiState hash: ${uiState.hashCode()}")
        Logger.d(tag, "uiState data: ${uiState.data}")
    }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.getFullTracks {}
            lazyState.animateScrollToItem(0)
        }
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                tracksListState != ListState.PAGINATION_EXHAUST &&
                    (
                        lazyState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (lazyState.layoutInfo.totalItemsCount - 6)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d(tag, "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d(tag, "tracksListState: $tracksListState")
        Logger.d(tag, "Continuation: $continuation")
        if (shouldStartPaginate.value && tracksListState == ListState.IDLE) {
            viewModel.getContinuationTrack(
                playlistId,
                continuation,
            )
        }
    }

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val playingTrack = nowPlayingState?.songEntity
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val isPlaying = controllerState?.isPlaying == true

    var currentItem by remember {
        mutableStateOf<Track?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            PlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = tracks.firstOrNull { it.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = playlistId) {
        if (playlistId != uiState.data?.id) {
            Logger.w(tag, "new id: $playlistId")
            viewModel.getData(playlistId)
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val isScrollingUp by lazyState.isScrollingUp()
    LaunchedEffect(lazyState, isScrollingUp) {
        snapshotFlow { lazyState.firstVisibleItemIndex == 0 && lazyState.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                if (isAtTop) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }
    val paletteState = rememberPaletteState()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    // Track which thumbnail URL we've already extracted a palette from.
    // Prevents palette flash when LazyColumn recycles the header item on scroll —
    // AsyncImage re-mount fires onSuccess again, but we skip the regenerate.
    var paletteGeneratedFor by remember {
        mutableStateOf<String?>(null)
    }
    val currentThumbnail = (uiState as? PlaylistUIState.Success)?.data?.thumbnail

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && currentThumbnail != null && paletteGeneratedFor != currentThumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentThumbnail
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), md_theme_dark_background))
            }
    }

    // Apple Music-inspired immersive treatment: gated to mobile portrait so tablets,
    // foldable open state, landscape orientation, and Desktop keep the existing layout.
    val screenInfo = getScreenSizeInfo()
    val isMobilePortrait = getPlatform() == Platform.Android && screenInfo.wDP < screenInfo.hDP
    val dominantColor = listColors.firstOrNull() ?: md_theme_dark_background
    // Apple Music-style page background from the artwork's dominant tone (see UIExt.toImmersiveBackground).
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()
    val artworkSizeDp =
        if (isMobilePortrait) {
            (screenInfo.wDP * 0.55f).coerceIn(180f, 220f).toInt()
        } else {
            250
        }

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
//    Box {
    Crossfade(
        targetState = uiState,
    ) { state ->
        Logger.w(tag, "State hash: ${state.hashCode()}")
        when (state) {
            is PlaylistUIState.Success -> {
                val data = state.data
                if (data == null) return@Crossfade

                SharedDetailTemplate(
                    title = data.title,
                    subtitle = data.author.name + " • " + (if (data.isRadio) stringResource(Res.string.radio) else stringResource(Res.string.playlist)) + " • " + data.year,
                    description = data.description,
                    thumbnailUrl = data.thumbnail,
                    listColors = listColors,
                    onBack = { onBack?.invoke() ?: navController.navigateUp() },
                    playButtonContent = {
                        Crossfade(isPlaying && playingPlaylistId == data.id) { isThisPlaying ->
                            if (isThisPlaying) {
                                RippleIconButton(
                                    resId = Res.drawable.baseline_pause_circle_24,
                                    fillMaxSize = true,
                                    tint = seed,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                }
                            } else {
                                RippleIconButton(
                                    resId = Res.drawable.baseline_play_circle_24,
                                    fillMaxSize = true,
                                    tint = seed,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                }
                            }
                        }
                    },
                    onShuffleClick = if (!data.isRadio) {
                        { viewModel.onUIEvent(PlaylistUIEvent.Shuffle) }
                    } else null,
                    onHeartClick = if (!data.isRadio) {
                        {
                            HeartCheckBox(
                                size = 32,
                                checked = liked,
                                onStateChange = {
                                    viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                },
                            )
                        }
                    } else null,
                    downloadButtonContent = {
                        Crossfade(targetState = downloadState) { state ->
                            when (state) {
                                DownloadState.STATE_DOWNLOADED -> {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.makeToast(getStringBlocking(Res.string.downloaded))
                                            },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.baseline_downloaded),
                                            tint = Color(0xFF00A0CB),
                                            contentDescription = "",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .padding(2.dp),
                                        )
                                    }
                                }
                                DownloadState.STATE_DOWNLOADING -> {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                viewModel.makeToast(getStringBlocking(Res.string.downloading))
                                            },
                                    ) {
                                        Image(
                                            painter = rememberLottiePainter(composition = composition),
                                            contentDescription = "Lottie animation",
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                Logger.w("PlaylistScreen", "downloadState: $downloadState")
                                                viewModel.onUIEvent(PlaylistUIEvent.Download)
                                            },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.download_button),
                                            tint = Color.LightGray,
                                            contentDescription = "",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .padding(2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onPaletteGenerated = { colors ->
                        viewModel.setBrush(colors)
                    },
                    lazyState = lazyState
                ) {
                    items(count = filteredTrack.size, key = { index ->
                        val item = filteredTrack.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val item = filteredTrack.getOrNull(index)
                        if (item != null) {
                            Column(modifier = Modifier.animateItem()) {
                                SongFullWidthItems(
                                    isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = {
                                        Logger.w("PlaylistScreen", "index: $index")
                                        onPlaylistItemClick(it)
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(arrayListOf(item))
                                    },
                                    modifier = Modifier,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.12f),
                                )
                            }
                        }
                    }
                    when (tracksListState) {
                        ListState.IDLE -> {
                            item {
                                EndOfPage()
                            }
                        }
                        ListState.LOADING, ListState.PAGINATING -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                        ListState.ERROR -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TextButton(onClick = { /* no retry mechanism available */ }) {
                                        Text(text = "Try again")
                                    }
                                }
                            }
                        }
                        ListState.PAGINATION_EXHAUST -> {
                            item {
                                EndOfPage()
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { searchBarHeightPx = it.size.height }
                            .then(
                                if (isMobilePortrait) {
                                    Modifier.hazeEffect(hazeState) {
                                        blurEnabled = true
                                        blurRadius = 24.dp
                                        backgroundColor = mutedPaletteBg
                                        tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                                    }
                                } else {
                                    Modifier.background(Color.Black)
                                },
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.statusBars),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RippleIconButton(
                                resId = Res.drawable.baseline_arrow_back_ios_new_24,
                            ) {
                                onBack?.invoke() ?: navController.navigateUp()
                            }
                            SearchBar(
                                modifier =
                                    Modifier
                                        .height(50.dp)
                                        .padding(horizontal = 12.dp)
                                        .weight(1f),
                                colors =
                                    SearchBarDefaults.colors().copy(
                                        containerColor = Color.Transparent,
                                    ),
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
                                                    style = typo().bodyMedium,
                                                )
                                            },
                                        )
                                    }
                                },
                                expanded = false,
                                onExpandedChange = {},
                                windowInsets = WindowInsets(0, 0, 0, 0),
                            ) {
                            }
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White)
                            }
                        }
                    }
                }

                if (itemBottomSheetShow && currentItem != null) {
                    val track = currentItem?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = {
                            itemBottomSheetShow = false
                            currentItem = null
                        },
                        navController = navController,
                        song = track,
                    )
                }
                if (playlistBottomSheetShow) {
                    Logger.w("PlaylistScreen", "PlaylistBottomSheet")
                    val addToQueue = {
                        viewModel.getFullTracks { track ->
                            sharedViewModel.addListToQueue(
                                track.toCollection(arrayListOf()),
                            )
                        }
                    }
                    PlaylistBottomSheet(
                        onDismiss = { playlistBottomSheetShow = false },
                        playlistId = data.id,
                        playlistName = data.title,
                        isYourYouTubePlaylist = isYourYouTubePlaylist && !data.isRadio,
                        onSaveToLocal = {
                            viewModel.getFullTracks { track ->
                                viewModel.saveToLocal(track)
                            }
                        },
                        onEditTitle = { newTitle ->
                            viewModel.updatePlaylistTitle(newTitle, data.id)
                        },
                        onAddToQueue = if (data.isRadio) null else addToQueue,
                    )
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar && !showSearchBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        windowInsets =
                            TopAppBarDefaults.windowInsets.exclude(
                                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                            ),
                        title = {
                            Text(
                                text = data.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
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
                                    onBack?.invoke() ?: navController.navigateUp()
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = Color.White)
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        modifier =
                            if (isMobilePortrait) {
                                Modifier.hazeEffect(hazeState) {
                                    blurEnabled = true
                                    blurRadius = 24.dp
                                    backgroundColor = mutedPaletteBg
                                    tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                                }
                            } else {
                                Modifier.angledGradientBackground(listColors, 90f)
                            },
                    )
                }
            }

            is PlaylistUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            is PlaylistUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                onBack?.invoke() ?: navController.navigateUp()
            }
        }
    }
}