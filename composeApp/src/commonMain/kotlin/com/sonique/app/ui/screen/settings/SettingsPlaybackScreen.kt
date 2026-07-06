package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.component.SettingsSectionHeader
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaybackScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    val savePlaybackStateFlow = remember(viewModel.savedPlaybackState) {
        viewModel.savedPlaybackState.map { it == DataStoreManager.Values.TRUE }
    }
    val savePlaybackState by savePlaybackStateFlow.collectAsStateWithLifecycle(initialValue = false)

    val saveLastPlayedFlow = remember(viewModel.saveRecentSongAndQueue) {
        viewModel.saveRecentSongAndQueue.map { it == DataStoreManager.Values.TRUE }
    }
    val saveLastPlayed by saveLastPlayedFlow.collectAsStateWithLifecycle(initialValue = false)

    val killServiceOnExitFlow = remember(viewModel.killServiceOnExit) {
        viewModel.killServiceOnExit.map { it == DataStoreManager.Values.TRUE }
    }
    val killServiceOnExit by killServiceOnExitFlow.collectAsStateWithLifecycle(initialValue = true)
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()


    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Playback") },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(
                            if (enableLiquidGlass) {
                                Modifier.liquidGlass(backdrop, shape = CircleShape, interactive = true)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            item {
                SettingsSectionHeader("State Preservation")
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
                    SettingsSectionHeader("Service Lifecycle")
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
