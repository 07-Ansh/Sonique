package com.sonique.app.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.sonique.app.Platform
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.component.SettingBasicDialog
import com.sonique.app.ui.component.SettingDialog
import com.sonique.app.ui.screen.settings.*
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

enum class SettingsSubCategory {
    APPEARANCE, GENERAL, UPDATES, AUDIO, PLAYBACK, SPOTIFY, SPONSORBLOCK, BACKUP, ABOUT, STORAGE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCalfApi::class)
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    startCategory: String? = null,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val updateViewModel: UpdateViewModel = koinViewModel()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()

    var activeSubCategory by rememberSaveable {
        mutableStateOf(
            startCategory?.let {
                try {
                    SettingsSubCategory.valueOf(it.uppercase())
                } catch (e: Exception) {
                    null
                }
            }
        )
    }

    com.sonique.app.expect.ui.BackHandler(
        enabled = activeSubCategory != null
    ) {
        activeSubCategory = null
    }
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    val basicAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()

    if (alertData != null) {
        SettingDialog(alert = alertData!!, onDismiss = { viewModel.setAlertData(null) })
    }
    if (basicAlertData != null) {
        SettingBasicDialog(alert = basicAlertData!!, onDismiss = { viewModel.setBasicAlertData(null) })
    }

    LaunchedEffect(Unit) {
        viewModel.getData()
    }

    Crossfade(targetState = activeSubCategory) { category ->
        if (category == null) {
            MainSettingsList(
                innerPadding = innerPadding,
                enableLiquidGlass = enableLiquidGlass,
                navController = navController,
                onCategoryClick = { activeSubCategory = it }
            )
        } else {
            if (category == SettingsSubCategory.UPDATES) {
                SettingsUpdateScreen(onBack = { activeSubCategory = null })
            } else {
                SubSettingsContainer(
                    title = when (category) {
                        SettingsSubCategory.APPEARANCE -> "Appearance"
                        SettingsSubCategory.GENERAL -> stringResource(Res.string.general)
                        SettingsSubCategory.UPDATES -> "App Updates"
                        SettingsSubCategory.AUDIO -> stringResource(Res.string.audio)
                        SettingsSubCategory.PLAYBACK -> stringResource(Res.string.playback)
                        SettingsSubCategory.SPOTIFY -> stringResource(Res.string.spotify)
                        SettingsSubCategory.SPONSORBLOCK -> stringResource(Res.string.sponsorBlock)
                        SettingsSubCategory.BACKUP -> stringResource(Res.string.backup)
                        SettingsSubCategory.ABOUT -> stringResource(Res.string.about_us)
                        SettingsSubCategory.STORAGE -> stringResource(Res.string.storage)
                    },
                    onBack = { activeSubCategory = null }
                ) {
                    when (category) {
                        SettingsSubCategory.APPEARANCE -> AppearanceSettingsContent(viewModel)
                        SettingsSubCategory.GENERAL -> GeneralSettingsContent(viewModel, sharedViewModel, navController)
                        SettingsSubCategory.AUDIO -> AudioSettingsContent(viewModel)
                        SettingsSubCategory.PLAYBACK -> PlaybackSettingsContent(viewModel, sharedViewModel)
                        SettingsSubCategory.SPOTIFY -> SpotifySettingsContent(viewModel, navController)
                        SettingsSubCategory.SPONSORBLOCK -> SponsorBlockSettingsContent(viewModel)
                        SettingsSubCategory.BACKUP -> BackupSettingsContent(viewModel)
                        SettingsSubCategory.ABOUT -> AboutSettingsContent(navController)
                        SettingsSubCategory.STORAGE -> StorageSettingsContent(viewModel)
                        SettingsSubCategory.UPDATES -> {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsList(
    innerPadding: PaddingValues,
    enableLiquidGlass: Boolean,
    navController: NavController,
    onCategoryClick: (SettingsSubCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(Res.string.settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Material3SettingsGroup(
                    title = "User Interface",
                    items = listOf(
                        Material3SettingsItem(
                            icon = Icons.Default.Palette,
                            title = { Text("Appearance") },
                            description = { Text("Theme, Liquid Glass, player styling & visual effects") },
                            onClick = { onCategoryClick(SettingsSubCategory.APPEARANCE) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Playback & Audio",
                    items = listOf(
                        Material3SettingsItem(
                            icon = Icons.Default.Audiotrack,
                            title = { Text(stringResource(Res.string.audio)) },
                            description = { Text("Equalizer, volume normalization, crossfade & audio effects") },
                            onClick = { onCategoryClick(SettingsSubCategory.AUDIO) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.PlayCircle,
                            title = { Text(stringResource(Res.string.playback)) },
                            description = { Text("Crossfade transitions, queue preservation, service & lyrics") },
                            onClick = { onCategoryClick(SettingsSubCategory.PLAYBACK) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.Block,
                            title = { Text(stringResource(Res.string.sponsorBlock)) },
                            description = { Text("Skip sponsored segments, intros, outros & non-music parts") },
                            onClick = { onCategoryClick(SettingsSubCategory.SPONSORBLOCK) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Integrations",
                    items = listOf(
                        Material3SettingsItem(
                            icon = Icons.Default.MusicNote,
                            title = { Text(stringResource(Res.string.spotify)) },
                            description = { Text("Connect account, import playlists & sync Spotify Canvas") },
                            onClick = { onCategoryClick(SettingsSubCategory.SPOTIFY) }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "System & Data",
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                icon = Icons.Default.Settings,
                                title = { Text(stringResource(Res.string.general)) },
                                description = { Text("App language, search history & navigation preferences") },
                                onClick = { onCategoryClick(SettingsSubCategory.GENERAL) }
                            )
                        )
                        add(
                            Material3SettingsItem(
                                icon = Icons.Default.Backup,
                                title = { Text(stringResource(Res.string.backup)) },
                                description = { Text("Export, import & restore playlists, favorites and settings") },
                                onClick = { onCategoryClick(SettingsSubCategory.BACKUP) }
                            )
                        )
                        if (getPlatform() == Platform.Android) {
                            add(
                                Material3SettingsItem(
                                    icon = Icons.Default.Storage,
                                    title = { Text(stringResource(Res.string.storage)) },
                                    description = { Text("Manage offline downloads, song cache & storage usage") },
                                    onClick = { onCategoryClick(SettingsSubCategory.STORAGE) }
                                )
                            )
                        }
                    }
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Updates & Info",
                    items = listOf(
                        Material3SettingsItem(
                            icon = Icons.Default.SystemUpdate,
                            title = { Text("App Updates") },
                            description = { Text("Check for updates, release notes & auto-update settings") },
                            onClick = { onCategoryClick(SettingsSubCategory.UPDATES) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.Info,
                            title = { Text(stringResource(Res.string.about_us)) },
                            description = { Text("App version, open-source licenses & contributors") },
                            onClick = { onCategoryClick(SettingsSubCategory.ABOUT) }
                        )
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubSettingsContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}