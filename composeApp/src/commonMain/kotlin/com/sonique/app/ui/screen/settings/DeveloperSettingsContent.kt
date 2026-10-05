package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.ui.component.GitHubStarDialog
import com.sonique.app.ui.component.LoadingDialog
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.component.SettingBasicDialog
import com.sonique.app.ui.component.UpdateDialog
import com.sonique.app.utils.VersionManager
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.domain.repository.ReleaseInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DeveloperSettingsContent(
    sharedViewModel: SharedViewModel,
    updateViewModel: UpdateViewModel = koinViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()

    val showChangelog by sharedViewModel.showChangelog.collectAsStateWithLifecycle()
    var showGitHubStarDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    var showBasicDialog by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }

    val changelogText by sharedViewModel.changelogText.collectAsStateWithLifecycle()
    val changelogVersionName by sharedViewModel.changelogVersionName.collectAsStateWithLifecycle()
    val latestReleaseInfo by updateViewModel.latestReleaseInfo.collectAsStateWithLifecycle()
    var previewReleaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2500)
            statusMessage = null
        }
    }

    LaunchedEffect(showUpdateDialog) {
        if (showUpdateDialog) {
            val cached = latestReleaseInfo
            if (cached != null) {
                previewReleaseInfo = cached
            } else {
                coroutineScope.launch {
                    updateViewModel.loadChangelog()
                    val fetched = updateViewModel.latestReleaseInfo.value
                    previewReleaseInfo = fetched ?: ReleaseInfo(
                        version = "v${VersionManager.getVersionName()}",
                        title = "Sonique v${VersionManager.getVersionName()} Update",
                        changelog = changelogText.ifBlank { "Latest release notes from GitHub repository." },
                        downloadUrl = "https://github.com/07-Ansh/Sonique/releases",
                        publishedAt = "",
                        htmlUrl = "https://github.com/07-Ansh/Sonique/releases"
                    )
                }
            }
        }
    }

    // Modal 3: GitHub Star Prompt
    if (showGitHubStarDialog) {
        GitHubStarDialog(
            onDismiss = { showGitHubStarDialog = false },
            onStar = { showGitHubStarDialog = false },
            onNeverShowAgain = { showGitHubStarDialog = false }
        )
    }

    // Modal 4: In-App Update Dialog
    if (showUpdateDialog && previewReleaseInfo != null) {
        UpdateDialog(
            releaseInfo = previewReleaseInfo!!,
            onDismiss = { showUpdateDialog = false },
            onDownload = { showUpdateDialog = false }
        )
    }

    // Modal 5: Loading Dialog
    if (showLoadingDialog) {
        LoadingDialog(
            loading = showLoadingDialog,
            message = "Testing Google Circular Loader..."
        )
        LaunchedEffect(Unit) {
            delay(2200)
            showLoadingDialog = false
        }
    }

    // Modal 6: Basic Alert Dialog
    if (showBasicDialog) {
        SettingBasicDialog(
            alert = SettingBasicAlertState(
                title = "Developer Alert Test",
                message = "This is a demonstration of SettingBasicDialog in Sonique.",
                confirm = "Confirm" to { showBasicDialog = false },
                dismiss = "Dismiss"
            ),
            onDismiss = { showBasicDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Banner (if an action ran)
        if (statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Section 1: Modal & Dialog Testing
        item {
            Material3SettingsGroup(
                title = "Modal & Dialog Previews",
                items = listOf(
                    Material3SettingsItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = { Text("What's New Dialog") },
                        description = { Text("Test the celebratory post-update launch dialog (v${VersionManager.getVersionName()})") },
                        isSwitch = true,
                        checked = showChangelog,
                        onCheckedChange = { checked ->
                            if (checked) {
                                sharedViewModel.fetchAndShowChangelog(forceShow = true)
                            } else {
                                sharedViewModel.dismissChangelog()
                            }
                        },
                        onClick = {
                            sharedViewModel.fetchAndShowChangelog(forceShow = true)
                        }
                    ),
                    Material3SettingsItem(
                        icon = Icons.Rounded.Star,
                        title = { Text("GitHub Star Dialog") },
                        description = { Text("Test the 'Enjoying Sonique?' rating prompt dialog") },
                        isSwitch = true,
                        checked = showGitHubStarDialog,
                        onCheckedChange = { showGitHubStarDialog = it },
                        onClick = { showGitHubStarDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = Icons.Rounded.SystemUpdate,
                        title = { Text("In-App Update Dialog") },
                        description = { Text("Test the APK update available alert dialog") },
                        isSwitch = true,
                        checked = showUpdateDialog,
                        onCheckedChange = { showUpdateDialog = it },
                        onClick = { showUpdateDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = Icons.Rounded.HourglassEmpty,
                        title = { Text("Loading Spinner Dialog") },
                        description = { Text("Test the full-screen Google circular progress modal (auto-dismisses)") },
                        isSwitch = true,
                        checked = showLoadingDialog,
                        onCheckedChange = { showLoadingDialog = it },
                        onClick = { showLoadingDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = Icons.Rounded.BugReport,
                        title = { Text("Basic Alert Dialog") },
                        description = { Text("Test the default SettingBasicDialog confirmation alert") },
                        isSwitch = true,
                        checked = showBasicDialog,
                        onCheckedChange = { showBasicDialog = it },
                        onClick = { showBasicDialog = true }
                    )
                )
            )
        }

        // Section 2: Flow Simulation & Resets
        item {
            Material3SettingsGroup(
                title = "State & Lifecycle Simulation",
                items = listOf(
                    Material3SettingsItem(
                        icon = Icons.Rounded.Refresh,
                        title = { Text("Simulate Post-Update Launch") },
                        description = { Text("Resets version history and immediately triggers the real post-update What's New dialog") },
                        onClick = {
                            sharedViewModel.devSimulateAppUpdate()
                            statusMessage = "Simulating post-update launch..."
                        }
                    ),
                    Material3SettingsItem(
                        icon = Icons.Rounded.Refresh,
                        title = { Text("Reset & Trigger GitHub Star Prompt") },
                        description = { Text("Clears rating preferences and immediately displays the GitHub Star dialog") },
                        onClick = {
                            sharedViewModel.devResetGitHubPopup()
                            statusMessage = "Displaying GitHub star prompt..."
                        }
                    )
                )
            )
        }

        // Section 3: Build & Environment Specifications
        item {
            Material3SettingsGroup(
                title = "Build Environment",
                items = emptyList()
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BuildInfoRow(label = "Application ID", value = "com.sonique.app.dev")
                    BuildInfoRow(label = "Version Name", value = "${VersionManager.getVersionName()}-dev")
                    BuildInfoRow(label = "Version Code", value = "${VersionManager.getVersionCode()}")
                    BuildInfoRow(label = "Build Type", value = "debug")
                    BuildInfoRow(label = "Debuggable", value = "true (Developer Mode Active)")
                }
            }
        }
    }
}

@Composable
private fun BuildInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
