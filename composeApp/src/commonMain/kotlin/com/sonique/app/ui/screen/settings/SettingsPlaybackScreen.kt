package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaybackScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val savePlaybackState by viewModel.savedPlaybackState.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val saveLastPlayed by viewModel.saveRecentSongAndQueue.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by viewModel.killServiceOnExit.map { it == DataStoreManager.Values.TRUE }.collectAsStateWithLifecycle(initialValue = true)
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()

    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback") },
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
                    title = stringResource(Res.string.save_playback_state),
                    subtitle = stringResource(Res.string.save_shuffle_and_repeat_mode),
                    switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.save_last_played),
                    subtitle = stringResource(Res.string.save_last_played_track_and_queue),
                    switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                )
                if (getPlatform() == Platform.Android) {
                    SettingItem(
                        title = stringResource(Res.string.kill_service_on_exit),
                        subtitle = stringResource(Res.string.kill_service_on_exit_description),
                        switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.keep_service_alive),
                        subtitle = stringResource(Res.string.keep_service_alive_description),
                        switch = (keepServiceAlive to { viewModel.setKeepServiceAlive(it) }),
                    )
                }
            }
        }
    }
}
