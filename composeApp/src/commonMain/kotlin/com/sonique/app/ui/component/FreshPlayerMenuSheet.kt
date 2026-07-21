package com.sonique.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kmpalette.rememberPaletteState
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.viewModel.NowPlayingBottomSheetUIEvent
import com.sonique.app.viewModel.NowPlayingBottomSheetViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.data.entities.SongEntity
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_access_alarm_24
import sonique.composeapp.generated.resources.baseline_album_24
import sonique.composeapp.generated.resources.baseline_favorite_24
import sonique.composeapp.generated.resources.baseline_favorite_border_24
import sonique.composeapp.generated.resources.baseline_people_alt_24
import sonique.composeapp.generated.resources.baseline_playlist_add_24
import sonique.composeapp.generated.resources.baseline_queue_music_24
import sonique.composeapp.generated.resources.baseline_sensors_24
import sonique.composeapp.generated.resources.baseline_share_24
import sonique.composeapp.generated.resources.download

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshPlayerMenuSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel,
    onShowSleepTimer: () -> Unit = {},
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()

    val paletteState = rememberPaletteState()

    LaunchedEffect(screenDataState.bitmap) {
        screenDataState.bitmap?.let { bitmap ->
            paletteState.generate(bitmap)
        }
    }

    val extractedArtworkColor = paletteState.palette?.getColorFromPalette()

    val songUIState = uiState.songUIState
    val isLiked = songUIState.liked
    val songTitle = song?.title ?: songUIState.title
    val artistName = song?.artistName?.joinToString(", ") ?: songUIState.listArtists.joinToString(", ") { it.name }
    val thumbnailUrl = song?.thumbnails ?: songUIState.thumbnails ?: ""
    val videoId = song?.videoId ?: songUIState.videoId

    val defaultSheetBg = extractedArtworkColor?.copy(alpha = 0.92f) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val finalBg = backgroundColor ?: defaultSheetBg
    val finalContent = contentColor ?: Color.White

    // Dynamic translucent card tinting for Ambience Mode
    val cardBg = Color.White.copy(alpha = 0.15f)

    val animatedBg by animateColorAsState(
        targetValue = finalBg,
        animationSpec = tween(300),
        label = "sheetBg"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = animatedBg,
        contentColor = finalContent,
    ) {
        LazyColumn(
            contentPadding = WindowInsets.systemBars.asPaddingValues(),
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                    ) {
                        if (thumbnailUrl.isNotEmpty()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = finalContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = finalContent.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Actions Grid (MetroList style with translucent dynamic artwork cards)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                NewActionGrid(
                    actions = listOf(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_queue_music_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = "Play Next",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
                                onDismiss()
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_playlist_add_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = "Add to Queue",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
                                onDismiss()
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_sensors_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = "Radio",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.StartRadio(videoId, songTitle))
                                onDismiss()
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (isLiked) Res.drawable.baseline_favorite_24 else Res.drawable.baseline_favorite_border_24
                                    ),
                                    contentDescription = null,
                                    tint = if (isLiked) Color.Red else finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = if (isLiked) "Liked" else "Like",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.download),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = "Download",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                                onDismiss()
                            }
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_share_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = "Share",
                            backgroundColor = cardBg,
                            contentColor = finalContent,
                            onClick = {
                                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share)
                                onDismiss()
                            }
                        )
                    ),
                    columns = 3,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Options Menu Groups
            item {
                val menuItems = mutableListOf<Material3MenuItemData>()

                menuItems.add(
                    Material3MenuItemData(
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_playlist_add_24),
                                contentDescription = null,
                                tint = finalContent,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = { Text("Add to playlist", color = finalContent) },
                        cardColors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardBg),
                        onClick = {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(0L))
                            onDismiss()
                        }
                    )
                )

                songUIState.listArtists.firstOrNull()?.id?.let { artistId ->
                    menuItems.add(
                        Material3MenuItemData(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_people_alt_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            title = { Text("Go to artist", color = finalContent) },
                            cardColors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardBg),
                            onClick = {
                                navController.navigate(ArtistDestination(artistId))
                                onDismiss()
                            }
                        )
                    )
                }

                songUIState.album?.id?.let { albumId ->
                    menuItems.add(
                        Material3MenuItemData(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_album_24),
                                    contentDescription = null,
                                    tint = finalContent,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            title = { Text("Go to album", color = finalContent) },
                            cardColors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardBg),
                            onClick = {
                                navController.navigate(AlbumDestination(albumId))
                                onDismiss()
                            }
                        )
                    )
                }

                menuItems.add(
                    Material3MenuItemData(
                        icon = {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_access_alarm_24),
                                contentDescription = null,
                                tint = finalContent,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = { Text("Sleep timer", color = finalContent) },
                        cardColors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cardBg),
                        onClick = {
                            onShowSleepTimer()
                            onDismiss()
                        }
                    )
                )

                Material3MenuGroup(items = menuItems)
            }
        }
    }
}
