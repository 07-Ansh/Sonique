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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    var showTroubleshooting by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_updates)
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
            // 1. Current Version
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

            // 2. Changelog / Release Notes (Always placed after current version, before update settings)
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

            // 3. Update Settings
            item {
                val updateSettingsItems = mutableListOf<Material3SettingsItem>()
                updateSettingsItems.add(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.auto_check_for_update)) },
                        description = { Text("Check for updates automatically when opening the app") },
                        isSwitch = true,
                        checked = autoCheckForUpdates,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                dataStoreManager.setAutoCheckForUpdates(checked)
                            }
                        }
                    )
                )
                if (autoCheckForUpdates) {
                    updateSettingsItems.add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.update_notifications)) },
                            isSwitch = true,
                            checked = updateNotifications,
                            onCheckedChange = { updateNotifications = it }
                        )
                    )
                }

                Material3SettingsGroup(
                    title = stringResource(Res.string.update_settings),
                    items = updateSettingsItems
                )
            }

            // 4. Action Button / Check For Updates (Turns into green Download/Install button when update is available)
            item {
                val greenColor = Color(0xFF10B981)
                val actionItem = if (updateAvailable != null) {
                    val release = updateAvailable!!
                    when (downloadStatus) {
                        is SharedViewModel.DownloadStatus.Idle -> {
                            Material3SettingsItem(
                                icon = Icons.Default.Refresh,
                                title = {
                                    Text(
                                        text = "Download Update (${release.version})",
                                        color = greenColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                description = {
                                    Text(
                                        text = "New update available! Click to start download.",
                                        color = greenColor.copy(alpha = 0.85f)
                                    )
                                },
                                isHighlighted = true,
                                onClick = {
                                    sharedViewModel.downloadAppUpdate(release.downloadUrl, release.title)
                                }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloading -> {
                            val progress = (downloadStatus as SharedViewModel.DownloadStatus.Downloading).progress
                            Material3SettingsItem(
                                title = {
                                    Text(
                                        text = "Downloading... ${(progress * 100).toInt()}%",
                                        color = greenColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                description = {
                                    Column {
                                        Text(
                                            text = "Click to cancel download",
                                            color = greenColor.copy(alpha = 0.85f)
                                        )
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            color = greenColor
                                        )
                                    }
                                },
                                onClick = { sharedViewModel.cancelDownload() }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Verifying -> {
                            Material3SettingsItem(
                                title = {
                                    Text(
                                        text = "Verifying update...",
                                        color = greenColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                description = {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        color = greenColor
                                    )
                                }
                            )
                        }
                        is SharedViewModel.DownloadStatus.Downloaded -> {
                            val path = (downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path
                            Material3SettingsItem(
                                title = {
                                    Text(
                                        text = "Install Update",
                                        color = greenColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                description = {
                                    Text(
                                        text = "Update ready to install. Click to open installer.",
                                        color = greenColor.copy(alpha = 0.85f)
                                    )
                                },
                                isHighlighted = true,
                                onClick = { sharedViewModel.installUpdate(path) }
                            )
                        }
                    }
                } else {
                    val titleText = when {
                        isChecking -> stringResource(Res.string.checking_for_updates)
                        latestReleaseInfo != null -> stringResource(Res.string.latest_version_format, latestReleaseInfo!!.version)
                        else -> stringResource(Res.string.check_for_updates_button)
                    }
                    Material3SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = { Text(titleText) },
                        onClick = {
                            if (!isChecking) {
                                updateViewModel.manualCheckForUpdate()
                            }
                        },
                        enabled = !isChecking
                    )
                }

                Material3SettingsGroup(
                    title = if (updateAvailable != null) "Update Available" else stringResource(Res.string.check_for_updates_title),
                    items = listOf(actionItem)
                )
            }

            // 5. Troubleshooting / Manual Install Guide
            item {
                Material3SettingsGroup(
                    title = "Troubleshooting",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text("Having trouble downloading?") },
                            description = { Text("Click to view manual installation guide") },
                            onClick = { showTroubleshooting = !showTroubleshooting }
                        )
                    )
                )
            }

            if (showTroubleshooting) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Manual Installation Guide",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            val latestReleaseUrl = updateAvailable?.let { "https://github.com/07-Ansh/Sonique/releases/tag/${it.version}" }
                                ?: "https://github.com/07-Ansh/Sonique/releases/latest"

                            Text(
                                text = "If automatic downloading or installation is not working on your device, follow these steps:\n\n" +
                                        "1. Open GitHub Releases\n" +
                                        "Tap the button below to visit the official GitHub Releases page.\n\n" +
                                        "2. Download the APK file\n" +
                                        "Under the 'Assets' section of the release, tap the '.apk' file to download it.\n\n" +
                                        "3. Install Update\n" +
                                        "Open the downloaded APK file from your browser downloads or File Manager.\n\n" +
                                        "4. Allow Unknown Apps\n" +
                                        "If Android prompts for permission, tap Settings -> enable 'Allow from this source' and proceed with installation.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                            )

                            Button(
                                onClick = { uriHandler.openUri(latestReleaseUrl) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Open Latest GitHub Release",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
