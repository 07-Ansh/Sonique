package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.component.SettingsSectionHeader
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
    var showChangelog by remember { mutableStateOf(false) }

    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsStateWithLifecycle()
    val currentVersion = updateViewModel.currentVersion
    val downloadStatus by sharedViewModel.downloadStatus.collectAsStateWithLifecycle()
    val isChecking by updateViewModel.isChecking.collectAsStateWithLifecycle()

    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.updater)
                    )
                },
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
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                SettingsSectionHeader(stringResource(Res.string.current_version))
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.version_format, currentVersion),
                    subtitle = "OpenSource"
                )
            }

            item {
                SettingsSectionHeader(stringResource(Res.string.update_settings))
            }
            item {
                SettingItem(
                    title = stringResource(Res.string.auto_check_for_update),
                    subtitle = "Check for updates automatically when opening the app",
                    switch = autoCheckForUpdates to { checked ->
                        coroutineScope.launch {
                            dataStoreManager.setAutoCheckForUpdates(checked)
                        }
                    }
                )
            }
            if (autoCheckForUpdates) {
                item {
                    SettingItem(
                        title = stringResource(Res.string.update_notifications),
                        switch = updateNotifications to { updateNotifications = it }
                    )
                }
            }

            item {
                SettingsSectionHeader(stringResource(Res.string.check_for_updates_title))
            }
            item {
                val titleText = when {
                    isChecking -> stringResource(Res.string.checking_for_updates)
                    updateAvailable != null -> "New update available!"
                    latestReleaseInfo != null -> stringResource(Res.string.latest_version_format, latestReleaseInfo!!.version)
                    else -> stringResource(Res.string.check_for_updates_button)
                }
                SettingItem(
                    title = titleText,
                    onClick = {
                        if (!isChecking) {
                            updateViewModel.manualCheckForUpdate()
                        }
                    },
                    loading = isChecking
                )
            }

            // Downloader controls / status
            if (updateAvailable != null) {
                item {
                    SettingsSectionHeader("Installation")
                }
                item {
                    when (downloadStatus) {
                        is SharedViewModel.DownloadStatus.Idle -> {
                            SettingItem(
                                title = "Download Update",
                                subtitle = "Click to start downloading the new version.",
                                onClick = {
                                    val release = updateAvailable!!
                                    sharedViewModel.downloadAppUpdate(release.downloadUrl, release.title)
                                }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloading -> {
                            val progress = (downloadStatus as SharedViewModel.DownloadStatus.Downloading).progress
                            SettingItem(
                                title = "Downloading... ${(progress * 100).toInt()}%",
                                otherView = {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                },
                                onClick = { sharedViewModel.cancelDownload() }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Verifying -> {
                            SettingItem(
                                title = "Verifying update...",
                                otherView = {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloaded -> {
                            val path = (downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path
                            SettingItem(
                                title = "Install Update",
                                subtitle = "Ready to install. Click to open installer.",
                                onClick = { sharedViewModel.installUpdate(path) }
                            )
                        }
                    }
                }

                // Changelog
                val releaseToDisplay = updateAvailable ?: latestReleaseInfo
                if (releaseToDisplay != null) {
                    item {
                        SettingsSectionHeader("Release Notes")
                    }
                    item {
                        SettingItem(
                            title = if (showChangelog) "Hide Changelog" else "View Changelog",
                            onClick = { showChangelog = !showChangelog }
                        )
                    }
                    if (showChangelog) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
