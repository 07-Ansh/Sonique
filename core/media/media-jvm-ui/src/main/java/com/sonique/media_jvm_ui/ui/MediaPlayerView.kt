package com.sonique.media_jvm_ui.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.domain.data.model.metadata.Lyrics
import com.sonique.domain.data.model.streams.TimeLine
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.logger.Logger
import com.sonique.media_jvm.GstreamerPlayerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.elements.PlayBin
import org.freedesktop.gstreamer.swing.GstVideoComponent
import org.koin.compose.koinInject
import java.awt.Dimension
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.swing.BoxLayout
import javax.swing.JPanel

@Composable
fun MediaPlayerViewWithUrl(
    url: String,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var gsVideoComponent by remember {
        mutableStateOf<GstVideoComponent?>(null)
    }
    DisposableEffect(url) {
        scope.launch(Dispatchers.Swing) {
            val gsv = GstVideoComponent()
            gsv.background = java.awt.Color.BLACK
            gsv.alignmentX = java.awt.Component.CENTER_ALIGNMENT
            gsVideoComponent = gsv
            val playBin =
                PlayBin("canvas").apply {
                    setVideoSink(gsv.element)
                }
            playBin.autoFlushBus = true
            playBin.bus.connect(
                Bus.EOS {
                    playBin.pause()
                    playBin.seek(0L, TimeUnit.MILLISECONDS)
                    playBin.play()
                },
            )
            playBin.setURI(URI(url))
            playBin.play()
        }
        onDispose {
            gsVideoComponent = null
        }
    }

    var sizePx by remember {
        mutableStateOf(0 to 0)
    }

    Box(
        modifier
            .graphicsLayer { clip = true }
            .onGloballyPositioned {
                sizePx =
                    it.size.width to it.size.height
            },
    ) {
        if (gsVideoComponent != null) {
            SwingPanel(
                factory = {
                    JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
                        background = java.awt.Color.BLACK
                        add(gsVideoComponent)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.Center),
                background = Color.Transparent,
            )
        }
    }
}

@Composable
fun MediaPlayerViewWithSubtitleJvm(
    modifier: Modifier,
    playerName: String,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics?,
    translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
) {
    val player: GstreamerPlayerAdapter = koinInject<GstreamerPlayerAdapter>()

    val state by mediaPlayerHandler.nowPlayingState.collectAsState()

    val scope = rememberCoroutineScope()
    var sizePx by remember {
        mutableStateOf(0 to 0)
    }
    var gsVideoComponent by remember {
        mutableStateOf<GstVideoComponent?>(null)
    }

    val showArtwork by derivedStateOf {
        gsVideoComponent == null
    }

    var artworkUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    var currentTranslatedLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.songEntity }.collect {
            withContext(Dispatchers.Swing) {
                gsVideoComponent = player.getCurrentPlayer()?.videoComponent
                Logger.d("MediaPlayerViewWithSubtitleJvm", "Video component: $gsVideoComponent")
            }
        }
    }

    LaunchedEffect(key1 = timelineState) {
        val lines = lyricsData?.lines ?: return@LaunchedEffect
        val translatedLines = translatedLyricsData?.lines
        if (timelineState.current > 0L) {
            lines.indices.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                 
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                         
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            translatedLines?.indices?.forEach { i ->
                val sentence = translatedLines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                 
                val endTimeMs =
                    if (i < translatedLines.size - 1) {
                        translatedLines[i + 1].startTimeMs.toLong()
                    } else {
                         
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    currentTranslatedLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                (
                    timelineState.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                        ).toLong()
                    )
                )
            ) {
                currentLineIndex = -1
                currentTranslatedLineIndex = -1
            }
        } else {
            currentLineIndex = -1
            currentTranslatedLineIndex = -1
        }
    }

    Box(
        modifier =
            modifier
                .graphicsLayer { clip = true }
                .onGloballyPositioned {
                    val width = it.size.width
                    val height = it.size.height
                    sizePx = width to height
                },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(showArtwork) {
            if (it) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(
                                artworkUri,
                            ).diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(
                                artworkUri,
                            ).crossfade(550)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .align(Alignment.Center),
                )
            } else {
                Box(
                    Modifier
                        .graphicsLayer { clip = true }
                        .wrapContentSize()
                        .align(Alignment.Center),
                ) {
                    if (gsVideoComponent != null && sizePx.first > 0 && sizePx.second > 0) {
                        SwingPanel(
                            factory = {
                                JPanel().apply {
                                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                                    background = java.awt.Color(255, 255, 255, 0)
                                    preferredSize = Dimension(sizePx.first, sizePx.second)
                                    add(gsVideoComponent)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .wrapContentWidth(),
                            background = Color.Transparent,
                        )
                    }
                }
            }
        }
        if (lyricsData != null && shouldShowSubtitle) {
            Crossfade(
                currentLineIndex != -1,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize(),
            ) {
                val lines = lyricsData.lines ?: return@Crossfade
                if (it) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = if (shouldScaleDownSubtitle) 10.dp else 40.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth(0.7f)) {
                            Column(
                                Modifier.align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = lines.getOrNull(currentLineIndex)?.words ?: return@Crossfade,
                                    style =
                                        mainTextStyle
                                            .let {
                                                if (shouldScaleDownSubtitle) {
                                                    it.copy(fontSize = it.fontSize * 0.8f)
                                                } else {
                                                    it
                                                }
                                            },
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .wrapContentWidth(),
                                )
                                Crossfade(translatedLyricsData?.lines != null, label = "") { translate ->
                                    val translateLines = translatedLyricsData?.lines ?: return@Crossfade
                                    if (translate) {
                                        Text(
                                            text = translateLines.getOrNull(currentTranslatedLineIndex)?.words ?: return@Crossfade,
                                            style =
                                                translatedTextStyle.let {
                                                    if (shouldScaleDownSubtitle) {
                                                        it.copy(fontSize = it.fontSize * 0.8f)
                                                    } else {
                                                        it
                                                    }
                                                },
                                            color = Color.Yellow,
                                            textAlign = TextAlign.Center,
                                            modifier =
                                                Modifier
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .wrapContentWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

