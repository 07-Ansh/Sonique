package com.sonique.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import com.sonique.app.expect.ui.BackHandler
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.app.expect.copyToClipboard
import com.sonique.app.expect.rememberVolumeController
import com.sonique.app.expect.ui.openEqResult
import com.sonique.app.extension.toHsl
import com.sonique.app.ui.navigation.destination.home.ListenTogetherDestination
import com.sonique.app.viewModel.NowPlayingBottomSheetUIEvent
import com.sonique.app.viewModel.NowPlayingBottomSheetViewModel
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.model.searchResult.songs.Artist
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.utils.connectArtists
import com.sonique.domain.utils.toListName
import kotlinx.coroutines.launch
import com.sonique.app.ui.component.SoniqueToastManager
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_downloaded
import sonique.composeapp.generated.resources.baseline_downloading_white
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.ic_artist_note
import sonique.composeapp.generated.resources.ic_library_add
import sonique.composeapp.generated.resources.metro_equalizer
import sonique.composeapp.generated.resources.metro_info
import sonique.composeapp.generated.resources.metro_link
import sonique.composeapp.generated.resources.metro_radio
import sonique.composeapp.generated.resources.metro_volume_down
import sonique.composeapp.generated.resources.metro_volume_mute
import sonique.composeapp.generated.resources.metro_volume_up
import sonique.composeapp.generated.resources.metro_lyrics
import sonique.composeapp.generated.resources.metro_palette
import sonique.composeapp.generated.resources.metro_tune
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.manager.DataStoreManager

/**
 * More options bottom sheet displaying quick actions, audio options, and playback settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMoreOptionsTwoStageSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel,
    onNavigateToOtherScreen: () -> Unit = {},
    backgroundColor: Color? = null,
    accentColor: Color? = null,
    contentColor: Color? = null,
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    onLyricsClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val ambienceModePref by dataStoreManager.ambienceMode.collectAsStateWithLifecycle(initialValue = DataStoreManager.TRUE)
    val isAmbienceEnabled = ambienceModePref == DataStoreManager.TRUE

    val crossfadeEnabledPref by dataStoreManager.crossfadeEnabled.collectAsStateWithLifecycle(initialValue = DataStoreManager.FALSE)
    val isCrossfadeEnabled = crossfadeEnabledPref == DataStoreManager.TRUE

    val lyricsProviderPref by dataStoreManager.lyricsProvider.collectAsStateWithLifecycle(initialValue = DataStoreManager.LRCLIB)
    val lyricsAutoFallbackPref by dataStoreManager.lyricsAutoFallback.collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(song) {
        viewModel.setSongEntity(song)
    }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }
    var showLyricsOptionsDialog by remember { mutableStateOf(false) }

    val audioSessionId = remember {
        runCatching { mediaPlayerHandler.player.audioSessionId }.getOrDefault(0)
    }
    val eqLauncher = openEqResult(audioSessionId)

    if (addToAPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { addToAPlaylist = false },
            onClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id))
            },
            onYTPlaylistClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(it.browseId))
            },
            videoId = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" },
        )
    }

    if (showArtistSheet) {
        val artistsList = uiState.songUIState.listArtists.ifEmpty {
            song?.artistName?.map { Artist(name = it, id = null) } ?: emptyList()
        }
        ArtistModalBottomSheet(
            isBottomSheetVisible = true,
            artists = artistsList,
            navController = navController,
            onNavigateToOtherScreen = onNavigateToOtherScreen,
            onDismiss = { showArtistSheet = false },
        )
    }

    if (showDetailsDialog) {
        val titleText = uiState.songUIState.title.ifBlank { song?.title ?: "Unknown" }
        val artistText = uiState.songUIState.listArtists.toListName().connectArtists()
            .ifBlank { song?.artistName?.connectArtists() ?: "Unknown" }
        val albumText = uiState.songUIState.album?.name ?: song?.albumName ?: ""
        val videoIdText = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" }
        val durationText = song?.duration ?: ""

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SongDetailRow(label = "Title", value = titleText)
                    SongDetailRow(label = "Artist", value = artistText)
                    if (albumText.isNotBlank()) {
                        SongDetailRow(label = "Album", value = albumText)
                    }
                    if (durationText.isNotBlank()) {
                        SongDetailRow(label = "Duration", value = durationText)
                    }
                    if (videoIdText.isNotBlank()) {
                        SongDetailRow(label = "Video ID", value = videoIdText)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showCancelDownloadDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showCancelDownloadDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDownloadDialog = false
                    viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDownloadDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = { Text("Download") },
            text = {
                val isDownloaded = uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED
                Text(if (isDownloaded) "Do you want to remove this download?" else "Do you want to cancel the download?")
            }
        )
    }

    if (showLyricsOptionsDialog) {
        LyricsOptionsDialog(
            onDismissRequest = { showLyricsOptionsDialog = false },
            dataStoreManager = dataStoreManager,
            sharedViewModel = sharedViewModel,
            onViewLyrics = onLyricsClick?.let {
                {
                    onDismiss()
                    it.invoke()
                }
            },
        )
    }

    val tint = accentColor ?: MaterialTheme.colorScheme.primary
    val isMonochrome = tint == Color.White || tint == Color.Black || tint.toHsl()[1] < 0.12f
    val darkBase = Color(0xFF141316)
    val defaultTintedBg = if (isMonochrome) darkBase else tint.copy(alpha = 0.16f).compositeOver(darkBase).copy(alpha = 1f)
    val sheetBg = (backgroundColor ?: defaultTintedBg).copy(alpha = 1f)
    val finalContent = contentColor ?: Color.White
    val finalAccent = tint
    // Solid tinted container for cards — strictly opaque, no transparency
    val cardBg = if (isMonochrome) {
        Color.White.copy(alpha = 0.08f).compositeOver(sheetBg).copy(alpha = 1f)
    } else {
        tint.copy(alpha = 0.12f).compositeOver(sheetBg).copy(alpha = 1f)
    }
    val cardBorder: Color? = null
    val cardContent = Color.White
    val cardSecondaryContent = Color.White.copy(alpha = 0.78f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val sheetHeightPx = screenHeightPx
        val sheetHeightDp = with(density) { sheetHeightPx.toDp() }

        val halfHeightPx = (screenHeightPx * 0.56f).coerceAtLeast(1f)
        val halfOffsetPx = (sheetHeightPx - halfHeightPx).coerceAtLeast(0f)
        val hiddenOffsetPx = sheetHeightPx + with(density) { 60.dp.toPx() }

        val offsetAnimatable = remember { Animatable(3000f) }
        val sheetSpringSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        // Track where the current drag gesture originated (Fullscreen vs Half)
        var gestureStartOffset by remember { mutableFloatStateOf(halfOffsetPx) }

        // When `visible` toggles:
        LaunchedEffect(visible, sheetHeightPx) {
            if (visible) {
                offsetAnimatable.snapTo(hiddenOffsetPx)
                scrollState.scrollTo(0)
                gestureStartOffset = halfOffsetPx
                offsetAnimatable.animateTo(
                    targetValue = halfOffsetPx,
                    animationSpec = sheetSpringSpec
                )
            } else {
                if (offsetAnimatable.value < hiddenOffsetPx) {
                    offsetAnimatable.animateTo(
                        targetValue = hiddenOffsetPx,
                        animationSpec = sheetSpringSpec
                    )
                }
            }
        }

        // BackHandler: collapses from Full to Half, or dismisses from Half
        val currentOffset = offsetAnimatable.value
        val isSheetVisible = currentOffset < hiddenOffsetPx - 10f
        if (visible && isSheetVisible) {
            BackHandler(enabled = true) {
                scope.launch {
                    if (offsetAnimatable.value < halfOffsetPx - 20f) {
                        // Collapse to Half
                        offsetAnimatable.animateTo(
                            targetValue = halfOffsetPx,
                            animationSpec = sheetSpringSpec
                        )
                    } else {
                        // Dismiss
                        offsetAnimatable.animateTo(
                            targetValue = hiddenOffsetPx,
                            animationSpec = sheetSpringSpec
                        )
                        onDismiss()
                    }
                }
            }
        }

        fun snapOrAnimateToAnchor(velocityY: Float = 0f) {
            val curr = offsetAnimatable.value
            val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
            scope.launch {
                if (velocityY < -500f) {
                    // Quick flick UP -> expand to Fullscreen
                    offsetAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = sheetSpringSpec,
                        initialVelocity = velocityY
                    )
                } else if (velocityY > 500f) {
                    // Quick flick DOWN
                    if (wasStartingFromFull) {
                        // Swiped down from Fullscreen -> ALWAYS collapse to Half-screen first, never dismiss directly
                        offsetAnimatable.animateTo(
                            targetValue = halfOffsetPx,
                            animationSpec = sheetSpringSpec,
                            initialVelocity = velocityY
                        )
                    } else {
                        // Swiped down from Half-screen -> Dismiss sheet
                        offsetAnimatable.animateTo(
                            targetValue = hiddenOffsetPx,
                            animationSpec = sheetSpringSpec,
                            initialVelocity = velocityY
                        )
                        onDismiss()
                    }
                } else {
                    // Low velocity / slow drag release:
                    if (wasStartingFromFull) {
                        // Started from Fullscreen: if dragged down past 25% of distance, snap to Half; else snap back to Full
                        val fullToHalfThreshold = halfOffsetPx * 0.25f
                        if (curr > fullToHalfThreshold) {
                            offsetAnimatable.animateTo(
                                targetValue = halfOffsetPx,
                                animationSpec = sheetSpringSpec
                            )
                        } else {
                            offsetAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = sheetSpringSpec
                            )
                        }
                    } else {
                        // Started from Half-screen:
                        val halfToFullThreshold = halfOffsetPx * 0.75f
                        val halfToHiddenThreshold = halfOffsetPx + (hiddenOffsetPx - halfOffsetPx) * 0.25f

                        when {
                            curr < halfToFullThreshold -> {
                                offsetAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = sheetSpringSpec
                                )
                            }
                            curr > halfToHiddenThreshold -> {
                                offsetAnimatable.animateTo(
                                    targetValue = hiddenOffsetPx,
                                    animationSpec = sheetSpringSpec
                                )
                                onDismiss()
                            }
                            else -> {
                                offsetAnimatable.animateTo(
                                    targetValue = halfOffsetPx,
                                    animationSpec = sheetSpringSpec
                                )
                            }
                        }
                    }
                }
            }
        }

        var isNestedScrollDragging by remember { mutableStateOf(false) }
        var touchStartedInContentScroll by remember { mutableStateOf(false) }

        androidx.compose.runtime.LaunchedEffect(scrollState.isScrollInProgress) {
            if (!scrollState.isScrollInProgress) {
                touchStartedInContentScroll = false
                isNestedScrollDragging = false
            }
        }

        val nestedScrollConnection = remember(sheetHeightPx, halfOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val deltaY = available.y

                    if (!isNestedScrollDragging) {
                        isNestedScrollDragging = true
                        touchStartedInContentScroll = scrollState.value > 0
                        gestureStartOffset = offsetAnimatable.value
                    }

                    // If gesture started while content was scrolled, let the scrollable container handle all movement
                    if (touchStartedInContentScroll) {
                        return Offset.Zero
                    }

                    // Dragging UP: if sheet is not fully expanded, consume drag to expand sheet
                    if (deltaY < 0 && offsetAnimatable.value > 0f) {
                        val newTarget = (offsetAnimatable.value + deltaY).coerceAtLeast(0f)
                        val consumed = newTarget - offsetAnimatable.value
                        scope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumed)
                    }

                    // Dragging DOWN when content is already at the top:
                    if (deltaY > 0 && scrollState.value == 0) {
                        val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                        val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                        val newTarget = (offsetAnimatable.value + deltaY).coerceIn(0f, maxTarget)
                        val consumed = newTarget - offsetAnimatable.value
                        scope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumed)
                    }

                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // Content scroll reached the top boundary: absorb without dragging the sheet down
                    if (touchStartedInContentScroll) {
                        return Offset.Zero
                    }

                    val deltaY = available.y
                    if (deltaY > 0 && scrollState.value == 0) {
                        val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                        val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                        val newTarget = (offsetAnimatable.value + deltaY).coerceIn(0f, maxTarget)
                        val consumedY = newTarget - offsetAnimatable.value
                        scope.launch { offsetAnimatable.snapTo(newTarget) }
                        return Offset(0f, consumedY)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (touchStartedInContentScroll) {
                        isNestedScrollDragging = false
                        touchStartedInContentScroll = false
                        return Velocity.Zero
                    }
                    if (offsetAnimatable.value > 0f) {
                        isNestedScrollDragging = false
                        snapOrAnimateToAnchor(available.y)
                        return available
                    }
                    isNestedScrollDragging = false
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val wasInContent = touchStartedInContentScroll
                    isNestedScrollDragging = false
                    touchStartedInContentScroll = false
                    if (wasInContent) {
                        return available
                    }
                    if (scrollState.value == 0 && available.y != 0f) {
                        snapOrAnimateToAnchor(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        val handleVelocityTracker = remember { VelocityTracker() }

        if (isSheetVisible) {
            val progress = ((hiddenOffsetPx - currentOffset) / hiddenOffsetPx).coerceIn(0f, 1f)
            val scrimAlpha = (progress * 0.28f).coerceIn(0f, 0.28f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            offsetAnimatable.animateTo(
                                targetValue = hiddenOffsetPx,
                                animationSpec = sheetSpringSpec
                            )
                            onDismiss()
                        }
                    }
            )
        }

        val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val expandFraction = if (halfOffsetPx > 0f) {
            ((halfOffsetPx - currentOffset) / halfOffsetPx).coerceIn(0f, 1f)
        } else 0f
        val dynamicTopPadding = statusBarTopPadding * expandFraction
        val dynamicCornerRadius = 28.dp * (1f - expandFraction)

        if (currentOffset < hiddenOffsetPx) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = currentOffset
                    }
                    .clip(RoundedCornerShape(topStart = dynamicCornerRadius, topEnd = dynamicCornerRadius))
                    .background(sheetBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 2.dp + dynamicTopPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            gestureStartOffset = offsetAnimatable.value
                                            handleVelocityTracker.resetTracking()
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            handleVelocityTracker.addPointerInputChange(change)
                                            val wasStartingFromFull = gestureStartOffset < halfOffsetPx * 0.45f
                                            val maxTarget = if (wasStartingFromFull) halfOffsetPx else hiddenOffsetPx
                                            val newTarget = (offsetAnimatable.value + dragAmount).coerceIn(0f, maxTarget)
                                            scope.launch { offsetAnimatable.snapTo(newTarget) }
                                        },
                                        onDragEnd = {
                                            val vy = handleVelocityTracker.calculateVelocity().y
                                            snapOrAnimateToAnchor(vy)
                                        },
                                        onDragCancel = {
                                            snapOrAnimateToAnchor(0f)
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
                                    .background(if (isMonochrome) Color.White.copy(alpha = 0.30f) else finalAccent.copy(alpha = 0.40f))
                            )
                        }

                        ModernVolumeSlider(
                            accentColor = finalAccent,
                            sheetBg = sheetBg,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                            color = if (isMonochrome) Color.White.copy(alpha = 0.10f) else finalAccent.copy(alpha = 0.22f),
                            thickness = 0.8.dp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection)
                            .verticalScroll(scrollState)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val songId = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" }
                        val songTitle = uiState.songUIState.title.ifBlank { song?.title ?: "" }

                        ModernQuickActionCard(
                            icon = Res.drawable.metro_radio,
                            label = "Start radio",
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            iconTint = cardContent,
                            labelColor = cardSecondaryContent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (songId.isNotBlank()) {
                                    viewModel.onUIEvent(
                                        NowPlayingBottomSheetUIEvent.StartRadio(
                                            videoId = songId,
                                            name = "\"$songTitle\" Radio",
                                        )
                                    )
                                    onDismiss()
                                }
                            }
                        )

                        ModernQuickActionCard(
                            icon = Res.drawable.baseline_playlist_add_24,
                            label = "Add to playlist",
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            iconTint = cardContent,
                            labelColor = cardSecondaryContent,
                            modifier = Modifier.weight(1f),
                            onClick = { addToAPlaylist = true }
                        )

                        ModernQuickActionCard(
                            icon = Res.drawable.metro_link,
                            label = "Copy link",
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            iconTint = cardContent,
                            labelColor = cardSecondaryContent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (songId.isNotBlank()) {
                                    val url = "https://music.youtube.com/watch?v=$songId"
                                    copyToClipboard("Song Link", url)
                                    SoniqueToastManager.show("Link copied to clipboard")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val isLiked = uiState.songUIState.liked

                    // Section 1: Library & Track Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val artistNames = uiState.songUIState.listArtists.toListName().connectArtists()
                            .ifBlank { song?.artistName?.connectArtists() ?: "" }

                        ModernOptionCard(
                            iconRes = Res.drawable.ic_artist_note,
                            title = "View artist",
                            subtitle = artistNames.ifBlank { null },
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = {
                                if (uiState.songUIState.listArtists.isNotEmpty() || !song?.artistName.isNullOrEmpty()) {
                                    showArtistSheet = true
                                } else {
                                    SoniqueToastManager.show("No artist information")
                                }
                            }
                        )

                        ModernOptionCard(
                            iconRes = Res.drawable.ic_library_add,
                            title = if (isLiked) "Remove from library" else "Add to library",
                            subtitle = null,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                                SoniqueToastManager.show(if (!isLiked) "Added to library" else "Removed from library")
                            }
                        )

                        val downloadState = uiState.songUIState.downloadState
                        val (downloadText, downloadSubtitle) = when (downloadState) {
                            DownloadState.STATE_DOWNLOADED -> "Downloaded" to "Saved to offline library"
                            DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> "Downloading" to "Saving to device..."
                            else -> "Download" to null
                        }

                        ModernOptionCard(
                            iconRes = when (downloadState) {
                                DownloadState.STATE_DOWNLOADED -> Res.drawable.baseline_downloaded
                                DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> Res.drawable.baseline_downloading_white
                                else -> null
                            },
                            iconVector = if (downloadState != DownloadState.STATE_DOWNLOADED &&
                                downloadState != DownloadState.STATE_DOWNLOADING &&
                                downloadState != DownloadState.STATE_PREPARING
                            ) Icons.Rounded.FileDownload else null,
                            title = downloadText,
                            subtitle = downloadSubtitle,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = {
                                if (downloadState == DownloadState.STATE_DOWNLOADED ||
                                    downloadState == DownloadState.STATE_DOWNLOADING ||
                                    downloadState == DownloadState.STATE_PREPARING
                                ) {
                                    showCancelDownloadDialog = true
                                } else {
                                    viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 2: Audio & Playback Experience
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ModernOptionCard(
                            iconVector = Icons.Rounded.Group,
                            title = "Listen Together",
                            subtitle = null,
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = {
                                onDismiss()
                                onNavigateToOtherScreen()
                                navController.navigate(ListenTogetherDestination)
                            }
                        )

                        val currentProviderLabel = when (lyricsProviderPref) {
                            DataStoreManager.BETTER_LYRICS -> "BetterLyrics"
                            DataStoreManager.YOUTUBE -> "YouTube"
                            DataStoreManager.SPOTIFY -> "Spotify"
                            else -> "LRCLIB"
                        }
                        val lyricsOptionsSubtitle = "$currentProviderLabel • ${if (lyricsAutoFallbackPref) "Auto fallback on" else "Auto fallback off"}"

                        ModernOptionCard(
                            iconRes = Res.drawable.metro_lyrics,
                            title = "Lyrics options",
                            subtitle = lyricsOptionsSubtitle,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = {
                                showLyricsOptionsDialog = true
                            }
                        )

                        ModernOptionCard(
                            iconRes = Res.drawable.metro_equalizer,
                            title = "Equalizer",
                            subtitle = "Open the audio equalizer",
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = { eqLauncher.launch() }
                        )

                        ModernOptionCard(
                            iconRes = Res.drawable.metro_info,
                            title = "Details",
                            subtitle = "View the song's details",
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            onClick = { showDetailsDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section 3: Experience Toggles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ModernOptionCard(
                            iconRes = Res.drawable.metro_palette,
                            title = "Ambience Mode",
                            subtitle = if (isAmbienceEnabled) "Dynamic background colors enabled" else "Dynamic background colors disabled",
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            trailingContent = {
                                Switch(
                                    checked = isAmbienceEnabled,
                                    onCheckedChange = null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = if (isMonochrome) Color.White else finalAccent,
                                        checkedTrackColor = if (isMonochrome) Color.White.copy(alpha = 0.35f) else finalAccent.copy(alpha = 0.35f),
                                        checkedBorderColor = if (isMonochrome) Color.White else finalAccent,
                                        uncheckedThumbColor = cardSecondaryContent,
                                        uncheckedTrackColor = cardBg,
                                        uncheckedBorderColor = cardBorder ?: cardSecondaryContent.copy(alpha = 0.3f),
                                    )
                                )
                            },
                            onClick = {
                                scope.launch {
                                    val next = !isAmbienceEnabled
                                    dataStoreManager.setAmbienceMode(next)
                                    SoniqueToastManager.show(if (next) "Ambience Mode enabled" else "Ambience Mode disabled")
                                }
                            }
                        )

                        ModernOptionCard(
                            iconRes = Res.drawable.metro_tune,
                            title = "Crossfade",
                            subtitle = if (isCrossfadeEnabled) "Smooth track transitions enabled" else "Smooth track transitions disabled",
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                            cardBg = cardBg,
                            borderColor = cardBorder,
                            contentColor = cardContent,
                            secondaryContentColor = cardSecondaryContent,
                            trailingContent = {
                                Switch(
                                    checked = isCrossfadeEnabled,
                                    onCheckedChange = null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = if (isMonochrome) Color.White else finalAccent,
                                        checkedTrackColor = if (isMonochrome) Color.White.copy(alpha = 0.35f) else finalAccent.copy(alpha = 0.35f),
                                        checkedBorderColor = if (isMonochrome) Color.White else finalAccent,
                                        uncheckedThumbColor = cardSecondaryContent,
                                        uncheckedTrackColor = cardBg,
                                        uncheckedBorderColor = cardBorder ?: cardSecondaryContent.copy(alpha = 0.3f),
                                    )
                                )
                            },
                            onClick = {
                                scope.launch {
                                    val next = !isCrossfadeEnabled
                                    dataStoreManager.setCrossfadeEnabled(next)
                                    SoniqueToastManager.show(if (next) "Crossfade enabled" else "Crossfade disabled")
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

/**
 * Modern 3-Dot More Options Bottom Sheet for NewPlayerScreen.
 * Strictly uses Sonique's MaterialTheme.colorScheme tokens with no hardcoded colors:
 * - Interactive Volume Slider pill using primary / surfaceVariant / onPrimary
 * - 3 Action Cards (Start radio, Add to playlist, Copy link)
 * - 7 Rounded Option Cards (View artist, Add to library, Pin to speed dial,
 *   Download, Listen Together, Details, Equalizer)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMoreOptionsContent(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel,
    onNavigateToOtherScreen: () -> Unit = {},
    backgroundColor: Color? = null,
    accentColor: Color? = null,
    contentColor: Color? = null,
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    onLyricsClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val ambienceModePref by dataStoreManager.ambienceMode.collectAsStateWithLifecycle(initialValue = DataStoreManager.TRUE)
    val isAmbienceEnabled = ambienceModePref == DataStoreManager.TRUE

    val crossfadeEnabledPref by dataStoreManager.crossfadeEnabled.collectAsStateWithLifecycle(initialValue = DataStoreManager.FALSE)
    val isCrossfadeEnabled = crossfadeEnabledPref == DataStoreManager.TRUE

    val lyricsProviderPref by dataStoreManager.lyricsProvider.collectAsStateWithLifecycle(initialValue = DataStoreManager.LRCLIB)
    val lyricsAutoFallbackPref by dataStoreManager.lyricsAutoFallback.collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(song) {
        viewModel.setSongEntity(song)
    }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }
    var showLyricsOptionsDialog by remember { mutableStateOf(false) }

    val audioSessionId = remember {
        runCatching { mediaPlayerHandler.player.audioSessionId }.getOrDefault(0)
    }
    val eqLauncher = openEqResult(audioSessionId)

    if (addToAPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { addToAPlaylist = false },
            onClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id))
            },
            onYTPlaylistClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(it.browseId))
            },
            videoId = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" },
        )
    }

    if (showArtistSheet) {
        val artistsList = uiState.songUIState.listArtists.ifEmpty {
            song?.artistName?.map { Artist(name = it, id = null) } ?: emptyList()
        }
        ArtistModalBottomSheet(
            isBottomSheetVisible = true,
            artists = artistsList,
            navController = navController,
            onNavigateToOtherScreen = onNavigateToOtherScreen,
            onDismiss = { showArtistSheet = false },
        )
    }

    if (showDetailsDialog) {
        val titleText = uiState.songUIState.title.ifBlank { song?.title ?: "Unknown" }
        val artistText = uiState.songUIState.listArtists.toListName().connectArtists()
            .ifBlank { song?.artistName?.connectArtists() ?: "Unknown" }
        val albumText = uiState.songUIState.album?.name ?: song?.albumName ?: ""
        val videoIdText = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" }
        val durationText = song?.duration ?: ""

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SongDetailRow(label = "Title", value = titleText)
                    SongDetailRow(label = "Artist", value = artistText)
                    if (albumText.isNotBlank()) {
                        SongDetailRow(label = "Album", value = albumText)
                    }
                    if (durationText.isNotBlank()) {
                        SongDetailRow(label = "Duration", value = durationText)
                    }
                    if (videoIdText.isNotBlank()) {
                        SongDetailRow(label = "Video ID", value = videoIdText)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showCancelDownloadDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismissRequest = { showCancelDownloadDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDownloadDialog = false
                    viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDownloadDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = { Text("Download") },
            text = {
                val isDownloaded = uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED
                Text(if (isDownloaded) "Do you want to remove this download?" else "Do you want to cancel the download?")
            }
        )
    }

    if (showLyricsOptionsDialog) {
        LyricsOptionsDialog(
            onDismissRequest = { showLyricsOptionsDialog = false },
            dataStoreManager = dataStoreManager,
            sharedViewModel = sharedViewModel,
            onViewLyrics = onLyricsClick?.let {
                {
                    onDismiss()
                    it.invoke()
                }
            },
        )
    }

    val tint = accentColor ?: MaterialTheme.colorScheme.primary
    val isMonochrome = tint == Color.White || tint == Color.Black || tint.toHsl()[1] < 0.12f
    val darkBase = Color(0xFF141316)
    val defaultTintedBg = if (isMonochrome) darkBase else tint.copy(alpha = 0.16f).compositeOver(darkBase).copy(alpha = 1f)
    val sheetBg = (backgroundColor ?: defaultTintedBg).copy(alpha = 1f)
    val finalContent = contentColor ?: Color.White
    val finalAccent = tint
    val cardBg = if (isMonochrome) {
        Color.White.copy(alpha = 0.08f).compositeOver(sheetBg).copy(alpha = 1f)
    } else {
        tint.copy(alpha = 0.12f).compositeOver(sheetBg).copy(alpha = 1f)
    }
    val cardBorder: Color? = null
    val cardContent = Color.White
    val cardSecondaryContent = Color.White.copy(alpha = 0.78f)

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.roundToInt().coerceAtLeast(0)) }
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(sheetBg)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dragOffsetY > 130f) {
                            onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isMonochrome) Color.White.copy(alpha = 0.30f) else finalAccent.copy(alpha = 0.40f))
                    )
                }

                ModernVolumeSlider(
                    accentColor = finalAccent,
                    sheetBg = sheetBg,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    color = finalAccent.copy(alpha = 0.22f),
                    thickness = 0.8.dp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val songId = uiState.songUIState.videoId.ifBlank { song?.videoId ?: "" }
                val songTitle = uiState.songUIState.title.ifBlank { song?.title ?: "" }

                ModernQuickActionCard(
                    icon = Res.drawable.metro_radio,
                    label = "Start radio",
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    iconTint = cardContent,
                    labelColor = cardSecondaryContent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (songId.isNotBlank()) {
                            viewModel.onUIEvent(
                                NowPlayingBottomSheetUIEvent.StartRadio(
                                    videoId = songId,
                                    name = "\"$songTitle\" Radio",
                                )
                            )
                            onDismiss()
                        }
                    }
                )

                ModernQuickActionCard(
                    icon = Res.drawable.baseline_playlist_add_24,
                    label = "Add to playlist",
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    iconTint = cardContent,
                    labelColor = cardSecondaryContent,
                    modifier = Modifier.weight(1f),
                    onClick = { addToAPlaylist = true }
                )

                ModernQuickActionCard(
                    icon = Res.drawable.metro_link,
                    label = "Copy link",
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    iconTint = cardContent,
                    labelColor = cardSecondaryContent,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (songId.isNotBlank()) {
                            val url = "https://music.youtube.com/watch?v=$songId"
                            copyToClipboard("Song Link", url)
                            SoniqueToastManager.show("Link copied to clipboard")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val isLiked = uiState.songUIState.liked

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val artistNames = uiState.songUIState.listArtists.toListName().connectArtists()
                    .ifBlank { song?.artistName?.connectArtists() ?: "" }

                ModernOptionCard(
                    iconRes = Res.drawable.ic_artist_note,
                    title = "View artist",
                    subtitle = artistNames.ifBlank { null },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        if (uiState.songUIState.listArtists.isNotEmpty() || !song?.artistName.isNullOrEmpty()) {
                            showArtistSheet = true
                        } else {
                            SoniqueToastManager.show("No artist information")
                        }
                    }
                )

                ModernOptionCard(
                    iconRes = Res.drawable.ic_library_add,
                    title = if (isLiked) "Remove from library" else "Add to library",
                    subtitle = null,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        SoniqueToastManager.show(if (!isLiked) "Added to library" else "Removed from library")
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val downloadState = uiState.songUIState.downloadState
            val (downloadText, downloadSubtitle) = when (downloadState) {
                DownloadState.STATE_DOWNLOADED -> "Downloaded" to "Saved to offline library"
                DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> "Downloading" to "Saving to device..."
                else -> "Download" to null
            }

            ModernOptionCard(
                iconRes = when (downloadState) {
                    DownloadState.STATE_DOWNLOADED -> Res.drawable.baseline_downloaded
                    DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> Res.drawable.baseline_downloading_white
                    else -> null
                },
                iconVector = if (downloadState != DownloadState.STATE_DOWNLOADED &&
                    downloadState != DownloadState.STATE_DOWNLOADING &&
                    downloadState != DownloadState.STATE_PREPARING
                ) Icons.Rounded.FileDownload else null,
                title = downloadText,
                subtitle = downloadSubtitle,
                shape = RoundedCornerShape(20.dp),
                cardBg = cardBg,
                borderColor = cardBorder,
                contentColor = cardContent,
                secondaryContentColor = cardSecondaryContent,
                onClick = {
                    if (downloadState == DownloadState.STATE_DOWNLOADED ||
                        downloadState == DownloadState.STATE_DOWNLOADING ||
                        downloadState == DownloadState.STATE_PREPARING
                    ) {
                        showCancelDownloadDialog = true
                    } else {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            ModernOptionCard(
                iconVector = Icons.Rounded.Group,
                title = "Listen Together",
                subtitle = null,
                shape = RoundedCornerShape(20.dp),
                cardBg = cardBg,
                borderColor = cardBorder,
                contentColor = cardContent,
                secondaryContentColor = cardSecondaryContent,
                onClick = {
                    onDismiss()
                    onNavigateToOtherScreen()
                    navController.navigate(ListenTogetherDestination)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            val currentProviderLabel = when (lyricsProviderPref) {
                DataStoreManager.BETTER_LYRICS -> "BetterLyrics"
                DataStoreManager.YOUTUBE -> "YouTube"
                DataStoreManager.SPOTIFY -> "Spotify"
                else -> "LRCLIB"
            }
            val lyricsOptionsSubtitle = "$currentProviderLabel • ${if (lyricsAutoFallbackPref) "Auto fallback on" else "Auto fallback off"}"

            ModernOptionCard(
                iconRes = Res.drawable.metro_lyrics,
                title = "Lyrics options",
                subtitle = lyricsOptionsSubtitle,
                shape = RoundedCornerShape(20.dp),
                cardBg = cardBg,
                borderColor = cardBorder,
                contentColor = cardContent,
                secondaryContentColor = cardSecondaryContent,
                onClick = {
                    showLyricsOptionsDialog = true
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModernOptionCard(
                    iconRes = Res.drawable.metro_palette,
                    title = "Ambience Mode",
                    subtitle = if (isAmbienceEnabled) "Dynamic background colors enabled" else "Dynamic background colors disabled",
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    trailingContent = {
                        Switch(
                            checked = isAmbienceEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isMonochrome) Color.White else finalAccent,
                                checkedTrackColor = if (isMonochrome) Color.White.copy(alpha = 0.35f) else finalAccent.copy(alpha = 0.35f),
                                checkedBorderColor = if (isMonochrome) Color.White else finalAccent,
                                uncheckedThumbColor = cardSecondaryContent,
                                uncheckedTrackColor = cardBg,
                                uncheckedBorderColor = cardBorder ?: cardSecondaryContent.copy(alpha = 0.3f),
                            )
                        )
                    },
                    onClick = {
                        scope.launch {
                            val next = !isAmbienceEnabled
                            dataStoreManager.setAmbienceMode(next)
                            SoniqueToastManager.show(if (next) "Ambience Mode enabled" else "Ambience Mode disabled")
                        }
                    }
                )

                ModernOptionCard(
                    iconRes = Res.drawable.metro_tune,
                    title = "Crossfade",
                    subtitle = if (isCrossfadeEnabled) "Smooth track transitions enabled" else "Smooth track transitions disabled",
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    trailingContent = {
                        Switch(
                            checked = isCrossfadeEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isMonochrome) Color.White else finalAccent,
                                checkedTrackColor = if (isMonochrome) Color.White.copy(alpha = 0.35f) else finalAccent.copy(alpha = 0.35f),
                                checkedBorderColor = if (isMonochrome) Color.White else finalAccent,
                                uncheckedThumbColor = cardSecondaryContent,
                                uncheckedTrackColor = cardBg,
                                uncheckedBorderColor = cardBorder ?: cardSecondaryContent.copy(alpha = 0.3f),
                            )
                        )
                    },
                    onClick = {
                        scope.launch {
                            val next = !isCrossfadeEnabled
                            dataStoreManager.setCrossfadeEnabled(next)
                            SoniqueToastManager.show(if (next) "Crossfade enabled" else "Crossfade disabled")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModernOptionCard(
                    iconRes = Res.drawable.metro_info,
                    title = "Details",
                    subtitle = "View the song's details",
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = { showDetailsDialog = true }
                )

                ModernOptionCard(
                    iconRes = Res.drawable.metro_equalizer,
                    title = "Equalizer",
                    subtitle = "Open the audio equalizer",
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = { eqLauncher.launch() }
                )
            }
        }
    }
}
}

@Composable
fun ModernMoreOptionsSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel,
    onNavigateToOtherScreen: () -> Unit = {},
    backgroundColor: Color? = null,
    accentColor: Color? = null,
    contentColor: Color? = null,
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
    sharedViewModel: SharedViewModel = koinInject(),
    onLyricsClick: (() -> Unit)? = null,
) {
    ModernMoreOptionsContent(
        onDismiss = onDismiss,
        navController = navController,
        song = song,
        viewModel = viewModel,
        onNavigateToOtherScreen = onNavigateToOtherScreen,
        backgroundColor = backgroundColor,
        accentColor = accentColor,
        contentColor = contentColor,
        mediaPlayerHandler = mediaPlayerHandler,
        dataStoreManager = dataStoreManager,
        sharedViewModel = sharedViewModel,
        onLyricsClick = onLyricsClick,
    )
}

/**
 * Interactive volume slider with in-track fill and vertical stop indicator.
 */
@Composable
private fun ModernVolumeSlider(
    accentColor: Color,
    sheetBg: Color,
    modifier: Modifier = Modifier,
) {
    val volumeController = rememberVolumeController()
    val currentVol = volumeController.currentVolume

    var sliderPosition by remember { mutableFloatStateOf(currentVol) }
    LaunchedEffect(currentVol) { sliderPosition = currentVol }

    val trackBg = accentColor.copy(alpha = 0.12f).compositeOver(sheetBg).copy(alpha = 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(trackBg)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val vol = (offset.x / size.width).coerceIn(0f, 1f)
                            sliderPosition = vol
                            volumeController.setVolume(vol)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val vol = (change.position.x / size.width).coerceIn(0f, 1f)
                            sliderPosition = vol
                            volumeController.setVolume(vol)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val vol = (offset.x / size.width).coerceIn(0f, 1f)
                        sliderPosition = vol
                        volumeController.setVolume(vol)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val fraction = sliderPosition.coerceIn(0f, 1f)
            val density = LocalDensity.current

            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(3.5.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            val iconRes = when {
                sliderPosition <= 0.01f -> Res.drawable.metro_volume_mute
                sliderPosition < 0.5f   -> Res.drawable.metro_volume_down
                else                    -> Res.drawable.metro_volume_up
            }

            val activeWidthPx = fraction * totalWidthPx
            val iconThresholdPx = with(density) { 40.dp.toPx() }
            val iconTint = if (activeWidthPx > iconThresholdPx) {
                Color(0xFF0E1418)
            } else {
                Color.White
            }

            Icon(
                painter = painterResource(iconRes),
                contentDescription = "Volume",
                tint = iconTint,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val next = if (sliderPosition > 0.01f) 0f else 0.5f
                        sliderPosition = next
                        volumeController.setVolume(next)
                    }
            )
        }
    }
}

/**
 * Top quick action card (Start radio, Add to playlist, Copy link)
 */
@Composable
private fun ModernQuickActionCard(
    icon: DrawableResource,
    label: String,
    cardBg: Color,
    iconTint: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .then(if (borderColor != null) Modifier.border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                ),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Rounded option card for vertical items with icon, title, and optional subtitle
 */
@Composable
private fun ModernOptionCard(
    title: String,
    cardBg: Color,
    contentColor: Color,
    secondaryContentColor: Color,
    subtitle: String? = null,
    iconRes: DrawableResource? = null,
    iconVector: ImageVector? = null,
    borderColor: Color? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .then(if (borderColor != null) Modifier.border(BorderStroke(1.dp, borderColor), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        } else if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = secondaryContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

@Composable
private fun SongDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LyricsOptionsDialog(
    onDismissRequest: () -> Unit,
    dataStoreManager: DataStoreManager,
    sharedViewModel: SharedViewModel,
    onViewLyrics: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val lyricsProvider by dataStoreManager.lyricsProvider.collectAsStateWithLifecycle(initialValue = DataStoreManager.LRCLIB)
    val lyricsAutoFallback by dataStoreManager.lyricsAutoFallback.collectAsStateWithLifecycle(initialValue = true)
    val lyricsOffsetMs by dataStoreManager.lyricsOffsetMs.collectAsStateWithLifecycle(initialValue = 0)
    val useAITranslation by sharedViewModel.useAITranslation.collectAsStateWithLifecycle(initialValue = false)

    var aiTesting by remember { mutableStateOf(false) }
    var aiTestResult by remember { mutableStateOf<String?>(null) }
    var aiTestIsError by remember { mutableStateOf(false) }

    val providers = listOf(
        DataStoreManager.BETTER_LYRICS to "BetterLyrics",
        DataStoreManager.LRCLIB to "LRCLIB",
        DataStoreManager.YOUTUBE to "YouTube Captions",
        DataStoreManager.SPOTIFY to "Spotify",
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = "Lyrics Options",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Main Lyrics Provider",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    providers.forEachIndexed { index, (key, label) ->
                        val isSelected = (lyricsProvider == key) || (lyricsProvider.isBlank() && key == DataStoreManager.LRCLIB)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        dataStoreManager.setLyricsProvider(key)
                                        sharedViewModel.setLyricsProvider(key)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch {
                                        dataStoreManager.setLyricsProvider(key)
                                        sharedViewModel.setLyricsProvider(key)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (index < providers.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable {
                            coroutineScope.launch {
                                dataStoreManager.setLyricsAutoFallback(!lyricsAutoFallback)
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Automatic Fallback",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Check other sources if lyrics are missing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = lyricsAutoFallback,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                dataStoreManager.setLyricsAutoFallback(checked)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics Offset",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (lyricsOffsetMs > 0) "+$lyricsOffsetMs" else "$lyricsOffsetMs"} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    dataStoreManager.setLyricsOffsetMs(lyricsOffsetMs - 100)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("-100ms", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    dataStoreManager.setLyricsOffsetMs(0)
                                }
                            },
                            modifier = Modifier.weight(0.8f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    dataStoreManager.setLyricsOffsetMs(lyricsOffsetMs + 100)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("+100ms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "AI Lyrics Translation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Translate synced lyrics using AI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useAITranslation,
                            onCheckedChange = { checked ->
                                sharedViewModel.setUseAITranslation(checked)
                            }
                        )
                    }

                    if (useAITranslation) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    aiTesting = true
                                    aiTestResult = null
                                    val res = sharedViewModel.testAIConnection()
                                    aiTesting = false
                                    res.onSuccess { msg ->
                                        aiTestIsError = false
                                        aiTestResult = msg
                                    }.onFailure { err ->
                                        aiTestIsError = true
                                        aiTestResult = err.message ?: "Connection failed"
                                    }
                                }
                            },
                            enabled = !aiTesting,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (aiTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing API...", style = MaterialTheme.typography.labelSmall)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test AI Connection", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (aiTestResult != null) {
                            Text(
                                text = aiTestResult ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aiTestIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onViewLyrics != null) {
                    TextButton(onClick = {
                        onDismissRequest()
                        onViewLyrics()
                    }) {
                        Text("View Lyrics", color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        sharedViewModel.setLyricsProvider()
                        SoniqueToastManager.show("Reloading lyrics...")
                    }) {
                        Text("Reload", color = MaterialTheme.colorScheme.secondary)
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("Done", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    )
}
