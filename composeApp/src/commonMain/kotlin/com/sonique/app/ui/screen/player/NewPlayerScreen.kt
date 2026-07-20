package com.sonique.app.ui.screen.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import com.sonique.app.ui.component.LyricsView
import com.sonique.app.ui.component.QueueBottomSheet
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import com.sonique.app.extension.formatDuration
import com.sonique.domain.mediaservice.handler.RepeatState
import com.sonique.domain.mediaservice.handler.ControlState
import com.sonique.domain.data.model.streams.TimeLine
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_access_alarm_24
import sonique.composeapp.generated.resources.baseline_fullscreen_24
import sonique.composeapp.generated.resources.more_horiz
import sonique.composeapp.generated.resources.fullscreen
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlayerScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val currentSongData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()

    var showInlineLyrics by remember { mutableStateOf(false) }

    val trackTitle = currentSongData?.nowPlayingTitle ?: ""
    val trackArtist = currentSongData?.artistName ?: ""
    val trackArtwork = currentSongData?.thumbnailURL ?: ""

    // Setup active/inactive colors & backgrounds matching Metrolist Blur Style
    val TextBackgroundColor = Color.White
    val buttonBgColor = Color.White
    val buttonIconColor = Color.Black
    val sideButtonContainerColor = Color.White.copy(alpha = 0.2f)
    val sideButtonContentColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Metrolist-style Album Art Blur Backdrop
        if (trackArtwork.isNotEmpty()) {
            AsyncImage(
                model = trackArtwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(120.dp)
                    .alpha(0.35f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // NewPlayerContent: Top Segment Navigation & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonWithCustomShape(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    shape = RoundedCornerShape(24.dp),
                    onClick = onDismiss
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButtonWithCustomShape(
                        painterResource = painterResource(Res.drawable.fullscreen),
                        shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 3.dp, bottomEnd = 3.dp),
                        onClick = { }
                    )
                    IconButtonWithCustomShape(
                        painterResource = painterResource(Res.drawable.more_horiz),
                        shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp),
                        onClick = { }
                    )
                }
            }

            // NewPlayerArtwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showInlineLyrics,
                    label = "LyricsToggle"
                ) { showLyrics ->
                    if (showLyrics && currentSongData.lyricsData != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LyricsView(
                                lyricsData = currentSongData.lyricsData!!,
                                timeLine = sharedViewModel.timeline,
                                onLineClick = { sharedViewModel.onUIEvent(UIEvent.UpdateProgress(it)) }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(310.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
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

            Spacer(modifier = Modifier.height(16.dp))

            // NewPlayerSongInfo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = trackTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextBackgroundColor,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = trackArtist,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, color = TextBackgroundColor.copy(alpha = 0.7f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
                IconButtonWithCustomShape(
                    icon = Icons.Rounded.Share,
                    shape = RoundedCornerShape(24.dp),
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NewPlayerProgressSlider
            val progressFactor = if (timelineState.total > 0) timelineState.current.toFloat() / timelineState.total else 0f
            var sliderValue by remember(progressFactor) { mutableFloatStateOf(progressFactor * 100f) }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val targetMs = (timelineState.total * (sliderValue / 100f)).roundToLong()
                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(targetMs.toFloat()))
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                thumb = { Spacer(modifier = Modifier.size(0.dp)) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextBackgroundColor.copy(alpha = 0.6f))
                )
                Text(
                    text = formatDuration(timelineState.total),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextBackgroundColor.copy(alpha = 0.6f))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NewPlayerControls
            NewPlayerControls(
                controllerState = controllerState,
                buttonBgColor = buttonBgColor,
                buttonIconColor = buttonIconColor,
                sideButtonContainerColor = sideButtonContainerColor,
                sideButtonContentColor = sideButtonContentColor,
                onUIEvent = { sharedViewModel.onUIEvent(it) }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // NewPlayerBottomActions
            NewPlayerBottomActions(
                controllerState = controllerState,
                showLyrics = showInlineLyrics,
                onToggleLyrics = { showInlineLyrics = !showInlineLyrics },
                onUIEvent = { sharedViewModel.onUIEvent(it) }
            )
        }
    }
}

@Composable
fun NewPlayerControls(
    controllerState: ControlState,
    buttonBgColor: Color,
    buttonIconColor: Color,
    sideButtonContainerColor: Color,
    sideButtonContentColor: Color,
    onUIEvent: (UIEvent) -> Unit,
) {
    val prevInteractionSource = remember { MutableInteractionSource() }
    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }

    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    val backButtonWeight by animateFloatAsState(
        targetValue = if (isPrevPressed) 0.65f else if (isPlayPausePressed || isNextPressed) 0.35f else 0.45f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "backWeight"
    )
    val playPauseWeight by animateFloatAsState(
        targetValue = if (isPlayPausePressed) 1.9f else if (isPrevPressed || isNextPressed) 1.1f else 1.5f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "playPauseWeight"
    )
    val nextButtonWeight by animateFloatAsState(
        targetValue = if (isNextPressed) 0.65f else if (isPrevPressed || isPlayPausePressed) 0.35f else 0.45f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "nextWeight"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous
        Box(
            modifier = Modifier
                .height(68.dp)
                .weight(backButtonWeight)
                .clip(RoundedCornerShape(50))
                .background(sideButtonContainerColor)
                .clickable(
                    interactionSource = prevInteractionSource,
                    indication = androidx.compose.material3.ripple(bounded = true),
                    onClick = { onUIEvent(UIEvent.Previous) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                tint = sideButtonContentColor,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play Pause
        val roundness by animateDpAsState(
            targetValue = if (controllerState.isPlaying) 24.dp else 36.dp,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "playPauseRoundness"
        )
        Box(
            modifier = Modifier
                .height(68.dp)
                .weight(playPauseWeight)
                .clip(RoundedCornerShape(roundness))
                .background(buttonBgColor)
                .clickable(
                    interactionSource = playPauseInteractionSource,
                    indication = androidx.compose.material3.ripple(bounded = true),
                    onClick = { onUIEvent(UIEvent.PlayPause) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (controllerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                tint = buttonIconColor,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Next
        Box(
            modifier = Modifier
                .height(68.dp)
                .weight(nextButtonWeight)
                .clip(RoundedCornerShape(50))
                .background(sideButtonContainerColor)
                .clickable(
                    interactionSource = nextInteractionSource,
                    indication = androidx.compose.material3.ripple(bounded = true),
                    onClick = { onUIEvent(UIEvent.Next) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                tint = sideButtonContentColor,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun NewPlayerBottomActions(
    controllerState: ControlState,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    onUIEvent: (UIEvent) -> Unit,
) {
    var showQueueSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Queue
        IconButtonWithCustomShape(
            icon = Icons.AutoMirrored.Outlined.QueueMusic,
            shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 3.dp, bottomEnd = 3.dp),
            onClick = { showQueueSheet = true }
        )

        // Sleep Timer
        IconButtonWithCustomShape(
            painterResource = painterResource(Res.drawable.baseline_access_alarm_24),
            shape = RoundedCornerShape(3.dp),
            onClick = { /* Sleep Timer Action */ }
        )

        // Shuffle
        IconButtonWithCustomShape(
            icon = Icons.Rounded.Shuffle,
            shape = RoundedCornerShape(3.dp),
            onClick = { onUIEvent(UIEvent.Shuffle) },
            active = controllerState.isShuffle
        )

        // Lyrics
        IconButtonWithCustomShape(
            painterResource = painterResource(Res.drawable.more_horiz),
            shape = RoundedCornerShape(3.dp),
            onClick = onToggleLyrics,
            active = showLyrics
        )

        // Repeat
        val repeatIcon = when (controllerState.repeatState) {
            RepeatState.One -> Icons.Rounded.RepeatOne
            else -> Icons.Rounded.Repeat
        }
        IconButtonWithCustomShape(
            icon = repeatIcon,
            shape = RoundedCornerShape(3.dp),
            onClick = { onUIEvent(UIEvent.Repeat) },
            active = controllerState.repeatState != RepeatState.None
        )

        // Like / Favorite
        val favoriteIcon = if (controllerState.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
        IconButtonWithCustomShape(
            icon = favoriteIcon,
            shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp),
            onClick = { onUIEvent(UIEvent.ToggleLike) }
        )
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            onDismiss = { showQueueSheet = false }
        )
    }
}

@Composable
fun IconButtonWithCustomShape(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painterResource: androidx.compose.ui.graphics.painter.Painter? = null,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val bgColor = if (active) Color.White else Color.White.copy(alpha = 0.2f)
    val contentColor = if (active) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                tint = contentColor,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        } else if (painterResource != null) {
            Icon(
                painter = painterResource,
                tint = contentColor,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
