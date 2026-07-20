package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.domain.manager.DataStoreManager
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

    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()
    var showChangelog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.update)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Material3SettingsGroup(
                    title = stringResource(Res.string.current_version),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.version_format, currentVersion)) },
                            description = { Text("OpenSource") }
                        )
                    )
                )
            }

            item {
                val updateSettingsItems = mutableListOf<Material3SettingsItem>()
                updateSettingsItems.add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.auto_check_for_update)) },
                        description = { Text("Check for updates automatically when opening the app") },
                        trailingContent = {
                            Switch(
                                checked = autoCheckForUpdates,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        dataStoreManager.setAutoCheckForUpdates(checked)
                                    }
                                }
                            )
                        }
                    )
                )
                if (autoCheckForUpdates) {
                    updateSettingsItems.add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.update_notifications)) },
                            trailingContent = {
                                Switch(
                                    checked = updateNotifications,
                                    onCheckedChange = { updateNotifications = it }
                                )
                            }
                        )
                    )
                }

                Material3SettingsGroup(
                    title = stringResource(Res.string.update_settings),
                    items = updateSettingsItems
                )
            }

            item {
                val titleText = when {
                    isChecking -> stringResource(Res.string.checking_for_updates)
                    updateAvailable != null -> "New update available!"
                    latestReleaseInfo != null -> stringResource(Res.string.latest_version_format, latestReleaseInfo!!.version)
                    else -> stringResource(Res.string.check_for_updates_button)
                }
                Material3SettingsGroup(
                    title = stringResource(Res.string.check_for_updates_title),
                    items = listOf(
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Default.Refresh),
                            title = { Text(titleText) },
                            onClick = {
                                if (!isChecking) {
                                    updateViewModel.manualCheckForUpdate()
                                }
                            },
                            enabled = !isChecking
                        )
                    )
                )
            }

            // Downloader controls / status
            if (updateAvailable != null) {
                item {
                    val installationItems = mutableListOf<Material3SettingsItem>()
                    when (downloadStatus) {
                        is SharedViewModel.DownloadStatus.Idle -> {
                            installationItems.add(
                                Material3SettingsItem(
                                    title = { Text("Download Update") },
                                    description = { Text("Click to start downloading the new version.") },
                                    onClick = {
                                        val release = updateAvailable!!
                                        sharedViewModel.downloadAppUpdate(release.downloadUrl, release.title)
                                    }
                                )
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloading -> {
                            val progress = (downloadStatus as SharedViewModel.DownloadStatus.Downloading).progress
                            installationItems.add(
                                Material3SettingsItem(
                                    title = { Text("Downloading... ${(progress * 100).toInt()}%") },
                                    description = {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                    },
                                    onClick = { sharedViewModel.cancelDownload() }
                                )
                            )
                        }
                        is SharedViewModel.DownloadStatus.Verifying -> {
                            installationItems.add(
                                Material3SettingsItem(
                                    title = { Text("Verifying update...") },
                                    description = {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                    }
                                )
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloaded -> {
                            val path = (downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path
                            installationItems.add(
                                Material3SettingsItem(
                                    title = { Text("Install Update") },
                                    description = { Text("Ready to install. Click to open installer.") },
                                    onClick = { sharedViewModel.installUpdate(path) }
                                )
                            )
                        }
                    }

                    Material3SettingsGroup(
                        title = "Installation",
                        items = installationItems
                    )
                }

                // Changelog
                val releaseToDisplay = updateAvailable ?: latestReleaseInfo
                if (releaseToDisplay != null) {
                    item {
                        Material3SettingsGroup(
                            title = "Release Notes",
                            items = listOf(
                                Material3SettingsItem(
                                    title = { Text(if (showChangelog) "Hide Changelog" else "View Changelog") },
                                    onClick = { showChangelog = !showChangelog }
                                )
                            )
                        )
                    }
                    if (showChangelog) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
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
                        }
                    }
                }
            }
        }
    }
}
