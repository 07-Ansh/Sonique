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
import com.sonique.domain.data.model.lyrics.RomanizationLanguage
import com.sonique.domain.data.model.metadata.Line
import com.sonique.domain.data.model.streams.TimeLine
import com.sonique.domain.repository.LyricsRomanizerRepository
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
import sonique.composeapp.generated.resources.now_playing_upper
import sonique.composeapp.generated.resources.unavailable
import sonique.composeapp.generated.resources.lyrics_offset
import sonique.composeapp.generated.resources.lyrics_offset_message
import sonique.composeapp.generated.resources.lyrics_offset_value
import sonique.composeapp.generated.resources.lyrics_sync_adjust
import sonique.composeapp.generated.resources.lyrics_offset_reset
import sonique.composeapp.generated.resources.share_lyrics
import androidx.compose.material.icons.filled.Share
import com.sonique.app.ui.component.lyrics.ShareLyricsSheet
import com.sonique.app.ui.component.lyrics.toShareLyricsLines
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.text.font.FontWeight
import com.sonique.domain.manager.DataStoreManager
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val TAG = "LyricsView"

private const val PLAYHEAD_TICK_MS = 50L

private const val EMP_AMOUNT_REF_MS = 2000f
private const val EMP_BLUR_REF_MS = 3000f
private const val EMP_MIN_DURATION_MS = 1000f
private const val EMP_AMOUNT_GAIN = 0.6f
private const val EMP_BLUR_GAIN = 0.5f
private const val EMP_AMOUNT_CAP = 1.2f
private const val EMP_BLUR_CAP = 0.8f
private const val EMP_LAST_WORD_AMOUNT = 1.6f
private const val EMP_LAST_WORD_BLUR = 1.5f
private const val EMP_SCALE_EM = 0.1f
private const val EMP_RISE_EM = 0.025f
private const val EMP_GLOW_RADIUS_EM = 0.3f

private const val FLARE_REACH_CHARS = 3.5f
private const val FLARE_FADE_MS = 2500
private const val FLARE_ATTACK_MS = 500
private const val FLARE_TAIL_CHARS = 2.5f

private const val CHAR_RISE_EM = 0.065f
private const val CHAR_RISE_MS = 1000

private const val SUNG_BASE_GLOW_ALPHA = 0.95f
private const val SUNG_BASE_GLOW_EM = 0.26f

private val EmpBezIn = CubicBezierEasing(0.2f, 0.4f, 0.58f, 1f)
private val EmpBezOut = CubicBezierEasing(0.3f, 0f, 0.58f, 1f)

private fun empEasing(x: Float): Float =
    if (x < 0.5f) {
        EmpBezIn.transform((x / 0.5f).coerceIn(0f, 1f))
    } else {
        1f - EmpBezOut.transform(((x - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }

@Composable
private fun rememberSmoothPlayhead(
    rawMs: Long,
    enabled: Boolean,
): State<Long> {
    val playhead = remember { mutableLongStateOf(rawMs) }
    LaunchedEffect(rawMs, enabled) {
        if (!enabled) {
            playhead.longValue = rawMs
            return@LaunchedEffect
        }
        var baseNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (baseNanos < 0L) baseNanos = frameNanos
                val elapsedMs = (frameNanos - baseNanos) / 1_000_000L
                playhead.longValue = rawMs + elapsedMs.coerceIn(0L, PLAYHEAD_TICK_MS)
            }
        }
    }
    return playhead
}

private const val INTERLUDE_MIN_GAP_MS = 3_000L
private const val INTERLUDE_DOT_COUNT = 3
private const val INTERLUDE_DOT = "\u2022"

private data class DisplayLines(
    val lines: List<Line>,
    val interludeIndices: Set<Int>,
)

private fun buildDisplayLines(
    lines: List<Line>?,
    syncType: String?,
): DisplayLines {
    val source = lines.orEmpty()
    if (syncType != "RICH_SYNCED" || source.size < 2) return DisplayLines(source, emptySet())

    val out = ArrayList<Line>(source.size)
    val inserted = mutableSetOf<Int>()
    source.forEachIndexed { index, line ->
        out += line
        val nextStartMs = source.getOrNull(index + 1)?.startTimeMs?.toLongOrNull() ?: return@forEachIndexed
        val lastWordStartMs =
            parseRichSyncWords(line.words, line.startTimeMs, line.endTimeMs)
                ?.words
                ?.lastOrNull()
                ?.startTimeMs
                ?: return@forEachIndexed
        val dotsStartMs = lastWordStartMs + INTERLUDE_MIN_GAP_MS
        if (nextStartMs <= dotsStartMs) return@forEachIndexed
        val interlude = interludeLine(dotsStartMs, nextStartMs) ?: return@forEachIndexed
        inserted += out.size
        out += interlude
    }
    return DisplayLines(out, inserted)
}

private fun interludeLine(
    startMs: Long,
    endMs: Long,
): Line? {
    val stepMs = (endMs - startMs) / INTERLUDE_DOT_COUNT
    val words =
        buildString {
            repeat(INTERLUDE_DOT_COUNT) { dot ->
                append(richSyncTimestamp(startMs + stepMs * dot) ?: return null)
                append(INTERLUDE_DOT)
            }
        }
    return Line(
        startTimeMs = startMs.toString(),
        endTimeMs = endMs.toString(),
        syllables = null,
        words = words,
    )
}

private fun richSyncTimestamp(ms: Long): String? {
    val centis = ms / 10
    val minutes = centis / 6000
    if (minutes > 99) return null
    return "<${twoDigits(minutes)}:${twoDigits((centis / 100) % 60)}.${twoDigits(centis % 100)}>"
}

private fun twoDigits(value: Long): String = if (value < 10) "0$value" else value.toString()

internal data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

internal fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    var lo = 0
    var hi = size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (this[mid].startTimeMs <= nowMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return if (ans >= 0) this[ans].index else -1
}

private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")
private val WHITESPACE_REGEX = Regex("""\s+""")

fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

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
    val dataStoreManager: DataStoreManager = koinInject()
    val romanizer: LyricsRomanizerRepository = koinInject()
    val lyricsOffsetMs by dataStoreManager.lyricsOffsetMs.collectAsStateWithLifecycle(0)
    val romanizationStored by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
    val romanizationLanguages = remember(romanizationStored) { RomanizationLanguage.parse(romanizationStored) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current by timeLine.collectAsStateWithLifecycle()
    val displayLines =
        remember(lyricsData.lyrics.lines, lyricsData.lyrics.syncType) {
            buildDisplayLines(lyricsData.lyrics.lines, lyricsData.lyrics.syncType)
        }

    val timedLineIndexes =
        remember(displayLines) {
            val timed =
                displayLines.lines
                    .mapIndexedNotNull { index, line ->
                        line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                    }
            if (timed.distinctBy { it.startTimeMs }.size <= 1) {
                emptyList()
            } else {
                timed.sortedBy { it.startTimeMs }
            }
        }

    val currentLineIndex by remember(timedLineIndexes, current, lyricsOffsetMs) {
        derivedStateOf {
            val now = current.current - lyricsOffsetMs
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now)
        }
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
            items(displayLines.lines.size) { index ->
                val line = displayLines.lines.getOrNull(index)
                val isInterlude = index in displayLines.interludeIndices
                val translatedWords =
                    if (isInterlude) {
                        null
                    } else if (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") {
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
                    val romanizedWords =
                        if (romanizationLanguages.isEmpty() || isInterlude) {
                            null
                        } else {
                            remember(words, romanizationLanguages) {
                                val source =
                                    if (lyricsData.lyrics.syncType == "RICH_SYNCED") words.stripRichSyncTimestamps() else words
                                romanizer.romanize(source, romanizationLanguages)
                            }
                        }
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
                                    romanizedWords = romanizedWords,
                                    currentTimeMs = current.current - lyricsOffsetMs,
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
                                    romanizedWords = romanizedWords,
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
                                romanizedWords = romanizedWords,
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
    romanizedWords: String? = null,
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
                if (romanizedWords != null) {
                    Text(
                        modifier =
                            Modifier.then(
                                if (isCurrent) {
                                    Modifier
                                } else {
                                    Modifier.blur(1.dp)
                                },
                            ),
                        text = romanizedWords,
                        style = typo().bodyMedium,
                        color =
                            if (isCurrent) {
                                playerContentColor.copy(alpha = 0.62f)
                            } else {
                                playerContentColor.copy(alpha = 0.3f)
                            },
                    )
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
            if (romanizedWords != null) {
                Text(
                    modifier = Modifier.blur(1.dp),
                    text = romanizedWords,
                    style = typo().bodyMedium,
                    color = playerContentColor.copy(alpha = 0.3f),
                )
            }
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
    romanizedWords: String? = null,
    currentTimeMs: Long,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    glow: Shadow? = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = 0f),
    pendingColorOverride: Color? = null,
    translatedColorOverride: Color? = null,
    wrappedLineSpacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
    playerContentColor: Color = Color.White,
) {
    val playhead = rememberSmoothPlayhead(currentTimeMs, enabled = isCurrent)

    val currentWordIndex by remember(parsedLine.words, isCurrent) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= playhead.value }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

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
            verticalArrangement =
                if (wrappedLineSpacing > 0.dp) Arrangement.spacedBy(wrappedLineSpacing) else Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                val wordEndTimeMs =
                    if (index < parsedLine.words.size - 1) {
                        parsedLine.words[index + 1].startTimeMs
                    } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                        if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                            val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                            wordTiming.startTimeMs + prevWordDuration
                        } else {
                            wordTiming.startTimeMs + 500L
                        }
                    } else {
                        parsedLine.lineEndTimeMs
                    }

                AnimatedWord(
                    word = wordTiming.text,
                    wordIndex = index,
                    wordStartTimeMs = wordTiming.startTimeMs,
                    wordEndTimeMs = wordEndTimeMs,
                    currentTimeMs = currentTimeMs,
                    playheadMs = playhead,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    customFontSize = customFontSize,
                    glow = glow,
                    isLastWord = index == parsedLine.words.lastIndex,
                    pendingColorOverride = pendingColorOverride,
                    playerContentColor = playerContentColor,
                )
            }
        }

        if (romanizedWords != null) {
            Text(
                modifier =
                    Modifier.then(
                        if (isCurrent) {
                            Modifier
                        } else {
                            Modifier.blur(1.dp)
                        },
                    ),
                text = romanizedWords,
                style = typo().bodyMedium,
                color = if (isCurrent) playerContentColor.copy(alpha = 0.62f) else playerContentColor.copy(alpha = 0.3f),
            )
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
                    translatedColorOverride
                        ?: if (isCurrent) {
                            musica_accent
                        } else {
                            musica_accent.copy(alpha = 0.3f)
                        },
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    wordIndex: Int,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    currentTimeMs: Long,
    playheadMs: State<Long>,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    glow: Shadow? = null,
    isLastWord: Boolean = false,
    pendingColorOverride: Color? = null,
    playerContentColor: Color = Color.White,
) {
    val style =
        typo().headlineLarge.copy(
            fontSize = customFontSize ?: typo().headlineLarge.fontSize,
        )

    if (!isCurrent) {
        Text(text = word, style = style, color = pendingColorOverride ?: playerContentColor.copy(alpha = 0.35f))
        return
    }

    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(1L)
    val anim =
        remember(wordStartTimeMs, wordEndTimeMs) {
            val initial =
                ((currentTimeMs - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            Animatable(initial)
        }

    LaunchedEffect(wordStartTimeMs, wordEndTimeMs, isActive, isPast) {
        when {
            isPast -> anim.snapTo(1f)
            isActive -> {
                val now = playheadMs.value
                val current =
                    ((now - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                        .coerceIn(0f, 1f)
                anim.snapTo(current)
                val remainingMs = (wordEndTimeMs - now).coerceAtLeast(0L).toInt().coerceAtLeast(1)
                anim.animateTo(1f, tween(remainingMs, easing = LinearEasing))
            }
        }
    }

    val progress = anim.value
    val wordProgress =
        when {
            isPast -> 1f
            isActive -> progress
            else -> 0f
        }

    val emphasisDurationMs = max(EMP_MIN_DURATION_MS, wordDurationMs.toFloat())
    val amount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_AMOUNT_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_AMOUNT_CAP, shaped * EMP_AMOUNT_GAIN * if (isLastWord) EMP_LAST_WORD_AMOUNT else 1f)
        }
    val blurAmount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_BLUR_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_BLUR_CAP, shaped * EMP_BLUR_GAIN * if (isLastWord) EMP_LAST_WORD_BLUR else 1f)
        }

    val flareGate by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = if (isActive) FLARE_ATTACK_MS else FLARE_FADE_MS,
                easing = LinearEasing,
            ),
        label = "flareGate",
    )

    val eased = if (amount <= 0f && blurAmount <= 0f) 0f else empEasing(wordProgress)
    val fontPx = with(LocalDensity.current) { style.fontSize.toPx() }
    val heldGlow =
        if (eased * blurAmount <= 0.01f) {
            null
        } else {
            glow?.copy(
                color = glow.color.copy(alpha = (eased * blurAmount).coerceIn(0f, 1f)),
                blurRadius = min(EMP_GLOW_RADIUS_EM, blurAmount * EMP_GLOW_RADIUS_EM) * fontPx,
            )
        }

    Box(
        modifier =
            Modifier.graphicsLayer {
                val scale = 1f + eased * EMP_SCALE_EM * amount
                scaleX = scale
                scaleY = scale
                translationY = -eased * EMP_RISE_EM * amount * fontPx
            },
    ) {
        val chars = word.toCharArray()
        val charCount = chars.size.coerceAtLeast(1)
        Row {
            chars.forEachIndexed { charIndex, ch ->
                val charFrom = charIndex.toFloat() / charCount
                val charTo = (charIndex + 1).toFloat() / charCount
                val charProgress = ((wordProgress - charFrom) / (charTo - charFrom)).coerceIn(0f, 1f)
                val charPast = wordProgress >= charTo
                val charActive = isActive && wordProgress >= charFrom && wordProgress < charTo

                val charCenter = (charFrom + charTo) / 2f
                val reach = (FLARE_REACH_CHARS / charCount).coerceAtLeast(0.0001f)
                val charFlare =
                    if (flareGate <= 0f || glow == null) {
                        0f
                    } else {
                        val delta = wordProgress - charCenter
                        val shape =
                            if (delta > 0f) {
                                exp(-delta / (FLARE_TAIL_CHARS / charCount))
                            } else {
                                (1f - abs(delta) / reach).coerceIn(0f, 1f)
                            }
                        shape * flareGate
                    }

                val charRise by animateFloatAsState(
                    targetValue = if (glow != null && charProgress > 0f) 1f else 0f,
                    animationSpec = tween(CHAR_RISE_MS, easing = FastOutSlowInEasing),
                    label = "charRise",
                )
                val restingColor = pendingColorOverride ?: playerContentColor.copy(alpha = 0.45f)
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            translationY = -charRise * CHAR_RISE_EM * fontPx
                        },
                ) {
                    val glowShadow = heldGlow ?: glow?.copy(blurRadius = SUNG_BASE_GLOW_EM * fontPx)
                    if (glowShadow != null) {
                        Text(
                            text = ch.toString(),
                            style =
                                style.copy(
                                    shadow =
                                        glowShadow.copy(
                                            color = glowShadow.color.copy(alpha = SUNG_BASE_GLOW_ALPHA * charFlare),
                                        ),
                                ),
                            color = Color.Transparent,
                        )
                    }
                    Text(
                        text = ch.toString(),
                        style = style,
                        color =
                            when {
                                charPast -> playerContentColor
                                charActive -> lerp(restingColor, playerContentColor, charProgress)
                                else -> restingColor
                            },
                    )
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

    var showTimingAdjustSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showShareLyricsSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val shareTimedLineIndexes =
        remember(screenDataState.lyricsData?.lyrics?.lines) {
            screenDataState.lyricsData
                ?.lyrics
                ?.lines
                .orEmpty()
                .mapIndexedNotNull { index, line ->
                    line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                }.sortedBy { it.startTimeMs }
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
                            if (screenDataState.lyricsData != null) {
                                IconButton(onClick = { showShareLyricsSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(Res.string.share_lyrics),
                                        tint = playerContentColor,
                                    )
                                }
                            }
                            IconButton(onClick = { showTimingAdjustSheet = true }) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_sync_24),
                                    contentDescription = stringResource(Res.string.lyrics_sync_adjust),
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
    if (showTimingAdjustSheet) {
        val timingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val lyricsOffsetMs by sharedViewModel.getLyricsOffsetMs().collectAsStateWithLifecycle(0)

        ModalBottomSheet(
            onDismissRequest = { showTimingAdjustSheet = false },
            sheetState = timingSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.lyrics_sync_adjust),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text =
                        stringResource(
                            Res.string.lyrics_offset_value,
                            if (lyricsOffsetMs > 0) "+$lyricsOffsetMs" else lyricsOffsetMs.toString(),
                        ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = stringResource(Res.string.lyrics_offset_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Slider(
                    value = lyricsOffsetMs.coerceIn(-5000, 5000).toFloat(),
                    onValueChange = { newValue ->
                        sharedViewModel.setLyricsOffsetMs(newValue.roundToInt())
                    },
                    valueRange = -5000f..5000f,
                    steps = 199,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { sharedViewModel.setLyricsOffsetMs(lyricsOffsetMs - 500) },
                    ) {
                        Text("-500")
                    }
                    OutlinedButton(
                        onClick = { sharedViewModel.setLyricsOffsetMs(lyricsOffsetMs - 100) },
                    ) {
                        Text("-100")
                    }
                    FilledTonalButton(
                        onClick = { sharedViewModel.setLyricsOffsetMs(0) },
                    ) {
                        Text(stringResource(Res.string.lyrics_offset_reset))
                    }
                    OutlinedButton(
                        onClick = { sharedViewModel.setLyricsOffsetMs(lyricsOffsetMs + 100) },
                    ) {
                        Text("+100")
                    }
                    OutlinedButton(
                        onClick = { sharedViewModel.setLyricsOffsetMs(lyricsOffsetMs + 500) },
                    ) {
                        Text("+500")
                    }
                }
            }
        }
    }

    screenDataState.lyricsData?.let { lyricsData ->
        if (showShareLyricsSheet) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = screenDataState.nowPlayingTitle,
                artistName = screenDataState.artistName,
                artwork = screenDataState.bitmap,
                seedColor = color,
                initialLineIndex = shareTimedLineIndexes.activeIndexAt(timelineState.current),
                onDismiss = { showShareLyricsSheet = false },
            )
        }
    }
}

