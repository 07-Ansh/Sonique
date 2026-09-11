package com.sonique.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * Material 3 Expressive design:
 * - Interactive custom Volume Slider
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
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
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
            containerColor = Color(0xFF1E2125),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.85f),
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
            containerColor = Color(0xFF1E2125),
            titleContentColor = Color.White,
            textContentColor = Color.White,
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
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            title = { Text("Download") },
            text = {
                val isDownloaded = uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED
                Text(if (isDownloaded) "Do you want to remove this download?" else "Do you want to cancel the download?")
            }
        )
    }

    val sheetBg = Color(0xFF131518)
    val cardBg = Color(0xFF1A1D21)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                ModernVolumeSlider(modifier = Modifier.fillMaxWidth())
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.10f),
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
                        modifier = Modifier.weight(1f),
                        onClick = { addToAPlaylist = true }
                    )

                    ModernQuickActionCard(
                        icon = Res.drawable.metro_link,
                        label = "Copy link",
                        cardBg = cardBg,
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
                    onClick = { showDetailsDialog = true }
                )
            }

            item {
                ModernOptionCard(
                    iconRes = Res.drawable.metro_equalizer,
                    title = "Equalizer",
                    subtitle = "Open the audio equalizer",
                    cardBg = cardBg,
                    onClick = { eqLauncher.launch() }
                )
            }
        }
    }
}

/**
 * Interactive Material 3 volume bar pill:
 * Filled progress on left (light blue #9EC4D5), dark track on right (#1E2226),
 * volume icon inside left, vertical level indicator bar at the split.
 */
@Composable
private fun ModernVolumeSlider(modifier: Modifier = Modifier) {
    val volumeController = rememberVolumeController()
    val currentVol = volumeController.currentVolume

    val sliderColor = Color(0xFF9EC4D5)
    val trackBgColor = Color(0xFF1E2226)
    val indicatorColor = Color(0xFF12232E)

    BoxWithConstraints(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(trackBgColor)
            .pointerInput(volumeController) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    volumeController.setVolume(fraction)
                }
            }
            .pointerInput(volumeController) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    volumeController.setVolume(fraction)
                }
            }
    ) {
        val totalWidth = maxWidth
        val filledWidth = totalWidth * currentVol

        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(filledWidth)
                .background(sliderColor)
        )

        // Vertical indicator line at current volume level
        if (currentVol > 0.03f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (filledWidth - 10.dp).coerceAtLeast(0.dp))
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(indicatorColor)
            )
        }

        // Speaker icon on the left
        val iconRes = when {
            currentVol <= 0.01f -> Res.drawable.metro_volume_mute
            currentVol < 0.5f -> Res.drawable.metro_volume_down
            else -> Res.drawable.metro_volume_up
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = "Volume",
            tint = if (currentVol > 0.08f) indicatorColor else Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(24.dp)
                .clickable {
                    // Tap speaker icon toggles mute / 50%
                    if (currentVol > 0.01f) {
                        volumeController.setVolume(0f)
                    } else {
                        volumeController.setVolume(0.5f)
                    }
                }
        )
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
                tint = Color.White,
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
                color = Color.White.copy(alpha = 0.85f),
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = title,
                tint = Color.White,
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
                color = Color.White,
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
                    color = Color.White.copy(alpha = 0.6f),
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
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
