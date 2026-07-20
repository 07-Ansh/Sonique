package com.sonique.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.domain.data.model.streams.TimeLine
import com.sonique.domain.lyrics.LyricsUtils
import com.sonique.domain.data.model.metadata.LyricsEntry
import com.sonique.domain.data.model.metadata.WordTimestamp
import com.sonique.logger.Logger
import com.sonique.app.extension.KeepScreenOn
import com.sonique.app.extension.ParsedRichSyncLine
import com.sonique.app.extension.animateScrollAndCentralizeItem
import com.sonique.app.extension.formatDuration
import com.sonique.app.extension.parseRichSyncWords
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.NowPlayingScreenData
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_keyboard_arrow_down_24
import sonique.composeapp.generated.resources.baseline_more_vert_24
import sonique.composeapp.generated.resources.now_playing_upper
import sonique.composeapp.generated.resources.unavailable
import kotlin.math.abs

private const val TAG = "LyricsView"

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    playerContentColor: Color = Color.White,
) {
    var currentLineHeight by remember {
        mutableIntStateOf(0)
    }
    val listState = rememberLazyListState()
    val current by timeLine.collectAsStateWithLifecycle()

    val showTopShadow by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        }
    }
    val showBottomShadow by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null) {
                lastVisibleItem.index < layoutInfo.totalItemsCount - 1 ||
                    lastVisibleItem.offset + lastVisibleItem.size > layoutInfo.viewportEndOffset
            } else {
                false
            }
        }
    }

    val parsedEntries: List<LyricsEntry> = remember(lyricsData.lyrics) {
        val rawLrc = lyricsData.lyrics.SoniqueLyricsId
        if (!rawLrc.isNullOrBlank()) {
            LyricsUtils.parseLyrics(rawLrc)
        } else {
            val lines = lyricsData.lyrics.lines ?: emptyList()
            lines.map { line ->
                val time = line.startTimeMs.toLongOrNull() ?: 0L
                val text = line.words
                LyricsEntry(
                    time = time,
                    text = text,
                    words = null,
                    agent = null,
                    isBackground = false
                )
            }
        }
    }

    val mergedList: List<LyricsListItem> = remember(parsedEntries) {
        val result = mutableListOf<LyricsListItem>()
        if (parsedEntries.isNotEmpty()) {
            val linesWithHead = listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedEntries

            linesWithHead.forEachIndexed { i, entry ->
                if (entry.text.isNotBlank() || entry == LyricsEntry.HEAD_LYRICS_ENTRY) {
                    result.add(LyricsListItem.Line(i, entry))
                }
                if (i < linesWithHead.size - 1) {
                    val nextStart = linesWithHead[i + 1].time
                    val entryWords = entry.words
                    val currentEnd = if (!entryWords.isNullOrEmpty()) {
                        (entryWords.last().endTime * 1000).toLong()
                    } else if (entry.text.isBlank()) {
                        entry.time
                    } else {
                        null
                    }

                    if (currentEnd != null) {
                        val endVal = currentEnd.toLong()
                        if (endVal < nextStart) {
                            val gap = nextStart - endVal
                            if (gap > 4000L) {
                                result.add(LyricsListItem.Indicator(i, gap, endVal, nextStart, linesWithHead[i + 1].agent))
                            }
                        }
                    }
                }
            }
        }
        result
    }

    val currentLineIndex = remember(parsedEntries, current.current) {
        LyricsUtils.findCurrentLineIndex(parsedEntries, current.current)
    }

    val activeListItemIndex = remember(mergedList, currentLineIndex) {
        val targetEntry = parsedEntries.getOrNull(currentLineIndex) ?: LyricsEntry.HEAD_LYRICS_ENTRY
        mergedList.indexOfFirst { item ->
            item is LyricsListItem.Line && item.entry.time == targetEntry.time
        }
    }

    LaunchedEffect(key1 = activeListItemIndex, key2 = currentLineHeight) {
        if (activeListItemIndex > -1 && currentLineHeight > 0) {
            listState.animateScrollAndCentralizeItem(
                index = activeListItemIndex,
                this,
            )
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        if (showScrollShadows) {
                            if (showTopShadow) {
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    backgroundColor,
                                                    backgroundColor.copy(alpha = 0.8f),
                                                    backgroundColor.copy(alpha = 0.4f),
                                                    Color.Transparent,
                                                ),
                                            startY = 0f,
                                            endY = 80.dp.toPx(),
                                        ),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, 80.dp.toPx()),
                                )
                            }
                            if (showBottomShadow) {
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    Color.Transparent,
                                                    backgroundColor.copy(alpha = 0.4f),
                                                    backgroundColor.copy(alpha = 0.8f),
                                                    backgroundColor,
                                                ),
                                            startY = size.height - 80.dp.toPx(),
                                            endY = size.height,
                                        ),
                                    topLeft = Offset(0f, size.height - 80.dp.toPx()),
                                    size = Size(size.width, 80.dp.toPx()),
                                )
                            }
                        }
                    },
        ) {
            items(mergedList.size) { index ->
                when (val listItem = mergedList[index]) {
                    is LyricsListItem.Line -> {
                        val entry = listItem.entry
                        val isActive = entry.time == parsedEntries.getOrNull(currentLineIndex)?.time
                        
                        LyricsLine(
                            index = index,
                            item = entry,
                            isSynced = lyricsData.lyrics.syncType != null,
                            isActiveLine = isActive,
                            bgVisible = true,
                            isSelected = false,
                            isSelectionModeActive = false,
                            currentPositionState = current.current,
                            isPlaying = true, 
                            lyricsOffset = 0L,
                            lyricsTextSize = 24f,
                            lyricsLineSpacing = 1.3f,
                            expressiveAccent = playerContentColor,
                            lyricsTextPosition = LyricsPosition.CENTER,
                            respectAgentPositioning = true,
                            isAutoScrollEnabled = true,
                            displayedCurrentLineIndex = activeListItemIndex,
                            romanizeAsMain = false,
                            romanizeLyrics = false,
                            onSizeChanged = { height ->
                                if (isActive) {
                                    currentLineHeight = height
                                }
                            },
                            onClick = {
                                if (entry.time > 0L && timeLine.value.total > 0L) {
                                    onLineClick(entry.time.toFloat() * 100 / timeLine.value.total)
                                }
                            },
                            onLongClick = {}
                        )
                    }
                    is LyricsListItem.Indicator -> {
                        IntervalIndicator(
                            gapStartMs = listItem.gapStartMs,
                            gapEndMs = listItem.gapEndMs - 650L,
                            currentPositionMs = current.current,
                            visible = current.current in listItem.gapStartMs..(listItem.gapEndMs - 650L),
                            color = playerContentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val enableExpressivePlayerControls by sharedViewModel.enableExpressivePlayerControls.collectAsStateWithLifecycle()
    val playerContentColor = if (enableExpressivePlayerControls) Color(0xFFFAF9F6) else Color.White
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars

    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

     
    var showControlButtons by rememberSaveable {
        mutableStateOf(true)
    }

     
    LaunchedEffect(key1 = showControlButtons) {
        if (showControlButtons) {
            delay(4000)  
            showControlButtons = false
        }
    }

    LaunchedEffect(key1 = timelineState) {
        sliderValue =
            if (timelineState.total > 0L) {
                timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
            } else {
                0f
            }
    }

    if (screenDataState.lyricsData != null) {
        KeepScreenOn()
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = color,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                     
                    showControlButtons = true
                },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Box {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors().copy(containerColor = color),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .fillMaxSize()
                            .padding(
                                bottom =
                                    with(localDensity) {
                                        windowInsets.getBottom(localDensity).toDp()
                                    },
                                top =
                                    with(localDensity) {
                                        windowInsets.getTop(localDensity).toDp()
                                    },
                            ),
                ) {
                     
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors =
                            TopAppBarDefaults.topAppBarColors().copy(
                                containerColor = Color.Transparent,
                            ),
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.now_playing_upper),
                                    style = typo().bodyMedium,
                                    color = playerContentColor,
                                )
                                Text(
                                    text = screenDataState.nowPlayingTitle,
                                    style = typo().labelMedium,
                                    color = playerContentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically).focusable(),
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_keyboard_arrow_down_24),
                                    contentDescription = "",
                                    tint = playerContentColor,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {}, modifier = Modifier.alpha(0f)) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_more_vert_24),
                                    contentDescription = "",
                                    tint = playerContentColor,
                                )
                            }
                        },
                    )

                     
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 50.dp),
                    ) {
                        Crossfade(
                            targetState = screenDataState.lyricsData != null,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (it) {
                                screenDataState.lyricsData?.let { lyrics ->
                                    LyricsView(
                                        lyricsData = lyrics,
                                        timeLine = sharedViewModel.timeline,
                                        onLineClick = { f ->
                                            sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        showScrollShadows = true,
                                        backgroundColor = color,
                                        playerContentColor = playerContentColor,
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.unavailable),
                                        style = typo().bodyMedium,
                                        color = playerContentColor,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                     
                    Column {
                         
                        Box(
                            Modifier
                                .padding(
                                    top = 15.dp,
                                ).padding(horizontal = 40.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!enableExpressivePlayerControls) {
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
                                                trackColor = Color.DarkGray,
                                                strokeCap = StrokeCap.Round,
                                                drawStopIndicator = {},
                                            )
                                        }
                                    }
                                }
                            }
                            }
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sharedViewModel.onUIEvent(
                                            UIEvent.UpdateProgress(it),
                                        )
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
                                        if (enableExpressivePlayerControls) {
                                            WavySliderTrack(
                                                sliderState = sliderState,
                                                isPlaying = controllerState.isPlaying,
                                                activeColor = playerContentColor,
                                                inactiveColor = playerContentColor.copy(alpha = 0.3f)
                                            )
                                        } else {
                                            SliderDefaults.Track(
                                                modifier =
                                                    Modifier
                                                        .height(5.dp),
                                                enabled = true,
                                                sliderState = sliderState,
                                                colors =
                                                    SliderDefaults.colors().copy(
                                                        thumbColor = playerContentColor,
                                                        activeTrackColor = playerContentColor,
                                                        inactiveTrackColor = Color.Transparent,
                                                    ),
                                                thumbTrackGapSize = 0.dp,
                                                drawTick = { _, _ -> },
                                                drawStopIndicator = null,
                                            )
                                        }
                                    },
                                    thumb = { sliderState ->
                                        if (enableExpressivePlayerControls) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(20.dp)
                                                    .background(playerContentColor, RoundedCornerShape(2.dp))
                                            )
                                        } else {
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
                                                        thumbColor = playerContentColor,
                                                        activeTrackColor = playerContentColor,
                                                        inactiveTrackColor = Color.Transparent,
                                                    ),
                                                enabled = true,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        LazyColumn {
                            item {
                                 
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 40.dp),
                                ) {
                                    Text(
                                        text = formatDuration(timelineState.current),
                                        style = typo().bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Left,
                                    )
                                    Text(
                                        text = formatDuration(timelineState.total),
                                        style = typo().bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Right,
                                    )
                                }

                                Spacer(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(5.dp),
                                )
                            }

                            item {
                                 
                                 
                                AnimatedVisibility(
                                    visible = showControlButtons,
                                    enter =
                                        expandVertically(
                                            tween(300),
                                        ),
                                    exit =
                                        shrinkVertically(
                                            tween(300),
                                        ),
                                ) {
                                    PlayerControlLayout(controllerState = controllerState, enableExpressive = enableExpressivePlayerControls) {
                                        sharedViewModel.onUIEvent(it)
                                    }
                                }
                                AnimatedVisibility(
                                    visible = showControlButtons,
                                    enter =
                                        expandVertically(
                                            tween(300),
                                        ),
                                    exit =
                                        shrinkVertically(
                                            tween(300),
                                        ),
                                ) {
                                     
                                     
                                    Box(
                                        modifier =
                                            Modifier
                                                .height(32.dp)
                                                .fillMaxWidth()
                                                .padding(horizontal = 40.dp),
                                    ) {
                                        IconButton(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .aspectRatio(1f)
                                                    .align(Alignment.CenterStart)
                                                    .clip(
                                                        CircleShape,
                                                    ),
                                            onClick = {
                                                showInfoBottomSheet = true
                                                showControlButtons = true  
                                            },
                                        ) {
                                            Icon(imageVector = Icons.Outlined.Info, tint = playerContentColor, contentDescription = "")
                                        }
                                        Row(
                                            Modifier.align(Alignment.CenterEnd),
                                        ) {
                                            Spacer(modifier = Modifier.size(8.dp))
                                            IconButton(
                                                modifier =
                                                    Modifier
                                                        .size(24.dp)
                                                        .aspectRatio(1f)
                                                        .clip(
                                                            CircleShape,
                                                        ),
                                                onClick = {
                                                    showQueueBottomSheet = true
                                                    showControlButtons = true  
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                                    tint = playerContentColor,
                                                    contentDescription = "",
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }

                     
                    if (!showControlButtons) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
        )
    }
    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }
}




