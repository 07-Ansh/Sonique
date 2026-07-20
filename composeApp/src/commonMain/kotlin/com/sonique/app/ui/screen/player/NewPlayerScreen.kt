package com.sonique.app.ui.screen.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sonique.app.extension.formatDuration
import com.sonique.app.ui.component.LyricsView
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
import sonique.composeapp.generated.resources.fullscreen
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
import sonique.composeapp.generated.resources.timer
import kotlin.math.roundToLong

// Matches Metrolist constants
private val PlayerHorizontalPadding = 32.dp
private val ThumbnailCornerRadius = 3.dp  // cornerRadius * 2 = 6.dp applied in UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlayerScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onDismiss: () -> Unit = {},
) {
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val currentSongData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val trackTitle = currentSongData?.nowPlayingTitle ?: ""
    val trackArtist = currentSongData?.artistName ?: ""
    val trackArtwork = currentSongData?.thumbnailURL ?: ""

    // Sheet/dialog visibility state
    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // ── Colors — matching Metrolist BLUR background mode defaults ────────────
    val TextBackgroundColor = Color.White
    val textButtonColor = Color.White
    val iconButtonColor = Color.Black
    val sideButtonContainerColor = Color.White.copy(alpha = 0.2f)
    val sideButtonContentColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)) // Metrolist surfaceContainer default dark
    ) {
        // ── Blur background — matches Metrolist PlayerBackgroundStyle.BLUR ──
        AnimatedContent(
            targetState = trackArtwork,
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
            // Mirrors Metrolist Thumbnail.kt: statusBarsPadding + Column with ThumbnailHeader
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
                                text = "Now Playing",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBackgroundColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your Queue",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }
                    }

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
                Column(modifier = Modifier.weight(1f)) {
                    // Title with AnimatedContent + basicMarquee
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
                            color = TextBackgroundColor,
                            modifier = Modifier.basicMarquee(
                                iterations = 1, initialDelayMillis = 3000, velocity = 30.dp
                            )
                        )
                    }

                    // Artist
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = 1, initialDelayMillis = 3000, velocity = 30.dp
                            )
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = trackArtist,
                            style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Pill-shaped Share + Favourite buttons — exact shapes from Player.kt lines 1126-1140
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
                    // Share button — shares current track link
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
                            painter = painterResource(Res.drawable.fullscreen),
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
                            tint = if (isLiked) MaterialTheme.colorScheme.error else iconButtonColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Slider — SliderStyle.SLIM from Player.kt lines 1449-1482 ──────
            // Custom Canvas-drawn track (PlayerSliderTrack), no thumb, 10dp height.
            var sliderPosition by remember { mutableStateOf<Long?>(null) }
            val displayPosition = sliderPosition?.toFloat()
                ?: if (timelineState.total > 0) timelineState.current.toFloat() else 0f
            val duration = timelineState.total.toFloat()

            // Colors match PlayerSliderColors.getSliderColors() for BLUR background:
            // activeColor = White, inactiveColor = White@40%
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
                value = displayPosition,
                valueRange = 0f..(if (duration > 0f) duration else 0f),
                onValueChange = { sliderPosition = it.toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        sharedViewModel.onUIEvent(UIEvent.UpdateProgress(it.toFloat()))
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
                    text = formatDuration((sliderPosition ?: timelineState.current).coerceAtLeast(0L)),
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

                // Play/Pause — white pill with icon + text label
                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                    shape = RoundedCornerShape(50),
                    interactionSource = ppSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
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

                // Sleep Timer button — shows a simple info dialog (no timer service in Sonique)
                PlayerQueueButton(
                    icon = Res.drawable.timer,
                    isActive = false,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { showSleepTimerDialog = true }
                )

                // Shuffle button
                val isShuffle = controllerState.isShuffle
                PlayerQueueButton(
                    icon = if (isShuffle) Res.drawable.shuffle_on else Res.drawable.shuffle,
                    isActive = isShuffle,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) }
                )

                // Lyrics button — toggles inline lyrics overlay
                val hasLyrics = currentSongData?.lyricsData != null
                PlayerQueueButton(
                    icon = Res.drawable.lyrics,
                    isActive = showLyricsSheet,
                    shape = middleShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    enabled = hasLyrics,
                    onClick = { if (hasLyrics) showLyricsSheet = !showLyricsSheet }
                )

                // Repeat button
                val isRepeat = controllerState.repeatState != RepeatState.None
                val isRepeatOne = controllerState.repeatState == RepeatState.One
                PlayerQueueButton(
                    icon = if (isRepeat) {
                        if (isRepeatOne) Res.drawable.repeat_one_on else Res.drawable.repeat_on
                    } else {
                        Res.drawable.repeat
                    },
                    isActive = isRepeat,
                    shape = repeatShape,
                    modifier = Modifier.size(buttonSize),
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    iconSize = iconSize,
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Repeat) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // More button — navigate to full NowPlayingScreen
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(textButtonColor)
                        .clickable { navController.navigate("nowPlaying") },
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

            // ── Lyrics sheet overlay ─────────────────────────────────────────
            if (showLyricsSheet) {
                val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showLyricsSheet = false },
                    sheetState = lyricsSheetState,
                    containerColor = Color(0xFF1C1B1F),
                ) {
                    currentSongData?.lyricsData?.let { lyricsData ->
                        LyricsView(
                            lyricsData = lyricsData,
                            timeLine = sharedViewModel.timeline,
                            onLineClick = { progress ->
                                sharedViewModel.onUIEvent(UIEvent.UpdateProgress(progress))
                            },
                            backgroundColor = Color(0xFF1C1B1F),
                            playerContentColor = Color.White,
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No lyrics available", color = Color.White)
                        }
                    }
                }
            }

            // ── Sleep timer info dialog ──────────────────────────────────────
            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = { Text("Sleep Timer") },
                    text = { Text("Sleep timer is not available in this version.") },
                    confirmButton = {
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            // ── Queue sheet ──────────────────────────────────────────────────
            if (showQueueSheet) {
                QueueBottomSheet(onDismiss = { showQueueSheet = false })
            }
        }
    }
}

/**
 * Pill-shaped action button used in the bottom bar of the player.
 * Matches Metrolist Queue.kt PlayerQueueButton composable.
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
        targetValue = if (isActive) textButtonColor else Color.White.copy(alpha = 0.15f),
        label = "queueButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) iconButtonColor else Color.White,
        label = "queueButtonContent"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Resizable icon button using a painter resource.
 * Matches Metrolist ResizableIconButton used in the old-style controls row.
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
