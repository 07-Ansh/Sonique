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
import com.eygraber.uri.toKmpUri
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.sonique.app.expect.ui.fileSaverResult
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.viewModel.SettingsViewModel
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
    val coroutineScope = rememberCoroutineScope()
    val pl = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val appName = stringResource(Res.string.app_name)

    val formatter = LocalDateTime.Format {
        byUnicodePattern("yyyyMMddHHmmss")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
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
                    title = stringResource(Res.string.backup_downloaded),
                    subtitle = stringResource(Res.string.backup_downloaded_description),
                    switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                )
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
