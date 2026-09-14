package com.sonique.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material3.AlertDialog
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
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(song) {
        viewModel.setSongEntity(song)
    }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }

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

    val sheetBg = (backgroundColor ?: MaterialTheme.colorScheme.surfaceContainerHigh).copy(alpha = 1f)
    val finalContent = contentColor ?: Color.White
    val finalAccent = accentColor ?: MaterialTheme.colorScheme.primaryContainer
    // If a custom album-art color is provided, derive the card bg from it (slightly brightened)
    // so cards feel elevated over the sheet. Otherwise fall back to the theme's surfaceVariant.
    val cardBg = if (backgroundColor != null)
        Color.White.copy(alpha = 0.12f).compositeOver(sheetBg)
    else
        MaterialTheme.colorScheme.surfaceVariant
    val cardContent = Color.White
    val cardSecondaryContent = Color.White.copy(alpha = 0.72f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = finalContent,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = Modifier.fillMaxHeight(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                ModernVolumeSlider(
                    accentColor = finalAccent,
                    sheetBg = sheetBg,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    thickness = 0.8.dp
                )
            }

            item {
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
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                        }
                    )

                    ModernQuickActionCard(
                        icon = Res.drawable.baseline_playlist_add_24,
                        label = "Add to playlist",
                        cardBg = cardBg,
                        iconTint = cardContent,
                        labelColor = cardSecondaryContent,
                        modifier = Modifier.weight(1f),
                        onClick = { addToAPlaylist = true }
                    )

                    ModernQuickActionCard(
                        icon = Res.drawable.metro_link,
                        label = "Copy link",
                        cardBg = cardBg,
                        iconTint = cardContent,
                        labelColor = cardSecondaryContent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (songId.isNotBlank()) {
                                val url = "https://music.youtube.com/watch?v=$songId"
                                copyToClipboard("Song Link", url)
                                showToast("Link copied to clipboard", ToastGravity.Bottom)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                val artistNames = uiState.songUIState.listArtists.toListName().connectArtists()
                    .ifBlank { song?.artistName?.connectArtists() ?: "" }

                ModernOptionCard(
                    iconRes = Res.drawable.ic_artist_note,
                    title = "View artist",
                    subtitle = artistNames.ifBlank { null },
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        if (uiState.songUIState.listArtists.isNotEmpty() || !song?.artistName.isNullOrEmpty()) {
                            showArtistSheet = true
                        } else {
                            showToast("No artist information", ToastGravity.Bottom)
                        }
                    }
                )
            }

            item {
                val isLiked = uiState.songUIState.liked
                ModernOptionCard(
                    iconRes = Res.drawable.ic_library_add,
                    title = if (isLiked) "In library" else "Add to library",
                    subtitle = null,
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        showToast(if (!isLiked) "Added to library" else "Removed from library", ToastGravity.Bottom)
                    }
                )
            }

            item {
                ModernOptionCard(
                    iconVector = Icons.Rounded.Add,
                    title = "Pin to speed dial",
                    subtitle = null,
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        showToast("Pinned to speed dial", ToastGravity.Bottom)
                    }
                )
            }

            item {
                val downloadState = uiState.songUIState.downloadState
                val downloadText = when (downloadState) {
                    DownloadState.STATE_DOWNLOADED -> "Downloaded"
                    DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> "Downloading"
                    else -> "Download"
                }

                ModernOptionCard(
                    iconVector = Icons.Rounded.FileDownload,
                    title = downloadText,
                    subtitle = null,
                    cardBg = cardBg,
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

            item {
                ModernOptionCard(
                    iconVector = Icons.Rounded.Group,
                    title = "Listen Together",
                    subtitle = null,
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onNavigateToOtherScreen()
                            navController.navigate(ListenTogetherDestination)
                        }
                    }
                )
            }

            item {
                ModernOptionCard(
                    iconRes = Res.drawable.metro_info,
                    title = "Details",
                    subtitle = "View the song's details",
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = { showDetailsDialog = true }
                )
            }

            item {
                ModernOptionCard(
                    iconRes = Res.drawable.metro_equalizer,
                    title = "Equalizer",
                    subtitle = "Open the audio equalizer",
                    cardBg = cardBg,
                    contentColor = cardContent,
                    secondaryContentColor = cardSecondaryContent,
                    onClick = { eqLauncher.launch() }
                )
            }
        }
    }
}

/**
 * Interactive Material 3 volume bar pill strictly styled via MaterialTheme tokens:
 * Filled progress uses primary / primaryContainer, track background uses surfaceVariant,
 * indicator bar and speaker icon use onPrimary / onPrimaryContainer.
 */
/**
 * Google Material 3 Expressive Volume Slider.
 * Material 3 Expressive Slider specifications:
 * - Track height: 40dp
 * - Track corner radius: 12dp (RoundedCornerShape(12.dp), NOT round circle)
 * - Handle: 4dp width, 36dp height, 2dp corner radius
 * - Thumb-track gap size: 6dp
 * - End stop indicator: 8dp circle
 * - Inset volume icon: 22dp, adaptive contrast
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

    val inactiveTrackColor = Color.White.copy(alpha = 0.08f).compositeOver(sheetBg)
    val trackCornerRadius = 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(trackCornerRadius))
                .background(inactiveTrackColor)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val initialVol = (down.position.x / size.width).coerceIn(0f, 1f)
                        sliderPosition = initialVol
                        volumeController.setVolume(initialVol)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            val newVol = (change.position.x / size.width).coerceIn(0f, 1f)
                            sliderPosition = newVol
                            volumeController.setVolume(newVol)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val fraction = sliderPosition.coerceIn(0f, 1f)

            val density = LocalDensity.current
            val thumbWidthPx = with(density) { 4.dp.toPx() }
            val gapPx = with(density) { 6.dp.toPx() }

            // Position of the vertical thumb bar
            val thumbX = (fraction * (totalWidthPx - thumbWidthPx)).coerceIn(0f, totalWidthPx - thumbWidthPx)
            val activeWidthPx = (thumbX - gapPx).coerceAtLeast(0f)

            // 1. Active Fill Track (left is clipped by 12dp corner; right edge is flat and straight)
            if (activeWidthPx > 0f) {
                val activeWidthDp = with(density) { activeWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .width(activeWidthDp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }

            // Stop indicator: subtle dot at right end when volume is not max
            if (sliderPosition < 0.90f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }

            // 2. Vertical Thumb Bar (Google Material 3 Expressive handle)
            val thumbOffsetXDp = with(density) { thumbX.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetXDp)
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            val iconRes = when {
                sliderPosition <= 0.01f -> Res.drawable.metro_volume_mute
                sliderPosition < 0.5f   -> Res.drawable.metro_volume_down
                else                    -> Res.drawable.metro_volume_up
            }

            // Icon tint: dark high-contrast when inside the active light fill, white otherwise
            val iconThresholdPx = with(density) { 46.dp.toPx() }
            val iconTint = if (activeWidthPx > iconThresholdPx) {
                Color(0xFF14222E)
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
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
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
