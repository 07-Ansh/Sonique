package com.sonique.app.ui.component

import androidx.activity.compose.BackHandler


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.extension.KeepScreenOn
import com.sonique.app.extension.shimmer
import com.sonique.app.ui.component.shimmerColor
import com.sonique.app.viewModel.LyricsProvider
import com.sonique.app.viewModel.NowPlayingScreenData
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import com.sonique.domain.data.model.metadata.LyricsEntry
import com.sonique.domain.data.model.metadata.WordTimestamp
import com.sonique.domain.data.model.streams.TimeLine
import com.sonique.domain.lyrics.LyricsUtils
import com.sonique.domain.lyrics.utils.HindiTransliterator
import com.sonique.domain.manager.DataStoreManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.RectangleShape
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LYRICS_TEXT_SIZE = 26f
private const val LYRICS_LINE_SPACING = 1.25f
private const val ANCHOR_RATIO = 0.35f           // active line sits at 35% from top
private const val FLING_FRICTION = 1.8f          // higher = faster decel
private const val AUTO_SCROLL_RESUME_MS = 5_000L // not used for timer, kept for reference
private const val MAX_SELECTED_LINES = 5

// ── Public entry-point composable ─────────────────────────────────────────────

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    playerContentColor: Color = Color.White,
    isAutoScrollEnabledState: Boolean = true,
    onAutoScrollStateChanged: ((Boolean) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") showScrollShadows: Boolean = false,
    @Suppress("UNUSED_PARAMETER") backgroundColor: Color = Color.Transparent,
) {
    val current by timeLine.collectAsStateWithLifecycle()
    KeepScreenOn()

    // ── Parse + build merged list ─────────────────────────────────────────────
    val parsedEntries: List<LyricsEntry> = remember(lyricsData.lyrics) {
        val rawLrc = lyricsData.lyrics.SoniqueLyricsId
        val initialList = if (!rawLrc.isNullOrBlank()) {
            LyricsUtils.parseLyrics(rawLrc)
        } else {
            lyricsData.lyrics.lines.orEmpty().map { line ->
                LyricsEntry(
                    time = line.startTimeMs.toLongOrNull() ?: 0L,
                    text = line.words,
                    words = null,
                    agent = null,
                    isBackground = false
                )
            }
        }
        initialList.map { entry ->
            if (entry.text.isBlank()) entry
            else entry.copy(text = formatMax3WordsPerRow(entry.text))
        }
    }

    LaunchedEffect(parsedEntries) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            parsedEntries.forEach { entry ->
                var transliterated: String? = null
                if (LyricsUtils.isHindi(entry.text)) {
                    transliterated = LyricsUtils.romanizeHindi(entry.text)
                } else if (LyricsUtils.isPunjabi(entry.text)) {
                    transliterated = LyricsUtils.romanizePunjabi(entry.text)
                } else {
                    val fallback = HindiTransliterator.transliterateLine(entry.text)
                    if (fallback.isNotBlank() && fallback != entry.text) {
                        transliterated = fallback
                    }
                }
                if (!transliterated.isNullOrBlank() && transliterated != entry.text) {
                    entry.romanizedTextFlow.value = formatMax3WordsPerRow(transliterated)
                }
            }
        }
    }





    val mergedList: List<LyricsListItem> = remember(parsedEntries) {
        buildMergedList(parsedEntries)
    }

    val isSynced = lyricsData.lyrics.syncType != null


    // Active line
    val activeLineIndices by remember(parsedEntries, current.current) {
        derivedStateOf { LyricsUtils.findActiveLineIndices(parsedEntries, current.current) }
    }
    val currentLineIndex by remember(parsedEntries, current.current) {
        derivedStateOf { LyricsUtils.findCurrentLineIndex(parsedEntries, current.current) }
    }
    val activeListItemIndex by remember(mergedList, currentLineIndex) {
        derivedStateOf {
            val targetEntry = parsedEntries.getOrNull(currentLineIndex) ?: LyricsEntry.HEAD_LYRICS_ENTRY
            val idx = mergedList.indexOfFirst { it is LyricsListItem.Line && it.entry.time == targetEntry.time }
            if (idx != -1) idx else 0
        }
    }



    // ── Scroll engine state ───────────────────────────────────────────────────
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var userManualOffset by remember { mutableFloatStateOf(0f) }
    var isAutoScrollEnabled by remember(isAutoScrollEnabledState) { mutableStateOf(isAutoScrollEnabledState) }
    
    val updateAutoScroll: (Boolean) -> Unit = { enabled ->
        isAutoScrollEnabled = enabled
        onAutoScrollStateChanged?.invoke(enabled)
    }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Selection state
    val selectedIndices = remember { mutableStateOf(setOf<Int>()) }
    var isSelectionModeActive by remember { mutableStateOf(false) }

    // Share intent (Android)
    val context = LocalContext.current

    // Re-enable auto-scroll when track changes
    LaunchedEffect(lyricsData) {
        isAutoScrollEnabled = true
        userManualOffset = 0f
        isSelectionModeActive = false
        selectedIndices.value = emptySet()
    }

    // Back handler — cancel selection before navigating away
    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.value = emptySet()
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val anchorY = containerHeightPx * ANCHOR_RATIO

        val lineHeightPx = with(density) { 68.dp.toPx() }
        val indicatorHeightPx = with(density) { 72.dp.toPx() }
        val constraintLineHeightPx = with(density) { 120.dp.toPx() }
        val gapPx = with(density) { 16.dp.toPx() }

        // Compute relative Y positions centered at activeListItemIndex = anchorY (Metrolist exact algorithm)
        val positions = remember(itemHeights.toMap(), activeListItemIndex, mergedList) {
            val map = mutableMapOf<Int, Float>()
            if (activeListItemIndex == -1 || mergedList.isEmpty()) return@remember map

            map[activeListItemIndex] = 0f
            var currentY = 0f
            for (i in activeListItemIndex - 1 downTo 0) {
                val item = mergedList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                currentY -= (height + if (noGap) 0f else gapPx)
                map[i] = currentY
            }
            currentY = 0f
            for (i in activeListItemIndex until mergedList.size - 1) {
                val currentItem = mergedList[i]
                val nextItem = mergedList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else lineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                currentY += (height + if (nextNoGap) 0f else gapPx)
                map[i + 1] = currentY
            }
            map
        }

        val minOffset = remember(itemHeights.toMap(), mergedList, activeListItemIndex, anchorY) {
            if (mergedList.isEmpty() || activeListItemIndex == -1) return@remember 0f
            val totalBelow = (activeListItemIndex until mergedList.size - 1).sumOf { i ->
                val currentItem = mergedList[i]
                val nextItem = mergedList[i + 1]
                val height = itemHeights[i]?.toFloat() ?: (if (currentItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val nextNoGap = (nextItem as? LyricsListItem.Line)?.entry?.isBackground == true || nextItem is LyricsListItem.Indicator
                (height + if (nextNoGap) 0f else gapPx).toDouble()
            }.toFloat()
            val lastItem = mergedList.last()
            val lastHeight = itemHeights[mergedList.size - 1]?.toFloat() ?: (if (lastItem is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
            with(density) { 100.dp.toPx() } - anchorY - totalBelow - lastHeight
        }

        val maxOffset = remember(itemHeights.toMap(), mergedList, activeListItemIndex, containerHeightPx, anchorY) {
            if (mergedList.isEmpty() || activeListItemIndex == -1) return@remember 0f
            val totalAbove = (0 until activeListItemIndex).sumOf { i ->
                val item = mergedList[i]
                val height = itemHeights[i]?.toFloat() ?: (if (item is LyricsListItem.Indicator) indicatorHeightPx else constraintLineHeightPx)
                val noGap = (item as? LyricsListItem.Line)?.entry?.isBackground == true || item is LyricsListItem.Indicator
                (height + if (noGap) 0f else gapPx).toDouble()
            }.toFloat()
            containerHeightPx - with(density) { 150.dp.toPx() } - anchorY + totalAbove
        }

        val scrollClampMin = minOf(minOffset, maxOffset)
        val scrollClampMax = maxOf(minOffset, maxOffset)

        var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        val decayAnimSpec = remember { exponentialDecay<Float>(frictionMultiplier = FLING_FRICTION) }

        LaunchedEffect(isAutoScrollEnabled) {
            if (isAutoScrollEnabled) {
                val start = userManualOffset
                if (abs(start) < 1f) {
                    userManualOffset = 0f
                    return@LaunchedEffect
                }
                val anim = Animatable(start)
                var lastValue = start
                anim.animateTo(0f, tween((abs(start) / 4f).toInt().coerceIn(200, 600), easing = FastOutSlowInEasing)) {
                    userManualOffset += (value - lastValue)
                    lastValue = value
                }
                userManualOffset = 0f
            }
        }

        // Drag + fling gesture
        val velocityTracker = remember { VelocityTracker() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(top = 100.dp, bottom = 140.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            flingJob?.cancel()
                            velocityTracker.resetTracking()
                            updateAutoScroll(false)
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            verticalDrag(down.id) { change ->
                                userManualOffset = (userManualOffset + change.positionChange().y).coerceIn(scrollClampMin, scrollClampMax)
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            }
                            val velocity = velocityTracker.calculateVelocity().y
                            flingJob = scope.launch {
                                AnimationState(initialValue = userManualOffset, initialVelocity = velocity).animateDecay(decayAnimSpec) {
                                    val clamped = value.coerceIn(scrollClampMin, scrollClampMax)
                                    userManualOffset = clamped
                                    if (value != clamped) cancelAnimation()
                                }
                            }
                        }
                    }
                }
        ) {
            // Render each item (Metrolist exact stagger + offset animation per line)
            mergedList.forEachIndexed { index, listItem ->



                val distance = abs(index - activeListItemIndex)
                val targetOffset = anchorY + positions.getOrDefault(index, (index - activeListItemIndex) * lineHeightPx)
                val animatedOffset by animateFloatAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(
                        durationMillis = 750,
                        delayMillis = (distance * 20L).toInt().coerceAtMost(200),
                        easing = FastOutSlowInEasing
                    ),
                    label = "lyricStaggeredOffset_$index"
                )

                val itemY = animatedOffset + userManualOffset

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, 0) {
                                placeable.place(0, 0)
                            }
                        }
                        .offset { IntOffset(0, itemY.roundToInt()) }
                ) {
                    when (listItem) {

                        is LyricsListItem.Line -> {
                            val entry = listItem.entry
                            val entryLineIndex = parsedEntries.indexOfFirst { it.time == entry.time }
                            val isActive = entryLineIndex >= 0 && entryLineIndex in activeLineIndices
                            val isSelected = index in selectedIndices.value

                            LyricsLine(
                                index = index,
                                item = entry,
                                isSynced = isSynced,
                                isActiveLine = isActive,
                                bgVisible = true,
                                isSelected = isSelected,
                                isSelectionModeActive = isSelectionModeActive,
                                currentPositionState = current.current,
                                isPlaying = true,
                                lyricsOffset = 0L,
                                lyricsTextSize = LYRICS_TEXT_SIZE,
                                lyricsLineSpacing = LYRICS_LINE_SPACING,
                                expressiveAccent = playerContentColor,
                                lyricsTextPosition = LyricsPosition.CENTER,
                                respectAgentPositioning = false,

                                isAutoScrollEnabled = isAutoScrollEnabled,
                                displayedCurrentLineIndex = activeListItemIndex,
                                romanizeAsMain = false,
                                romanizeLyrics = true,
                                enabledLanguages = listOf("hi"),


                                onSizeChanged = { height ->
                                    if (itemHeights[index] != height) {
                                        itemHeights[index] = height
                                    }
                                },
                                onClick = {
                                    if (isSelectionModeActive) {
                                        // Toggle selection
                                        val current = selectedIndices.value.toMutableSet()
                                        if (index in current) current.remove(index)
                                        else if (current.size < MAX_SELECTED_LINES) current.add(index)
                                        selectedIndices.value = current
                                    } else {
                                        // Seek
                                        if (entry.time > 0L && timeLine.value.total > 0L) {
                                            onLineClick(entry.time.toFloat() * 100f / timeLine.value.total)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionModeActive) {
                                        isSelectionModeActive = true
                                        val s = mutableSetOf(index)
                                        selectedIndices.value = s
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is LyricsListItem.Indicator -> {
                            IntervalIndicator(
                                gapStartMs = listItem.gapStartMs,
                                gapEndMs = listItem.gapEndMs - 650L,
                                currentPositionMs = current.current,
                                visible = current.current in listItem.gapStartMs..(listItem.gapEndMs - 650L),
                                color = playerContentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            itemHeights[index] = placeable.height
                                            placeable.place(0, 0)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // ── Action overlay — auto-scroll re-engage + selection share ──────────
        LyricsActionOverlay(
            isAutoScrollEnabled = isAutoScrollEnabled,
            isSynced = isSynced,
            isSelectionModeActive = isSelectionModeActive,
            anySelected = selectedIndices.value.isNotEmpty(),
            onSyncClick = {
                updateAutoScroll(true)
                userManualOffset = 0f
            },
            onCancelSelection = {
                isSelectionModeActive = false
                selectedIndices.value = emptySet()
            },
            onShareSelection = {
                val lines = selectedIndices.value
                    .sorted()
                    .mapNotNull { idx ->
                        (mergedList.getOrNull(idx) as? LyricsListItem.Line)?.entry?.text
                    }
                    .joinToString("\n")
                if (lines.isNotBlank()) {
                    val intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "\"$lines\"")
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(intent, "Share lyrics")
                    )
                }
                isSelectionModeActive = false
                selectedIndices.value = emptySet()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

// ── Shimmer placeholder for loading state ─────────────────────────────────────

@Composable
fun LyricsShimmer(
    modifier: Modifier = Modifier,
    lineColor: Color = shimmerColor()
) {
    val widths = remember { listOf(0.75f, 0.55f, 0.85f, 0.6f, 0.7f, 0.5f, 0.8f, 0.65f, 0.9f, 0.55f) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        widths.forEach { widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(lineColor)
                    .shimmer()
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildMergedList(parsedEntries: List<LyricsEntry>): List<LyricsListItem> {
    val result = mutableListOf<LyricsListItem>()
    if (parsedEntries.isEmpty()) return result

    val linesWithHead = listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedEntries
    linesWithHead.forEachIndexed { i, entry ->
        if (entry.text.isNotBlank() || entry == LyricsEntry.HEAD_LYRICS_ENTRY) {
            result.add(LyricsListItem.Line(i, entry))
        }
        if (i < linesWithHead.size - 1) {
            val nextStart = linesWithHead[i + 1].time
            val entryWords = entry.words
            val currentEnd = when {
                !entryWords.isNullOrEmpty() -> (entryWords.last().endTime * 1000).toLong()
                entry.text.isBlank() -> entry.time
                else -> null
            }
            if (currentEnd != null && currentEnd < nextStart) {
                val gap = nextStart - currentEnd
                if (gap > 4000L) {
                    result.add(
                        LyricsListItem.Indicator(i, gap, currentEnd, nextStart, linesWithHead[i + 1].agent)
                    )
                }
            }
        }
    }
    return result
}

/**
 * Human-readable display name for each lyrics provider shown in the provider credit.
 */
private fun LyricsProvider.displayName(): String = when (this) {
    LyricsProvider.LRCLIB -> "LrcLib"
    LyricsProvider.YOUTUBE -> "YouTube"
    LyricsProvider.SPOTIFY -> "Spotify"
    LyricsProvider.AI -> "AI"
    LyricsProvider.OFFLINE -> "Offline"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lyricsData = screenDataState.lyricsData
    if (lyricsData != null) {
        KeepScreenOn()
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        containerColor = color,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (lyricsData != null) {
                LyricsView(
                    lyricsData = lyricsData,
                    timeLine = sharedViewModel.timeline,
                    onLineClick = { progress -> sharedViewModel.onUIEvent(UIEvent.UpdateProgress(progress)) }
                )
            }
        }
    }
}

private fun formatMax3WordsPerRow(text: String): String {
    if (text.isBlank()) return text
    return text.lines().joinToString("\n") { line ->
        val words = line.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size <= 3) {
            line.trim()
        } else {
            words.chunked(3).joinToString("\n") { chunk -> chunk.joinToString(" ") }
        }
    }
}
