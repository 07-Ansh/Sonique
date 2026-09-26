package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.toKmpUri
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.core.LocalPlatformContext as CalfPlatformContext
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.sonique.app.expect.ui.fileSaverResult
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.domain.extension.now
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalCalfApi::class)
@Composable
internal fun BackupSettingsContent(viewModel: SettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val pl = CalfPlatformContext.current
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val appName = stringResource(Res.string.app_name)

    val formatter = LocalDateTime.Format {
        year(); monthNumber(); day(); hour(); minute(); second()
    }

    val backupLauncher = fileSaverResult(
        "${appName}_${now().format(formatter)}.backup",
        "application/octet-stream",
    ) { uri ->
        uri?.let { viewModel.backup(it.toKmpUri()) }
    }

    val restoreLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.All,
        selectionMode = FilePickerSelectionMode.Single,
    ) { file ->
        file.firstOrNull()?.getPath(pl)?.toKmpUri()?.let { viewModel.restore(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Automation",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.backup_downloaded)) },
                        description = { Text(stringResource(Res.string.backup_downloaded_description)) },
                        isSwitch = true,
                        checked = backupDownloaded,
                        onCheckedChange = { viewModel.setBackupDownloaded(it) }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Manual Actions",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.backup)) },
                        description = {
                            Text(
                                when (backupState) {
                                    is SettingsViewModel.BackupRestoreState.InProgress -> "Backing up data..."
                                    is SettingsViewModel.BackupRestoreState.Success -> "\u2713 Backup complete!"
                                    is SettingsViewModel.BackupRestoreState.Error -> "\u2717 Backup failed"
                                    else -> stringResource(Res.string.save_all_your_playlist_data)
                                }
                            )
                        },
                        onClick = {
                            if (backupState !is SettingsViewModel.BackupRestoreState.InProgress) {
                                coroutineScope.launch { backupLauncher.launch() }
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.restore_your_data)) },
                        description = {
                            Text(
                                when (restoreState) {
                                    is SettingsViewModel.BackupRestoreState.InProgress -> "Restoring data..."
                                    is SettingsViewModel.BackupRestoreState.Success -> "\u2713 Restore complete!"
                                    is SettingsViewModel.BackupRestoreState.Error -> "\u2717 Restore failed"
                                    else -> stringResource(Res.string.restore_your_saved_data)
                                }
                            )
                        },
                        onClick = {
                            if (restoreState !is SettingsViewModel.BackupRestoreState.InProgress) {
                                coroutineScope.launch { restoreLauncher.launch() }
                            }
                        }
                    )
                )
            )
        }
    }
}
