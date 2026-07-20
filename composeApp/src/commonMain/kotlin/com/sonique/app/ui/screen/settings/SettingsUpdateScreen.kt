package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.domain.manager.DataStoreManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUpdateScreen(
    onBack: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(),
) {
    val dataStoreManager: DataStoreManager = koinInject()
    val autoCheckForUpdatesString by dataStoreManager.autoCheckForUpdates.collectAsStateWithLifecycle(initialValue = "TRUE")
    val autoCheckForUpdates = autoCheckForUpdatesString == "TRUE"
    val coroutineScope = rememberCoroutineScope()
    var updateNotifications by remember { mutableStateOf(true) }

    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsStateWithLifecycle()
    val currentVersion = updateViewModel.currentVersion
    val downloadStatus by sharedViewModel.downloadStatus.collectAsStateWithLifecycle()
    val isChecking by updateViewModel.isChecking.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.updater)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))

                // Group 1: Current Version
                Material3SettingsGroup(
                    title = stringResource(Res.string.current_version),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(Res.drawable.baseline_info_24),
                            title = {
                                Text(stringResource(Res.string.version_format, currentVersion))
                            },
                            description = {
                                Text("Build ${com.sonique.app.utils.VersionManager.getVersionCode()}")
                            }
                        )
                    )
                )
            }

            item {
                // Group 2: Update settings
                Material3SettingsGroup(
                    title = stringResource(Res.string.update_settings),
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.auto_check_for_update)) },
                                trailingContent = {
                                    Switch(
                                        checked = autoCheckForUpdates,
                                        onCheckedChange = { checked ->
                                            coroutineScope.launch {
                                                dataStoreManager.setAutoCheckForUpdates(checked)
                                            }
                                        }
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        dataStoreManager.setAutoCheckForUpdates(!autoCheckForUpdates)
                                    }
                                }
                            )
                        )

                        if (autoCheckForUpdates) {
                            add(
                                Material3SettingsItem(
                                    title = { Text(stringResource(Res.string.update_notifications)) },
                                    trailingContent = {
                                        Switch(
                                            checked = updateNotifications,
                                            onCheckedChange = { updateNotifications = it }
                                        )
                                    },
                                    onClick = { updateNotifications = !updateNotifications }
                                )
                            )
                        }
                    }
                )
            }

            item {
                // Group 3: Check for updates
                Material3SettingsGroup(
                    title = stringResource(Res.string.check_for_updates_title),
                    items = listOf(
                        Material3SettingsItem(
                            title = {
                                when {
                                    isChecking -> Text(stringResource(Res.string.checking_for_updates))
                                    updateAvailable != null -> Text("New update available!")
                                    latestReleaseInfo != null -> Text(stringResource(Res.string.latest_version_format, latestReleaseInfo!!.version))
                                    else -> Text(stringResource(Res.string.check_for_updates_button))
                                }
                            },
                            trailingContent = {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (updateAvailable != null) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(Res.string.update_available_title),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                if (!isChecking) {
                                    updateViewModel.manualCheckForUpdate()
                                }
                            }
                        )
                    )
                )
            }

            // Downloader controls / status
            if (updateAvailable != null) {
                item {
                    Material3SettingsGroup(
                        title = "Installation",
                        items = listOf(
                            when (downloadStatus) {
                                is SharedViewModel.DownloadStatus.Idle -> {
                                    Material3SettingsItem(
                                        title = { Text("Download Update") },
                                        description = { Text("Click to start downloading the new version.") },
                                        onClick = {
                                            val release = updateAvailable!!
                                            sharedViewModel.downloadAppUpdate(release.downloadUrl, release.title)
                                        }
                                    )
                                }
                                is SharedViewModel.DownloadStatus.Downloading -> {
                                    val progress = (downloadStatus as SharedViewModel.DownloadStatus.Downloading).progress
                                    Material3SettingsItem(
                                        title = { Text("Downloading... ${(progress * 100).toInt()}%") },
                                        description = {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { sharedViewModel.cancelDownload() }) {
                                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                            }
                                        }
                                    )
                                }
                                is SharedViewModel.DownloadStatus.Verifying -> {
                                    Material3SettingsItem(
                                        title = { Text("Verifying update...") },
                                        description = {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                            )
                                        }
                                    )
                                }
                                is SharedViewModel.DownloadStatus.Downloaded -> {
                                    val path = (downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path
                                    Material3SettingsItem(
                                        title = { Text("Install Update") },
                                        description = { Text("Ready to install. Click to open installer.") },
                                        onClick = { sharedViewModel.installUpdate(path) }
                                    )
                                }
                            }
                        )
                    )
                }

                // Changelog Group
                val releaseToDisplay = updateAvailable ?: latestReleaseInfo
                if (releaseToDisplay != null) {
                    item {
                        var showChangelog by remember { mutableStateOf(false) }
                        Material3SettingsGroup(
                            title = "Release Notes",
                            items = buildList {
                                add(
                                    Material3SettingsItem(
                                        title = { Text(if (showChangelog) "Hide Changelog" else "View Changelog") },
                                        onClick = { showChangelog = !showChangelog }
                                    )
                                )
                                if (showChangelog) {
                                    add(
                                        Material3SettingsItem(
                                            title = {
                                                Markdown(
                                                    content = releaseToDisplay.changelog,
                                                    colors = markdownColor(
                                                        text = MaterialTheme.colorScheme.onSurface,
                                                        codeBackground = MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    typography = markdownTypography(
                                                        text = MaterialTheme.typography.bodyMedium
                                                    )
                                                )
                                            }
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
