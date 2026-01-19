package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import com.sonique.app.Platform
import com.sonique.app.extension.bytesToMB
import com.sonique.app.getPlatform
import com.sonique.app.ui.component.SettingBasicDialog
import com.sonique.app.ui.component.SettingDialog
import com.sonique.app.ui.component.SettingItem
import com.sonique.app.ui.theme.md_theme_dark_primary
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.LIMIT_CACHE_SIZE
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStorageScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    if (getPlatform() != Platform.Android) return

    val platformContext = LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var width by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getThumbCacheSize(platformContext)
    }

    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val canvasCache by viewModel.canvasCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()

    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    val basicAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()

    if (alertData != null) {
        SettingDialog(
            alert = alertData!!,
            onDismiss = { viewModel.setAlertData(null) }
        )
    }
    if (basicAlertData != null) {
        SettingBasicDialog(
            alert = basicAlertData!!,
            onDismiss = { viewModel.setBasicAlertData(null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage") },
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
                    title = stringResource(Res.string.player_cache),
                    subtitle = "${playerCache.bytesToMB()} MB",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = getString(Res.string.clear_player_cache),
                                    message = null,
                                    confirm = getString(Res.string.clear) to {
                                        viewModel.clearPlayerCache()
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.downloaded_cache),
                    subtitle = "${downloadedCache.bytesToMB()} MB",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = getString(Res.string.clear_downloaded_cache),
                                    message = null,
                                    confirm = getString(Res.string.clear) to {
                                        viewModel.clearDownloadedCache()
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.thumbnail_cache),
                    subtitle = "${thumbnailCache.bytesToMB()} MB",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = getString(Res.string.clear_thumbnail_cache),
                                    message = null,
                                    confirm = getString(Res.string.clear) to {
                                        viewModel.clearThumbnailCache(platformContext)
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.spotify_canvas_cache),
                    subtitle = "${canvasCache.bytesToMB()} MB",
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = getString(Res.string.clear_canvas_cache),
                                    message = null,
                                    confirm = getString(Res.string.clear) to {
                                        viewModel.clearCanvasCache()
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.limit_player_cache),
                    subtitle = LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString(),
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
                                            LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected()),
                                        )
                                    },
                                    dismiss = getString(Res.string.cancel),
                                ),
                            )
                        }
                    },
                )

                // Visualizer
                Box(
                    Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .onGloballyPositioned { layoutCoordinates ->
                                with(localDensity) {
                                    width = layoutCoordinates.size.width.toDp().value.toInt()
                                }
                            },
                    ) {
                        item {
                            Box(modifier = Modifier.width((fraction.otherApp * width).dp).background(md_theme_dark_primary).fillMaxHeight())
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
                }

                // Legend
                Column {
                    LegendItem(Color(0xD540FF17), stringResource(Res.string.downloaded_cache))
                    LegendItem(musica_accent, stringResource(Res.string.player_cache))
                    LegendItem(Color.Cyan, stringResource(Res.string.spotify_canvas_cache))
                    LegendItem(Color.Magenta, stringResource(Res.string.thumbnail_cache))
                    LegendItem(Color.White, stringResource(Res.string.database))
                    LegendItem(Color.LightGray, stringResource(Res.string.free_space))
                    LegendItem(md_theme_dark_primary, stringResource(Res.string.other_app))
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
