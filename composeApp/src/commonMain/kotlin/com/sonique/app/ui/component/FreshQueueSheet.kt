package com.sonique.app.ui.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    contentColor: Color? = null,
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lazyListState = rememberLazyListState()

    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()

    val queue = remember(queueData?.data?.listTracks) {
        queueData?.data?.listTracks ?: emptyList()
    }

    val currentSongIndex = remember(queueData) {
        musicServiceHandler.currentOrderIndex().coerceAtLeast(0)
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

    val sheetBg = (backgroundColor ?: MaterialTheme.colorScheme.surfaceContainerHigh).copy(alpha = 1f)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sheetBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(sheetBg)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            scope.launch {
                                dataStoreManager.setEndlessQueue(!endlessQueueEnable)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (endlessQueueEnable) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Endless Queue",
                            tint = if (endlessQueueEnable) finalContent else finalContent.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${queue.size} songs",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = finalContent.copy(alpha = 0.75f)
                        )
                        if (totalDurationText.isNotEmpty()) {
                            Text(
                                text = totalDurationText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
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
                    modifier = Modifier.fillMaxSize()
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
                            // Highlighted card for currently playing track
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        musicServiceHandler.playMediaItemInMediaSource(actualIndex)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.16f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Artwork with Play triangle overlay
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(finalContent.copy(alpha = 0.12f))
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
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Currently Playing",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Title & Artist
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title ?: "",
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

                                    // Three dots menu
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
                            // Flat row for upcoming tracks
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        musicServiceHandler.playMediaItemInMediaSource(actualIndex)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Artwork without play overlay
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(finalContent.copy(alpha = 0.12f))
                                ) {
                                    AsyncImage(
                                        model = track.thumbnails?.lastOrNull()?.url ?: "",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Title & Artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title ?: "",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Normal,
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

                                // Three dots menu
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
                    // Shuffle button
                    val isShuffle = controllerState.isShuffle
                    IconButton(
                        onClick = { sharedViewModel.onUIEvent(UIEvent.Shuffle) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isShuffle) Res.drawable.shuffle_on else Res.drawable.shuffle),
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else finalContent.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Collapse Chevron
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

                    // Repeat button
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
                            tint = if (isRepeat) MaterialTheme.colorScheme.primary else finalContent.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
