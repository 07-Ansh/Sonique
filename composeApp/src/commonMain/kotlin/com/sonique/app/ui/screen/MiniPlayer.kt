package com.sonique.app.ui.screen

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.utils.connectArtists
import com.sonique.logger.Logger
import com.sonique.app.Platform
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.toImageBitmap
import com.sonique.app.extension.formatDuration
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.ExplicitBadge
import com.sonique.app.ui.component.HeartCheckBox
import com.sonique.app.ui.component.PlayPauseButton
import com.sonique.app.ui.component.PlayerControlLayout
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.holder
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "MiniPlayer"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    modifier: Modifier,
    backdrop: PlatformBackdrop,
    sharedViewModel: SharedViewModel = koinInject(),
    onClose: () -> Unit,
    onClick: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0f) }

    val textColor by animateColorAsState(
        targetValue = if (luminanceAnimation.value > 0.6f) Color.Black else Color.White,
        label = "MiniPlayerTextColor",
        animationSpec = tween(500),
    )

    val (songEntity, setSongEntity) =
        remember {
            mutableStateOf<SongEntity?>(null)
        }
    val (liked, setLiked) =
        remember {
            mutableStateOf(false)
        }
    val (isPlaying, setIsPlaying) =
        remember {
            mutableStateOf(false)
        }
    val (progress, setProgress) =
        remember {
            mutableFloatStateOf(0f)
        }
    val (isCrossfading, setIsCrossfading) =
        remember {
            mutableStateOf(false)
        }

    val coroutineScope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "",
    )

     
    val paletteState = rememberPaletteState()
    val background =
        remember {
            Animatable(Color.DarkGray)
        }

    val offsetX = remember { Animatable(initialValue = 0f) }
    val offsetY = remember { Animatable(0f) }

    var loading by rememberSaveable {
        mutableStateOf(true)
    }

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
                background.animateTo(it.getColorFromPalette())
            }
    }

    LaunchedEffect(key1 = true) {
        val job1 =
            launch {
                sharedViewModel.nowPlayingState.collect { item ->
                    if (item != null) {
                        setSongEntity(item.songEntity)
                    }
                }
            }
        val job2 =
            launch {
                sharedViewModel.controllerState.collectLatest { state ->
                    setLiked(state.isLiked)
                    setIsPlaying(state.isPlaying)
                    setIsCrossfading(state.isCrossfading)
                }
            }
        val job4 =
            launch {
                sharedViewModel.timeline.collect { timeline ->
                    loading = timeline.loading
                    val prog =
                        if (timeline.total > 0L && timeline.current >= 0L) {
                            timeline.current.toFloat() / timeline.total
                        } else {
                            0f
                        }
                    setProgress(prog)
                }
            }
        job1.join()
        job2.join()
        job4.join()
    }

    if (getPlatform() == Platform.Android) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = md_theme_dark_background,
                    disabledContainerColor = md_theme_dark_background,
                ),
            modifier =
                modifier
                    .then(
                        Modifier,
                    ).then(
                        Modifier
                            .clipToBounds()
                            .offset { IntOffset(0, offsetY.value.roundToInt()) }
                            .clickable(
                                onClick = onClick,
                            ).pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                    },
                                    onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        onDrag(dragAmount)
                                        change.consume()
                                    },
                                    onDragCancel = {
                                        onDragEnd()
                                    },
                                    onDragEnd = {
                                        onDragEnd()
                                    },
                                )
                            },
                    ),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(modifier = Modifier.weight(1F)) {
                        Row(
                            modifier =
                                Modifier
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                            },
                                            onHorizontalDrag = {
                                                change: PointerInputChange,
                                                dragAmount: Float,
                                                ->
                                                coroutineScope.launch {
                                                    change.consume()
                                                    offsetX.animateTo(offsetX.value + dragAmount * 2)
                                                    Logger.w("MiniPlayer", "Dragged ${offsetX.value}")
                                                }
                                            },
                                            onDragCancel = {
                                                Logger.w("MiniPlayer", "Drag Cancelled")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                            onDragEnd = {
                                                Logger.w("MiniPlayer", "Drag Ended")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                        )
                                    },
                        ) {
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(songEntity?.thumbnails)
                                        .crossfade(550)
                                        .build(),
                                placeholder = painterResource(Res.drawable.holder),
                                error = painterResource(Res.drawable.holder),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = {
                                    bitmap =
                                        it.result.image.toImageBitmap()
                                },
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterVertically)
                                        .clip(
                                            RoundedCornerShape(4.dp),
                                        ),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AnimatedContent(
                                targetState = songEntity,
                                modifier = Modifier.weight(1F).fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart,
                                transitionSpec = {
                                     
                                    if (targetState != initialState) {
                                         
                                         
                                        (
                                            slideInHorizontally { width ->
                                                width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> +width } + fadeOut(),
                                        )
                                    } else {
                                         
                                         
                                        (
                                            slideInHorizontally { width ->
                                                +width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> width } + fadeOut(),
                                        )
                                    }.using(
                                         
                                         
                                        SizeTransform(clip = false),
                                    )
                                },
                            ) { target ->
                                if (target != null) {
                                    Column(
                                        Modifier
                                            .wrapContentHeight()
                                            .align(Alignment.CenterVertically),
                                    ) {
                                        Text(
                                            text = (songEntity?.title ?: "").toString(),
                                            style = typo().labelSmall,
                                            color = textColor,
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                                ExplicitBadge(
                                                    modifier =
                                                        Modifier
                                                            .size(20.dp)
                                                            .padding(end = 4.dp)
                                                            .weight(1f),
                                                )
                                            }
                                            Text(
                                                text = (songEntity?.artistName?.connectArtists() ?: ""),
                                                style = typo().bodySmall,
                                                maxLines = 1,
                                                color = textColor,
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .wrapContentHeight(
                                                            align = Alignment.CenterVertically,
                                                        ).basicMarquee(
                                                            iterations = Int.MAX_VALUE,
                                                            animationMode = MarqueeAnimationMode.Immediately,
                                                        ).focusable(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    HeartCheckBox(checked = liked, size = 30) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Crossfade(targetState = loading, label = "") {
                        if (it) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.LightGray,
                                    strokeWidth = 3.dp,
                                )
                            }
                        } else {
                            PlayPauseButton(isPlaying = isPlaying, modifier = Modifier.size(48.dp)) {
                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(15.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .wrapContentSize(Alignment.Center)
                            .padding(
                                horizontal = 10.dp,
                            ).align(Alignment.BottomCenter),
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        color = seed,
                        trackColor = Color.DarkGray.copy(alpha = 0.5f),
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    } else {
        var isSliding by rememberSaveable {
            mutableStateOf(false)
        }
        var sliderValue by rememberSaveable {
            mutableFloatStateOf(0f)
        }
        LaunchedEffect(key1 = timelineState, key2 = isSliding) {
            if (!isSliding) {
                sliderValue =
                    if (timelineState.total > 0L) {
                        timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
                    } else {
                        0f
                    }
            }
        }
        Box(
            modifier.then(
                Modifier.clickable {
                    onClick()
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                 
                Box(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(songEntity?.thumbnails)
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterResource(Res.drawable.holder),
                            error = painterResource(Res.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            onSuccess = {
                                bitmap =
                                    it.result.image.toImageBitmap()
                            },
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .align(Alignment.CenterVertically)
                                    .clip(
                                        RoundedCornerShape(4.dp),
                                    ),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = (songEntity?.title ?: "").toString(),
                                style = typo().labelSmall,
                                color = textColor,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                    ExplicitBadge(
                                        modifier =
                                            Modifier
                                                .size(20.dp)
                                                .padding(end = 4.dp)
                                                .weight(1f),
                                    )
                                }
                                Text(
                                    text = (songEntity?.artistName?.connectArtists() ?: ""),
                                    style = typo().bodySmall,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .wrapContentHeight(
                                                align = Alignment.CenterVertically,
                                            ).basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
                            }
                        }
                    }
                }
                 
                Box(modifier = Modifier.weight(1f)) {
                    Column(Modifier.width(380.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayerControlLayout(
                                controllerState,
                                isSmallSize = true,
                            ) {
                                sharedViewModel.onUIEvent(it)
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                                style = typo().bodyMedium,
                                textAlign = TextAlign.Left,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(50.dp),
                            )
                             
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Crossfade(timelineState.loading) {
                                        if (it) {
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                LinearProgressIndicator(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .padding(
                                                                horizontal = 3.dp,
                                                            ).clip(
                                                                RoundedCornerShape(8.dp),
                                                            ),
                                                    color = Color.Gray,
                                                    trackColor = Color.DarkGray,
                                                    strokeCap = StrokeCap.Round,
                                                )
                                            }
                                        } else {
                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                LinearProgressIndicator(
                                                    progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .padding(
                                                                horizontal = 3.dp,
                                                            ).clip(
                                                                RoundedCornerShape(8.dp),
                                                            ),
                                                    color = Color.Gray,
                                                    trackColor =
                                                        Color.Gray.copy(
                                                            alpha = 0.6f,
                                                        ),
                                                    strokeCap = StrokeCap.Round,
                                                    drawStopIndicator = {},
                                                )
                                            }
                                        }
                                    }
                                }
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    Slider(
                                        value = sliderValue,
                                        onValueChangeFinished = {
                                            isSliding = false
                                            sharedViewModel.onUIEvent(
                                                UIEvent.UpdateProgress(sliderValue),
                                            )
                                        },
                                        onValueChange = {
                                            isSliding = true
                                            sliderValue = it
                                        },
                                        valueRange = 0f..100f,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 3.dp)
                                                .align(
                                                    Alignment.TopCenter,
                                                ),
                                        track = { sliderState ->
                                            SliderDefaults.Track(
                                                modifier =
                                                    Modifier
                                                        .height(5.dp),
                                                enabled = true,
                                                sliderState = sliderState,
                                                colors =
                                                    SliderDefaults.colors().copy(
                                                        thumbColor = seed,
                                                        activeTrackColor = seed,
                                                        inactiveTrackColor = Color.DarkGray.copy(alpha = 0.5f),
                                                    ),
                                                thumbTrackGapSize = 0.dp,
                                                drawTick = { _, _ -> },
                                                drawStopIndicator = null,
                                            )
                                        },
                                        thumb = {
                                            SliderDefaults.Thumb(
                                                modifier =
                                                    Modifier
                                                        .height(18.dp)
                                                        .width(8.dp)
                                                        .padding(
                                                            vertical = 4.dp,
                                                        ),
                                                thumbSize = DpSize(8.dp, 8.dp),
                                                interactionSource =
                                                    remember {
                                                        MutableInteractionSource()
                                                    },
                                                colors =
                                                    SliderDefaults.colors().copy(
                                                        thumbColor = seed,
                                                        activeTrackColor = seed,
                                                        inactiveTrackColor = Color.DarkGray.copy(alpha = 0.5f),
                                                    ),
                                                enabled = true,
                                            )
                                        },
                                    )
                                }
                            }
                            Text(
                                text = formatDuration(timelineState.total),
                                style = typo().bodyMedium,
                                textAlign = TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(50.dp),
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                        HeartCheckBox(checked = controllerState.isLiked, size = 30) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Speaker, "")
                        Spacer(Modifier.width(4.dp))
                        var isVolumeSliding by rememberSaveable {
                            mutableStateOf(false)
                        }
                        var volumeValue by rememberSaveable {
                            mutableFloatStateOf(0f)
                        }
                        LaunchedEffect(key1 = controllerState, key2 = isVolumeSliding) {
                            if (!isVolumeSliding) {
                                volumeValue = controllerState.volume
                            }
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            Slider(
                                value = volumeValue,
                                onValueChangeFinished = {
                                    isVolumeSliding = false
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateVolume(volumeValue.coerceIn(0f, 1f)),
                                    )
                                },
                                onValueChange = {
                                    isVolumeSliding = true
                                    volumeValue = it
                                },
                                valueRange = 0f..1f,
                                modifier =
                                    Modifier
                                        .padding(top = 3.dp)
                                        .width(64.dp),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier =
                                            Modifier
                                                .height(5.dp),
                                        enabled = true,
                                        sliderState = sliderState,
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color.White,
                                                inactiveTrackColor = Color.Gray,
                                            ),
                                        thumbTrackGapSize = 0.dp,
                                        drawTick = { _, _ -> },
                                        drawStopIndicator = null,
                                    )
                                },
                                thumb = {
                                    SliderDefaults.Thumb(
                                        modifier =
                                            Modifier
                                                .height(18.dp)
                                                .width(8.dp)
                                                .padding(
                                                    vertical = 4.dp,
                                                ),
                                        thumbSize = DpSize(8.dp, 8.dp),
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color.White,
                                                inactiveTrackColor = Color.DarkGray,
                                            ),
                                        enabled = true,
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onClose() }) {
                            Icon(Icons.Rounded.Close, "")
                        }
                    }
                }
            }
        }
    }
}

