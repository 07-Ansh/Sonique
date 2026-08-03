package com.sonique.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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

private const val LYRICS_ANCHOR_RATIO = 0.35f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 130.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200

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
    val effectiveLyricsData = remember(lyricsData) {
        val rawLrc = lyricsData.lyrics.SoniqueLyricsId
        if (lyricsData.lyrics.lines.isNullOrEmpty() && !rawLrc.isNullOrBlank()) {
            val parsedLyrics = com.sonique.domain.utils.parseRawLrcToLyrics(rawLrc)
            lyricsData.copy(lyrics = parsedLyrics)
        } else {
            lyricsData
        }
    }
    val scope = rememberCoroutineScope()
    val current by timeLine.collectAsStateWithLifecycle()
    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(key1 = current) {
        val lines = effectiveLyricsData.lyrics.lines
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

    fun findClosestTranslatedLine(originalTimeMs: String): String? {
        val translatedLines = effectiveLyricsData.translatedLyrics?.first?.lines ?: return null
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

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()
        val anchorY = maxHeightPx * LYRICS_ANCHOR_RATIO
        val density = LocalDensity.current
        val lineHeightPx = with(density) { LYRICS_ITEM_FALLBACK_HEIGHT_DP.toPx() }

        val lines = effectiveLyricsData.lyrics.lines ?: emptyList()
        val activeListIndex = currentLineIndex.coerceAtLeast(0)

        val itemHeights = remember(lines) { mutableStateMapOf<Int, Int>() }

        val positions = remember(itemHeights.toMap(), activeListIndex, lines) {
            val map = mutableMapOf<Int, Float>()
            if (activeListIndex == -1 || lines.isEmpty()) return@remember map

            map[activeListIndex] = 0f
            var currentY = 0f
            for (i in activeListIndex - 1 downTo 0) {
                val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY -= (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i] = currentY
            }
            currentY = 0f
            for (i in activeListIndex until lines.size - 1) {
                val height = itemHeights[i]?.toFloat() ?: lineHeightPx
                currentY += (height + with(density) { LYRICS_ITEM_GAP_DP.toPx() })
                map[i + 1] = currentY
            }
            map
        }

        var userManualOffset by remember { mutableFloatStateOf(0f) }
        var isAutoScrollEnabled by remember { mutableStateOf(true) }
        var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        val velocityTracker = remember { VelocityTracker() }
        val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = 1.8f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(top = LYRICS_FADE_TOP_DP, bottom = LYRICS_FADE_BOTTOM_DP)
                .clipToBounds()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            flingJob?.cancel()
                            velocityTracker.resetTracking()
                            isAutoScrollEnabled = false
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            verticalDrag(down.id) { change ->
                                userManualOffset += change.positionChange().y
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            }
                            val velocity = velocityTracker.calculateVelocity().y
                            flingJob = scope.launch {
                                AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                    userManualOffset = value
                                }
                            }
                        }
                    }
                }
        ) {
            lines.forEachIndexed { listIndex, line ->
                key(listIndex) {
                    val distance = abs(listIndex - activeListIndex)
                    val targetOffset = anchorY + positions.getOrDefault(listIndex, (listIndex - activeListIndex) * lineHeightPx)
                    val frozenOffset = remember { mutableFloatStateOf(targetOffset) }

                    LaunchedEffect(isAutoScrollEnabled, targetOffset) {
                        if (isAutoScrollEnabled) frozenOffset.floatValue = targetOffset
                    }

                    val animatedOffset by animateFloatAsState(
                        targetValue = if (isAutoScrollEnabled) targetOffset else frozenOffset.floatValue,
                        animationSpec = tween(
                            durationMillis = 750,
                            delayMillis = (distance * LYRICS_STAGGER_DELAY_PER_DISTANCE).coerceAtMost(LYRICS_STAGGER_DELAY_MAX_MS),
                            easing = FastOutSlowInEasing
                        ),
                        label = "lyricStaggeredOffset_$listIndex"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                                layout(placeable.width, 0) { placeable.place(0, 0) }
                            }
                            .offset { IntOffset(0, (animatedOffset + userManualOffset).roundToInt()) }
                    ) {
                        val distanceFromCurrent = if (currentLineIndex >= 0) abs(listIndex - currentLineIndex) else 99
                        val words = line.words ?: ""
                        val translatedWords = if (effectiveLyricsData.lyrics.syncType == "LINE_SYNCED" || effectiveLyricsData.lyrics.syncType == "RICH_SYNCED") {
                            line.startTimeMs.let { findClosestTranslatedLine(it) }
                        } else {
                            effectiveLyricsData.translatedLyrics?.first?.lines?.getOrNull(listIndex)?.words
                        }

                        when {
                            effectiveLyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                                val parsedLine = remember(words, line.startTimeMs, line.endTimeMs) {
                                    parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                }
                                if (parsedLine != null) {
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = translatedWords,
                                        currentTimeMs = current.current,
                                        isCurrent = listIndex == currentLineIndex,
                                        playerContentColor = playerContentColor,
                                        modifier = Modifier
                                            .clickable {
                                                isAutoScrollEnabled = true
                                                userManualOffset = 0f
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }
                                            .onSizeChanged { size ->
                                                itemHeights[listIndex] = size.height
                                            }
                                    )
                                } else {
                                    LyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isBold = listIndex <= currentLineIndex,
                                        isCurrent = listIndex == currentLineIndex,
                                        distanceFromCurrent = distanceFromCurrent,
                                        playerContentColor = playerContentColor,
                                        modifier = Modifier
                                            .clickable {
                                                isAutoScrollEnabled = true
                                                userManualOffset = 0f
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }
                                            .onSizeChanged { size ->
                                                itemHeights[listIndex] = size.height
                                            }
                                    )
                                }
                            }
                            else -> {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    isBold = listIndex <= currentLineIndex || effectiveLyricsData.lyrics.syncType != "LINE_SYNCED",
                                    isCurrent = listIndex == currentLineIndex || effectiveLyricsData.lyrics.syncType != "LINE_SYNCED",
                                    distanceFromCurrent = distanceFromCurrent,
                                    playerContentColor = playerContentColor,
                                    modifier = Modifier
                                        .clickable(enabled = effectiveLyricsData.lyrics.syncType == "LINE_SYNCED") {
                                            isAutoScrollEnabled = true
                                            userManualOffset = 0f
                                            onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                        }
                                        .onSizeChanged { size ->
                                            itemHeights[listIndex] = size.height
                                        }
                                )
                            }
                        }
                    }
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
    distanceFromCurrent: Int = 99,
    modifier: Modifier = Modifier,
    playerContentColor: Color = Color.White,
) {
    val targetAlpha =
        if (isCurrent) {
            1.0f
        } else {
            when (distanceFromCurrent) {
                1 -> 0.25f
                2 -> 0.20f
                3 -> 0.15f
                else -> 0.08f
            }
        }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "lineAlpha",
    )

    val fontSize = if (isCurrent) 32.sp else 24.sp
    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCurrent) {
                            Modifier
                        } else {
                            Modifier.blur(1.dp)
                        },
                    ),
            text = originalWords,
            style =
                TextStyle(
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center,
                    fontFamily = typo().headlineLarge.fontFamily,
                ),
            color = playerContentColor.copy(alpha = animatedAlpha),
        )
        if (translatedWords != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = translatedWords,
                style = typo().bodyMedium,
                textAlign = TextAlign.Center,
                color = musica_accent.copy(alpha = animatedAlpha * 0.85f),
            )
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
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(12.dp))

         
        FlowRow(
            modifier =
                Modifier.fillMaxWidth().then(
                    if (isCurrent) {
                        Modifier
                    } else {
                        Modifier.blur(1.dp)
                    },
                ),
            horizontalArrangement = Arrangement.Center,
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
                    Modifier.fillMaxWidth().then(
                        if (isCurrent) {
                            Modifier
                        } else {
                            Modifier.blur(1.dp)
                        },
                    ),
                text = translatedWords,
                style = typo().bodyMedium,
                textAlign = TextAlign.Center,
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




