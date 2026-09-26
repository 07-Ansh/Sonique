package com.sonique.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.expect.ui.openEqResult
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.screen.home.DelaySection
import com.sonique.app.ui.screen.home.ReverbSection
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.QUALITY
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@Composable
internal fun AudioSettingsContent(viewModel: SettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()

    val normalizeVolumeFlow = remember(viewModel.normalizeVolume) {
        viewModel.normalizeVolume.map { it == TRUE }
    }
    val normalizeVolume by normalizeVolumeFlow.collectAsStateWithLifecycle(initialValue = false)

    val skipSilentFlow = remember(viewModel.skipSilent) {
        viewModel.skipSilent.map { it == TRUE }
    }
    val skipSilent by skipSilentFlow.collectAsStateWithLifecycle(initialValue = false)
    val delayEnabled by viewModel.delayEnabled.collectAsStateWithLifecycle()
    val reverbEnabled by viewModel.reverbEnabled.collectAsStateWithLifecycle()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDjMode by viewModel.crossfadeDjMode.collectAsStateWithLifecycle()
    val crossfadeSkipAlbum by viewModel.crossfadeSkipAlbum.collectAsStateWithLifecycle()
    val resultLauncher = openEqResult(viewModel.getAudioSessionId())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Quality Settings",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.quality)) },
                        description = { Text(quality ?: "") },
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
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.download_quality)) },
                        description = { Text(downloadQuality ?: "") },
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
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Audio Effects & Playback",
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.normalize_volume)) },
                            description = { Text(stringResource(Res.string.balance_media_loudness)) },
                            isSwitch = true,
                            checked = normalizeVolume,
                            onCheckedChange = { viewModel.setNormalizeVolume(it) }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.skip_silent)) },
                            description = { Text(stringResource(Res.string.skip_no_music_part)) },
                            isSwitch = true,
                            checked = skipSilent,
                            onCheckedChange = { viewModel.setSkipSilent(it) }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.audio_delay)) },
                            description = { Text(stringResource(Res.string.audio_delay_description)) },
                            isSwitch = true,
                            checked = delayEnabled,
                            onCheckedChange = { viewModel.setDelayEnabled(it) }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.audio_reverb)) },
                            description = { Text(stringResource(Res.string.audio_reverb_description)) },
                            isSwitch = true,
                            checked = reverbEnabled,
                            onCheckedChange = { viewModel.setReverbEnabled(it) }
                        )
                    )
                    if (getPlatform() == Platform.Android) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.open_system_equalizer)) },
                                description = { Text(stringResource(Res.string.use_your_system_equalizer)) },
                                onClick = {
                                    coroutineScope.launch {
                                        resultLauncher.launch()
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }

        item {
            AnimatedVisibility(visible = delayEnabled) {
                DelaySection(viewModel)
            }
        }

        item {
            AnimatedVisibility(visible = reverbEnabled) {
                ReverbSection(viewModel)
            }
        }

        item {
            CrossfadeSettingsGroup(
                viewModel = viewModel,
                crossfadeEnabled = crossfadeEnabled,
                crossfadeDuration = crossfadeDuration,
                crossfadeDjMode = crossfadeDjMode,
                crossfadeSkipAlbum = crossfadeSkipAlbum,
            )
        }
    }
}

@Composable
internal fun CrossfadeSettingsGroup(
    viewModel: SettingsViewModel,
    crossfadeEnabled: Boolean,
    crossfadeDuration: Int,
    crossfadeDjMode: Boolean,
    crossfadeSkipAlbum: Boolean,
) {
    val crossfadeDurationTitle = stringResource(Res.string.crossfade_duration)
    val crossfadeAutoString = stringResource(Res.string.crossfade_auto)
    val changeString = stringResource(Res.string.change)
    val cancelString = stringResource(Res.string.cancel)
    Material3SettingsGroup(
        title = "Crossfade Settings",
        items = buildList {
            add(
                Material3SettingsItem(
                    title = { Text(stringResource(Res.string.crossfade)) },
                    description = { Text(stringResource(Res.string.crossfade_description)) },
                    isSwitch = true,
                    checked = crossfadeEnabled,
                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                )
            )
            if (crossfadeEnabled) {
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.crossfade_duration)) },
                        description = {
                            Text(
                                if (crossfadeDuration == DataStoreManager.Values.CROSSFADE_DURATION_AUTO) {
                                    stringResource(Res.string.crossfade_auto)
                                } else {
                                    "${crossfadeDuration / 1000}s"
                                }
                            )
                        },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = crossfadeDurationTitle,
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = listOf(
                                            (crossfadeDuration == DataStoreManager.Values.CROSSFADE_DURATION_AUTO) to
                                                    crossfadeAutoString,
                                            (crossfadeDuration == 1000) to "1s",
                                            (crossfadeDuration == 2000) to "2s",
                                            (crossfadeDuration == 3000) to "3s",
                                            (crossfadeDuration == 5000) to "5s",
                                            (crossfadeDuration == 8000) to "8s",
                                            (crossfadeDuration == 10000) to "10s",
                                            (crossfadeDuration == 12000) to "12s",
                                            (crossfadeDuration == 15000) to "15s",
                                            (crossfadeDuration == 20000) to "20s",
                                            (crossfadeDuration == 30000) to "30s",
                                        ),
                                    ),
                                    confirm = changeString to { state ->
                                        val duration = when (state.selectOne?.getSelected()) {
                                            crossfadeAutoString -> DataStoreManager.Values.CROSSFADE_DURATION_AUTO
                                            "1s" -> 1000
                                            "2s" -> 2000
                                            "3s" -> 3000
                                            "5s" -> 5000
                                            "8s" -> 8000
                                            "10s" -> 10000
                                            "12s" -> 12000
                                            "15s" -> 15000
                                            "20s" -> 20000
                                            "30s" -> 30000
                                            else -> 5000
                                        }
                                        viewModel.setCrossfadeDuration(duration)
                                    },
                                    dismiss = cancelString,
                                )
                            )
                        }
                    )
                )
                if (getPlatform() == Platform.Android) {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.crossfade_dj_mode)) },
                            description = { Text(stringResource(Res.string.crossfade_dj_mode_description)) },
                            isSwitch = true,
                            checked = crossfadeDjMode,
                            onCheckedChange = { viewModel.setCrossfadeDjMode(it) }
                        )
                    )
                }
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.crossfade_skip_album)) },
                        description = { Text(stringResource(Res.string.crossfade_skip_album_description)) },
                        isSwitch = true,
                        checked = crossfadeSkipAlbum,
                        onCheckedChange = { viewModel.setCrossfadeSkipAlbum(it) }
                    )
                )
            }
        }
    )
}
