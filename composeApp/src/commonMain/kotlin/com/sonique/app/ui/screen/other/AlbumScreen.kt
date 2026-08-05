package com.sonique.app.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import com.sonique.app.ui.theme.backgroundCard
import com.sonique.app.ui.theme.overlayMedium
import com.sonique.app.ui.theme.textHighEmphasis
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.sonique.domain.utils.toSongEntity
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.DescriptionView
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HeartCheckBox
import com.sonique.app.ui.component.HomeItemContentPlaylist
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.component.SongFullWidthItems
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.AlbumViewModel
import com.sonique.app.viewModel.LocalPlaylistState
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.album_length
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_downloaded
import sonique.composeapp.generated.resources.done
import sonique.composeapp.generated.resources.baseline_pause_circle_24
import sonique.composeapp.generated.resources.baseline_play_circle_24
import sonique.composeapp.generated.resources.baseline_shuffle_24
import sonique.composeapp.generated.resources.download_button
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.downloading
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.no_description
import sonique.composeapp.generated.resources.other_version
import sonique.composeapp.generated.resources.year_and_category

import com.sonique.app.extension.isScrollingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    browseId: String,
    navController: NavController,
    viewModel: AlbumViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    onScrolling: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    val playingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var chosenSong: Track? by remember { mutableStateOf(null) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }

    LaunchedEffect(browseId) {
        viewModel.updateBrowseId(browseId)
    }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
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
    val paletteState = rememberPaletteState()
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

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
                viewModel.setBrush(listOf(it.getColorFromPalette(), md_theme_dark_background))
            }
    }

    Crossfade(uiState.loadState) {
        when (it) {
            LocalPlaylistState.PlaylistLoadState.Success -> {
                if (showCancelDownloadDialog) {
                androidx.compose.material3.AlertDialog(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurface,
                    onDismissRequest = { showCancelDownloadDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showCancelDownloadDialog = false
                            viewModel.cancelDownload()
                        }) {
                            Text(text = "Yes", style = typo().labelSmall)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDownloadDialog = false }) {
                            Text(text = "Cancel", style = typo().labelSmall)
                        }
                    },
                    title = {
                        Text(text = "Warning", style = typo().labelSmall)
                    },
                    text = {
                        val text =
                            if (uiState.downloadState == DownloadState.STATE_DOWNLOADED) {
                                "Do you want to remove this download?"
                            } else {
                                "Do you want to cancel the download?"
                            }
                        Text(text = text, style = typo().bodyMedium)
                    },
                )
                }

                SharedDetailTemplate(
                    title = uiState.title,
                    subtitle = uiState.artist.name + " • " + uiState.year + " • Album",
                    description = uiState.description,
                    thumbnailUrl = uiState.thumbnail,
                    listColors = uiState.colors,
                    onBack = { onBack?.invoke() ?: navController.navigateUp() },
                    onShuffleClick = {
                        val firstTrack = uiState.listTrack.shuffled().firstOrNull()
                        if (firstTrack != null) {
                            viewModel.playTrack(firstTrack)
                        }
                    },
                    playButtonContent = {
                        Crossfade(
                            playingVideoId.isNotEmpty() &&
                                    playingPlaylistId == browseId.replaceFirst("VL", ""),
                        ) { isThisPlaying ->
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(min = 110.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable {
                                        if (isThisPlaying) {
                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                        } else {
                                            viewModel.playTrack(uiState.listTrack.firstOrNull() ?: return@clickable)
                                        }
                                    }
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(
                                            if (isThisPlaying) Res.drawable.baseline_pause_circle_24 else Res.drawable.baseline_play_circle_24
                                        ),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isThisPlaying) "Pause" else "Play",
                                        color = Color.Black,
                                        style = typo().labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    },
                    downloadButtonContent = {
                        Crossfade(targetState = uiState.downloadState) { state ->
                            when (state) {
                                DownloadState.STATE_DOWNLOADED -> {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { showCancelDownloadDialog = true },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.done),
                                            tint = Color.White,
                                            contentDescription = "Downloaded",
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
                                            .clickable { showCancelDownloadDialog = true },
                                    ) {
                                        Image(
                                            painter = rememberLottiePainter(composition = composition),
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.downloadFullAlbum() },
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
                    items(count = uiState.trackCount, key = { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        item?.videoId + "item_$index"
                    }) { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        if (item != null) {
                            SongFullWidthItems(
                                isPlaying = item.videoId == playingVideoId,
                                index = index,
                                track = item,
                                onMoreClickListener = {
                                    chosenSong = item
                                    showBottomSheet = true
                                },
                                onClickListener = {
                                    viewModel.playTrack(item)
                                },
                                onAddToQueue = {
                                    sharedViewModel.playNext(item)
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    item(contentType = "other_version") {
                        AnimatedVisibility(uiState.otherVersion.isNotEmpty()) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(Res.string.other_version),
                                    style = typo().labelMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                )
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    items(uiState.otherVersion) { album ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    AlbumDestination(browseId = album.browseId)
                                                )
                                            },
                                            data = album,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                }
                            }
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
                        song = chosenSong?.toSongEntity(),
                    )
                }
            }

            LocalPlaylistState.PlaylistLoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Text(
                            text = "Failed to load album data",
                            color = Color.White,
                            style = typo().bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onBack?.invoke() ?: navController.navigateUp() }) {
                            Text(text = "Go Back", color = Color.White)
                        }
                    }
                }
            }

            LocalPlaylistState.PlaylistLoadState.Loading -> {
                CenterLoadingBox(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

