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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import com.sonique.domain.data.model.metadata.Line
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlinx.coroutines.isActive
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

private const val LYRICS_ANCHOR_RATIO = 0.5f
private val LYRICS_ITEM_FALLBACK_HEIGHT_DP = 68.dp
private val LYRICS_ITEM_GAP_DP = 16.dp
private val LYRICS_FADE_TOP_DP = 130.dp
private val LYRICS_FADE_BOTTOM_DP = 160.dp
private const val LYRICS_STAGGER_DELAY_PER_DISTANCE = 20
private const val LYRICS_STAGGER_DELAY_MAX_MS = 200
private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongIntroIndicator(
    gapStartMs: Long,
    gapEndMs: Long,
    currentPositionMs: Long,
    visible: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }
    val rowHeightPx = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            rowHeightPx.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
            alpha.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
        } else {
            alpha.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
            rowHeightPx.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
        }
    }

    if (rowHeightPx.value <= 0.01f && !visible) {
        return
    }

    val progress = if (gapEndMs > gapStartMs) {
        ((currentPositionMs - gapStartMs).toFloat() / (gapEndMs - gapStartMs).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "introIntervalProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp * rowHeightPx.value)
            .padding(vertical = (8.dp * rowHeightPx.value).coerceAtLeast(0.dp))
            .graphicsLayer {
                this.alpha = alpha.value
                this.clip = true
            },
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(36.dp)
                .alpha(alpha.value),
            color = color,
            trackColor = color.copy(alpha = 0.22f),
        )
    }
}

data class SyncedWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startCharIdx: Int,
    val endCharIdx: Int,
)

fun prepareLineWords(
    rawText: String,
    cleanText: String,
    lineStartTimeMs: Long,
    lineEndTimeMs: Long,
    syncType: String?,
): List<SyncedWord> {
    if (cleanText.isBlank()) return emptyList()

    // 1. If RICH_SYNCED, parse per-word timestamps like <00:12.34> word
    if (syncType == "RICH_SYNCED") {
        val parsed = parseRichSyncWords(rawText, lineStartTimeMs.toString(), lineEndTimeMs.toString())
        if (parsed != null && parsed.words.isNotEmpty()) {
            val result = mutableListOf<SyncedWord>()
            var searchCursor = 0
            for (i in parsed.words.indices) {
                val w = parsed.words[i]
                val nextStart = parsed.words.getOrNull(i + 1)?.startTimeMs
                val wordEnd = nextStart ?: (w.startTimeMs + 650L).coerceAtMost(lineEndTimeMs)

                val foundIndex = cleanText.indexOf(w.text, searchCursor, ignoreCase = true)
                val startIdx = if (foundIndex != -1) foundIndex else searchCursor.coerceAtMost(cleanText.length - 1)
                val endIdx = (startIdx + w.text.length).coerceAtMost(cleanText.length)
                searchCursor = endIdx

                result.add(
                    SyncedWord(
                        text = w.text,
                        startTimeMs = w.startTimeMs,
                        endTimeMs = wordEnd.coerceAtLeast(w.startTimeMs + 50L),
                        startCharIdx = startIdx,
                        endCharIdx = endIdx
                    )
                )
            }
            if (result.isNotEmpty()) return result
        }
    }

    // 2. Fallback or LINE_SYNCED: synthesize smooth word timings distributed across line duration
    val words = cleanText.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()

    val lineDuration = if (lineEndTimeMs > lineStartTimeMs) {
        (lineEndTimeMs - lineStartTimeMs).coerceIn(800L, 10000L)
    } else {
        3500L
    }
    // Active singing takes about 85% of line duration, leaving a brief musical pause at line end
    val activeSingingDuration = (lineDuration * 0.85f).toLong()
    val totalChars = words.sumOf { it.length }.coerceAtLeast(1)

    val result = mutableListOf<SyncedWord>()
    var accumulatedTime = 0L
    var searchCursor = 0

    for (i in words.indices) {
        val word = words[i]
        val proportion = word.length.toFloat() / totalChars
        val wordDuration = (proportion * activeSingingDuration).toLong().coerceIn(150L, 3000L)
        val wStart = lineStartTimeMs + accumulatedTime
        val wEnd = wStart + wordDuration
        accumulatedTime += wordDuration

        val foundIndex = cleanText.indexOf(word, searchCursor)
        val startIdx = if (foundIndex != -1) foundIndex else searchCursor.coerceAtMost(cleanText.length - 1)
        val endIdx = (startIdx + word.length).coerceAtMost(cleanText.length)
        searchCursor = endIdx

        result.add(
            SyncedWord(
                text = word,
                startTimeMs = wStart,
                endTimeMs = wEnd,
                startCharIdx = startIdx,
                endCharIdx = endIdx
            )
        )
    }

    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    playerContentColor: Color = Color.White,
    onShareLyrics: ((Int) -> Unit)? = null,
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
    val current by timeLine.collectAsStateWithLifecycle()

    val lines = remember(effectiveLyricsData.lyrics.lines) {
        effectiveLyricsData.lyrics.lines.orEmpty()
    }
    val syncType = effectiveLyricsData.lyrics.syncType
    val isSynced = syncType == "LINE_SYNCED" || syncType == "RICH_SYNCED"
    val firstStartMs = remember(lines) {
        lines.firstOrNull()?.startTimeMs?.toLongOrNull() ?: 0L
    }

    var smoothPosition by remember { mutableLongStateOf(current.current) }
    var lastPlayerPos by remember { mutableLongStateOf(current.current) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(current.current) {
        lastPlayerPos = current.current
        lastUpdateTime = System.currentTimeMillis()
        smoothPosition = current.current
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                val now = System.currentTimeMillis()
                if (lastUpdateTime > 0L) {
                    val elapsed = now - lastUpdateTime
                    smoothPosition = lastPlayerPos + elapsed.coerceAtMost(1500L)
                }
            }
        }
    }

    val currentLineIndex by remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced || lines.isEmpty()) return@derivedStateOf -1
            val pos = smoothPosition
            var found = -1
            for (i in lines.indices) {
                val startTimeMs = lines[i].startTimeMs.toLongOrNull() ?: 0L
                val nextStartMs = lines.getOrNull(i + 1)?.startTimeMs?.toLongOrNull()
                val endTimeMs = nextStartMs ?: (startTimeMs + 60000L)
                if (pos in startTimeMs until endTimeMs) {
                    found = i
                    break
                }
            }
            if (found != -1) {
                found
            } else if (pos < firstStartMs) {
                -1
            } else {
                lines.lastIndex
            }
        }
    }

    val hasIntro = isSynced && firstStartMs >= 3500L
    val isIntroActive = hasIntro && smoothPosition < (firstStartMs - 500L)

    fun findClosestTranslatedLine(originalTimeMs: String): String? {
        val translatedLines = effectiveLyricsData.translatedLyrics?.first?.lines ?: return null
        if (translatedLines.isEmpty()) return null

        val originalTime = originalTimeMs.toLongOrNull() ?: return null

        return translatedLines
            .minByOrNull {
                abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
            }?.let {
                val absDiff = abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                if (absDiff < 1000L) {
                    it
                } else {
                    null
                }
            }?.words
    }

    val lazyListState = rememberLazyListState()
    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()
    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // When user drags, pause auto-scroll. Resume after 3.5s of inactivity.
    var autoScrollResumeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(isDragged) {
        if (isDragged) {
            isAutoScrollEnabled = false
            autoScrollResumeJob?.cancel()
            autoScrollResumeJob = scope.launch {
                delay(3500)
                isAutoScrollEnabled = true
            }
        }
    }

    // Smooth scroll to active line using reading anchor ratio (~0.38f)
    LaunchedEffect(currentLineIndex, isAutoScrollEnabled, isIntroActive) {
        val targetListIndex = if (hasIntro) {
            if (isIntroActive) 0 else if (currentLineIndex >= 0) currentLineIndex + 1 else -1
        } else {
            currentLineIndex
        }

        if (isAutoScrollEnabled && targetListIndex >= 0) {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetListIndex }
            if (itemInfo != null) {
                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val targetAnchorY = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight * LYRICS_ANCHOR_RATIO).toInt()
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - targetAnchorY
                if (abs(offset) > 8) {
                    lazyListState.animateScrollBy(
                        value = offset.toFloat(),
                        animationSpec = tween<Float>(durationMillis = 700, easing = FastOutSlowInEasing)
                    )
                }
            } else {
                lazyListState.animateScrollToItem(targetListIndex)
            }
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().padding(bottom = 8.dp)
    ) {
        val verticalPadding = (maxHeight / 2) - 40.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val topFade = 64.dp.toPx()
                    val bottomFade = 80.dp.toPx()
                    val topStop = (topFade / size.height).coerceIn(0f, 0.25f)
                    val bottomStop = (1f - bottomFade / size.height).coerceIn(0.75f, 1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            topStop to Color.Black,
                            bottomStop to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = verticalPadding, bottom = verticalPadding),
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (hasIntro) {
                    item(key = "song_intro_wavy_indicator") {
                        SongIntroIndicator(
                            gapStartMs = 0L,
                            gapEndMs = firstStartMs - 500L,
                            currentPositionMs = smoothPosition,
                            visible = isIntroActive,
                            color = playerContentColor,
                        )
                    }
                }
                itemsIndexed(
                    items = lines,
                    key = { index, line -> "$index-${line.startTimeMs}" }
                ) { listIndex, line ->
                    val isCurrent = listIndex == currentLineIndex
                    val distanceFromCurrent = if (currentLineIndex >= 0) abs(listIndex - currentLineIndex) else 99
                    val words = line.words ?: ""
                    val cleanText = remember(words) { words.stripRichSyncTimestamps() }
                    val translatedWords = if (isSynced) {
                        line.startTimeMs.let { findClosestTranslatedLine(it) }
                    } else {
                        effectiveLyricsData.translatedLyrics?.first?.lines?.getOrNull(listIndex)?.words
                    }

                    val lineStartTime = remember(line.startTimeMs) { line.startTimeMs.toLongOrNull() ?: 0L }
                    val lineEndTime = remember(line.endTimeMs, lines, listIndex) {
                        val nextStartMs = lines.getOrNull(listIndex + 1)?.startTimeMs?.toLongOrNull()
                        line.endTimeMs.toLongOrNull() ?: nextStartMs ?: (lineStartTime + 4000L)
                    }

                    val lineWords = remember(words, cleanText, lineStartTime, lineEndTime, syncType) {
                        prepareLineWords(
                            rawText = words,
                            cleanText = cleanText,
                            lineStartTimeMs = lineStartTime,
                            lineEndTimeMs = lineEndTime,
                            syncType = syncType
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSynced) {
                                        isAutoScrollEnabled = true
                                        val timeMs = line.startTimeMs.toFloatOrNull() ?: 0f
                                        val total = timeLine.value.total
                                        if (total > 0) {
                                            onLineClick((timeMs / total) * 100f)
                                        }
                                    }
                                },
                                onLongClick = {
                                    onShareLyrics?.invoke(listIndex)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        SyncedWordLevelLyricsLine(
                            cleanText = cleanText,
                            lineWords = lineWords,
                            translatedWords = translatedWords,
                            isCurrent = isCurrent,
                            isSynced = isSynced,
                            distanceFromCurrent = distanceFromCurrent,
                            smoothPositionProvider = { smoothPosition },
                            playerContentColor = playerContentColor,
                        )
                    }
                }
            }

            // Floating "Sync" pill when user has manually scrolled away
            AnimatedVisibility(
                visible = !isAutoScrollEnabled && currentLineIndex in lines.indices,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Surface(
                    onClick = { isAutoScrollEnabled = true },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_sync_24),
                            contentDescription = "Sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sync to song",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncedWordLevelLyricsLine(
    cleanText: String,
    lineWords: List<SyncedWord>,
    translatedWords: String?,
    isCurrent: Boolean,
    isSynced: Boolean,
    distanceFromCurrent: Int,
    smoothPositionProvider: () -> Long,
    playerContentColor: Color,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val targetAlpha = if (!isSynced || isCurrent) {
        1.0f
    } else {
        when (distanceFromCurrent) {
            1 -> 0.45f
            2 -> 0.35f
            3 -> 0.25f
            4 -> 0.18f
            else -> 0.12f
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "lineAlpha"
    )

    val fontSize = if (isCurrent) 24.sp else 18.sp
    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium

    val lyricStyle = TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = (-0.3).sp,
        textAlign = TextAlign.Center,
        fontFamily = typo().headlineLarge.fontFamily,
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = constraints.maxWidth.coerceAtLeast(1)
        val layoutResult = remember(cleanText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(
                text = cleanText,
                style = lyricStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }

        val lineHeightDp = with(density) { layoutResult.size.height.toDp() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineHeightDp)
                    .then(if (isCurrent) Modifier else Modifier.blur(1.dp))
            ) {
                if (cleanText.isEmpty()) return@Canvas

                if (!isSynced || !isCurrent) {
                    drawText(layoutResult, color = playerContentColor.copy(alpha = animatedAlpha))
                } else {
                    val currentSmoothPos = smoothPositionProvider()

                    // 1. Draw base un-sung text (dimmed)
                    drawText(layoutResult, color = playerContentColor.copy(alpha = 0.30f))

                    // 2. Draw sung and actively singing words with smooth karaoke sweep
                    for (word in lineWords) {
                        val isWordSung = currentSmoothPos >= word.endTimeMs
                        val isWordActive = currentSmoothPos in word.startTimeMs until word.endTimeMs

                        if (!isWordSung && !isWordActive) continue

                        val sungFactor = if (isWordSung) 1f else {
                            val dur = (word.endTimeMs - word.startTimeMs).coerceAtLeast(40L)
                            ((currentSmoothPos - word.startTimeMs).toFloat() / dur).coerceIn(0f, 1f)
                        }

                        if (word.startCharIdx >= cleanText.length) continue
                        val sIdx = word.startCharIdx.coerceIn(0, cleanText.length - 1)
                        val eIdx = (word.endCharIdx - 1).coerceIn(0, cleanText.length - 1)

                        val startBounds = layoutResult.getBoundingBox(sIdx)
                        val endBounds = layoutResult.getBoundingBox(eIdx)

                        if (startBounds.top == endBounds.top) {
                            val wLeft = minOf(startBounds.left, endBounds.left)
                            val wRight = maxOf(startBounds.right, endBounds.right)
                            val wTop = startBounds.top
                            val wBottom = startBounds.bottom

                            if (isWordSung) {
                                clipRect(
                                    left = wLeft - 2f,
                                    top = wTop - 4f,
                                    right = wRight + 2f,
                                    bottom = wBottom + 4f
                                ) {
                                    drawText(layoutResult, color = playerContentColor)
                                }
                            } else if (isWordActive) {
                                val fillWidth = (wRight - wLeft) * sungFactor
                                clipRect(
                                    left = wLeft - 2f,
                                    top = wTop - 4f,
                                    right = wLeft + fillWidth,
                                    bottom = wBottom + 4f
                                ) {
                                    drawText(layoutResult, color = playerContentColor)
                                }
                            }
                        } else {
                            // Word spans across wrapped lines
                            val wordChars = (sIdx..eIdx).toList()
                            val totalWordChars = wordChars.size.coerceAtLeast(1)
                            for ((charPos, cIdx) in wordChars.withIndex()) {
                                val charBounds = layoutResult.getBoundingBox(cIdx)
                                val charSung = isWordSung || (sungFactor >= (charPos + 1).toFloat() / totalWordChars)
                                val charActive = !charSung && (sungFactor > charPos.toFloat() / totalWordChars)

                                if (charSung) {
                                    clipRect(
                                        left = charBounds.left - 1f,
                                        top = charBounds.top - 4f,
                                        right = charBounds.right + 1f,
                                        bottom = charBounds.bottom + 4f
                                    ) {
                                        drawText(layoutResult, color = playerContentColor)
                                    }
                                } else if (charActive) {
                                    val charProgress = ((sungFactor * totalWordChars) - charPos).coerceIn(0f, 1f)
                                    val cFill = charBounds.width * charProgress
                                    clipRect(
                                        left = charBounds.left - 1f,
                                        top = charBounds.top - 4f,
                                        right = charBounds.left + cFill,
                                        bottom = charBounds.bottom + 4f
                                    ) {
                                        drawText(layoutResult, color = playerContentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (translatedWords != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isCurrent) Modifier else Modifier.blur(1.dp)),
                    text = translatedWords,
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isCurrent) musica_accent else musica_accent.copy(alpha = animatedAlpha * 0.75f),
                )
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

