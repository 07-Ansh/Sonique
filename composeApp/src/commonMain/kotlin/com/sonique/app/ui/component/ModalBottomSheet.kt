package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.NowPlayingBottomSheetUIEvent
import com.sonique.app.viewModel.NowPlayingBottomSheetViewModel
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.utils.connectArtists
import com.sonique.domain.utils.toListName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.add_to_a_playlist
import sonique.composeapp.generated.resources.add_to_queue
import sonique.composeapp.generated.resources.artists
import sonique.composeapp.generated.resources.baseline_access_alarm_24
import sonique.composeapp.generated.resources.baseline_album_24
import sonique.composeapp.generated.resources.baseline_delete_24
import sonique.composeapp.generated.resources.baseline_downloaded
import sonique.composeapp.generated.resources.baseline_downloading_white
import sonique.composeapp.generated.resources.baseline_people_alt_24
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.baseline_queue_music_24
import sonique.composeapp.generated.resources.baseline_sensors_24
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.cancel
import sonique.composeapp.generated.resources.delete
import sonique.composeapp.generated.resources.delete_song_from_playlist
import sonique.composeapp.generated.resources.download
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.downloading
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.no_album
import sonique.composeapp.generated.resources.outline_download_for_offline_24
import sonique.composeapp.generated.resources.play_circle
import sonique.composeapp.generated.resources.play_next
import sonique.composeapp.generated.resources.playback_speed_pitch
import sonique.composeapp.generated.resources.radio
import sonique.composeapp.generated.resources.round_speed_24
import sonique.composeapp.generated.resources.share
import sonique.composeapp.generated.resources.sleep_timer
import sonique.composeapp.generated.resources.sleep_timer_off
import sonique.composeapp.generated.resources.sleep_timer_warning
import sonique.composeapp.generated.resources.start_radio
import sonique.composeapp.generated.resources.warning
import sonique.composeapp.generated.resources.yes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel = koinViewModel(),
    setSleepTimerEnable: Boolean = false,
    onNavigateToOtherScreen: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onLibraryDelete: (() -> Unit)? = null,
    dataStoreManager: DataStoreManager = koinInject<DataStoreManager>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    var addToAPlaylist by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf(false) }

    var sleepTimer by remember {
        mutableStateOf(false)
    }
    var sleepTimerWarning by remember {
        mutableStateOf(false)
    }
    var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
    var changePlaybackSpeedPitch by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState.songUIState.videoId.isNotEmpty() && !isBottomSheetVisible) {
            isBottomSheetVisible = true
        }
    }

    LaunchedEffect(key1 = song) {
        viewModel.setSongEntity(song)
    }

    if (changePlaybackSpeedPitch) {
        val playbackSpeed by dataStoreManager.playbackSpeed.collectAsState(1f)
        val pitch by dataStoreManager.pitch.collectAsState(0)
        PlaybackSpeedPitchBottomSheet(
            onDismiss = { changePlaybackSpeedPitch = false },
            playbackSpeed = playbackSpeed,
            pitch = pitch,
        ) { speed, p ->
            viewModel.onUIEvent(
                NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch(
                    speed = speed,
                    pitch = p,
                ),
            )
        }
    }

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
            videoId = uiState.songUIState.videoId,
        )
    }
    if (artist) {
        ArtistModalBottomSheet(
            isBottomSheetVisible = true,
            artists = uiState.songUIState.listArtists,
            navController = navController,
            onNavigateToOtherScreen = onNavigateToOtherScreen,
            onDismiss = { artist = false },
        )
    }

    if (sleepTimer) {
        SleepTimerBottomSheet(onDismiss = { sleepTimer = false }) { minutes: Int ->
            if (setSleepTimerEnable) {
                viewModel.onUIEvent(
                    NowPlayingBottomSheetUIEvent.SetSleepTimer(
                        cancel = false,
                        minutes = minutes,
                    ),
                )
            }
        }
    }

    if (sleepTimerWarning) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            onDismissRequest = { sleepTimerWarning = false },
            confirmButton = {
                TextButton(onClick = {
                    sleepTimerWarning = false
                    viewModel.onUIEvent(
                        NowPlayingBottomSheetUIEvent.SetSleepTimer(
                            cancel = true,
                        ),
                    )
                }) {
                    Text(text = stringResource(Res.string.yes), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepTimerWarning = false }) {
                    Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = {
                Text(text = stringResource(Res.string.warning), style = typo().labelSmall)
            },
            text = {
                Text(text = stringResource(Res.string.sleep_timer_warning), style = typo().bodyMedium)
            },
        )
    }

    if (showCancelDownloadDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            onDismissRequest = { showCancelDownloadDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDownloadDialog = false
                    viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                }) {
                    Text(text = stringResource(Res.string.yes), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDownloadDialog = false }) {
                    Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = {
                Text(text = "Warning", style = typo().labelSmall)
            },
            text = {
                val text = if (uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED) "Do you want to remove this download?" else "Do you want to cancel the download?"
                Text(text = text, style = typo().bodyMedium)
            },
        )
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = md_theme_dark_background.copy(alpha = .5f),
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
                    modifier = Modifier.verticalScroll(rememberScrollState()),
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
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = uiState.songUIState.thumbnails
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterResource(Res.drawable.holder),
                            error = painterResource(Res.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.Inside,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterVertically)
                                    .clip(
                                        RoundedCornerShape(10.dp),
                                    ).size(60.dp),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = uiState.songUIState.title,
                                style = typo().labelMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically).focusable(),
                            )
                            Text(
                                text =
                                    uiState.songUIState.listArtists
                                        .toListName()
                                        .connectArtists(),
                                style = typo().bodyMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically).focusable(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    HorizontalDivider(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        thickness = 1.dp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Crossfade(targetState = onDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_delete_24),
                                text = Res.string.delete_song_from_playlist,
                            ) {
                                hideModalBottomSheet()
                                onDelete?.invoke()
                            }
                        }
                    }
                    Crossfade(targetState = onLibraryDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_delete_24),
                                text = Res.string.delete,
                            ) {
                                hideModalBottomSheet()
                                onLibraryDelete?.invoke()
                            }
                        }
                    }
                    CheckBoxActionButton(
                        defaultChecked = uiState.songUIState.liked,
                        isHeartIcon = true,
                        onChangeListener = {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        },
                    )

                    ActionButton(
                        icon =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED ->
                                    painterResource(
                                        Res.drawable.outline_download_for_offline_24,
                                    )

                                DownloadState.STATE_DOWNLOADING,
                                DownloadState.STATE_PREPARING ->
                                    painterResource(
                                        Res.drawable.baseline_downloading_white,
                                    )

                                DownloadState.STATE_DOWNLOADED ->
                                    painterResource(
                                        Res.drawable.baseline_downloaded,
                                    )

                                else ->
                                    painterResource(
                                        Res.drawable.outline_download_for_offline_24,
                                    )
                            },
                        text =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED -> Res.string.download
                                DownloadState.STATE_DOWNLOADING,
                                DownloadState.STATE_PREPARING -> Res.string.downloading
                                DownloadState.STATE_DOWNLOADED -> Res.string.downloaded
                                else -> Res.string.download
                            },
                    ) {
                        if (uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADING ||
                            uiState.songUIState.downloadState == DownloadState.STATE_PREPARING ||
                            uiState.songUIState.downloadState == DownloadState.STATE_DOWNLOADED
                        ) {
                            showCancelDownloadDialog = true
                        } else {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                        }
                    }

                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_playlist_add_24),
                        text = Res.string.add_to_a_playlist,
                    ) {
                        addToAPlaylist = true
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.play_circle),
                        text = Res.string.play_next,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_queue_music_24),
                        text = Res.string.add_to_queue,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_people_alt_24),
                        text = Res.string.artists,
                    ) {
                        artist = true
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_album_24),
                        text = if (uiState.songUIState.album == null) Res.string.no_album else null,
                        textString = uiState.songUIState.album?.name,
                        enable = uiState.songUIState.album != null,
                    ) {
                        uiState.songUIState.album?.id?.let { id ->
                            onNavigateToOtherScreen()
                            navController.navigate(
                                AlbumDestination(
                                    browseId = id,
                                ),
                            )
                        }
                    }
                    val radioString = stringResource(Res.string.radio)
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_sensors_24),
                        text = Res.string.start_radio,
                    ) {
                        viewModel.onUIEvent(
                            NowPlayingBottomSheetUIEvent.StartRadio(
                                videoId = uiState.songUIState.videoId,
                                name = "\"${uiState.songUIState.title}\" $radioString",
                            ),
                        )
                        hideModalBottomSheet()
                    }

                    Crossfade(targetState = setSleepTimerEnable) {
                        val sleepTimerState = uiState.sleepTimer
                        if (it) {
                            Crossfade(targetState = sleepTimerState.timeRemaining > 0) { running ->
                                if (running) {
                                    ActionButton(
                                        icon = painterResource(Res.drawable.baseline_access_alarm_24),
                                        textString = stringResource(Res.string.sleep_timer, sleepTimerState.timeRemaining.toString()),
                                        text = null,
                                        textColor = seed,
                                        iconColor = seed,
                                    ) {
                                        sleepTimerWarning = true
                                    }
                                } else {
                                    ActionButton(
                                        icon = painterResource(Res.drawable.baseline_access_alarm_24),
                                        text = Res.string.sleep_timer_off,
                                    ) {
                                        sleepTimer = true
                                    }
                                }
                            }
                        }
                    }
                    Crossfade(targetState = setSleepTimerEnable) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.round_speed_24),
                                text = Res.string.playback_speed_pitch,
                            ) {
                                changePlaybackSpeedPitch = true
                            }
                        }
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_share_24),
                        text = Res.string.share,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share)
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}
