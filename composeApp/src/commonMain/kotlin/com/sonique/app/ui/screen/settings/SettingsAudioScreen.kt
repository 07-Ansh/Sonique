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
import com.sonique.app.expect.ui.openEqResult
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.QUALITY
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
fun SettingsAudioScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val normalizeVolume by viewModel.normalizeVolume.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by viewModel.skipSilent.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)

    val resultLauncher = openEqResult(viewModel.getAudioSessionId())
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
                title = { Text("Audio") },
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
                    title = stringResource(Res.string.quality),
                    subtitle = quality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = getString(Res.string.quality),
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = QUALITY.items.map { item ->
                                            (item.toString() == quality) to item.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        viewModel.changeQuality(state.selectOne?.getSelected())
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.download_quality),
                    subtitle = downloadQuality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = getString(Res.string.download_quality),
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = QUALITY.items.map { item ->
                                            (item.toString() == downloadQuality) to item.toString()
                                        },
                                    ),
                                    confirm = getString(Res.string.change) to { state ->
                                        state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) }
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                if (getPlatform() == Platform.Android) {
                    SettingItem(
                        title = stringResource(Res.string.normalize_volume),
                        subtitle = stringResource(Res.string.balance_media_loudness),
                        switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.skip_silent),
                        subtitle = stringResource(Res.string.skip_no_music_part),
                        switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.open_system_equalizer),
                        subtitle = stringResource(Res.string.use_your_system_equalizer),
                        onClick = {
                            coroutineScope.launch {
                                resultLauncher.launch()
                            }
                        },
                    )
                }
            }
        }
    }
}
