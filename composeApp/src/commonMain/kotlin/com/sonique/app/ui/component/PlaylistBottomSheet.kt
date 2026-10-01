package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonique.app.expect.shareUrl
import com.sonique.app.expect.ui.photoPickerResult
import com.sonique.app.extension.displayNameRes
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.typo
import com.sonique.domain.data.entities.LocalPlaylistEntity
import com.sonique.domain.data.model.searchResult.playlists.PlaylistsResult
import com.sonique.domain.repository.LocalPlaylistRepository
import com.sonique.domain.utils.FilterState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.add_to_queue
import sonique.composeapp.generated.resources.baseline_add_photo_alternate_24
import sonique.composeapp.generated.resources.baseline_delete_24
import sonique.composeapp.generated.resources.baseline_edit_24
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.baseline_queue_music_24
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.baseline_sync_24
import sonique.composeapp.generated.resources.baseline_sync_disabled_24
import sonique.composeapp.generated.resources.baseline_update_24
import sonique.composeapp.generated.resources.delete_playlist
import sonique.composeapp.generated.resources.done
import sonique.composeapp.generated.resources.edit_thumbnail
import sonique.composeapp.generated.resources.edit_title
import sonique.composeapp.generated.resources.no_playlist_found
import sonique.composeapp.generated.resources.playlist_name_cannot_be_empty
import sonique.composeapp.generated.resources.save
import sonique.composeapp.generated.resources.save_to_local_playlist
import sonique.composeapp.generated.resources.saved_to_local_playlist
import sonique.composeapp.generated.resources.share
import sonique.composeapp.generated.resources.share_url
import sonique.composeapp.generated.resources.sort_by
import sonique.composeapp.generated.resources.sync
import sonique.composeapp.generated.resources.sync_first
import sonique.composeapp.generated.resources.synced
import sonique.composeapp.generated.resources.title
import sonique.composeapp.generated.resources.update_playlist
import sonique.composeapp.generated.resources.your_playlists
import sonique.composeapp.generated.resources.your_youtube_playlists

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    listLocalPlaylist: List<LocalPlaylistEntity>,
    listYouTubePlaylist: List<PlaylistsResult>,
    videoId: String? = null,
    onClick: (LocalPlaylistEntity) -> Unit,
    onYTPlaylistClick: (PlaylistsResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
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
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = BottomSheetDefaults.ExpandedShape,
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp),
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

                    val chipRowState = rememberScrollState()
                    var isYouTubePlaylistClicked by remember {
                        mutableStateOf(false)
                    }
                    if (listYouTubePlaylist.isNotEmpty()) {
                        Row(
                            modifier =
                                Modifier
                                    .horizontalScroll(chipRowState)
                                    .padding(horizontal = 15.dp)
                                    .padding(vertical = 8.dp)
                                    .background(Color.Transparent),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Chip(
                                isAnimated = false,
                                isSelected = !isYouTubePlaylistClicked,
                                text = stringResource(Res.string.your_playlists),
                                onClick = {
                                    isYouTubePlaylistClicked = false
                                },
                            )
                            Chip(
                                isAnimated = false,
                                isSelected = isYouTubePlaylistClicked,
                                text = stringResource(Res.string.your_youtube_playlists),
                                onClick = {
                                    isYouTubePlaylistClicked = true
                                },
                            )
                        }
                    }

                    if ((listLocalPlaylist.isEmpty() && !isYouTubePlaylistClicked) ||
                        (listYouTubePlaylist.isEmpty() && isYouTubePlaylistClicked)
                    ) {
                        Text(
                            text = stringResource(Res.string.no_playlist_found),
                            style = typo().labelSmall,
                            modifier = Modifier.padding(20.dp),
                            color = Color.Gray,
                        )
                    } else {
                        Crossfade(isYouTubePlaylistClicked) { clicked ->
                            if (clicked) {
                                LazyColumn {
                                    items(listYouTubePlaylist) { playlist ->
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable(
                                                        enabled = true,
                                                        onClick = {
                                                            onYTPlaylistClick(playlist)
                                                            hideModalBottomSheet()
                                                        },
                                                    ),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier =
                                                    Modifier
                                                        .padding(12.dp)
                                                        .align(Alignment.CenterStart),
                                            ) {
                                                Image(
                                                    painter =
                                                        painterResource(
                                                            Res.drawable.baseline_playlist_add_24,
                                                        ),
                                                    contentDescription = "",
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = playlist.title,
                                                    style = typo().labelSmall,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn {
                                    items(listLocalPlaylist) { playlist ->
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable(
                                                        enabled = playlist.tracks?.contains(videoId) != true,
                                                        onClick = {
                                                            onClick(playlist)
                                                            hideModalBottomSheet()
                                                        },
                                                    ),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier =
                                                    Modifier
                                                        .padding(12.dp)
                                                        .align(Alignment.CenterStart),
                                            ) {
                                                Crossfade(
                                                    targetState = playlist.tracks?.contains(videoId) == true,
                                                ) {
                                                    if (it) {
                                                        Image(
                                                            painter = painterResource(Res.drawable.done),
                                                            contentDescription = "",
                                                        )
                                                    } else {
                                                        Image(
                                                            painter =
                                                                painterResource(
                                                                    Res.drawable.baseline_playlist_add_24,
                                                                ),
                                                            contentDescription = "",
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = playlist.title,
                                                    style = typo().labelSmall,
                                                    color = if (playlist.tracks?.contains(videoId) == true) Color.Gray else Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheet(
    onDismiss: () -> Unit,
    playlistId: String,
    playlistName: String,
    isYourYouTubePlaylist: Boolean,
    onEditTitle: (newTitle: String) -> Unit = {},
    onSaveToLocal: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    localPlaylistRepository: LocalPlaylistRepository = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var isSavedToLocal by remember { mutableStateOf(false) }
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
    var showEditTitle by remember { mutableStateOf(false) }
    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(playlistName) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showEditTitleSheetState.hide()
                    onDismiss()
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
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
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.title))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    val playlistNameError = stringResource(Res.string.playlist_name_cannot_be_empty)
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                SoniqueToastManager.show(playlistNameError)
                            } else {
                                onEditTitle(newTitle)
                                hideEditTitleBottomSheet()
                                hideModalBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }

    LaunchedEffect(true) {
        localPlaylistRepository.getAllLocalPlaylists().collect {
            isSavedToLocal = it.any { playlist -> playlist.youtubePlaylistId == playlistId }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0) },
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
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                if (onAddToQueue != null) {
                    ActionButton(icon = painterResource(Res.drawable.baseline_queue_music_24), text = Res.string.add_to_queue) {
                        onAddToQueue()
                        hideModalBottomSheet()
                    }
                }
                if (isYourYouTubePlaylist) {
                    ActionButton(icon = painterResource(Res.drawable.baseline_edit_24), text = Res.string.edit_title) {
                        showEditTitle = true
                    }
                    ActionButton(
                        icon =
                            if (isSavedToLocal) {
                                painterResource(Res.drawable.baseline_sync_disabled_24)
                            } else {
                                painterResource(Res.drawable.baseline_sync_24)
                            },
                        text =
                            if (isSavedToLocal) {
                                Res.string.saved_to_local_playlist
                            } else {
                                Res.string.save_to_local_playlist
                            },
                        enable = !isSavedToLocal,
                    ) {
                        onSaveToLocal.invoke()
                        hideModalBottomSheet()
                    }
                }
                val shareTitle = stringResource(Res.string.share)
                ActionButton(
                    icon = painterResource(Res.drawable.baseline_share_24),
                    text = Res.string.share,
                ) {
                    val url = "https://music.youtube.com/playlist?list=${
                        playlistId.replaceFirst(
                            "VL",
                            "",
                        )
                    }"
                    shareUrl(shareTitle, url)
                }
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistBottomSheet(
    isBottomSheetVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    ytPlaylistId: String? = null,
    onEditTitle: (newTitle: String) -> Unit,
    onEditThumbnail: (newThumbnailUri: String) -> Unit,
    onAddToQueue: () -> Unit,
    onSync: () -> Unit,
    onUpdatePlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditTitle by remember { mutableStateOf(false) }
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
    val resultLauncher =
        photoPickerResult {
            it?.let {
                onEditThumbnail(it)
            }
        }
    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(title) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showEditTitleSheetState.hide()
                    onDismiss()
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
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
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.title))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    val playlistNameError = stringResource(Res.string.playlist_name_cannot_be_empty)
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                SoniqueToastManager.show(playlistNameError)
                            } else {
                                onEditTitle(newTitle)
                                hideEditTitleBottomSheet()
                                hideModalBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
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
                    ActionButton(icon = painterResource(Res.drawable.baseline_edit_24), text = Res.string.edit_title) {
                        showEditTitle = true
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_add_photo_alternate_24), text = Res.string.edit_thumbnail) {
                        resultLauncher.launch()
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_queue_music_24), text = Res.string.add_to_queue) {
                        onAddToQueue()
                    }
                    ActionButton(
                        icon =
                            if (ytPlaylistId != null) {
                                painterResource(Res.drawable.baseline_sync_disabled_24)
                            } else {
                                painterResource(Res.drawable.baseline_sync_24)
                            },
                        text =
                            if (ytPlaylistId != null) {
                                Res.string.synced
                            } else {
                                Res.string.sync
                            },
                    ) {
                        onSync()
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_update_24),
                        text = Res.string.update_playlist,
                        enable = (ytPlaylistId != null),
                    ) {
                        onUpdatePlaylist()
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_delete_24), text = Res.string.delete_playlist) {
                        onDelete()
                        hideModalBottomSheet()
                    }
                    val shareTitle = stringResource(Res.string.share_url)
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_share_24),
                        text = if (ytPlaylistId != null) Res.string.share else Res.string.sync_first,
                        enable = (ytPlaylistId != null),
                    ) {
                        val url = "https://music.youtube.com/playlist?list=${
                            ytPlaylistId?.replaceFirst(
                                "VL",
                                "",
                            )
                        }"
                        shareUrl(shareTitle, url)
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortPlaylistBottomSheet(
    selectedState: FilterState,
    onDismiss: () -> Unit,
    onSortChanged: (FilterState) -> Unit,
) {
    val modelBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filterOptions =
        remember {
            listOf(
                FilterState.CustomOrder,
                FilterState.NewerFirst,
                FilterState.OlderFirst,
                FilterState.Title,
            )
        }

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
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Text(
                    stringResource(Res.string.sort_by),
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(start = 16.dp, top = 16.dp, bottom = 24.dp)
                            .align(Alignment.Start),
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(filterOptions, key = { filterOption -> filterOption.hashCode() }) { filterOption ->
                        val isSelected = filterOption == selectedState
                        Row(
                            Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSortChanged(filterOption)
                                    onDismiss()
                                }.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(filterOption.displayNameRes()),
                                style = typo().labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) seed else Color.White,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Image(
                                    painter = painterResource(Res.drawable.done),
                                    contentDescription = "Selected",
                                    colorFilter = ColorFilter.tint(seed),
                                    modifier = Modifier.size(32.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
                EndOfModalBottomSheet()
            }
        }
    }
}
