package com.sonique.app.ui.screen.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.ui.component.GoogleCircularProgressIndicator
import com.sonique.app.ui.component.LyricsShimmer
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sonique.app.extension.formatDuration
import com.sonique.app.ui.component.FreshPlayerMenuSheet
import com.sonique.app.ui.component.FreshQueueSheet
import com.sonique.app.ui.component.LyricsView
import com.sonique.app.ui.component.NowPlayingBottomSheet
import com.sonique.app.ui.component.QueueBottomSheet
import com.sonique.app.viewModel.NowPlayingScreenData
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import com.sonique.domain.mediaservice.handler.RepeatState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.favorite
import sonique.composeapp.generated.resources.favorite_border
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

// Matches Sonique constants
private val PlayerHorizontalPadding = 32.dp
private val ThumbnailCornerRadius = 3.dp  // cornerRadius * 2 = 6.dp applied in UI

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewPlayerScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onDismiss: () -> Unit = {},
) {
    val nowPlayingBottomSheetViewModel: NowPlayingBottomSheetViewModel = koinViewModel()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val currentSongData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val queueData by sharedViewModel.queueData.collectAsStateWithLifecycle(initialValue = null)
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val sleepTimerState by sharedViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val ambienceMode by sharedViewModel.ambienceMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val trackTitle = currentSongData?.nowPlayingTitle ?: ""
    val trackArtist = currentSongData?.artistName ?: ""
    val firstArtist = remember(trackArtist) {
        if (trackArtist.isBlank()) ""
        else trackArtist.split(",", ";", "&", " feat.", " Feat.", " ft.", " Ft.").firstOrNull()?.trim() ?: trackArtist
    }
    val trackArtwork = currentSongData?.thumbnailURL ?: ""
    val playingContextText = remember(currentSongData?.playlistName, queueData?.data?.playlistName) {
        currentSongData?.playlistName?.takeIf { it.isNotBlank() }
            ?: queueData?.data?.playlistName?.takeIf { it.isNotBlank() }
            ?: "Current Queue"
    }

    // Sheet/dialog visibility state
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInlineLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showLyricsMenu by remember { mutableStateOf(false) }
    var isLyricsAutoScrollEnabled by remember { mutableStateOf(true) }

    val paletteState = com.kmpalette.rememberPaletteState()
    val startColor = remember { androidx.compose.animation.Animatable(Color(0xFF1C1B1F)) }

    LaunchedEffect(currentSongData?.bitmap) {
        currentSongData?.bitmap?.let { bitmap ->
            paletteState.generate(bitmap)
        }
    }

    LaunchedEffect(paletteState.palette) {
        paletteState.palette?.let { palette ->
            startColor.animateTo(palette.getColorFromPalette())
        }
    }


    // ── GPU Hardware-Accelerated Motion Engine ──────────────────────
    val offsetYAnimatable = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()

    // Reset translation to 0f whenever screen is opened
    LaunchedEffect(Unit) {
        offsetYAnimatable.snapTo(0f)
    }

    // ── Colors — matching Sonique BLUR background mode defaults ────────────
    val TextBackgroundColor = Color.White
    val textButtonColor = Color.White
    val iconButtonColor = Color.Black
    val sideButtonContainerColor = Color.White.copy(alpha = 0.12f)
    val sideButtonContentColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // GPU-accelerated hardware translation — 120Hz zero-recomposition lag
                translationY = offsetYAnimatable.value.coerceAtLeast(0f)
                alpha = (1f - (offsetYAnimatable.value / 1200f)).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
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
                                    targetValue = 1800f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                onDismiss()
                                offsetYAnimatable.snapTo(0f)
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
            .background(Color(0xFF1C1B1F)) // Sonique surfaceContainer default dark
    ) {
        // ── Blur background — links to player artwork when Ambience Mode is enabled, solid background when disabled ──
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
                            .blur(150.dp)
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
                .animateContentSize()
        ) {
            // ── Thumbnail section (header + artwork) ─────────────────────────
            // Mirrors Sonique Thumbnail.kt: statusBarsPadding + Column with ThumbnailHeader
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // statusBarsPadding matches Thumbnail.kt line 329
                    Spacer(modifier = Modifier.statusBarsPadding())

                    // ThumbnailHeader — matches Thumbnail.kt ThumbnailHeader composable
                    AnimatedVisibility(
                        visible = !showInlineLyrics,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 48.dp)
                            ) {
                                Text(
                                    text = "NOW PLAYING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Normal,
                                    color = TextBackgroundColor.copy(alpha = 0.65f),
                                    letterSpacing = 1.2.sp
                                )
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
                    }

                    // Album artwork or Lyrics - toggles inline where album art is
                    AnimatedContent(
                        targetState = showInlineLyrics,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "thumbnailOrLyrics"
                    ) { showLyrics ->
                        if (showLyrics) {
                            if (currentSongData?.lyricsData != null) {
                                LyricsView(
                                    lyricsData = currentSongData!!.lyricsData!!,
                                    timeLine = sharedViewModel.timeline,
                                    onLineClick = { progress ->
                                        sharedViewModel.onUIEvent(UIEvent.UpdateProgress(progress))
                                    },
                                    isAutoScrollEnabledState = isLyricsAutoScrollEnabled,
                                    onAutoScrollStateChanged = { isLyricsAutoScrollEnabled = it },
                                    backgroundColor = Color.Transparent,
                                    playerContentColor = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    LyricsShimmer(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    GoogleCircularProgressIndicator()
                                }
                            }
                        } else {

                            // Album artwork — matches Thumbnail.kt BoxWithConstraints approach
                            // thumbnailSize = containerWidth - (PlayerHorizontalPadding * 2)
                            BoxWithConstraints(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val thumbnailSize = maxWidth - (PlayerHorizontalPadding * 2)
                                Box(
                                    modifier = Modifier
                                        .size(thumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius * 2)) // 6.dp
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (trackArtwork.isNotEmpty()) {
                                        AsyncImage(
                                            model = trackArtwork,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── controlsContent — mirrors Player.kt controlsContent lambda ───
            val playPauseRoundness by animateDpAsState(
                targetValue = if (controllerState.isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness"
            )

            // Song info + action buttons row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                // Conditional small artwork thumbnail next to details (only shown when lyrics are open)
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
                                        model = trackArtwork,
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
                    // Title with AnimatedContent (no marquee, medium font weight)
                    AnimatedContent(
                        targetState = trackTitle,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "title"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // First Artist only (no marquee, normal font weight)
                    Text(
                        text = firstArtist,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = TextBackgroundColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Pill-shaped buttons — exact shapes from Player.kt lines 1126-1140
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
                    // Share button
                    FilledIconButton(
                        onClick = {
                            val songId = currentSongData?.songInfoData?.videoId ?: ""
                            if (songId.isNotEmpty()) {
                                val intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/watch?v=$songId"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, null))
                            }
                        },
                        shape = shareShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_share_24),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Like button
                    val isLiked = controllerState.isLiked
                    FilledIconButton(
                        onClick = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) },
                        shape = favShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) Res.drawable.favorite else Res.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else iconButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Slider ──────
            var sliderPosition by remember { mutableStateOf<Float?>(null) }
            val displayPosition = sliderPosition
                ?: if (timelineState.total > 0) {
                    (timelineState.current.toFloat() / timelineState.total.toFloat()) * 100f
                } else {
                    0f
                }

            val sliderColors = SliderDefaults.colors(
                activeTrackColor = textButtonColor,
                activeTickColor = textButtonColor,
                thumbColor = textButtonColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                disabledActiveTrackColor = textButtonColor,
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.4f),
                disabledThumbColor = textButtonColor,
            )

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
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            )

            Spacer(Modifier.height(4.dp))

            // Duration labels — matches Player.kt lines 1487-1510
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = formatDuration(
                        if (sliderPosition != null) {
                            (timelineState.total * (sliderPosition!! / 100f)).roundToLong()
                        } else {
                            timelineState.current.coerceAtLeast(0L)
                        }
                    ),
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

            Spacer(Modifier.height(24.dp))

            // ── New design playback controls — Player.kt lines 1520-1701 ─────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
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

                // Previous
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
                    modifier = Modifier.height(68.dp).weight(backWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play/Pause — white pill with icon + text label (uses Button to keep color rendering correct)
                androidx.compose.material3.Button(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                    shape = RoundedCornerShape(50),
                    interactionSource = ppSource,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = textButtonColor,
                        contentColor = iconButtonColor
                    ),
                    modifier = Modifier.height(68.dp).weight(ppWeight)
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

                // Next
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
                    modifier = Modifier.height(68.dp).weight(nextWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // ── Bottom action row — matches Queue.kt new design (lines 266-417) ─
            // Queue · Sleep Timer · Shuffle · Lyrics · Repeat · More (circle)

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 12.dp)
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .pointerInput(Unit) {
                        var totalY = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalY = 0f },
                            onDragEnd = {
                                if (totalY < -50f) {
                                    showQueueSheet = true
                                }
                            },
                            onDragCancel = { totalY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                totalY += dragAmount
                            }
                        )
                    }
            ) {
                val buttonSize = 42.dp
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

                // Queue button
                PlayerQueueButton(
                    icon = Res.drawable.queue_music,
                    isActive = false,
                    shape = queueShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { showQueueSheet = true }
                )

                // Sleep Timer button
                val isSleepTimerActive = sleepTimerState.timeRemaining > 0
                PlayerQueueButton(
                    icon = Res.drawable.bedtime,
                    isActive = isSleepTimerActive,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { showSleepTimerDialog = true }
                )

                // Shuffle button — use transparent shuffle drawable always to prevent black background box
                val isShuffle = controllerState.isShuffle
                PlayerQueueButton(
                    icon = Res.drawable.shuffle,
                    isActive = isShuffle,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) }
                )

                // Lyrics button — toggles inline lyrics overlay instantly
                PlayerQueueButton(
                    icon = Res.drawable.lyrics,
                    isActive = showInlineLyrics,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    enabled = true,
                    onClick = { showInlineLyrics = !showInlineLyrics }
                )


                // Repeat button — use transparent repeat/repeat_one drawables always
                val isRepeat = controllerState.repeatState != RepeatState.None
                val isRepeatOne = controllerState.repeatState == RepeatState.One
                PlayerQueueButton(
                    icon = if (isRepeatOne) Res.drawable.baseline_repeat_one_24 else Res.drawable.repeat,
                    isActive = isRepeat,
                    shape = repeatShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Repeat) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // More button — shows NowPlayingBottomSheet instead of crashing with navigation
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

            // ── More options bottom sheet ────────────────────────────────────
            if (showMoreOptions) {
                FreshPlayerMenuSheet(
                    onDismiss = { showMoreOptions = false },
                    navController = navController,
                    song = nowPlayingState?.songEntity,
                    viewModel = nowPlayingBottomSheetViewModel,
                    backgroundColor = if (ambienceMode && startColor.value != Color(0xFF1C1B1F)) startColor.value.copy(alpha = 0.92f) else null,
                    onShowSleepTimer = { showSleepTimerDialog = true }
                )
            }

            // ── Sleep timer info dialog ──────────────────────────────────────
            if (showSleepTimerDialog) {
                val activeRemaining = sleepTimerState.timeRemaining
                var sleepTimerDefault by remember { mutableFloatStateOf(30f) }
                var sleepTimerValue by remember(activeRemaining) {
                    mutableFloatStateOf(if (activeRemaining > 0) activeRemaining.toFloat() else sleepTimerDefault)
                }

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
                            Text(
                                text = if (activeRemaining > 0) "Active: ${formatDuration(activeRemaining * 1000L)}"
                                else "${sleepTimerValue.roundToInt()} minutes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Slider(
                                value = sleepTimerValue,
                                onValueChange = { sleepTimerValue = it },
                                valueRange = 5f..120f,
                                steps = (120 - 5) / 5 - 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val isAtDefault = sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt()
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

                                OutlinedButton(
                                    onClick = {
                                        showSleepTimerDialog = false
                                        sharedViewModel.stopSleepTimer()
                                    }
                                ) {
                                    Text("End of song")
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



            // ── Queue sheet ──────────────────────────────────────────────────
            if (showQueueSheet) {
                FreshQueueSheet(
                    onDismiss = { showQueueSheet = false },
                    backgroundColor = if (ambienceMode && startColor.value != Color(0xFF1C1B1F)) startColor.value.copy(alpha = 0.92f) else null,
                )
            }
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
    val borderModifier = if (isActive) Modifier else Modifier.border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.3f),
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
