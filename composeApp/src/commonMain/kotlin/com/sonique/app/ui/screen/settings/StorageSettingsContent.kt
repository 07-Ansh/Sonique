package com.sonique.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.LocalPlatformContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.extension.bytesToMB
import com.sonique.app.ui.component.Material3SettingsGroup
import com.sonique.app.ui.component.Material3SettingsItem
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SettingAlertState
import com.sonique.app.viewModel.SettingBasicAlertState
import com.sonique.app.viewModel.SettingsViewModel
import com.sonique.common.LIMIT_CACHE_SIZE
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@Composable
internal fun StorageSettingsContent(viewModel: SettingsViewModel) {
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
