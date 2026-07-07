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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.domain.repository.ReleaseInfo
import org.koin.compose.viewmodel.koinViewModel
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.mikepenz.markdown.m3.Markdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsUpdateScreen(
    onBack: () -> Unit,
    updateViewModel: UpdateViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(),
) {
    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()
    val downloadStatus by sharedViewModel.downloadStatus.collectAsStateWithLifecycle()
    val isChecking by updateViewModel.isChecking.collectAsStateWithLifecycle()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Update", style = MaterialTheme.typography.titleSmall) },
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
                .padding(start = 16.dp, end = 16.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (updateAvailable != null) {
                UpdateAvailableContent(
                    releaseInfo = updateAvailable!!,
                    status = downloadStatus,
                    onUpdate = {
                         sharedViewModel.downloadAppUpdate(it.downloadUrl, it.title)
                    },
                    onCancel = {
                        sharedViewModel.cancelDownload()
                    },
                    onInstall = {
                        sharedViewModel.installUpdate(it)
                    }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isChecking) "Checking for updates..." else "Your app is up to date!",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { updateViewModel.manualCheckForUpdate() },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isChecking) "Checking..." else "Check for Updates")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableContent(
    releaseInfo: ReleaseInfo,
    status: SharedViewModel.DownloadStatus,
    onUpdate: (ReleaseInfo) -> Unit,
    onCancel: () -> Unit,
    onInstall: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "New Version Available: ${releaseInfo.version}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Changelog", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Markdown(content = releaseInfo.changelog)
            Spacer(modifier = Modifier.height(16.dp))
            
            when (status) {
                is SharedViewModel.DownloadStatus.Downloading -> {
                    Text("Downloading... ${(status.progress * 100).toInt()}%")
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
                is SharedViewModel.DownloadStatus.Verifying -> {
                    Text("Verifying update...")
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is SharedViewModel.DownloadStatus.Downloaded -> {
                    Button(
                        onClick = { onInstall(status.path) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install Now")
                    }
                }
                is SharedViewModel.DownloadStatus.Idle -> {
                    Button(
                        onClick = { onUpdate(releaseInfo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download & Install")
                    }
                }
            }
        }
    }
}
