package com.sonique.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import org.koin.compose.koinInject
import com.sonique.app.viewModel.SharedViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.component.liquidGlass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.ui.component.HomeItemContentPlaylist
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.theme.typo
import com.sonique.common.Config
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.utils.toTrack
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.holder

@Composable
fun SpeedDialSection(
    navController: NavController,
    data: HomeItem,
    onPlayClick: (Any) -> Unit // Callback for playing
) {
    if (data.contents.isEmpty()) return

    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Speed dial", // Static title as requested or data.title
                    style = typo().headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (enableLiquidGlass) {
            // Horizontal List of Big Boxes
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(data.contents.filterNotNull()) { item ->
                    SpeedDialBigItem(
                        item = item,
                        navController = navController,
                        onPlayClick = onPlayClick
                    )
                }
            }
        } else {
            // Chunk items into groups of 9 (3x3 grid)
            val chunks = data.contents.filterNotNull().chunked(9)
            val pagerState = rememberPagerState(pageCount = { chunks.size })

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageItems = chunks.getOrNull(page) ?: emptyList()
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3 Rows
                    val rows = pageItems.chunked(3)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 3 Columns
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SpeedDialGridItem(
                                        item = item,
                                        navController = navController,
                                        onPlayClick = onPlayClick
                                    )
                                }
                            }
                            // Fill empty spaces if last row has < 3 items
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Pager Indicator (Dots)
            if (chunks.size > 1) {
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedDialBigItem(
    item: com.sonique.domain.data.model.home.Content,
    navController: NavController,
    onPlayClick: (Any) -> Unit
) {
    val context = LocalPlatformContext.current
    
    val onClick = {
         val browseId = item.browseId
         val playlistId = item.playlistId
         if (playlistId != null && (item.videoId == null || item.videoId == "")) {
             navController.navigate(PlaylistDestination(playlistId))
         } else if (browseId != null && (item.videoId == null || item.videoId == "")) {
             if (browseId.startsWith("UC")) {
                 navController.navigate(ArtistDestination(browseId))
             } else {
                 navController.navigate(AlbumDestination(browseId))
             }
         } else {
             onPlayClick(item)
         }
    }

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.thumbnails.lastOrNull()?.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(1f)
                .clip(cardShape),
            placeholder = painterResource(Res.drawable.holder),
            error = painterResource(Res.drawable.holder)
        )

        // Title and Artists below the card
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                style = typo().bodyMedium,
                color = Color.White,
                maxLines = 1,
            )
            val subtitle = item.artists?.joinToString { it.name } ?: item.description ?: ""
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = typo().bodySmall,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SpeedDialGridItem(
    item: com.sonique.domain.data.model.home.Content,
    navController: NavController,
    onPlayClick: (Any) -> Unit
) {
    val context = LocalPlatformContext.current
    
    val onClick = {
         val browseId = item.browseId
         val playlistId = item.playlistId
         if (playlistId != null && (item.videoId == null || item.videoId == "")) {
             navController.navigate(PlaylistDestination(playlistId))
         } else if (browseId != null && (item.videoId == null || item.videoId == "")) {
             if (browseId.startsWith("UC")) {
                 navController.navigate(ArtistDestination(browseId))
             } else {
                 navController.navigate(AlbumDestination(browseId))
             }
         } else {
             onPlayClick(item)
         }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.thumbnails.lastOrNull()?.url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder)
            )
            
            // Text Overlay Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )

            // Text overlaid inside
            Text(
                text = item.title,
                style = typo().labelMedium,
                color = Color.White,
                maxLines = 2,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}
