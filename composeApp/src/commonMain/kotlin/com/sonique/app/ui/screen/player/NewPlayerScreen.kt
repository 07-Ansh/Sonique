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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
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
import sonique.composeapp.generated.resources.timer
import sonique.composeapp.generated.resources.shuffle_on
import sonique.composeapp.generated.resources.shuffle
import sonique.composeapp.generated.resources.lyrics
import sonique.composeapp.generated.resources.repeat_one_on
import sonique.composeapp.generated.resources.repeat_on
import sonique.composeapp.generated.resources.repeat
import sonique.composeapp.generated.resources.skip_previous
import sonique.composeapp.generated.resources.skip_next
import sonique.composeapp.generated.resources.favorite
import sonique.composeapp.generated.resources.favorite_border
import sonique.composeapp.generated.resources.play
import sonique.composeapp.generated.resources.pause
import sonique.composeapp.generated.resources.queue_music
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

    val trackTitle = currentSongData?.nowPlayingTitle ?: ""
    val trackArtist = currentSongData?.artistName ?: ""
    val trackArtwork = currentSongData?.thumbnailURL ?: ""

    // Spacings and Colors matching Metrolist code perfectly
    val TextBackgroundColor = Color.White
    val textButtonColor = Color.White
    val iconButtonColor = Color.Black
    val sideButtonContainerColor = Color.White.copy(alpha = 0.2f)
    val sideButtonContentColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)) // OuterTune / Metrolist Default Background
    ) {
        // Blur Background
        if (trackArtwork.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = trackArtwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(150.dp)
                        .alpha(0.35f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = 24.dp)
                .animateContentSize()
        ) {
            // Metrolist TitleHeader Block
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
                    Text(
                        text = "Your Queue",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextBackgroundColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            // Thumbnail container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .clip(RoundedCornerShape(6.dp)) // ThumbnailCornerRadius (3.dp * 2 = 6.dp)
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

            // controlsContent body
            val playPauseRoundness by animateDpAsState(
                targetValue = if (controllerState.isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness"
            )

            // Song Info layout Block
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp) // PlayerHorizontalPadding = 32.dp
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = trackTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextBackgroundColor,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = trackArtist,
                        style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action segment pill shapes (Share / Favorite)
                val shareShape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 3.dp, bottomEnd = 3.dp)
                val favShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { },
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

                    val isLiked = controllerState.isLiked
                    FilledIconButton(
                        onClick = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) },
                        shape = favShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = if (isLiked) MaterialTheme.colorScheme.error else iconButtonColor
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) Res.drawable.favorite else Res.drawable.favorite_border
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress Slider
            val progressFactor = if (timelineState.total > 0) timelineState.current.toFloat() / timelineState.total else 0f
            var sliderValue by remember(progressFactor) { mutableFloatStateOf(progressFactor * 100f) }
            val inactiveTrackColor = Color.White.copy(alpha = 0.4f)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val targetMs = (timelineState.total * (sliderValue / 100f)).roundToLong()
                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(targetMs.toFloat()))
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    activeTrackColor = textButtonColor,
                    activeTickColor = textButtonColor,
                    thumbColor = textButtonColor,
                    inactiveTrackColor = inactiveTrackColor,
                    disabledActiveTrackColor = textButtonColor,
                    disabledInactiveTrackColor = inactiveTrackColor,
                    disabledThumbColor = textButtonColor
                ),
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                modifier = Modifier.padding(horizontal = 32.dp) // PlayerHorizontalPadding = 32.dp
            )

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp) // PlayerHorizontalPadding + 4.dp = 36.dp
            ) {
                Text(
                    text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDuration(timelineState.total),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(24.dp))

            // Playback controls Row (SkipPrevious, PlayPause, SkipNext)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp) // PlayerHorizontalPadding
            ) {
                val backInteractionSource = remember { MutableInteractionSource() }
                val nextInteractionSource = remember { MutableInteractionSource() }
                val playPauseInteractionSource = remember { MutableInteractionSource() }

                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                val isBackPressed by backInteractionSource.collectIsPressedAsState()
                val isNextPressed by nextInteractionSource.collectIsPressedAsState()

                val playPauseWeight by animateFloatAsState(
                    targetValue = if (isPlayPausePressed) 1.9f else if (isBackPressed || isNextPressed) 1.1f else 1.3f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "playPauseWeight"
                )
                val backButtonWeight by animateFloatAsState(
                    targetValue = if (isBackPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "backButtonWeight"
                )
                val nextButtonWeight by animateFloatAsState(
                    targetValue = if (isNextPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "nextButtonWeight"
                )

                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Previous) },
                    shape = RoundedCornerShape(50),
                    interactionSource = backInteractionSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = sideButtonContainerColor,
                        contentColor = sideButtonContentColor
                    ),
                    modifier = Modifier
                        .height(68.dp)
                        .weight(backButtonWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_previous),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                    shape = RoundedCornerShape(50),
                    interactionSource = playPauseInteractionSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = textButtonColor,
                        contentColor = iconButtonColor
                    ),
                    modifier = Modifier
                        .height(68.dp)
                        .weight(playPauseWeight)
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
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Next) },
                    shape = RoundedCornerShape(50),
                    interactionSource = nextInteractionSource,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = sideButtonContainerColor,
                        contentColor = sideButtonContentColor
                    ),
                    modifier = Modifier
                        .height(68.dp)
                        .weight(nextButtonWeight)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.skip_next),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // Bottom action icons row (Queue, Sleep Timer, Shuffle, Lyrics, Repeat, More Options)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp) // PlayerHorizontalPadding
            ) {
                var showQueueSheet by remember { mutableStateOf(false) }

                ResizableIconButton(
                    icon = Res.drawable.queue_music,
                    color = TextBackgroundColor,
                    modifier = Modifier.size(32.dp),
                    onClick = { showQueueSheet = true }
                )

                ResizableIconButton(
                    icon = Res.drawable.timer,
                    color = TextBackgroundColor,
                    modifier = Modifier.size(32.dp),
                    onClick = { }
                )

                val isShuffle = controllerState.isShuffle
                ResizableIconButton(
                    icon = if (isShuffle) Res.drawable.shuffle_on else Res.drawable.shuffle,
                    color = if (isShuffle) MaterialTheme.colorScheme.primary else TextBackgroundColor,
                    modifier = Modifier.size(32.dp),
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) }
                )

                ResizableIconButton(
                    icon = Res.drawable.lyrics,
                    color = TextBackgroundColor,
                    modifier = Modifier.size(32.dp),
                    onClick = { }
                )

                val isRepeat = controllerState.repeatState != RepeatState.None
                val isRepeatOne = controllerState.repeatState == RepeatState.One
                val repeatIcon = if (isRepeat) {
                    if (isRepeatOne) Res.drawable.repeat_one_on else Res.drawable.repeat_on
                } else {
                    Res.drawable.repeat
                }
                ResizableIconButton(
                    icon = repeatIcon,
                    color = if (isRepeat) MaterialTheme.colorScheme.primary else TextBackgroundColor,
                    modifier = Modifier.size(32.dp),
                    onClick = { sharedViewModel.onUIEvent(UIEvent.Repeat) }
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(textButtonColor)
                        .clickable { }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.more_horiz),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showQueueSheet) {
                    QueueBottomSheet(
                        onDismiss = { showQueueSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ResizableIconButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
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
