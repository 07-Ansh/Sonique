package com.sonique.app.ui.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.typo
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
    val dragDropState =
        rememberDragDropState(lazyListState) { from, to ->
            coroutineScope.launch {
                musicServiceHandler.swap(from, to)
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    var shouldShowQueueItemBottomSheet by rememberSaveable { mutableStateOf(false) }
    var clickMoreIndex by rememberSaveable { mutableIntStateOf(0) }
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val queue by remember {
        derivedStateOf {
            queueData?.data?.listTracks ?: emptyList()
        }
    }
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
                    modifier =
                        Modifier
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, offset ->
                                        Logger.d("QueueBottomSheet", "onDrag $offset")
                                        change.consume()
                                        dragDropState.onDrag(offset = offset)

                                        if (overscrollJob?.isActive == true) {
                                            return@detectDragGesturesAfterLongPress
                                        }

                                        dragDropState
                                            .checkForOverScroll()
                                            .takeIf { it != 0f }
                                            ?.let {
                                                overscrollJob =
                                                    coroutineScope.launch {
                                                        dragDropState.state.animateScrollBy(
                                                            it * 1.3f,
                                                            tween(easing = FastOutLinearInEasing),
                                                        )
                                                    }
                                            }
                                            ?: run { overscrollJob?.cancel() }
                                    },
                                    onDragStart = { offset ->
                                        Logger.d("QueueBottomSheet", "onDragStart $offset")
                                        dragDropState.onDragStart(offset)
                                    },
                                    onDragEnd = {
                                        Logger.d("QueueBottomSheet", "onDragEnd")
                                        dragDropState.onDragInterrupted(true)
                                        overscrollJob?.cancel()
                                    },
                                    onDragCancel = {
                                        Logger.d("QueueBottomSheet", "onDragCancel")
                                        dragDropState.onDragInterrupted()
                                        overscrollJob?.cancel()
                                    },
                                )
                            },
                ) {
                    itemsIndexed(
                        queue,
                        key = { i, t -> i.toString() + t.videoId },
                    ) { index, track ->
                        if (index != -1) {
                            DraggableItem(
                                dragDropState = dragDropState,
                                index = index,
                                modifier = Modifier,
                            ) { _ ->
                                SongFullWidthItems(
                                    track = track,
                                    isPlaying = track.videoId == songEntity?.videoId,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
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

private enum class QueueItemAction {
    UP,
    DOWN,
    DELETE,
}

@Composable
@ExperimentalMaterial3Api
fun QueueItemBottomSheet(
    onDismiss: () -> Unit,
    index: Int,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    val listAction =
        listOf(
            QueueItemAction.UP,
            QueueItemAction.DOWN,
            QueueItemAction.DELETE,
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                LazyColumn {
                    val canMoveUp =
                        index > 0 &&
                            index < (
                                musicServiceHandler.queueData.value
                                    ?.data
                                    ?.listTracks
                                    ?.size ?: 0
                            )
                    val canMoveDown =
                        index >= 0 &&
                            index < (
                                musicServiceHandler.queueData.value
                                    ?.data
                                    ?.listTracks
                                    ?.size ?: 0
                            ) - 1
                    items(listAction) { action ->
                        val disable =
                            when (action) {
                                QueueItemAction.UP -> !canMoveUp
                                QueueItemAction.DOWN -> !canMoveDown
                                QueueItemAction.DELETE -> false
                            }
                        if (disable) return@items
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hideModalBottomSheet()
                                        when (action) {
                                            QueueItemAction.UP -> {
                                                coroutineScope.launch {
                                                    musicServiceHandler.moveItemUp(index)
                                                }
                                            }

                                            QueueItemAction.DOWN -> {
                                                coroutineScope.launch {
                                                    musicServiceHandler.moveItemDown(index)
                                                }
                                            }

                                            QueueItemAction.DELETE -> {
                                                musicServiceHandler.removeMediaItem(index)
                                            }
                                        }
                                    },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(20.dp)
                                        .align(Alignment.CenterStart),
                            ) {
                                when (action) {
                                    QueueItemAction.UP -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_keyboard_double_arrow_up_24,
                                                ),
                                            contentDescription = "Move up",
                                        )
                                    }

                                    QueueItemAction.DOWN -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_keyboard_double_arrow_down_24,
                                                ),
                                            contentDescription = "Move down",
                                        )
                                    }

                                    QueueItemAction.DELETE -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_delete_24,
                                                ),
                                            contentDescription = "Delete",
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text =
                                        stringResource(
                                            when (action) {
                                                QueueItemAction.UP -> Res.string.move_up
                                                QueueItemAction.DOWN -> Res.string.move_down
                                                QueueItemAction.DELETE -> Res.string.delete
                                            },
                                        ),
                                    style = typo().labelSmall,
                                )
                            }
                        }
                    }
                    item {
                        EndOfModalBottomSheet()
                    }
                }
            }
        }
    }
}
