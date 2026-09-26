package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.data.model.lyrics.RomanizationDictionaryState
import com.sonique.domain.data.model.lyrics.RomanizationLanguage
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.*

@Composable
internal fun PlaybackSettingsContent(
    viewModel: SettingsViewModel,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val savePlaybackStateFlow = remember(viewModel.savedPlaybackState) {
        viewModel.savedPlaybackState.map { it == TRUE }
    }
    val savePlaybackState by savePlaybackStateFlow.collectAsStateWithLifecycle(initialValue = false)

    val saveLastPlayedFlow = remember(viewModel.saveRecentSongAndQueue) {
        viewModel.saveRecentSongAndQueue.map { it == TRUE }
    }
    val saveLastPlayed by saveLastPlayedFlow.collectAsStateWithLifecycle(initialValue = false)

    val killServiceOnExitFlow = remember(viewModel.killServiceOnExit) {
        viewModel.killServiceOnExit.map { it == TRUE }
    }
    val killServiceOnExit by killServiceOnExitFlow.collectAsStateWithLifecycle(initialValue = true)
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()

    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDjMode by viewModel.crossfadeDjMode.collectAsStateWithLifecycle()
    val crossfadeSkipAlbum by viewModel.crossfadeSkipAlbum.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsProvider by viewModel.lyricsProvider.collectAsStateWithLifecycle()
    val lyricsAutoFallback by viewModel.lyricsAutoFallback.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val romanizationStored by sharedViewModel.getRomanizationLanguages().collectAsStateWithLifecycle("")
    val japaneseDictionaryState by viewModel.japaneseDictionaryState.collectAsStateWithLifecycle()

    val romanizationLabels =
        listOf(
            RomanizationLanguage.JAPANESE to stringResource(Res.string.romanization_japanese),
            RomanizationLanguage.KOREAN to stringResource(Res.string.romanization_korean),
            RomanizationLanguage.CHINESE to stringResource(Res.string.romanization_chinese),
            RomanizationLanguage.HINDI to stringResource(Res.string.romanization_hindi),
            RomanizationLanguage.PUNJABI to stringResource(Res.string.romanization_punjabi),
            RomanizationLanguage.RUSSIAN to stringResource(Res.string.romanization_russian),
            RomanizationLanguage.UKRAINIAN to stringResource(Res.string.romanization_ukrainian),
            RomanizationLanguage.SERBIAN to stringResource(Res.string.romanization_serbian),
            RomanizationLanguage.BULGARIAN to stringResource(Res.string.romanization_bulgarian),
            RomanizationLanguage.BELARUSIAN to stringResource(Res.string.romanization_belarusian),
            RomanizationLanguage.KYRGYZ to stringResource(Res.string.romanization_kyrgyz),
            RomanizationLanguage.MACEDONIAN to stringResource(Res.string.romanization_macedonian),
        )
    val romanizationSelected = RomanizationLanguage.parse(romanizationStored)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "State Preservation",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.save_playback_state)) },
                        description = { Text(stringResource(Res.string.save_shuffle_and_repeat_mode)) },
                        isSwitch = true,
                        checked = savePlaybackState,
                        onCheckedChange = { viewModel.setSavedPlaybackState(it) }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.save_last_played)) },
                        description = { Text(stringResource(Res.string.save_last_played_track_and_queue)) },
                        isSwitch = true,
                        checked = saveLastPlayed,
                        onCheckedChange = { viewModel.setSaveLastPlayed(it) }
                    )
                )
            )
        }

        if (getPlatform() == Platform.Android) {
            item {
                Material3SettingsGroup(
                    title = "Service Lifecycle",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.kill_service_on_exit)) },
                            description = { Text(stringResource(Res.string.kill_service_on_exit_description)) },
                            isSwitch = true,
                            checked = killServiceOnExit,
                            onCheckedChange = { viewModel.setKillServiceOnExit(it) }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.keep_service_alive)) },
                            description = { Text(stringResource(Res.string.keep_service_alive_description)) },
                            isSwitch = true,
                            checked = keepServiceAlive,
                            onCheckedChange = { viewModel.setKeepServiceAlive(it) }
                        )
                    )
                )
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

        item {
            val lyricsOffsetTitle = stringResource(Res.string.lyrics_offset)
            val lyricsOffsetMessage = stringResource(Res.string.lyrics_offset_message)
            val lyricsOffsetInvalid = stringResource(Res.string.lyrics_offset_invalid)
            val changeString = stringResource(Res.string.change)
            val cancelString = stringResource(Res.string.cancel)
            val saveString = stringResource(Res.string.save)
            val lyricsRomanizationTitle = stringResource(Res.string.lyrics_romanization)
            Material3SettingsGroup(
                title = "Lyrics Settings",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.main_lyrics_provider)) },
                        description = {
                            val label = when (lyricsProvider) {
                                DataStoreManager.BETTER_LYRICS -> stringResource(Res.string.better_lyrics)
                                DataStoreManager.YOUTUBE -> stringResource(Res.string.youtube_transcript)
                                DataStoreManager.SPOTIFY -> stringResource(Res.string.spotify)
                                else -> stringResource(Res.string.lrclib)
                            }
                            Text(label)
                        },
                        onClick = {
                            coroutineScope.launch {
                                val labelLrclib = getString(Res.string.lrclib)
                                val labelBetterLyrics = getString(Res.string.better_lyrics)
                                val labelYouTube = getString(Res.string.youtube_transcript)
                                val labelSpotify = getString(Res.string.spotify)
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.main_lyrics_provider),
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = listOf(
                                                (lyricsProvider == DataStoreManager.BETTER_LYRICS) to labelBetterLyrics,
                                                (lyricsProvider == DataStoreManager.LRCLIB) to labelLrclib,
                                                (lyricsProvider == DataStoreManager.YOUTUBE) to labelYouTube,
                                                (lyricsProvider == DataStoreManager.SPOTIFY) to labelSpotify,
                                            )
                                        ),
                                        confirm = getString(Res.string.change) to { state ->
                                            val sel = state.selectOne?.getSelected()
                                            val provider = when (sel) {
                                                labelBetterLyrics -> DataStoreManager.BETTER_LYRICS
                                                labelYouTube -> DataStoreManager.YOUTUBE
                                                labelSpotify -> DataStoreManager.SPOTIFY
                                                else -> DataStoreManager.LRCLIB
                                            }
                                            viewModel.setLyricsProvider(provider)
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text("Automatic Lyrics Fallback") },
                        description = { Text("If lyrics are missing from your preferred provider, automatically check other sources (BetterLyrics, LRCLIB, YouTube, Spotify)") },
                        isSwitch = true,
                        checked = lyricsAutoFallback,
                        onCheckedChange = { viewModel.setLyricsAutoFallback(it) }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.lyrics_offset)) },
                        description = {
                            Text(
                                stringResource(
                                    Res.string.lyrics_offset_value,
                                    if (lyricsOffsetMs > 0) "+$lyricsOffsetMs" else lyricsOffsetMs.toString(),
                                )
                            )
                        },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = lyricsOffsetTitle,
                                    message = lyricsOffsetMessage,
                                    textField =
                                        SettingAlertState.TextFieldData(
                                            label = lyricsOffsetTitle,
                                            value = lyricsOffsetMs.toString(),
                                            verifyCodeBlock = {
                                                (it.trim().toIntOrNull() != null) to lyricsOffsetInvalid
                                            },
                                        ),
                                    confirm =
                                        changeString to { state ->
                                            state.textField
                                                ?.value
                                                ?.trim()
                                                ?.toIntOrNull()
                                                ?.let { viewModel.setLyricsOffsetMs(it) }
                                        },
                                    dismiss = cancelString,
                                )
                            )
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.lyrics_romanization)) },
                        description = {
                            Text(
                                if (romanizationSelected.isEmpty()) {
                                    stringResource(Res.string.lyrics_romanization_description)
                                } else {
                                    val selectedNames =
                                        romanizationLabels.filter { it.first in romanizationSelected }.joinToString(", ") { it.second }
                                    when {
                                        RomanizationLanguage.JAPANESE !in romanizationSelected -> selectedNames
                                        japaneseDictionaryState == RomanizationDictionaryState.DOWNLOADING ->
                                            "$selectedNames — ${stringResource(Res.string.romanization_japanese_dict_downloading)}"
                                        japaneseDictionaryState == RomanizationDictionaryState.FAILED ->
                                            "$selectedNames — ${stringResource(Res.string.romanization_japanese_dict_failed)}"
                                        else -> selectedNames
                                    }
                                }
                            )
                        },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = lyricsRomanizationTitle,
                                    multipleSelect =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                                romanizationLabels.map { (language, label) ->
                                                    (language in romanizationSelected) to label
                                                },
                                        ),
                                    confirm =
                                        saveString to { state ->
                                            val chosen = state.multipleSelect?.getListSelected().orEmpty()
                                            val languages =
                                                romanizationLabels.filter { it.second in chosen }.map { it.first }.toSet()
                                            sharedViewModel.setRomanizationLanguages(languages)
                                            if (RomanizationLanguage.JAPANESE in languages) {
                                                viewModel.downloadJapaneseDictionaryIfNeeded()
                                            }
                                        },
                                    dismiss = cancelString,
                                ),
                            )
                        }
                    )
                )
            )
        }
    }
}
