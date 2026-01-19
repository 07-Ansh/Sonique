package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.VIDEO_QUALITY
import com.sonique.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

import com.sonique.app.ui.component.SettingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsVideoScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    if (getPlatform() == Platform.Desktop) return

    val coroutineScope = rememberCoroutineScope()
    val videoQuality by viewModel.videoQuality.collectAsStateWithLifecycle()
    val videoDownloadQuality by viewModel.videoDownloadQuality.collectAsStateWithLifecycle()
    val playVideo by viewModel.playVideoInsteadOfAudio.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    if (alertData != null) {
        SettingDialog(
            alert = alertData!!,
            onDismiss = { viewModel.setAlertData(null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            item {
                SettingItem(
                    title = stringResource(Res.string.play_video_for_video_track_instead_of_audio_only),
                    subtitle = stringResource(Res.string.such_as_music_video_lyrics_video_podcasts_and_more),
                    smallSubtitle = true,
                    switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                    isEnable = getPlatform() != Platform.Desktop,
                )
                SettingItem(
                    title = stringResource(Res.string.video_quality),
                    subtitle = videoQuality ?: "",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = getString(Res.string.video_quality),
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = VIDEO_QUALITY.items.map { item ->
                                            (item.toString() == videoQuality) to item.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "")
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.video_download_quality),
                    subtitle = videoDownloadQuality ?: "",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = getString(Res.string.video_download_quality),
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = VIDEO_QUALITY.items.map { item ->
                                            (item.toString() == videoDownloadQuality) to item.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        viewModel.setVideoDownloadQuality(state.selectOne?.getSelected() ?: "")
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}
