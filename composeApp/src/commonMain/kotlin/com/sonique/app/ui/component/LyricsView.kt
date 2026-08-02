package com.sonique.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Surface
import sonique.composeapp.generated.resources.baseline_sync_24
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
    val scope = rememberCoroutineScope()
    val current by timeLine.collectAsStateWithLifecycle()
    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

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

    LaunchedEffect(key1 = current) {
        val lines = lyricsData.lyrics.lines
        if (current.current > 0L) {
            lines?.indices?.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                 
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                         
                        startTimeMs + 60000
                    }
                if (current.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            if (!lines.isNullOrEmpty() &&
                (
                    current.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                        ).toLong()
                    )
                )
            ) {
                currentLineIndex = -1
            }
        } else {
            currentLineIndex = -1
        }
    }
    var userIsScrolling by remember { mutableStateOf(false) }

    // Pause auto-scroll when user manually scrolls lyrics
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userIsScrolling = true
        } else if (userIsScrolling) {
            // When user stops scrolling, wait 5 seconds of inactivity before resuming auto-scroll
            delay(5000)
            userIsScrolling = false
        }
    }

    // Auto-scroll to active line smoothly when user is not scrolling
    LaunchedEffect(currentLineIndex, userIsScrolling) {
        if (!userIsScrolling && currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            listState.animateScrollAndCentralizeItem(
                index = currentLineIndex,
                scope = this,
            )
        }
    }

    fun findClosestTranslatedLine(originalTimeMs: String): String? {
        val translatedLines = lyricsData.translatedLyrics?.first?.lines ?: return null
        if (translatedLines.isEmpty()) return null

        val originalTime = originalTimeMs.toLongOrNull() ?: return null

        return translatedLines
            .minByOrNull {
                abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
            }?.let {
                val abs = abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                if (abs < 1000L) {
                    it
                } else {
                    null
                }
            }?.words
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
            items(lyricsData.lyrics.lines?.size ?: 0) { index ->
                val line = lyricsData.lyrics.lines?.getOrNull(index)
                 
                val translatedWords =
                    if (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") {
                        line?.startTimeMs?.let { findClosestTranslatedLine(it) }
                    } else {
                        lyricsData.translatedLyrics
                            ?.first
                            ?.lines
                            ?.getOrNull(index)
                            ?.words
                    }
                Logger.d(TAG, "Line $index: ${line?.words}, Translated: $translatedWords")

                line?.words?.let { words ->
                    Logger.d(TAG, "SyncType: ${lyricsData.lyrics.syncType}, Line $index content preview: ${words.take(50)}")
                    when {
                         
                        lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                            val parsedLine =
                                remember(words, line.startTimeMs, line.endTimeMs) {
                                    val result = parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                    Logger.d(TAG, "Line $index parseRichSyncWords result: ${if (result != null) "${result.words.size} words" else "null"}")
                                    result
                                }

                            if (parsedLine != null) {
                                RichSyncLyricsLineItem(
                                    parsedLine = parsedLine,
                                    translatedWords = translatedWords,
                                    currentTimeMs = current.current,
                                    isCurrent = index == currentLineIndex,
                                    playerContentColor = playerContentColor,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                userIsScrolling = false
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }.onGloballyPositioned { c ->
                                                currentLineHeight = c.size.height
                                            },
                                )
                            } else {
                                 
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    isBold = index <= currentLineIndex,
                                    isCurrent = index == currentLineIndex,
                                    playerContentColor = playerContentColor,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                userIsScrolling = false
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }.onGloballyPositioned { c ->
                                                currentLineHeight = c.size.height
                                            },
                                )
                            }
                        }

                         
                        else -> {
                            LyricsLineItem(
                                originalWords = words,
                                translatedWords = translatedWords,
                                isBold = index <= currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                isCurrent = index == currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                playerContentColor = playerContentColor,
                                modifier =
                                    Modifier
                                        .clickable(enabled = lyricsData.lyrics.syncType == "LINE_SYNCED") {
                                            userIsScrolling = false
                                            onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                        }.onGloballyPositioned { c ->
                                            currentLineHeight = c.size.height
                                        },
                            )
                        }
                    }
                }
            }
        }

        // Floating Sync to current lyric button when user scrolled away to read lyrics
        AnimatedVisibility(
            visible = userIsScrolling && currentLineIndex >= 0 && (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED"),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        ) {
            Surface(
                onClick = {
                    userIsScrolling = false
                    if (currentLineIndex >= 0) {
                        listState.animateScrollAndCentralizeItem(currentLineIndex, scope)
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                contentColor = playerContentColor,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, playerContentColor.copy(alpha = 0.15f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.baseline_sync_24),
                        contentDescription = "Sync",
                        modifier = Modifier.size(18.dp),
                        tint = playerContentColor,
                    )
                    Text(
                        text = "Sync to current lyric",
                        style = typo().labelLarge,
                        color = playerContentColor,
                    )
                }
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier,
    playerContentColor: Color = Color.White,
) {
    Crossfade(targetState = isBold) {
        if (it) {
            Column(
                modifier = modifier,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    modifier =
                        Modifier.then(
                            if (isCurrent) {
                                Modifier
                            } else {
                                Modifier.blur(1.dp)
                            },
                        ),
                    text = originalWords,
                    style = typo().headlineLarge,
                    color =
                        if (isCurrent) {
                            playerContentColor
                        } else {
                            Color.LightGray.copy(
                                alpha = 0.35f,
                            )
                        },
                )
                if (translatedWords != null) {
                    Text(
                        modifier =
                            Modifier.then(
                                if (isCurrent) {
                                    Modifier
                                } else {
                                    Modifier.blur(1.dp)
                                },
                            ),
                        text = translatedWords,
                        style = typo().bodyMedium,
                        color =
                            if (isCurrent) {
                                musica_accent
                            } else {
                                musica_accent.copy(
                                    alpha = 0.3f,
                                )
                            },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    if (!isBold) {
        Column(
            modifier = modifier,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.blur(1.dp),
                text = originalWords,
                style = typo().headlineMedium,
                color =
                    Color.LightGray.copy(
                        alpha = 0.35f,
                    ),
            )
            if (translatedWords != null) {
                Text(
                    modifier = Modifier.blur(1.dp),
                    text = translatedWords,
                    style = typo().bodyMedium,
                    color =
                        musica_accent.copy(
                            alpha = 0.3f,
                        ),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichSyncLyricsLineItem(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    currentTimeMs: Long,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    playerContentColor: Color = Color.White,
) {
     
    val currentWordIndex by remember(currentTimeMs, parsedLine.words) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1

             
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(12.dp))

         
        FlowRow(
            modifier =
                Modifier.then(
                    if (isCurrent) {
                        Modifier
                    } else {
                        Modifier.blur(1.dp)
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                AnimatedWord(
                    word = wordTiming.text,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    playerContentColor = playerContentColor,
                )
            }
        }

         
        if (translatedWords != null) {
            Text(
                modifier =
                    Modifier.then(
                        if (isCurrent) {
                            Modifier
                        } else {
                            Modifier.blur(1.dp)
                        },
                    ),
                text = translatedWords,
                style = typo().bodyMedium,
                color =
                    if (isCurrent) {
                        musica_accent
                    } else {
                        musica_accent.copy(
                            alpha = 0.3f,
                        )
                    },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    playerContentColor: Color = Color.White,
) {
     
    val color by animateColorAsState(
        targetValue =
            when {
                !isCurrent -> Color.LightGray.copy(alpha = 0.35f)  
                isPast -> playerContentColor.copy(alpha = 0.7f)  
                isActive -> playerContentColor  
                else -> Color.LightGray.copy(alpha = 0.5f)  
            },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "wordColor",
    )

    Text(
        text = word,
        style = typo().headlineLarge,
        color = color,
    )
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




