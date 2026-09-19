package com.sonique.app.ui.screen.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.extension.getAmbientSheetColor
import com.sonique.app.extension.getAmbientAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sonique.app.ui.component.GoogleCircularProgressIndicator
import com.sonique.logger.Logger
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import com.sonique.app.viewModel.NowPlayingBottomSheetViewModel
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import com.sonique.app.expect.ui.toImageBitmap
import com.sonique.app.extension.getAmbientSheetColor
import com.sonique.app.extension.getAmbientAccentColor
import com.sonique.app.extension.getAmbientTintedWhiteColor
import com.sonique.app.extension.cleanSongTitle
import com.sonique.app.extension.formatDuration
import com.sonique.app.ui.component.BottomSheet
import com.sonique.app.ui.component.rememberBottomSheetState
import com.sonique.app.ui.component.collapsedAnchor
import com.sonique.app.ui.component.FreshQueueContent
import com.sonique.app.ui.component.FreshPlayerMenuSheet
import com.sonique.app.ui.component.FreshQueueSheet
import com.sonique.app.ui.component.LyricsView
import com.sonique.app.ui.component.lyrics.ShareLyricsSheet
import com.sonique.app.ui.component.lyrics.toShareLyricsLines
import com.sonique.app.expect.ui.BackHandler
import com.sonique.app.ui.component.ModernMoreOptionsContent
import com.sonique.app.ui.component.ModernMoreOptionsSheet
import com.sonique.app.ui.component.ModernMoreOptionsTwoStageSheet
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.QueueBottomSheet
import androidx.compose.runtime.mutableIntStateOf
import com.sonique.app.viewModel.NowPlayingScreenData
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.LyricsProvider
import com.sonique.app.viewModel.UIEvent
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.RepeatState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.IconButton
import com.sonique.app.ui.component.ExplicitBadge
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import com.sonique.app.expect.shareUrl
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.favorite
import sonique.composeapp.generated.resources.favorite_border
import sonique.composeapp.generated.resources.ic_share_curved
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.baseline_repeat_one_24
import sonique.composeapp.generated.resources.lyrics
import sonique.composeapp.generated.resources.more_horiz
import sonique.composeapp.generated.resources.pause
import sonique.composeapp.generated.resources.play
import sonique.composeapp.generated.resources.queue_music
import sonique.composeapp.generated.resources.repeat
import sonique.composeapp.generated.resources.repeat_on
import sonique.composeapp.generated.resources.repeat_one_on
import sonique.composeapp.generated.resources.shuffle
import sonique.composeapp.generated.resources.shuffle_on
import sonique.composeapp.generated.resources.skip_next
import sonique.composeapp.generated.resources.skip_previous
import sonique.composeapp.generated.resources.bedtime
import sonique.composeapp.generated.resources.baseline_close_24

import kotlin.math.roundToLong

private val PlayerHorizontalPadding = 24.dp
private val ThumbnailCornerRadius = 8.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewPlayerScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    navController: NavController,
    isVisible: Boolean = false,
    onDismiss: () -> Unit = {},
) {
    val nowPlayingBottomSheetViewModel: NowPlayingBottomSheetViewModel = koinViewModel()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val currentSongData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val queueData by sharedViewModel.queueData.collectAsStateWithLifecycle(initialValue = null)
    val activeQueueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val sleepTimerState by sharedViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val ambienceMode by sharedViewModel.ambienceMode.collectAsStateWithLifecycle()
    val likeStatus by sharedViewModel.likeStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val rawTrackTitle = currentSongData?.nowPlayingTitle ?: ""
    val trackTitle = rawTrackTitle.cleanSongTitle()
    val trackArtist = currentSongData?.artistName ?: ""
    val firstArtist = if (trackArtist.isBlank()) ""
    else trackArtist.split(",", ";", "&", " feat.", " Feat.", " ft.", " Ft.").firstOrNull()?.trim() ?: trackArtist
    val trackArtwork = currentSongData?.thumbnailURL ?: ""
    val playingContextText = currentSongData?.playlistName?.takeIf { it.isNotBlank() }
        ?: queueData?.data?.playlistName?.takeIf { it.isNotBlank() }
        ?: "Current Queue"

    var showInlineLyrics by remember { mutableStateOf(false) }
    var showShareLyricsSheet by remember { mutableStateOf(false) }
    var shareInitialLineIndex by remember { mutableIntStateOf(0) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val paletteState = com.kmpalette.rememberPaletteState()
    val defaultBg = MaterialTheme.colorScheme.background
    val startColor = remember(defaultBg) { androidx.compose.animation.Animatable(defaultBg) }
    val defaultSheetBg = Color(0xFF141316)
    val ambientSheetColor = remember { androidx.compose.animation.Animatable(defaultSheetBg) }
    val ambientAccentColor = remember { androidx.compose.animation.Animatable(Color.White) }
    val ambientTintedWhiteColor = remember { androidx.compose.animation.Animatable(Color.White) }
    var extractedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var lastPaletteBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val platformContext = LocalPlatformContext.current
    LaunchedEffect(trackArtwork) {
        if (trackArtwork.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(platformContext)
                        .data(trackArtwork)
                        .build()
                    val result = coil3.SingletonImageLoader.get(platformContext).execute(request)
                    if (result is coil3.request.SuccessResult) {
                        val bm = result.image.toImageBitmap()
                        extractedBitmap = bm
                        sharedViewModel.setBitmap(bm)
                    }
                } catch (e: Exception) {
                    Logger.e("NewPlayerScreen", "Failed to extract bitmap: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(extractedBitmap, currentSongData?.bitmap) {
        val bm = extractedBitmap ?: currentSongData?.bitmap
        if (bm != null && bm != lastPaletteBitmap) {
            lastPaletteBitmap = bm
            withContext(Dispatchers.Default) {
                try {
                    paletteState.generate(bm)
                } catch (e: Exception) {
                    Logger.e("NewPlayerScreen", "Failed to generate palette: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest { pal ->
                pal?.let {
                    startColor.animateTo(it.getColorFromPalette())
                    ambientSheetColor.animateTo(it.getAmbientSheetColor())
                    ambientAccentColor.animateTo(it.getAmbientAccentColor())
                    ambientTintedWhiteColor.animateTo(it.getAmbientTintedWhiteColor())
                }
            }
    }

    val sheetBg = ambientSheetColor.value.copy(alpha = 1f)
    val activeAccentColor = ambientAccentColor.value
    val activeTintedWhite = ambientTintedWhiteColor.value

    val offsetYAnimatable = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        offsetYAnimatable.snapTo(0f)
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            offsetYAnimatable.snapTo(0f)
        }
    }

    LaunchedEffect(showInlineLyrics) {
        if (showInlineLyrics && currentSongData?.lyricsData == null) {
            sharedViewModel.setLyricsProvider()
        }
    }

    val TextBackgroundColor = Color.White
    val textButtonColor = activeTintedWhite
    val iconButtonColor = Color(0xFF141316)
    val sideButtonContainerColor = activeTintedWhite.copy(alpha = 0.14f)
    val sideButtonContentColor = activeTintedWhite
    val buttonBorderColor = activeTintedWhite.copy(alpha = 0.30f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offsetYAnimatable.value.coerceAtLeast(0f)
                alpha = (1f - (offsetYAnimatable.value / 1200f)).coerceIn(0f, 1f)
            }
            .background(defaultBg)
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        val dynamicHorizontalPadding = (screenWidth * 0.062f).coerceIn(20.dp, 28.dp)
        val dynamicTopSpacing = (screenHeight * 0.014f).coerceIn(8.dp, 16.dp)
        val dynamicHeaderToArtworkSpacing = (screenHeight * 0.012f).coerceIn(8.dp, 16.dp)
        val dynamicArtworkToInfoSpacing = (screenHeight * 0.018f).coerceIn(12.dp, 22.dp)
        val dynamicInfoToSliderSpacing = (screenHeight * 0.030f).coerceIn(22.dp, 28.dp)
        val dynamicSliderToControlsSpacing = (screenHeight * 0.030f).coerceIn(22.dp, 28.dp)
        val dynamicControlsHeight = (screenHeight * 0.082f).coerceIn(62.dp, 70.dp)
        val dynamicControlsToBottomSpacing = (screenHeight * 0.038f).coerceIn(26.dp, 34.dp)
        val dynamicActionButtonSize = 42.dp
        val dynamicBottomBarContentHeight = (screenHeight * 0.076f).coerceIn(60.dp, 68.dp)
        val dynamicBottomButtonSize = 42.dp

        val bottomInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        val collapsedBarHeight = dynamicBottomBarContentHeight + bottomInsets
        val queueSheetState = rememberBottomSheetState(
            dismissedBound = 0.dp,
            expandedBound = maxHeight,
            collapsedBound = collapsedBarHeight,
            initialAnchor = collapsedAnchor
        )

        BackHandler(enabled = isVisible && queueSheetState.isCollapsed) {
            if (showInlineLyrics) {
                showInlineLyrics = false
            } else {
                scope.launch {
                    offsetYAnimatable.animateTo(
                        targetValue = 1500f,
                        animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                    )
                    onDismiss()
                }
            }
        }

        AnimatedContent(
            targetState = if (ambienceMode) trackArtwork else "",
            transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
            label = "blurBackground"
        ) { url ->
            if (url.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(48.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = collapsedBarHeight)
                .then(
                    if (!showInlineLyrics && queueSheetState.isCollapsed) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    velocityTracker.addPointerInputChange(change)
                                    val newY = (offsetYAnimatable.value + dragAmount).coerceAtLeast(0f)
                                    scope.launch {
                                        offsetYAnimatable.snapTo(newY)
                                    }
                                },
                                onDragCancel = {
                                    velocityTracker.resetTracking()
                                    scope.launch {
                                        offsetYAnimatable.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                },
                                onDragEnd = {
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    velocityTracker.resetTracking()
                                    scope.launch {
                                        if (offsetYAnimatable.value > 180f || velocityY > 1200f) {
                                            offsetYAnimatable.animateTo(
                                                targetValue = 1500f,
                                                animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                                            )
                                            onDismiss()
                                        } else {
                                            offsetYAnimatable.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(dynamicTopSpacing))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            offsetYAnimatable.animateTo(
                                targetValue = 1500f,
                                animationSpec = tween(durationMillis = 180, easing = LinearEasing)
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Dismiss player",
                        tint = TextBackgroundColor
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp)
                ) {
                    Text(
                        text = if (showInlineLyrics) {
                            val provider = currentSongData?.lyricsData?.lyricsProvider
                            if (provider != null) {
                                val providerName = when (provider) {
                                    LyricsProvider.BETTER_LYRICS -> "BetterLyrics"
                                    LyricsProvider.LRCLIB -> "LRCLIB"
                                    LyricsProvider.YOUTUBE -> "YouTube"
                                    LyricsProvider.SPOTIFY -> "Spotify"
                                    LyricsProvider.OFFLINE -> "Offline Cache"
                                    LyricsProvider.AI -> "AI Translated"
                                }
                                "Lyrics from $providerName"
                            } else {
                                "LYRICS"
                            }
                        } else {
                            "NOW PLAYING"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Normal,
                        color = TextBackgroundColor.copy(alpha = 0.65f),
                        letterSpacing = 1.2.sp
                    )
                    if (!showInlineLyrics) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = trackTitle.ifBlank { "Unknown Title" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = TextBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showInlineLyrics) {
                    val hasLyrics = currentSongData?.lyricsData != null
                    IconButton(
                        onClick = { if (hasLyrics) showShareLyricsSheet = true },
                        enabled = hasLyrics,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Lyrics",
                            tint = if (hasLyrics) TextBackgroundColor else TextBackgroundColor.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dynamicHeaderToArtworkSpacing))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (showInlineLyrics) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp)
                    ) {
                        val lyricsData = currentSongData?.lyricsData
                        // Track how long we have been waiting for lyrics
                        var lyricsTimedOut by remember { mutableStateOf(false) }
                        LaunchedEffect(lyricsData) {
                            if (lyricsData == null) {
                                lyricsTimedOut = false
                                kotlinx.coroutines.delay(8000)
                                if (currentSongData?.lyricsData == null) {
                                    lyricsTimedOut = true
                                }
                            }
                        }
                        if (lyricsData != null) {
                            LyricsView(
                                lyricsData = lyricsData,
                                timeLine = sharedViewModel.timeline,
                                onLineClick = { progress ->
                                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(progress))
                                },
                                onShareLyrics = { lineIdx ->
                                    shareInitialLineIndex = lineIdx
                                    showShareLyricsSheet = true
                                },
                                backgroundColor = Color.Transparent,
                                playerContentColor = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (lyricsTimedOut) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        lyricsTimedOut = false
                                        sharedViewModel.setLyricsProvider()
                                    }
                            ) {
                                Text(
                                    text = "Lyrics unavailable • Tap to retry",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextBackgroundColor.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                GoogleCircularProgressIndicator(
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // Album artwork — smooth stable queue-based swipe (zero flicker, fast & slow)
                    val queue = activeQueueData?.data?.listTracks?.ifEmpty { null }
                        ?: queueData?.data?.listTracks
                        ?: emptyList()
                    val currentVideoId = nowPlayingState?.mediaItem?.mediaId?.takeIf { it.isNotBlank() }
                        ?: nowPlayingState?.track?.videoId?.takeIf { it.isNotBlank() }
                        ?: currentSongData?.songInfoData?.videoId?.takeIf { it.isNotBlank() }
                    val currentQueueIndex = remember(queue, currentVideoId, trackTitle) {
                        val idMatch = if (!currentVideoId.isNullOrBlank()) {
                            queue.indexOfFirst { it.videoId == currentVideoId }
                        } else -1
                        if (idMatch != -1) {
                            idMatch
                        } else {
                            val titleMatch = queue.indexOfFirst {
                                it.title.equals(rawTrackTitle, ignoreCase = true) ||
                                it.title?.cleanSongTitle().equals(trackTitle, ignoreCase = true)
                            }
                            if (titleMatch != -1) {
                                titleMatch
                            } else {
                                val orderIdx = musicServiceHandler.currentOrderIndex()
                                if (orderIdx in queue.indices) orderIdx else 0
                            }
                        }
                    }

                    val totalPages = if (queue.isNotEmpty()) queue.size else 1
                    val safeInitialPage = currentQueueIndex.coerceIn(0, maxOf(0, totalPages - 1))

                    val pagerState = rememberPagerState(
                        initialPage = safeInitialPage,
                        pageCount = { totalPages }
                    )

                    val flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.25f,
                    )

                    // Track whether the user physically dragged the album artwork pager
                    val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
                    var userSwipedPager by remember { mutableStateOf(false) }

                    LaunchedEffect(isPagerDragged) {
                        if (isPagerDragged) {
                            userSwipedPager = true
                        }
                    }

                    // Keep pager in sync with active track when changed externally (next button, queue, auto-advance, app open)
                    LaunchedEffect(currentQueueIndex) {
                        if (currentQueueIndex in 0 until totalPages &&
                            currentQueueIndex != pagerState.currentPage &&
                            !pagerState.isScrollInProgress
                        ) {
                            pagerState.scrollToPage(currentQueueIndex)
                        }
                    }

                    // ONLY trigger track switch when the user PHYSICALLY swiped the pager and it has settled
                    LaunchedEffect(pagerState, queue) {
                        snapshotFlow { Pair(pagerState.settledPage, pagerState.isScrollInProgress) }
                            .collect { (settledPage, isScrolling) ->
                                if (!isScrolling && userSwipedPager) {
                                    userSwipedPager = false
                                    if (settledPage != currentQueueIndex && settledPage in queue.indices) {
                                        musicServiceHandler.playMediaItemInMediaSource(settledPage)
                                    }
                                }
                            }
                    }

                    BoxWithConstraints(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = dynamicHorizontalPadding)
                    ) {
                        val thumbnailSize = minOf(maxWidth, maxHeight, 380.dp)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(thumbnailSize)
                                .then(
                                    if (totalPages <= 1) {
                                        Modifier.pointerInput(Unit) {
                                            var totalDragX = 0f
                                            var isSwipeHandled = false
                                            val threshold = 50.dp.toPx()
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    totalDragX = 0f
                                                    isSwipeHandled = false
                                                },
                                                onDragEnd = {
                                                    totalDragX = 0f
                                                    isSwipeHandled = false
                                                },
                                                onDragCancel = {
                                                    totalDragX = 0f
                                                    isSwipeHandled = false
                                                },
                                            ) { change, dragAmount ->
                                                change.consume()
                                                if (!isSwipeHandled) {
                                                    totalDragX += dragAmount
                                                    if (totalDragX < -threshold) {
                                                        if (controllerState.isNextAvailable) {
                                                            sharedViewModel.onUIEvent(UIEvent.Next)
                                                            isSwipeHandled = true
                                                        }
                                                    } else if (totalDragX > threshold) {
                                                        if (controllerState.isPreviousAvailable) {
                                                            sharedViewModel.onUIEvent(UIEvent.Previous)
                                                            isSwipeHandled = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else Modifier
                                )
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                flingBehavior = flingBehavior,
                                key = { page -> "${page}_${queue.getOrNull(page)?.videoId ?: page}" },
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = totalPages > 1,
                            ) { page ->
                                val song = queue.getOrNull(page)
                                val isCurrentActivePage = (page == currentQueueIndex) || (queue.isEmpty() && page == 0)
                                val artUrl = if (isCurrentActivePage && trackArtwork.isNotEmpty()) {
                                    trackArtwork
                                } else {
                                    song?.thumbnails?.lastOrNull()?.url?.ifEmpty { null } ?: trackArtwork
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(thumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (artUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(artUrl)
                                                    .crossfade(150)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                onSuccess = {
                                                    if (page == currentQueueIndex || (queue.isEmpty() && page == 0)) {
                                                        val bm = it.result.image.toImageBitmap()
                                                        extractedBitmap = bm
                                                        sharedViewModel.setBitmap(bm)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dynamicArtworkToInfoSpacing))

            val playPauseRoundness by animateDpAsState(
                targetValue = if (controllerState.isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness"
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dynamicHorizontalPadding)
            ) {
                AnimatedContent(
                    targetState = showInlineLyrics,
                    label = "ThumbnailAnimation"
                ) { showLyrics ->
                    if (showLyrics) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius * 2))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (trackArtwork.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(trackArtwork)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = trackTitle,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "title"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentSongData?.isExplicit == true) {
                            ExplicitBadge(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = firstArtist.ifBlank { "Unknown Artist" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = TextBackgroundColor.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                val song = nowPlayingState?.songEntity
                                (song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                    ?: currentSongData?.songInfoData?.authorId)?.let { channelId ->
                                    onDismiss()
                                    navController.navigate(ArtistDestination(channelId = channelId))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val shareShape = RoundedCornerShape(
                    topStart = 50.dp, bottomStart = 50.dp,
                    topEnd = 3.dp, bottomEnd = 3.dp
                )
                val favShape = RoundedCornerShape(
                    topStart = 3.dp, bottomStart = 3.dp,
                    topEnd = 50.dp, bottomEnd = 50.dp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            val songId = currentSongData?.songInfoData?.videoId?.takeIf { it.isNotBlank() }
                                ?: nowPlayingState?.mediaItem?.mediaId?.removePrefix("Video")?.takeIf { it.isNotBlank() }
                                ?: nowPlayingState?.track?.videoId?.takeIf { it.isNotBlank() }
                                ?: ""
                            if (songId.isNotEmpty()) {
                                shareUrl(
                                    title = trackTitle,
                                    url = "https://music.youtube.com/watch?v=$songId"
                                )
                            }
                        },
                        shape = shareShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier.size(dynamicActionButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_share_curved),
                            contentDescription = "Share",
                            tint = iconButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    val currentTrackId = nowPlayingState?.mediaItem?.mediaId?.takeIf { it.isNotBlank() }
                        ?: currentSongData?.songInfoData?.videoId?.takeIf { it.isNotBlank() }
                        ?: currentSongData?.nowPlayingTitle
                    var localOptimisticLiked by remember(currentTrackId) { mutableStateOf<Boolean?>(null) }
                    val isLiked = localOptimisticLiked ?: (likeStatus || controllerState.isLiked)
                    val popScale = remember { Animatable(1f) }
                    val popElevationY = remember { Animatable(0f) }
                    val popRotationZ = remember { Animatable(0f) }
                    val popRotationX = remember { Animatable(0f) }
                    val burstScale = remember { Animatable(0.4f) }
                    val burstAlpha = remember { Animatable(0f) }
                    var isPopping by remember { mutableStateOf(false) }
                    val currentDensity = LocalDensity.current.density

                    Box(
                        modifier = Modifier.size(dynamicActionButtonSize),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledIconButton(
                            onClick = {
                                val nextState = !isLiked
                                localOptimisticLiked = nextState
                                if (nextState) {
                                    isPopping = true
                                    coroutineScope.launch {
                                        launch {
                                            burstScale.snapTo(0.4f)
                                            burstAlpha.snapTo(0.85f)
                                            burstScale.animateTo(2.3f, tween(360, easing = FastOutSlowInEasing))
                                            burstAlpha.animateTo(0f, tween(160, easing = LinearEasing))
                                        }
                                        launch {
                                            popScale.snapTo(0.4f)
                                            popElevationY.snapTo(0f)
                                            popRotationZ.snapTo(-16f)
                                            popRotationX.snapTo(-25f)

                                            launch {
                                                popElevationY.animateTo(-16f, tween(140, easing = FastOutSlowInEasing))
                                                popElevationY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                            }
                                            launch {
                                                popRotationZ.animateTo(10f, tween(130))
                                                popRotationZ.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                            }
                                            launch {
                                                popRotationX.animateTo(0f, tween(260))
                                            }
                                            popScale.animateTo(
                                                targetValue = 2.15f,
                                                animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMedium)
                                            )
                                            popScale.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                            )
                                            isPopping = false
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        popScale.snapTo(1f)
                                        popScale.animateTo(0.65f, tween(100))
                                        popScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                                sharedViewModel.addToYouTubeLiked()
                            },
                            shape = favShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = textButtonColor,
                                contentColor = iconButtonColor
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Crossfade(
                                targetState = isLiked,
                                animationSpec = tween(durationMillis = 150),
                                label = "yt_like_crossfade"
                            ) { liked ->
                                Icon(
                                    painter = painterResource(
                                        if (liked) Res.drawable.favorite else Res.drawable.favorite_border
                                    ),
                                    contentDescription = if (liked) "Liked on YouTube" else "Like on YouTube",
                                    tint = if (liked) Color(0xFFE53935) else iconButtonColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            alpha = if (isPopping) 0f else 1f
                                            if (!isLiked) {
                                                scaleX = popScale.value
                                                scaleY = popScale.value
                                            }
                                        }
                                )
                            }
                        }

                        if (isPopping) {
                            Box(
                                modifier = Modifier
                                    .size(dynamicActionButtonSize)
                                    .graphicsLayer {
                                        scaleX = burstScale.value
                                        scaleY = burstScale.value
                                        alpha = burstAlpha.value
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0x55FF2A55))
                            )

                            val particles = listOf(
                                0.0 to 1.0f,
                                60.0 to 0.85f,
                                120.0 to 1.1f,
                                180.0 to 0.95f,
                                240.0 to 1.05f,
                                300.0 to 0.9f
                            )
                            particles.forEach { (angleDeg, distanceMult) ->
                                val rad = kotlin.math.PI * 2 * (angleDeg / 360.0)
                                val dist = burstScale.value * 34f * distanceMult * currentDensity
                                val x = (kotlin.math.cos(rad) * dist).toFloat()
                                val y = (kotlin.math.sin(rad) * dist).toFloat()

                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .graphicsLayer {
                                            translationX = x
                                            translationY = y
                                            alpha = burstAlpha.value
                                            scaleX = (1.2f - (burstScale.value / 2.5f)).coerceAtLeast(0.3f)
                                            scaleY = (1.2f - (burstScale.value / 2.5f)).coerceAtLeast(0.3f)
                                        }
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B5C))
                                )
                            }

                            Icon(
                                painter = painterResource(Res.drawable.favorite),
                                contentDescription = null,
                                tint = Color(0xFFFF2A55),
                                modifier = Modifier
                                    .size(26.dp)
                                    .graphicsLayer {
                                        cameraDistance = 16f * currentDensity
                                        scaleX = popScale.value
                                        scaleY = popScale.value
                                        translationY = popElevationY.value * currentDensity
                                        rotationZ = popRotationZ.value
                                        rotationX = popRotationX.value
                                        shadowElevation = 24f * (popScale.value - 1f).coerceAtLeast(0f)
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dynamicInfoToSliderSpacing))

            PlayerTimelineSection(
                sharedViewModel = sharedViewModel,
                isPlaying = controllerState.isPlaying,
                textButtonColor = textButtonColor,
                TextBackgroundColor = TextBackgroundColor,
                horizontalPadding = dynamicHorizontalPadding,
                screenHeight = screenHeight,
            )

            Spacer(modifier = Modifier.height(dynamicSliderToControlsSpacing))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dynamicHorizontalPadding)
            ) {
                val backSource = remember { MutableInteractionSource() }
                val nextSource = remember { MutableInteractionSource() }
                val ppSource = remember { MutableInteractionSource() }

                val isPPPressed by ppSource.collectIsPressedAsState()
                val isBackPressed by backSource.collectIsPressedAsState()
                val isNextPressed by nextSource.collectIsPressedAsState()

                val ppWeight by animateFloatAsState(
                    targetValue = if (isPPPressed) 1.9f
                    else if (isBackPressed || isNextPressed) 1.1f
                    else 1.3f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "playPauseWeight"
                )
                val backWeight by animateFloatAsState(
                    targetValue = if (isBackPressed) 0.65f
                    else if (isPPPressed) 0.35f
                    else 0.45f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "backButtonWeight"
                )
                val nextWeight by animateFloatAsState(
                    targetValue = if (isNextPressed) 0.65f
                    else if (isPPPressed) 0.35f
                    else 0.45f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "nextButtonWeight"
                )

                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Previous) },
                    enabled = controllerState.isPreviousAvailable,
                    shape = RoundedCornerShape(50),
                    interactionSource = backSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = sideButtonContainerColor,
                        contentColor = sideButtonContentColor,
                        disabledContainerColor = sideButtonContainerColor.copy(alpha = 0.4f),
                        disabledContentColor = sideButtonContentColor.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.height(dynamicControlsHeight).weight(backWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                androidx.compose.material3.Button(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                    shape = RoundedCornerShape(50),
                    interactionSource = ppSource,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = textButtonColor,
                        contentColor = iconButtonColor
                    ),
                    modifier = Modifier.height(dynamicControlsHeight).weight(ppWeight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (controllerState.isPlaying) Res.drawable.pause else Res.drawable.play
                            ),
                            contentDescription = null,
                            tint = iconButtonColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (controllerState.isPlaying) "Pause" else "Play",
                            style = MaterialTheme.typography.titleMedium,
                            color = iconButtonColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Next) },
                    enabled = controllerState.isNextAvailable,
                    shape = RoundedCornerShape(50),
                    interactionSource = nextSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = sideButtonContainerColor,
                        contentColor = sideButtonContentColor,
                        disabledContainerColor = sideButtonContainerColor.copy(alpha = 0.4f),
                        disabledContentColor = sideButtonContentColor.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.height(dynamicControlsHeight).weight(nextWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dynamicControlsToBottomSpacing))
        }

        BottomSheet(
            state = queueSheetState,
            modifier = Modifier.fillMaxSize(),
            background = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(sheetBg)
                )
            },
            collapsedContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dynamicHorizontalPadding)
                        .padding(bottom = bottomInsets)
                        .fillMaxHeight()
                ) {
                    val buttonSize = dynamicBottomButtonSize
                    val iconSize = 24.dp
                    val queueShape = RoundedCornerShape(
                        topStart = 50.dp, bottomStart = 50.dp,
                        topEnd = 3.dp, bottomEnd = 3.dp
                    )
                    val middleShape = RoundedCornerShape(3.dp)
                    val repeatShape = RoundedCornerShape(
                        topStart = 3.dp, bottomStart = 3.dp,
                        topEnd = 50.dp, bottomEnd = 50.dp
                    )

                    PlayerQueueButton(
                        icon = Res.drawable.queue_music,
                        isActive = false,
                        shape = queueShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        borderColor = buttonBorderColor,
                        iconSize = iconSize,
                        onClick = { queueSheetState.expandSoft() }
                    )

                    val isSleepTimerActive = sleepTimerState.timeRemaining > 0
                    PlayerQueueButton(
                        icon = Res.drawable.bedtime,
                        isActive = isSleepTimerActive,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        borderColor = buttonBorderColor,
                        iconSize = iconSize,
                        onClick = { showSleepTimerDialog = true }
                    )

                    val isShuffle = controllerState.isShuffle
                    PlayerQueueButton(
                        icon = Res.drawable.shuffle,
                        isActive = isShuffle,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        borderColor = buttonBorderColor,
                        iconSize = iconSize,
                        onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) }
                    )

                    PlayerQueueButton(
                        icon = Res.drawable.lyrics,
                        isActive = showInlineLyrics,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        borderColor = buttonBorderColor,
                        iconSize = iconSize,
                        enabled = true,
                        onClick = { showInlineLyrics = !showInlineLyrics }
                    )

                    val isRepeat = controllerState.repeatState != RepeatState.None
                    val isRepeatOne = controllerState.repeatState == RepeatState.One
                    PlayerQueueButton(
                        icon = if (isRepeatOne) Res.drawable.baseline_repeat_one_24 else Res.drawable.repeat,
                        isActive = isRepeat,
                        shape = repeatShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        borderColor = buttonBorderColor,
                        iconSize = iconSize,
                        onClick = { sharedViewModel.onUIEvent(UIEvent.Repeat) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(textButtonColor)
                            .clickable { showMoreOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.more_horiz),
                            contentDescription = null,
                            tint = iconButtonColor,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        ) {
            FreshQueueContent(
                nestedScrollConnection = queueSheetState.preUpPostDownNestedScrollConnection,
                onDismiss = { queueSheetState.collapseSoft() },
                backgroundColor = sheetBg,
                accentColor = activeAccentColor,
                contentColor = TextBackgroundColor,
            )
        }

        ModernMoreOptionsTwoStageSheet(
            visible = showMoreOptions,
            onDismiss = { showMoreOptions = false },
            navController = navController,
            onNavigateToOtherScreen = onDismiss,
            song = nowPlayingState?.songEntity,
            viewModel = nowPlayingBottomSheetViewModel,
            backgroundColor = sheetBg,
            accentColor = activeAccentColor,
            contentColor = TextBackgroundColor,
            onLyricsClick = {
                queueSheetState.collapseSoft()
                showInlineLyrics = true
            },
        )

        if (showShareLyricsSheet && currentSongData?.lyricsData != null) {
            ShareLyricsSheet(
                lines = currentSongData.lyricsData!!.toShareLyricsLines(),
                songTitle = trackTitle,
                artistName = trackArtist,
                artwork = extractedBitmap,
                seedColor = startColor.value,
                initialLineIndex = shareInitialLineIndex,
                onDismiss = { showShareLyricsSheet = false }
            )
        }

        if (showSleepTimerDialog) {
            val activeRemaining = sleepTimerState.timeRemaining
            var sleepTimerDefault by remember { mutableFloatStateOf(30f) }
            val initialSliderValue = remember {
                val activeMins = if (activeRemaining > 0) (activeRemaining / 60).toFloat() else sleepTimerDefault
                activeMins.coerceIn(5f, 120f)
            }
            var sleepTimerValue by remember { mutableFloatStateOf(initialSliderValue) }

            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = {
                    Text(
                        text = "Sleep Timer",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val activeMinutes = activeRemaining / 60
                        val activeSeconds = activeRemaining % 60
                        Text(
                            text = if (activeRemaining > 0) {
                                if (activeMinutes > 0) "Active: ${activeMinutes}m ${activeSeconds}s remaining"
                                else "Active: ${activeSeconds}s remaining"
                            } else "${sleepTimerValue.roundToInt()} minutes",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = 22
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    sleepTimerDefault = sleepTimerValue
                                }
                            ) {
                                Text("Set as default")
                            }

                            if (activeRemaining > 0) {
                                OutlinedButton(
                                    onClick = {
                                        showSleepTimerDialog = false
                                        sharedViewModel.stopSleepTimer()
                                    }
                                ) {
                                    Text("Stop Timer")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showSleepTimerDialog = false
                                sharedViewModel.stopSleepTimer()
                            }
                        ) {
                            Text("Reset")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showSleepTimerDialog = false }
                            ) {
                                Text("Cancel")
                            }
                            TextButton(
                                onClick = {
                                    showSleepTimerDialog = false
                                    sharedViewModel.setSleepTimer(sleepTimerValue.roundToInt())
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    }
                },
                dismissButton = null
            )
        }
    }
}

/**
 * Pill-shaped action button used in the bottom bar of the player.
 * Matches Sonique Queue.kt PlayerQueueButton composable.
 * Active state = filled with textButtonColor; inactive = semi-transparent outline.
 */
@Composable
fun PlayerQueueButton(
    icon: DrawableResource,
    isActive: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    textButtonColor: Color = Color.White,
    iconButtonColor: Color = Color.Black,
    borderColor: Color = Color.White.copy(alpha = 0.3f),
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) textButtonColor else Color.Transparent,
        label = "queueButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) iconButtonColor else Color.White,
        label = "queueButtonContent"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isActive) Color.Transparent else borderColor,
        label = "queueButtonBorder"
    )
    val borderModifier = if (isActive) Modifier else Modifier.border(
        width = 1.dp,
        color = animatedBorderColor,
        shape = shape
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .then(borderModifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Resizable icon button using a painter resource.
 * Matches Sonique ResizableIconButton used in the old-style controls row.
 */
@Composable
fun ResizableIconButton(
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = false),
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
    )
}

/**
 * Isolated timeline and progress slider section.
 * Subscribes to sharedViewModel.timeline locally so that playback ticks
 * do not cause the entire player screen to recompose.
 */
@Composable
private fun PlayerTimelineSection(
    sharedViewModel: SharedViewModel,
    isPlaying: Boolean,
    textButtonColor: Color,
    TextBackgroundColor: Color,
    horizontalPadding: Dp,
    screenHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    var sliderPosition by remember { mutableStateOf<Float?>(null) }
    val animatedPercent = remember { Animatable(0f) }

    LaunchedEffect(timelineState.current, isPlaying, timelineState.total) {
        if (sliderPosition != null) return@LaunchedEffect

        if (timelineState.total <= 0L || timelineState.current < 0L) {
            animatedPercent.snapTo(0f)
            return@LaunchedEffect
        }

        val currentPercent = (timelineState.current.toFloat() / timelineState.total.toFloat()).coerceIn(0f, 1f) * 100f
        val drift = kotlin.math.abs(animatedPercent.value - currentPercent)

        if (drift > 3f || !isPlaying || timelineState.loading) {
            animatedPercent.snapTo(currentPercent)
        } else {
            val remainingMs = (timelineState.total - timelineState.current).coerceAtLeast(0L)
            val stepDuration = minOf(1000L, remainingMs).toInt().coerceAtLeast(1)
            val nextPercent = ((timelineState.current + stepDuration).toFloat() / timelineState.total.toFloat()).coerceIn(0f, 1f) * 100f
            animatedPercent.animateTo(
                targetValue = nextPercent,
                animationSpec = tween(durationMillis = stepDuration, easing = LinearEasing)
            )
        }
    }

    val displayPosition = sliderPosition ?: animatedPercent.value.coerceIn(0f, 100f)

    val sliderColors = SliderDefaults.colors(
        activeTrackColor = textButtonColor,
        activeTickColor = textButtonColor,
        thumbColor = textButtonColor,
        inactiveTrackColor = Color.White.copy(alpha = 0.4f),
        disabledActiveTrackColor = textButtonColor,
        disabledInactiveTrackColor = Color.White.copy(alpha = 0.4f),
        disabledThumbColor = textButtonColor,
    )

    val sliderToDurationSpacing = 4.dp

    Column(modifier = modifier) {
        Slider(
            value = displayPosition.coerceIn(0f, 100f),
            valueRange = 0f..100f,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                sliderPosition?.let {
                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(it))
                }
                sliderPosition = null
            },
            colors = sliderColors,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )

        Spacer(Modifier.height(sliderToDurationSpacing))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding + 4.dp)
        ) {
            val currentElapsedMs = if (sliderPosition != null) {
                (timelineState.total * (sliderPosition!! / 100f)).toLong()
            } else {
                timelineState.current.coerceIn(0L, timelineState.total.coerceAtLeast(0L))
            }

            Text(
                text = formatDuration(currentElapsedMs),
                style = MaterialTheme.typography.labelMedium,
                color = TextBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (timelineState.total > 0) formatDuration(timelineState.total) else "",
                style = MaterialTheme.typography.labelMedium,
                color = TextBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

