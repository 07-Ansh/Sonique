package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.domain.repository.ReleaseInfo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUpdateScreen(
    onBack: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(),
) {
    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsStateWithLifecycle()
    val currentVersion = updateViewModel.currentVersion
    val downloadStatus by sharedViewModel.downloadStatus.collectAsStateWithLifecycle()
    val isChecking by updateViewModel.isChecking.collectAsStateWithLifecycle()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    var showChangelog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.updater)) },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            Material3SettingsGroup(
                title = stringResource(Res.string.current_version),
                items = listOf(
                    Material3SettingsItem(
                        title = {
                            Text(stringResource(Res.string.version_format, currentVersion))
                        },
                        description = {
                            // Dummy architecture and variant for UI purpose or fetch actual if available
                            val arch = "arm64-v8a" 
                            val variant = "FOSS"
                            Text("$arch - $variant")
                        },
                    ),
                ),
            )

            Spacer(Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(Res.string.check_for_updates_title),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.refresh),
                        title = {
                            if (isChecking) {
                                Text(stringResource(Res.string.checking_for_updates))
                            } else if (updateAvailable != null) {
                                Text(stringResource(Res.string.latest_version_format, updateAvailable!!.version))
                            } else {
                                Text(stringResource(Res.string.check_for_updates_button))
                            }
                        },
                        trailingContent = {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else if (updateAvailable != null) {
                                Icon(
                                    painter = painterResource(Res.drawable.download),
                                    contentDescription = stringResource(Res.string.update_available_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { if (!isChecking) updateViewModel.manualCheckForUpdate() },
                    ),
                ),
            )

            if (updateAvailable != null) {
                Spacer(Modifier.height(16.dp))

                when (downloadStatus) {
                    is SharedViewModel.DownloadStatus.Idle -> {
                        Button(
                            onClick = { 
                                val release = updateAvailable!!
                                sharedViewModel.downloadAppUpdate(release.downloadUrl, release.title) 
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) {
                            Text(stringResource(Res.string.download_and_install))
                        }
                    }
                    is SharedViewModel.DownloadStatus.Downloading -> {
                        val progress = (downloadStatus as SharedViewModel.DownloadStatus.Downloading).progress
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(stringResource(Res.string.downloading_progress, (progress * 100).toInt()))
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { sharedViewModel.cancelDownload() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(Res.string.cancel))
                            }
                        }
                    }
                    is SharedViewModel.DownloadStatus.Verifying -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(stringResource(Res.string.verifying_update))
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    is SharedViewModel.DownloadStatus.Downloaded -> {
                        Button(
                            onClick = { sharedViewModel.installUpdate((downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) {
                            Text(stringResource(Res.string.install_now))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showChangelog = !showChangelog },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(if (showChangelog) stringResource(Res.string.hide_changelog) else stringResource(Res.string.view_changelog))
                }

                if (showChangelog) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.changelog),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Markdown(content = updateAvailable!!.changelog)
                        }
                    }
                }
            } else if (latestReleaseInfo != null && !isChecking) {
                Spacer(Modifier.height(32.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Changelog for v${latestReleaseInfo!!.version}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Markdown(content = latestReleaseInfo!!.changelog)
                    }
                }
            }

            Spacer(Modifier.height(140.dp))
        }
    }
}
