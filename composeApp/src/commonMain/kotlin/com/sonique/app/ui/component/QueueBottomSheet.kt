package com.sonique.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sonique.app.extension.cleanSongTitle
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.typo
import com.sonique.domain.utils.connectArtists
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.logger.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_delete_24
import sonique.composeapp.generated.resources.baseline_keyboard_arrow_down_24
import sonique.composeapp.generated.resources.baseline_keyboard_double_arrow_down_24
import sonique.composeapp.generated.resources.baseline_keyboard_double_arrow_up_24
import sonique.composeapp.generated.resources.delete
import sonique.composeapp.generated.resources.endless_queue
import sonique.composeapp.generated.resources.move_down
import sonique.composeapp.generated.resources.move_up
import sonique.composeapp.generated.resources.now_playing
import sonique.composeapp.generated.resources.now_playing_upper
import sonique.composeapp.generated.resources.queue

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val lazyListState = rememberLazyListState()
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val queue by remember {
        derivedStateOf {
            queueData?.data?.listTracks ?: emptyList()
        }
    }
    val queueItems = remember(queue) {
        val counts = mutableMapOf<String, Int>()
        queue.map { track ->
            val count = counts.getOrElse(track.videoId) { 0 }
            counts[track.videoId] = count + 1
            QueueDisplayItem(
                stableKey = "${track.videoId}_#$count",
                originalTrack = track,
            )
        }
    }
    var localQueueItems by remember(queueItems) {
        mutableStateOf(queueItems)
    }
    val dragDropState =
        rememberDragDropState(
            lazyListState = lazyListState,
            minDragIndex = 0,
            onMove = { from, to ->
                val currentList = localQueueItems.toMutableList()
                if (from in currentList.indices && to in currentList.indices && from != to) {
                    val moved = currentList.removeAt(from)
                    currentList.add(to, moved)
                    localQueueItems = currentList
                }
            },
            onDrop = { from, to ->
                coroutineScope.launch {
                    musicServiceHandler.swap(from, to)
                }
            },
        )
    var shouldShowQueueItemBottomSheet by rememberSaveable { mutableStateOf(false) }
    var clickMoreIndex by rememberSaveable { mutableIntStateOf(0) }
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
    val loadMoreState by remember {
        derivedStateOf {
            queueData?.queueState ?: QueueData.StateSource.STATE_CREATED
        }
    }
    val endlessQueueEnable by dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }.collectAsState(false)

    val shouldLoadMore =
        remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val lastVisibleItem =
                    layoutInfo.visibleItemsInfo.lastOrNull()
                        ?: return@derivedStateOf true

                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3 && layoutInfo.totalItemsCount > 0
            }
        }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect {
                if (it && loadMoreState == QueueData.StateSource.STATE_INITIALIZED) musicServiceHandler.loadMore()
            }
    }

    LaunchedEffect(queue) {
        Logger.w("QueueBottomSheet", "queue: $queue")
    }

    DisposableEffect(Unit) {
        val currentSongIndex = musicServiceHandler.currentOrderIndex().takeIf { i -> i > -1 } ?: 0
        Logger.d("QueueBottomSheet", "currentSongIndex: $currentSongIndex")
        coroutineScope.launch {
            lazyListState.requestScrollToItem(currentSongIndex)
        }
        onDispose { }
    }

    val showQueueItemBottomSheet: (Int) -> Unit = { index ->
        clickMoreIndex = index
        shouldShowQueueItemBottomSheet = true
    }

    if (shouldShowQueueItemBottomSheet) {
        QueueItemBottomSheet(
            onDismiss = { shouldShowQueueItemBottomSheet = false },
            index = clickMoreIndex,
            musicServiceHandler = musicServiceHandler,
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = md_theme_dark_background,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = md_theme_dark_background.copy(alpha = .5f),
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors().copy(containerColor = md_theme_dark_background),
        ) {
            Column(
                modifier =
                    Modifier.padding(
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
                                color = Color.White,
                            )
                            Text(
                                text = screenDataState.playlistName,
                                style = typo().labelMedium,
                                color = Color.White,
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
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.now_playing),
                    style = typo().titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                SongFullWidthItems(
                    songEntity = songEntity,
                    isPlaying = false,
                    onAddToQueue = null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.queue),
                        style = typo().titleMedium,
                        modifier =
                            Modifier
                                .padding(horizontal = 20.dp)
                                .weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.endless_queue),
                            style = typo().bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Switch(
                            checked = endlessQueueEnable,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    dataStoreManager.setEndlessQueue(it)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    horizontalAlignment = Alignment.Start,
                    state = lazyListState,
                    modifier = Modifier.dragDropList(dragDropState),
                ) {
                    itemsIndexed(
                        localQueueItems,
                        key = { _, item -> item.stableKey },
                    ) { index, item ->
                        val track = item.originalTrack
                        DraggableItem(
                            dragDropState = dragDropState,
                            key = item.stableKey,
                            modifier = Modifier.fillMaxWidth(),
                        ) { isDragging ->
                            SongFullWidthItems(
                                track = track,
                                isPlaying = track.videoId == songEntity?.videoId,
                                modifier = Modifier.fillMaxWidth(),
                                onClickListener = { videoId ->
                                    if (videoId == track.videoId) {
                                        musicServiceHandler.playMediaItemInMediaSource(index)
                                    }
                                },
                                onMoreClickListener = {
                                    showQueueItemBottomSheet(index)
                                },
                                onAddToQueue = {
                                    sharedViewModel.addListToQueue(
                                        arrayListOf(track),
                                    )
                                },
                            )
                        }
                    }
                    item {
                        if (loadMoreState == QueueData.StateSource.STATE_INITIALIZING) {
                            CenterLoadingBox(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                            )
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun QueueItemBottomSheet(
    onDismiss: () -> Unit,
    index: Int,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideModalBottomSheet: () -> Unit = {
        coroutineScope.launch {
            modelBottomSheetState.hide()
            onDismiss()
        }
    }

    val track = remember(musicServiceHandler.queueData, index) {
        musicServiceHandler.queueData.value?.data?.listTracks?.getOrNull(index)
    }

    val canMoveUp = index > 0 && index < (musicServiceHandler.queueData.value?.data?.listTracks?.size ?: 0)
    val canMoveDown = index >= 0 && index < (musicServiceHandler.queueData.value?.data?.listTracks?.size ?: 0) - 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF18171C)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                // Track Info Header
                if (track != null) {
                    val artistNames = remember(track.artists) {
                        track.artists?.mapNotNull { it.name }?.connectArtists() ?: ""
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            AsyncImage(
                                model = track.thumbnails?.lastOrNull()?.url ?: "",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (track.title ?: "").cleanSongTitle(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (artistNames.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = artistNames,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Actions List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (canMoveUp) {
                        QueueActionRow(
                            iconRes = Res.drawable.baseline_keyboard_double_arrow_up_24,
                            iconTint = Color.White,
                            iconBgColor = Color.White.copy(alpha = 0.08f),
                            title = stringResource(Res.string.move_up),
                            titleColor = Color.White,
                            description = "Move this song earlier in the queue",
                            onClick = {
                                hideModalBottomSheet()
                                coroutineScope.launch {
                                    musicServiceHandler.moveItemUp(index)
                                }
                            }
                        )
                    }

                    if (canMoveDown) {
                        QueueActionRow(
                            iconRes = Res.drawable.baseline_keyboard_double_arrow_down_24,
                            iconTint = Color.White,
                            iconBgColor = Color.White.copy(alpha = 0.08f),
                            title = stringResource(Res.string.move_down),
                            titleColor = Color.White,
                            description = "Move this song later in the queue",
                            onClick = {
                                hideModalBottomSheet()
                                coroutineScope.launch {
                                    musicServiceHandler.moveItemDown(index)
                                }
                            }
                        )
                    }

                    QueueActionRow(
                        iconRes = Res.drawable.baseline_delete_24,
                        iconTint = Color(0xFFFF453A),
                        iconBgColor = Color(0xFFFF3B30).copy(alpha = 0.14f),
                        title = stringResource(Res.string.delete),
                        titleColor = Color(0xFFFF453A),
                        description = "Remove this song from the current queue",
                        onClick = {
                            hideModalBottomSheet()
                            musicServiceHandler.removeMediaItem(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueActionRow(
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    iconTint: Color,
    iconBgColor: Color,
    title: String,
    titleColor: Color,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = titleColor,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
