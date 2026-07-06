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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.toKmpUri
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.sonique.app.expect.ui.fileSaverResult
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.component.SettingsSectionHeader
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.sonique.domain.extension.now
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCalfApi::class)
@Composable
fun SettingsBackupScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    val coroutineScope = rememberCoroutineScope()
    val pl = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val appName = stringResource(Res.string.app_name)

    val formatter = LocalDateTime.Format {
        year()
        monthNumber()
        day()
        hour()
        minute()
        second()
    }

    val backupLauncher = fileSaverResult(
        "${appName}_${now().format(formatter)}.backup",
        "application/octet-stream",
    ) { uri ->
        uri?.let {
            viewModel.backup(it.toKmpUri())
        }
    }

    val restoreLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.All,
        selectionMode = FilePickerSelectionMode.Single,
    ) { file ->
        file.firstOrNull()?.getPath(pl)?.toKmpUri()?.let {
            viewModel.restore(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Backup") },
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
                SettingsSectionHeader("Automation")
                SettingItem(
                    title = stringResource(Res.string.backup_downloaded),
                    subtitle = stringResource(Res.string.backup_downloaded_description),
                    switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                )
                SettingsSectionHeader("Manual Actions")
                SettingItem(
                    title = stringResource(Res.string.backup),
                    subtitle = when (backupState) {
                        is SettingsViewModel.BackupRestoreState.InProgress -> "Backing up data..."
                        is SettingsViewModel.BackupRestoreState.Success -> "✓ Backup complete!"
                        is SettingsViewModel.BackupRestoreState.Error -> "✗ Backup failed"
                        else -> stringResource(Res.string.save_all_your_playlist_data)
                    },
                    onClick = {
                        if (backupState !is SettingsViewModel.BackupRestoreState.InProgress) {
                            coroutineScope.launch {
                                backupLauncher.launch()
                            }
                        }
                    },
                    loading = backupState is SettingsViewModel.BackupRestoreState.InProgress
                )
                SettingItem(
                    title = stringResource(Res.string.restore_your_data),
                    subtitle = when (restoreState) {
                        is SettingsViewModel.BackupRestoreState.InProgress -> "Restoring data..."
                        is SettingsViewModel.BackupRestoreState.Success -> "✓ Restore complete!"
                        is SettingsViewModel.BackupRestoreState.Error -> "✗ Restore failed"
                        else -> stringResource(Res.string.restore_your_saved_data)
                    },
                    onClick = {
                        if (restoreState !is SettingsViewModel.BackupRestoreState.InProgress) {
                            coroutineScope.launch {
                                restoreLauncher.launch()
                            }
                        }
                    },
                    loading = restoreState is SettingsViewModel.BackupRestoreState.InProgress
                )
            }
        }
    }
}
