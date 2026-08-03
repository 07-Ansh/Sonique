package com.sonique.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kmpalette.rememberPaletteState
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.utils.connectArtists
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_close_24
import sonique.composeapp.generated.resources.more_horiz

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class, ExperimentalFoundationApi::class)
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
    val paletteState = rememberPaletteState()

    LaunchedEffect(screenDataState.bitmap) {
        screenDataState.bitmap?.let { bitmap ->
            paletteState.generate(bitmap)
        }
    }

    val extractedArtworkColor = paletteState.palette?.getColorFromPalette()

    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()

    val queue by remember {
        derivedStateOf {
            queueData?.data?.listTracks ?: emptyList()
        }
    }

    val currentSongIndex by remember {
        derivedStateOf {
            musicServiceHandler.currentOrderIndex().coerceAtLeast(0)
        }
    }

    // Filter queue to start from current playing song onwards (no past songs displayed)
    val visibleQueue by remember(queue, currentSongIndex) {
        derivedStateOf {
            if (queue.isEmpty()) emptyList()
            else queue.drop(currentSongIndex.coerceAtMost(queue.size))
        }
    }

    val dragDropState = rememberDragDropState(lazyListState) { from, to ->
        val actualFrom = currentSongIndex + from
        val actualTo = currentSongIndex + to
        scope.launch {
            musicServiceHandler.swap(actualFrom, actualTo)
        }
    }

    val loadMoreState by remember {
        derivedStateOf {
            queueData?.queueState ?: QueueData.StateSource.STATE_CREATED
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 3 && layoutInfo.totalItemsCount > 0
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

    val defaultSheetBg = extractedArtworkColor?.copy(alpha = 0.92f) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val finalBg = backgroundColor ?: defaultSheetBg
    val finalContent = contentColor ?: Color.White

    // Dynamic translucent card tinting for Ambience Mode
    val cardBg = Color.White.copy(alpha = 0.15f)

    val animatedBg by animateColorAsState(
        targetValue = finalBg,
        animationSpec = tween(300),
        label = "sheetBackground"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = animatedBg,
        contentColor = finalContent,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with Material You dynamic color
            Surface(
                color = animatedBg,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAYING QUEUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = screenDataState.playlistName.ifBlank { "Current Queue" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = finalContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${visibleQueue.size} songs upcoming",
                            style = MaterialTheme.typography.bodySmall,
                            color = finalContent.copy(alpha = 0.75f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_close_24),
                            contentDescription = "Close queue",
                            tint = finalContent
                        )
                    }
                }
            }

            HorizontalDivider(color = finalContent.copy(alpha = 0.15f))

            // Queue List (starts at currently playing song)
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.systemBars.asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = visibleQueue,
                    key = { index, track -> "${track.videoId}_${currentSongIndex + index}" }
                ) { index, track ->
                    val actualIndex = currentSongIndex + index
                    val isCurrentTrack = index == 0

                    val itemCardBg = if (isCurrentTrack) {
                        Color.White.copy(alpha = 0.28f)
                    } else {
                        cardBg
                    }

                    val titleColor = finalContent

                    val artistColor = finalContent.copy(alpha = 0.75f)

                    val artistNames = remember(track.artists) {
                        track.artists?.mapNotNull { it.name }?.connectArtists() ?: ""
                    }

                    DraggableItem(
                        dragDropState = dragDropState,
                        index = index,
                        modifier = Modifier
                    ) { isDragging ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable {
                                    musicServiceHandler.playMediaItemInMediaSource(actualIndex)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = itemCardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 0.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Index or Active indicator
                                        Box(
                                            modifier = Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrentTrack) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = artistColor
                                                )
                                            }
                                        }

                                        // Artwork
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
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
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = track.title ?: "",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = artistNames,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = artistColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { selectedItemForMenu = actualIndex },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.more_horiz),
                                                contentDescription = "Track options",
                                                tint = artistColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
