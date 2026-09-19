package com.sonique.app.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.compositeOver
import com.sonique.app.extension.toHsl
import com.sonique.app.extension.cleanSongTitle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UIEvent
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.mediaservice.handler.RepeatState
import com.sonique.domain.utils.connectArtists
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_more_vert_24
import sonique.composeapp.generated.resources.repeat
import sonique.composeapp.generated.resources.repeat_on
import sonique.composeapp.generated.resources.repeat_one_on
import sonique.composeapp.generated.resources.shuffle
import sonique.composeapp.generated.resources.shuffle_on

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshQueueSheet(
    onDismiss: () -> Unit,
    backgroundColor: Color? = null,
    accentColor: Color? = null,
    contentColor: Color? = null,
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tint = accentColor ?: MaterialTheme.colorScheme.primary
    val isMonochrome = tint == Color.White || tint == Color.Black || tint.toHsl()[1] < 0.12f
    val darkBase = Color(0xFF141316)
    val defaultTintedBg = if (isMonochrome) darkBase else tint.copy(alpha = 0.16f).compositeOver(darkBase).copy(alpha = 1f)
    val sheetBg = (backgroundColor ?: defaultTintedBg).copy(alpha = 1f)
    val finalContent = contentColor ?: Color.White

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = finalContent,
        dragHandle = null,
        shape = RectangleShape,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.fillMaxHeight(),
    ) {
        FreshQueueContent(
            onDismiss = onDismiss,
            backgroundColor = backgroundColor,
            accentColor = accentColor,
            contentColor = contentColor,
            sharedViewModel = sharedViewModel,
            musicServiceHandler = musicServiceHandler,
            dataStoreManager = dataStoreManager,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshQueueContent(
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
    onDismiss: () -> Unit,
    backgroundColor: Color? = null,
    accentColor: Color? = null,
    contentColor: Color? = null,
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()

    var localPendingVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(nowPlayingState?.mediaItem?.mediaId) {
        val playingId = nowPlayingState?.mediaItem?.mediaId
        if (playingId != null && playingId == localPendingVideoId) {
            localPendingVideoId = null
        }
    }

    val queue = remember(queueData?.data?.listTracks) {
        queueData?.data?.listTracks ?: emptyList()
    }

    val currentSongId = localPendingVideoId
        ?: nowPlayingState?.mediaItem?.mediaId?.takeIf { it.isNotBlank() }
        ?: nowPlayingState?.track?.videoId?.takeIf { it.isNotBlank() }
        ?: screenDataState.songInfoData?.videoId?.takeIf { it.isNotBlank() }
    val currentSongTitle = screenDataState.nowPlayingTitle

    val currentSongIndex = remember(queue, currentSongId, currentSongTitle, controllerState) {
        val indexFromId = if (!currentSongId.isNullOrBlank()) {
            queue.indexOfFirst { it.videoId == currentSongId }.takeIf { it >= 0 }
        } else null
        val indexFromTitle = if (indexFromId == null && currentSongTitle.isNotBlank()) {
            queue.indexOfFirst { it.title.equals(currentSongTitle, ignoreCase = true) }.takeIf { it >= 0 }
        } else null
        indexFromId ?: indexFromTitle ?: musicServiceHandler.currentOrderIndex().coerceAtLeast(0)
    }

    val visibleQueue = remember(queue, currentSongIndex) {
        if (queue.isEmpty()) emptyList()
        else queue.drop(currentSongIndex.coerceAtMost(queue.size))
    }

    // Precalculate total duration only when the queue list instance changes
    val totalDurationText = remember(queue) {
        var totalSec = 0L
        for (track in queue) {
            val sec = track.durationSeconds ?: 0
            if (sec > 0) {
                totalSec += sec
            } else {
                val dur = track.duration
                if (!dur.isNullOrBlank()) {
                    val parts = dur.split(":")
                    if (parts.size == 2) {
                        totalSec += (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
                    } else if (parts.size == 3) {
                        totalSec += (parts[0].toLongOrNull() ?: 0) * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)
                    }
                }
            }
        }
        if (totalSec <= 0L) ""
        else {
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            val seconds = totalSec % 60
            if (hours > 0) {
                "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
            } else {
                "${minutes}:${seconds.toString().padStart(2, '0')}"
            }
        }
    }

    val endlessQueueEnable by dataStoreManager.endlessQueue
        .map { it == DataStoreManager.TRUE }
        .collectAsState(false)

    val loadMoreState = queueData?.queueState ?: QueueData.StateSource.STATE_CREATED

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 2 && layoutInfo.totalItemsCount > 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect {
                if (it && loadMoreState == QueueData.StateSource.STATE_INITIALIZED) {
                    musicServiceHandler.loadMore()
                }
            }
    }

    var selectedItemForMenu by rememberSaveable { mutableIntStateOf(-1) }
    if (selectedItemForMenu >= 0) {
        QueueItemBottomSheet(
            onDismiss = { selectedItemForMenu = -1 },
            index = selectedItemForMenu,
            musicServiceHandler = musicServiceHandler,
        )
    }

    val tint = accentColor ?: MaterialTheme.colorScheme.primary
    val isMonochrome = tint == Color.White || tint == Color.Black || tint.toHsl()[1] < 0.12f
    val darkBase = Color(0xFF141316)
    val defaultTintedBg = if (isMonochrome) darkBase else tint.copy(alpha = 0.16f).compositeOver(darkBase).copy(alpha = 1f)
    val sheetBg = (backgroundColor ?: defaultTintedBg).copy(alpha = 1f)
    val finalContent = contentColor ?: Color.White

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(sheetBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val headerVelocityTracker = remember { VelocityTracker() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 2.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { headerVelocityTracker.resetTracking() },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                headerVelocityTracker.addPointerInputChange(change)
                                if (dragAmount > 20f) {
                                    onDismiss()
                                }
                            },
                            onDragEnd = {
                                val vy = headerVelocityTracker.calculateVelocity().y
                                if (vy > 150f) {
                                    onDismiss()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isMonochrome) Color.White.copy(alpha = 0.35f) else tint.copy(alpha = 0.45f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 18.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close Queue",
                        tint = finalContent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = screenDataState.playlistName.ifBlank {
                        queue.getOrNull(currentSongIndex)?.title?.let { "$it Mix" } ?: "Current Queue"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    color = finalContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            dataStoreManager.setEndlessQueue(!endlessQueueEnable)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (endlessQueueEnable) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = "Endless Queue",
                        tint = if (endlessQueueEnable) finalContent else finalContent.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${queue.size} songs",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = finalContent.copy(alpha = 0.75f)
                    )
                    if (totalDurationText.isNotEmpty()) {
                        Text(
                            text = totalDurationText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = finalContent.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 88.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
            ) {
                    itemsIndexed(
                        items = visibleQueue,
                        key = { index, track -> "${track.videoId}_${currentSongIndex + index}" },
                        contentType = { index, _ -> if (index == 0) "active_track" else "upcoming_track" }
                    ) { index, track ->
                        val actualIndex = currentSongIndex + index
                        val isCurrentTrack = index == 0
                        val artistNames = remember(track.artists) {
                            track.artists?.mapNotNull { it.name }?.connectArtists() ?: ""
                        }

                        if (isCurrentTrack) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        localPendingVideoId = track.videoId
                                        musicServiceHandler.playMediaItemInMediaSource(actualIndex)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMonochrome) {
                                        Color.White.copy(alpha = 0.10f).compositeOver(sheetBg).copy(alpha = 1f)
                                    } else {
                                        tint.copy(alpha = 0.16f).compositeOver(sheetBg).copy(alpha = 1f)
                                    }
                                ),
                                border = null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tint.copy(alpha = 0.18f))
                                    ) {
                                        AsyncImage(
                                            model = track.thumbnails?.lastOrNull()?.url ?: "",
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PlayingEqualizerBars(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = (track.title ?: "").cleanSongTitle(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = finalContent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = artistNames,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp
                                            ),
                                            color = finalContent.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedItemForMenu = actualIndex },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.baseline_more_vert_24),
                                            contentDescription = "Track options",
                                            tint = finalContent.copy(alpha = 0.75f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        localPendingVideoId = track.videoId
                                        musicServiceHandler.playMediaItemInMediaSource(actualIndex)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMonochrome) Color.White.copy(alpha = 0.08f) else tint.copy(alpha = 0.14f))
                                ) {
                                    AsyncImage(
                                        model = track.thumbnails?.lastOrNull()?.url ?: "",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = (track.title ?: "").cleanSongTitle(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 15.sp
                                        ),
                                        color = finalContent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitleText = remember(artistNames, track.duration) {
                                        val dur = track.duration
                                        if (!dur.isNullOrBlank()) {
                                            if (artistNames.isNotBlank()) "$artistNames • $dur" else dur
                                        } else {
                                            artistNames
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp
                                        ),
                                        color = finalContent.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { selectedItemForMenu = actualIndex },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.baseline_more_vert_24),
                                        contentDescription = "Track options",
                                        tint = finalContent.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                sheetBg.copy(alpha = 0.85f),
                                sheetBg.copy(alpha = 0.98f),
                                sheetBg
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isShuffle = controllerState.isShuffle
                    IconButton(
                        onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isShuffle) Res.drawable.shuffle_on else Res.drawable.shuffle),
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) (if (isMonochrome) Color.White else tint) else finalContent.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Close queue",
                            tint = finalContent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    val isRepeat = controllerState.repeatState != RepeatState.None
                    val isRepeatOne = controllerState.repeatState == RepeatState.One
                    IconButton(
                        onClick = { sharedViewModel.onUIEvent(UIEvent.Repeat) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isRepeatOne) Res.drawable.repeat_one_on
                                else if (isRepeat) Res.drawable.repeat_on
                                else Res.drawable.repeat
                            ),
                            contentDescription = "Repeat",
                            tint = if (isRepeat) (if (isMonochrome) Color.White else tint) else finalContent.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun PlayingEqualizerBars(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "queue_equalizer")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar1)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar2)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar3)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color)
        )
    }
}
