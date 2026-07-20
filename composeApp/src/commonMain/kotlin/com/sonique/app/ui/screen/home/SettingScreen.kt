package com.sonique.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.toKmpUri
import com.mohamedrejeb.calf.core.LocalPlatformContext as CalfPlatformContext
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.sonique.app.Platform
import com.sonique.app.expect.ui.fileSaverResult
import com.sonique.app.expect.ui.openEqResult
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.SettingBasicDialog
import com.sonique.app.ui.screen.settings.SettingsUpdateScreen
import com.sonique.app.ui.component.SettingDialog
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.expect.ui.rememberBackdrop
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.sonique.app.ui.navigation.destination.login.LoginDestination
import com.sonique.app.ui.navigation.destination.login.SpotifyLoginDestination
import com.sonique.domain.utils.LocalResource
import com.sonique.app.ui.theme.backgroundCard
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.app.utils.VersionManager
import com.sonique.common.LIMIT_CACHE_SIZE
import com.sonique.common.QUALITY
import com.sonique.common.SponsorBlockType
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.common.SUPPORTED_LOCATION
import com.sonique.domain.extension.now
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import com.sonique.domain.repository.ReleaseInfo
import com.sonique.app.extension.bytesToMB
import com.sonique.app.extension.displayString
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*
import kotlin.math.roundToInt

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

    // Dialog state handlers (shared across all sections)
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
                        SettingsSubCategory.PLAYBACK -> PlaybackSettingsContent(viewModel)
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
                            onClick = { onCategoryClick(SettingsSubCategory.AUDIO) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.PlayCircle,
                            title = { Text(stringResource(Res.string.playback)) },
                            onClick = { onCategoryClick(SettingsSubCategory.PLAYBACK) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.Block,
                            title = { Text(stringResource(Res.string.sponsorBlock)) },
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
                                onClick = { onCategoryClick(SettingsSubCategory.GENERAL) }
                            )
                        )
                        add(
                            Material3SettingsItem(
                                icon = Icons.Default.Backup,
                                title = { Text(stringResource(Res.string.backup)) },
                                onClick = { onCategoryClick(SettingsSubCategory.BACKUP) }
                            )
                        )
                        if (getPlatform() == Platform.Android) {
                            add(
                                Material3SettingsItem(
                                    icon = Icons.Default.Storage,
                                    title = { Text(stringResource(Res.string.storage)) },
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
                            onClick = { onCategoryClick(SettingsSubCategory.UPDATES) }
                        ),
                        Material3SettingsItem(
                            icon = Icons.Default.Info,
                            title = { Text(stringResource(Res.string.about_us)) },
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

@Composable
private fun AppearanceSettingsContent(viewModel: SettingsViewModel) {
    val ambienceMode by viewModel.ambienceMode.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val liquidGlassGlassiness by viewModel.liquidGlassGlassiness.collectAsStateWithLifecycle()
    val blurPlayerBackground by viewModel.blurPlayerBackground.collectAsStateWithLifecycle()
    val enableExpressivePlayerControls by viewModel.enableExpressivePlayerControls.collectAsStateWithLifecycle()
    val enablePageTransitions by viewModel.enablePageTransitions.collectAsStateWithLifecycle()
    val continueListeningLayout by viewModel.continueListeningLayout.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val currentLayoutLabel = when (continueListeningLayout) {
        "1_row" -> "1 Row (Standard)"
        "2_row" -> "2 Rows (Compact Grid)"
        "3x3" -> "3x3 Grid (Pages)"
        "list" -> "Vertical List"
        else -> "1 Row (Standard)"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Background Effects",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Ambience Mode") },
                        description = { Text("Show gradient background based on album art colors") },
                        trailingContent = {
                            Switch(
                                checked = ambienceMode,
                                onCheckedChange = { viewModel.setAmbienceMode(it) }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text("Frosted Player Background") },
                        description = { Text("Blur background artwork based on album art using frosted glassmorphism") },
                        trailingContent = {
                            Switch(
                                checked = blurPlayerBackground,
                                onCheckedChange = { viewModel.setBlurPlayerBackground(it) }
                            )
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Player Screen",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Expressive Player Controls") },
                        description = { Text("Use Material 3 Expressive shapes for playback buttons") },
                        trailingContent = {
                            Switch(
                                checked = enableExpressivePlayerControls,
                                onCheckedChange = { viewModel.setEnableExpressivePlayerControls(it) }
                            )
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Home Screen",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text("Continue Listening Layout") },
                        description = { Text(currentLayoutLabel) },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Continue Listening Layout",
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = listOf(
                                                (continueListeningLayout == "1_row") to "1 Row (Standard)",
                                                (continueListeningLayout == "2_row") to "2 Rows (Compact Grid)",
                                                (continueListeningLayout == "3x3") to "3x3 Grid (Pages)",
                                                (continueListeningLayout == "list") to "Vertical List"
                                            )
                                        ),
                                        confirm = "Change" to { state ->
                                            val selectedLabel = state.selectOne?.getSelected() ?: ""
                                            val layoutValue = when (selectedLabel) {
                                                "1 Row (Standard)" -> "1_row"
                                                "2 Rows (Compact Grid)" -> "2_row"
                                                "3x3 Grid (Pages)" -> "3x3"
                                                "Vertical List" -> "list"
                                                else -> "1_row"
                                            }
                                            viewModel.setContinueListeningLayout(layoutValue)
                                        },
                                        dismiss = "Cancel"
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        if (getPlatform() == Platform.Android) {
            item {
                Material3SettingsGroup(
                    title = "Liquid Glass",
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                title = { Text("Apple Liquid Glass") },
                                description = { Text("Apple-style floating bottom layout with real-time backdrop luminance sensing") },
                                onClick = { viewModel.setEnableLiquidGlass(!enableLiquidGlass) },
                                trailingContent = {
                                    Switch(
                                        checked = enableLiquidGlass,
                                        onCheckedChange = { viewModel.setEnableLiquidGlass(it) }
                                    )
                                }
                            )
                        )
                        if (!enableLiquidGlass) {
                            add(
                                Material3SettingsItem(
                                    title = { Text("Page Transitions") },
                                    description = { Text("Enable sliding animation when switching pages") },
                                    onClick = { viewModel.setEnablePageTransitions(!enablePageTransitions) },
                                    trailingContent = {
                                        Switch(
                                            checked = enablePageTransitions,
                                            onCheckedChange = { viewModel.setEnablePageTransitions(it) }
                                        )
                                    }
                                )
                            )
                        }
                    }
                )
            }

            if (enableLiquidGlass) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Liquid Glass Opacity (Glassiness)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(liquidGlassGlassiness * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Opacity,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                             var isInteracting by remember { mutableStateOf(false) }
                             val thumbScaleX = animateFloatAsState(targetValue = if (isInteracting) 1.25f else 1.0f)
                             val thumbScaleY = animateFloatAsState(targetValue = if (isInteracting) 0.82f else 1.0f)
                             val thumbWidth = animateDpAsState(targetValue = if (isInteracting) 54.dp else 24.dp)
                             val thumbHeight = animateDpAsState(targetValue = if (isInteracting) 44.dp else 24.dp)
                             val thumbCornerRadius = animateDpAsState(targetValue = if (isInteracting) 22.dp else 12.dp)
                             val solidAlpha = animateFloatAsState(targetValue = if (isInteracting) 0.0f else 1.0f)
                             var trackWidth by remember { mutableStateOf(0f) }
                             val density = LocalDensity.current

                             Box(
                                 modifier = Modifier
                                     .weight(1f)
                                     .height(44.dp)
                                     .pointerInput(Unit) {
                                         awaitPointerEventScope {
                                             while (true) {
                                                 val down = awaitFirstDown(requireUnconsumed = false)
                                                 isInteracting = true
                                                 if (trackWidth > 0f) {
                                                     val progress = (down.position.x / trackWidth).coerceIn(0f, 1f)
                                                     viewModel.setLiquidGlassGlassiness(progress)
                                                 }
                                                 var pointerId = down.id
                                                 var dragEvent: androidx.compose.ui.input.pointer.PointerInputChange? = null
                                                 do {
                                                     val event = awaitPointerEvent()
                                                     val dragChange = event.changes.firstOrNull { change -> change.id == pointerId }
                                                     if (dragChange != null && dragChange.pressed) {
                                                         if (trackWidth > 0f) {
                                                             val progress = (dragChange.position.x / trackWidth).coerceIn(0f, 1f)
                                                                 viewModel.setLiquidGlassGlassiness(progress)
                                                         }
                                                         dragChange.consume()
                                                         dragEvent = dragChange
                                                     } else {
                                                         dragEvent = null
                                                     }
                                                 } while (dragEvent != null || event.changes.any { change -> change.pressed })
                                                 isInteracting = false
                                             }
                                         }
                                     }
                                     .onSizeChanged { size -> trackWidth = size.width.toFloat() },
                                 contentAlignment = Alignment.CenterStart
                             ) {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .height(4.dp)
                                         .clip(RoundedCornerShape(2.dp))
                                         .background(Color.White.copy(alpha = 0.15f))
                                 )
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth(liquidGlassGlassiness)
                                         .height(4.dp)
                                         .clip(RoundedCornerShape(2.dp))
                                         .background(Color.White)
                                 )
                                 Box(
                                     modifier = Modifier
                                         .offset {
                                             val thumbWidthPx = with(density) { thumbWidth.value.toPx() }
                                             val xOffset = (liquidGlassGlassiness * trackWidth) - (thumbWidthPx / 2f)
                                             IntOffset(xOffset.roundToInt().coerceIn(0, (trackWidth - thumbWidthPx).roundToInt()), 0)
                                         }
                                         .size(width = thumbWidth.value, height = thumbHeight.value)
                                         .graphicsLayer {
                                             scaleX = thumbScaleX.value
                                             scaleY = thumbScaleY.value
                                         }
                                         .clip(RoundedCornerShape(thumbCornerRadius.value))
                                         .background(Color.White.copy(alpha = 0.15f * (1f - solidAlpha.value)))
                                         .border(1.dp, Color.White.copy(alpha = 0.25f * (1f - solidAlpha.value)), RoundedCornerShape(thumbCornerRadius.value))
                                 ) {
                                     if (solidAlpha.value > 0f) {
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .background(Color.White.copy(alpha = solidAlpha.value))
                                         )
                                     }
                                 }
                             }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSettingsContent(viewModel: SettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()

    val normalizeVolumeFlow = remember(viewModel.normalizeVolume) {
        viewModel.normalizeVolume.map { it == TRUE }
    }
    val normalizeVolume by normalizeVolumeFlow.collectAsStateWithLifecycle(initialValue = false)

    val skipSilentFlow = remember(viewModel.skipSilent) {
        viewModel.skipSilent.map { it == TRUE }
    }
    val skipSilent by skipSilentFlow.collectAsStateWithLifecycle(initialValue = false)
    val resultLauncher = openEqResult(viewModel.getAudioSessionId())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Quality Settings",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.quality)) },
                        description = { Text(quality ?: "") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.quality),
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = QUALITY.items.map { item ->
                                                (item.toString() == quality) to item.toString()
                                            },
                                        ),
                                        confirm = getString(Res.string.change) to { state ->
                                            viewModel.changeQuality(state.selectOne?.getSelected())
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.download_quality)) },
                        description = { Text(downloadQuality ?: "") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.download_quality),
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = QUALITY.items.map { item ->
                                                (item.toString() == downloadQuality) to item.toString()
                                            },
                                        ),
                                        confirm = getString(Res.string.change) to { state ->
                                            state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) }
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Audio Effects",
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.normalize_volume)) },
                            description = { Text(stringResource(Res.string.balance_media_loudness)) },
                            trailingContent = {
                                Switch(
                                    checked = normalizeVolume,
                                    onCheckedChange = { viewModel.setNormalizeVolume(it) }
                                )
                            }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.skip_silent)) },
                            description = { Text(stringResource(Res.string.skip_no_music_part)) },
                            trailingContent = {
                                Switch(
                                    checked = skipSilent,
                                    onCheckedChange = { viewModel.setSkipSilent(it) }
                                )
                            }
                        )
                    )
                    if (getPlatform() == Platform.Android) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.open_system_equalizer)) },
                                description = { Text(stringResource(Res.string.use_your_system_equalizer)) },
                                onClick = {
                                    coroutineScope.launch {
                                        resultLauncher.launch()
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaybackSettingsContent(viewModel: SettingsViewModel) {
    val savePlaybackStateFlow = remember(viewModel.savedPlaybackState) {
        viewModel.savedPlaybackState.map { it == TRUE }
    }
    val savePlaybackState by savePlaybackStateFlow.collectAsStateWithLifecycle(initialValue = false)

    val saveLastPlayedFlow = remember(viewModel.saveRecentSongAndQueue) {
        viewModel.saveRecentSongAndQueue.map { it == TRUE }
    }
    val saveLastPlayed by saveLastPlayedFlow.collectAsStateWithLifecycle(initialValue = false)

    val killServiceOnExitFlow = remember(viewModel.killServiceOnExit) {
        viewModel.killServiceOnExit.map { it == TRUE }
    }
    val killServiceOnExit by killServiceOnExitFlow.collectAsStateWithLifecycle(initialValue = true)
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()

    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDjMode by viewModel.crossfadeDjMode.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "State Preservation",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.save_playback_state)) },
                        description = { Text(stringResource(Res.string.save_shuffle_and_repeat_mode)) },
                        trailingContent = {
                            Switch(
                                checked = savePlaybackState,
                                onCheckedChange = { viewModel.setSavedPlaybackState(it) }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.save_last_played)) },
                        description = { Text(stringResource(Res.string.save_last_played_track_and_queue)) },
                        trailingContent = {
                            Switch(
                                checked = saveLastPlayed,
                                onCheckedChange = { viewModel.setSaveLastPlayed(it) }
                            )
                        }
                    )
                )
            )
        }

        if (getPlatform() == Platform.Android) {
            item {
                Material3SettingsGroup(
                    title = "Service Lifecycle",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.kill_service_on_exit)) },
                            description = { Text(stringResource(Res.string.kill_service_on_exit_description)) },
                            trailingContent = {
                                Switch(
                                    checked = killServiceOnExit,
                                    onCheckedChange = { viewModel.setKillServiceOnExit(it) }
                                )
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.keep_service_alive)) },
                            description = { Text(stringResource(Res.string.keep_service_alive_description)) },
                            trailingContent = {
                                Switch(
                                    checked = keepServiceAlive,
                                    onCheckedChange = { viewModel.setKeepServiceAlive(it) }
                                )
                            }
                        )
                    )
                )
            }
        }

        item {
            Material3SettingsGroup(
                title = "Crossfade Settings",
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.crossfade)) },
                            description = { Text(stringResource(Res.string.crossfade_description)) },
                            trailingContent = {
                                Switch(
                                    checked = crossfadeEnabled,
                                    onCheckedChange = { viewModel.setCrossfadeEnabled(it) }
                                )
                            }
                        )
                    )
                    if (crossfadeEnabled) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.crossfade_duration)) },
                                description = {
                                    Text(
                                        if (crossfadeDuration == DataStoreManager.Values.CROSSFADE_DURATION_AUTO) {
                                            stringResource(Res.string.crossfade_auto)
                                        } else {
                                            "${crossfadeDuration / 1000}s"
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = runBlocking { getString(Res.string.crossfade_duration) },
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = listOf(
                                                    (crossfadeDuration == DataStoreManager.Values.CROSSFADE_DURATION_AUTO) to
                                                            runBlocking { getString(Res.string.crossfade_auto) },
                                                    (crossfadeDuration == 1000) to "1s",
                                                    (crossfadeDuration == 2000) to "2s",
                                                    (crossfadeDuration == 3000) to "3s",
                                                    (crossfadeDuration == 5000) to "5s",
                                                    (crossfadeDuration == 8000) to "8s",
                                                    (crossfadeDuration == 10000) to "10s",
                                                    (crossfadeDuration == 12000) to "12s",
                                                    (crossfadeDuration == 15000) to "15s",
                                                    (crossfadeDuration == 20000) to "20s",
                                                    (crossfadeDuration == 30000) to "30s",
                                                ),
                                            ),
                                            confirm = runBlocking { getString(Res.string.change) } to { state ->
                                                val duration = when (state.selectOne?.getSelected()) {
                                                    runBlocking { getString(Res.string.crossfade_auto) } -> DataStoreManager.Values.CROSSFADE_DURATION_AUTO
                                                    "1s" -> 1000
                                                    "2s" -> 2000
                                                    "3s" -> 3000
                                                    "5s" -> 5000
                                                    "8s" -> 8000
                                                    "10s" -> 10000
                                                    "12s" -> 12000
                                                    "15s" -> 15000
                                                    "20s" -> 20000
                                                    "30s" -> 30000
                                                    else -> 5000
                                                }
                                                viewModel.setCrossfadeDuration(duration)
                                            },
                                            dismiss = runBlocking { getString(Res.string.cancel) },
                                        )
                                    )
                                }
                            )
                        )
                        if (getPlatform() == Platform.Android) {
                            add(
                                Material3SettingsItem(
                                    title = { Text(stringResource(Res.string.crossfade_dj_mode)) },
                                    description = { Text(stringResource(Res.string.crossfade_dj_mode_description)) },
                                    trailingContent = {
                                        Switch(
                                            checked = crossfadeDjMode,
                                            onCheckedChange = { viewModel.setCrossfadeDjMode(it) }
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SpotifySettingsContent(viewModel: SettingsViewModel, navController: NavController) {
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Spotify Account",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.log_in_to_spotify)) },
                        description = {
                            Text(
                                if (spotifyLoggedIn) stringResource(Res.string.logged_in)
                                else stringResource(Res.string.intro_login_to_spotify)
                            )
                        },
                        onClick = {
                            if (spotifyLoggedIn) {
                                viewModel.setSpotifyLogIn(false)
                            } else {
                                navController.navigate(SpotifyLoginDestination)
                            }
                        }
                    )
                )
            )
        }

        item {
            Material3SettingsGroup(
                title = "Metadata Enhancements",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.enable_spotify_lyrics)) },
                        description = { Text(stringResource(Res.string.spotify_lyrics_info)) },
                        enabled = spotifyLoggedIn,
                        trailingContent = {
                            Switch(
                                checked = spotifyLyrics && spotifyLoggedIn,
                                onCheckedChange = { viewModel.setSpotifyLyrics(it) },
                                enabled = spotifyLoggedIn
                            )
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.enable_canvas)) },
                        description = { Text(stringResource(Res.string.canvas_info)) },
                        enabled = spotifyLoggedIn,
                        trailingContent = {
                            Switch(
                                checked = spotifyCanvas && spotifyLoggedIn,
                                onCheckedChange = { viewModel.setSpotifyCanvas(it) },
                                enabled = spotifyLoggedIn
                            )
                        }
                    )
                )
            )
        }
    }
}

@Composable
private fun SponsorBlockSettingsContent(viewModel: SettingsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val enableSponsorBlockFlow = remember(viewModel.sponsorBlockEnabled) {
        viewModel.sponsorBlockEnabled.map { it == TRUE }
    }
    val enableSponsorBlock by enableSponsorBlockFlow.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Skip Configuration",
                items = buildList {
                    add(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.enable_sponsor_block)) },
                            description = { Text(stringResource(Res.string.skip_sponsor_part_of_video)) },
                            trailingContent = {
                                Switch(
                                    checked = enableSponsorBlock,
                                    onCheckedChange = { viewModel.setSponsorBlockEnabled(it) }
                                )
                            }
                        )
                    )
                    if (enableSponsorBlock) {
                        val listName = SponsorBlockType.toList().map { it.displayString() }
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.categories_sponsor_block)) },
                                description = { Text(stringResource(Res.string.what_segments_will_be_skipped)) },
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.setAlertData(
                                            SettingAlertState(
                                                title = getString(Res.string.categories_sponsor_block),
                                                multipleSelect = SettingAlertState.SelectData(
                                                    listSelect = listName.mapIndexed { index, item ->
                                                        (skipSegments?.contains(SponsorBlockType.toList().getOrNull(index)?.value) == true) to item
                                                    },
                                                ),
                                                confirm = getString(Res.string.save) to { state ->
                                                    viewModel.setSponsorBlockCategories(
                                                        state.multipleSelect?.getListSelected()?.map { selected ->
                                                            listName.indexOf(selected)
                                                        }?.mapNotNull { s ->
                                                            SponsorBlockType.toList().getOrNull(s)?.value
                                                        }?.toCollection(ArrayList()) ?: arrayListOf()
                                                    )
                                                },
                                                dismiss = getString(Res.string.cancel),
                                            )
                                        )
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalCalfApi::class)
@Composable
private fun BackupSettingsContent(viewModel: SettingsViewModel) {
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
                        trailingContent = {
                            Switch(
                                checked = backupDownloaded,
                                onCheckedChange = { viewModel.setBackupDownloaded(it) }
                            )
                        }
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
                                    is SettingsViewModel.BackupRestoreState.Success -> "✓ Backup complete!"
                                    is SettingsViewModel.BackupRestoreState.Error -> "✗ Backup failed"
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
                                    is SettingsViewModel.BackupRestoreState.Success -> "✓ Restore complete!"
                                    is SettingsViewModel.BackupRestoreState.Error -> "✗ Restore failed"
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

@Composable
private fun AboutSettingsContent(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    val sharedViewModel: SharedViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cookieBlobShape = remember {
        androidx.compose.foundation.shape.GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.width / 2f
            var first = true
            val points = 360
            for (i in 0..points) {
                val angle = (i * 2.0 * Math.PI / points).toFloat()
                val r = outerR * (0.95f + 0.05f * kotlin.math.cos(12f * angle))
                
                val px = cx + r * kotlin.math.cos(angle)
                val py = cy + r * kotlin.math.sin(angle)
                
                if (first) {
                    moveTo(px, py)
                    first = false
                } else {
                    lineTo(px, py)
                }
            }
            close()
        }
    }

    val devName = "Ansh Sharma"
    val devGitHub = "07-Ansh"
    val devAvatarUrl = "https://github.com/$devGitHub.png"
    val devFavSongVideoId = "dQw4w9WgXcQ"

    val wannaPlay = stringResource(Res.string.wanna_play_favorite_song)
    val yeah = stringResource(Res.string.yeah)

    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    val cardModifier = if (enableLiquidGlass) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(32.dp))
            .liquidGlass(backdrop, shape = RoundedCornerShape(32.dp), interactive = false)
    } else {
        Modifier.fillMaxWidth()
    }

    val cardColors = CardDefaults.elevatedCardColors(
        containerColor = if (enableLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            leadingContent = {
                                Image(
                                    painter = painterResource(Res.drawable.app_icon),
                                    contentDescription = "Sonique",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                )
                            },
                            title = {
                                Text(
                                    text = "Sonique",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            description = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "v${VersionManager.getVersionName()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                    ) {
                                        Text(
                                            text = "Open Source",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        )
                    )
                )
            }

            item {
                var leadClickCount by remember { mutableIntStateOf(0) }
                val fallback = painterResource(Res.drawable.app_icon)

                ElevatedCard(
                    shape = RoundedCornerShape(32.dp),
                    modifier = cardModifier,
                    colors = cardColors,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                onClick = {
                                    val newCount = leadClickCount + 1
                                    leadClickCount = newCount
                                    if (newCount >= 3) {
                                        leadClickCount = 0
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = wannaPlay,
                                                actionLabel = yeah,
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                sharedViewModel.loadSharedMediaItem(devFavSongVideoId)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(72.dp),
                                shape = cookieBlobShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 4.dp,
                            ) {
                                AsyncImage(
                                    model = devAvatarUrl,
                                    contentDescription = devName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    placeholder = fallback,
                                    fallback = fallback,
                                    error = fallback,
                                )
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = devName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(Res.string.credits_lead_developer),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://github.com/$devGitHub") },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_github),
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("GitHub", style = MaterialTheme.typography.labelMedium)
                            }
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://github.com/$devGitHub/Sonique") },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_github),
                                    contentDescription = "Repo",
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Repo", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { uriHandler.openUri("https://buymeacoffee.com/07ansh") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.buymeacoffee),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Buy me a coffee",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(Res.string.community_and_info),
                    items = listOf(
                        Material3SettingsItem(
                            iconPainter = painterResource(Res.drawable.ic_github),
                            title = { Text(stringResource(Res.string.credits_source_code)) },
                            onClick = { uriHandler.openUri("https://github.com/07-Ansh/Sonique") }
                        ),
                        Material3SettingsItem(
                            iconPainter = painterResource(Res.drawable.baseline_info_24),
                            title = { Text(stringResource(Res.string.credits_license_name)) },
                            description = { Text(stringResource(Res.string.credits_license_desc)) },
                            onClick = { uriHandler.openUri("https://github.com/07-Ansh/Sonique/blob/main/LICENSE") }
                        )
                    )
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Made with ❤️ and Kotlin",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "© 2025 Sonique",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


@Composable
private fun MinimalLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsContent(viewModel: SettingsViewModel, sharedViewModel: SharedViewModel, navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()

    val sendDataFlow = remember(viewModel.sendBackToGoogle) {
        viewModel.sendBackToGoogle.map { it == TRUE }
    }
    val sendData by sendDataFlow.collectAsStateWithLifecycle(initialValue = false)
    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val showMostPlayed by sharedViewModel.showMostPlayed.collectAsStateWithLifecycle()

    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.getAllGoogleAccount()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val langIndex = SUPPORTED_LANGUAGE.codes.indexOf(language)
                val langName = if (langIndex != -1) SUPPORTED_LANGUAGE.items[langIndex].toString() else ""

                Material3SettingsGroup(
                    title = "Regional & Content",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.youtube_account)) },
                            description = { Text(stringResource(Res.string.manage_your_youtube_accounts)) },
                            onClick = {
                                viewModel.getAllGoogleAccount()
                                showYouTubeAccountDialog = true
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.language)) },
                            description = { Text(langName) },
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.language),
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = SUPPORTED_LANGUAGE.codes.mapIndexed { index, code ->
                                                    (code == language) to SUPPORTED_LANGUAGE.items[index].toString()
                                                },
                                            ),
                                            confirm = getString(Res.string.change) to { state ->
                                                val selectedName = state.selectOne?.getSelected()
                                                val index = SUPPORTED_LANGUAGE.items.indexOfFirst { it.toString() == selectedName }
                                                if (index != -1) {
                                                    val code = SUPPORTED_LANGUAGE.codes[index]
                                                    viewModel.changeLanguage(code)
                                                }
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        )
                                    )
                                }
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.content_country)) },
                            description = { Text(location ?: "") },
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = getString(Res.string.content_country),
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = SUPPORTED_LOCATION.items.map { item ->
                                                    (item.toString() == location) to item.toString()
                                                },
                                            ),
                                            confirm = getString(Res.string.change) to { state ->
                                                val selectedName = state.selectOne?.getSelected()
                                                if (selectedName != null) {
                                                    viewModel.changeLocation(selectedName)
                                                }
                                            },
                                            dismiss = getString(Res.string.cancel),
                                        )
                                    )
                                }
                            }
                        )
                    )
                )
            }

            item {
                Material3SettingsGroup(
                    title = "Playback Preferences",
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.play_explicit_content)) },
                            description = { Text(stringResource(Res.string.play_explicit_content_description)) },
                            trailingContent = {
                                Switch(
                                    checked = explicitContentEnabled,
                                    onCheckedChange = { viewModel.setExplicitContentEnabled(it) }
                                )
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text("Offline Playlist Cache") },
                            description = { Text("Keep your YouTube playlists synchronized offline") },
                            trailingContent = {
                                Switch(
                                    checked = keepYoutubePlaylistOffline,
                                    onCheckedChange = { viewModel.setKeepYouTubePlaylistOffline(it) }
                                )
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text("Show Most Played") },
                            description = { Text("Show most played tracks under library suggestions") },
                            trailingContent = {
                                Switch(
                                    checked = showMostPlayed,
                                    onCheckedChange = { sharedViewModel.setShowMostPlayed(it) }
                                )
                            }
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(Res.string.send_back_listening_data_to_google)) },
                            description = { Text(stringResource(Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in)) },
                            trailingContent = {
                                Switch(
                                    checked = sendData,
                                    onCheckedChange = { viewModel.setSendBackToGoogle(it) }
                                )
                            }
                        )
                    )
                )
            }
        }

        if (showYouTubeAccountDialog) {
            BasicAlertDialog(
                onDismissRequest = { showYouTubeAccountDialog = false },
                modifier = Modifier.widthIn(min = 360.dp, max = 560.dp).fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                ) {
                    val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                        minActiveState = Lifecycle.State.RESUMED,
                    )

                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(Res.string.youtube_account),
                                style = typo().titleLarge,
                                color = white
                            )
                            IconButton(
                                onClick = { showYouTubeAccountDialog = false },
                                colors = IconButtonDefaults.iconButtonColors().copy(contentColor = white),
                            ) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Accounts List / Active Profile
                        if (googleAccounts is LocalResource.Success) {
                            val data = (googleAccounts as LocalResource.Success).data
                            if (data.isNullOrEmpty()) {
                                Text(
                                    stringResource(Res.string.no_account),
                                    style = typo().bodyMedium,
                                    color = white.copy(0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    data.forEach { account ->
                                        val isSelected = account.isUsed
                                        val cardBg = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(0.15f)
                                        } else {
                                            backgroundCard
                                        }
                                        val cardBorderColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(0.5f)
                                        } else {
                                            white.copy(0.08f)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(cardBg)
                                                .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                .clickable { viewModel.setUsedAccount(account) }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(modifier = Modifier.size(48.dp)) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                                        .data(account.thumbnailUrl)
                                                        .crossfade(550)
                                                        .build(),
                                                    placeholder = painterResource(Res.drawable.baseline_people_alt_24),
                                                    error = painterResource(Res.drawable.baseline_people_alt_24),
                                                    contentDescription = account.name,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .border(1.dp, white.copy(0.15f), CircleShape),
                                                )
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    account.name,
                                                    style = typo().labelLarge,
                                                    color = white
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    account.email,
                                                    style = typo().bodySmall,
                                                    color = white.copy(0.6f)
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(0.2f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        stringResource(Res.string.signed_in),
                                                        style = typo().labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            CenterLoadingBox(Modifier.fillMaxWidth().height(80.dp))
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = white.copy(0.08f))
                        Spacer(Modifier.height(12.dp))

                        // Actions Menu (Guest, Log out, Add account)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Guest Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setUsedAccount(null)
                                        showYouTubeAccountDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_people_alt_24),
                                    contentDescription = null,
                                    tint = white.copy(0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    stringResource(Res.string.guest),
                                    style = typo().bodyMedium,
                                    color = white.copy(0.85f)
                                )
                            }

                            // Add an Account Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showYouTubeAccountDialog = false
                                        navController.navigate(LoginDestination)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_playlist_add_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    stringResource(Res.string.add_an_account),
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Log out Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.setBasicAlertData(
                                                SettingBasicAlertState(
                                                    title = getString(Res.string.warning),
                                                    message = getString(Res.string.log_out_warning),
                                                    confirm = getString(Res.string.log_out) to {
                                                        viewModel.logOutAllYouTube()
                                                        showYouTubeAccountDialog = false
                                                    },
                                                    dismiss = getString(Res.string.cancel),
                                                ),
                                            )
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_close_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    stringResource(Res.string.log_out),
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// UpdatesSettingsContent has been moved to SettingsUpdateScreen.kt

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Composable
private fun StorageSettingsContent(viewModel: SettingsViewModel) {
    val platformContext = LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var width by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.getThumbCacheSize(platformContext)
    }

    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val canvasCache by viewModel.canvasCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Material3SettingsGroup(
                title = "Cache Allocation",
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.player_cache)) },
                        description = { Text("${playerCache.bytesToMB()} MB") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = getString(Res.string.clear_player_cache),
                                        message = null,
                                        confirm = getString(Res.string.clear) to { viewModel.clearPlayerCache() },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.downloaded_cache)) },
                        description = { Text("${downloadedCache.bytesToMB()} MB") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = getString(Res.string.clear_downloaded_cache),
                                        message = null,
                                        confirm = getString(Res.string.clear) to { viewModel.clearDownloadedCache() },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.thumbnail_cache)) },
                        description = { Text("${thumbnailCache.bytesToMB()} MB") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = getString(Res.string.clear_thumbnail_cache),
                                        message = null,
                                        confirm = getString(Res.string.clear) to { viewModel.clearThumbnailCache(platformContext) },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.spotify_canvas_cache)) },
                        description = { Text("${canvasCache.bytesToMB()} MB") },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = getString(Res.string.clear_canvas_cache),
                                        message = null,
                                        confirm = getString(Res.string.clear) to { viewModel.clearCanvasCache() },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(Res.string.limit_player_cache)) },
                        description = { Text(LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString()) },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = getString(Res.string.limit_player_cache),
                                        selectOne = SettingAlertState.SelectData(
                                            listSelect = LIMIT_CACHE_SIZE.items.map { item ->
                                                (item == LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache)) to item.toString()
                                            },
                                        ),
                                        confirm = getString(Res.string.change) to { state ->
                                            viewModel.setPlayerCacheLimit(
                                                LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected())
                                            )
                                        },
                                        dismiss = getString(Res.string.cancel),
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Storage Visualizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .onGloballyPositioned { layoutCoordinates ->
                                with(localDensity) {
                                    width = layoutCoordinates.size.width.toDp().value.toInt()
                                }
                            },
                    ) {
                        item {
                            Box(modifier = Modifier.width((fraction.otherApp * width).dp).background(MaterialTheme.colorScheme.primary).fillMaxHeight())
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.downloadCache * width).dp).background(Color(0xD540FF17)).fillMaxHeight())
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.playerCache * width).dp).background(Color(0xD5FFFF00)).fillMaxHeight())
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.canvasCache * width).dp).background(Color.Cyan).fillMaxHeight())
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.thumbCache * width).dp).background(Color.Magenta).fillMaxHeight())
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.appDatabase * width).dp).background(Color.White))
                        }
                        item {
                            Box(modifier = Modifier.width((fraction.freeSpace * width).dp).background(Color.DarkGray).fillMaxHeight())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LegendItem(Color(0xD540FF17), stringResource(Res.string.downloaded_cache))
                    LegendItem(musica_accent, stringResource(Res.string.player_cache))
                    LegendItem(Color.Cyan, stringResource(Res.string.spotify_canvas_cache))
                    LegendItem(Color.Magenta, stringResource(Res.string.thumbnail_cache))
                    LegendItem(Color.White, stringResource(Res.string.database))
                    LegendItem(Color.DarkGray, stringResource(Res.string.free_space))
                    LegendItem(MaterialTheme.colorScheme.primary, stringResource(Res.string.other_app))
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = typo().bodySmall)
    }
}
