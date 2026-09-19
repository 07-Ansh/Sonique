package com.sonique.app.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.layerBackdrop
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.artworkScrimBrush
import com.sonique.app.extension.formatTimeAgo
import com.sonique.app.extension.rgbFactor
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.LiquidGlassIconButton
import com.sonique.app.ui.component.liquidGlass
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.NotificationViewModel
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.domain.data.entities.NotificationEntity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.album
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.delete
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.new_release
import sonique.composeapp.generated.resources.no_notification
import sonique.composeapp.generated.resources.notification
import sonique.composeapp.generated.resources.singles

private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val ROW_SHAPE = RoundedCornerShape(18.dp)
private val BACK_BUTTON_STRIP = 56.dp

@Composable
fun NotificationScreen(
    navController: NavController,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: NotificationViewModel = koinViewModel(),
) {
    val listNotification by viewModel.listNotification.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    val sharedViewModel: SharedViewModel = koinInject()
    val enableLiquidGlass by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val backdrop = rememberBackdrop()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val bg = MaterialTheme.colorScheme.background
        val glow = if (bg.luminance() > 0.5f) {
            lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
        } else {
            MaterialTheme.colorScheme.primary.rgbFactor(0.3f)
        }

        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            Box(Modifier.matchParentSize().background(bg))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .angledGradientBackground(listOf(glow, bg), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(bg)),
                )
            }
        }

        Crossfade(
            targetState = listNotification,
            label = "NotificationContentCrossfade"
        ) { notifications ->
            if (notifications == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CenterLoadingBox(modifier = Modifier.align(Alignment.Center))
                }
            } else if (notifications.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "header_spacer") {
                        Spacer(Modifier.height(BACK_BUTTON_STRIP))
                    }

                    item(key = "title_block") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(Res.string.notification),
                                    style = typo().titleLarge.copy(
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                Chip(
                                    text = "Clear all",
                                    icon = Icons.Outlined.DeleteSweep,
                                    onClick = { showClearDialog = true }
                                )
                            }

                            Text(
                                text = "Latest releases from artists you follow",
                                style = typo().bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${notifications.size} ${if (notifications.size == 1) "update" else "updates"}",
                                        style = typo().labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationItem(
                            notification = notification,
                            navController = navController,
                            enableLiquidGlass = enableLiquidGlass,
                            backdrop = backdrop,
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                    }

                    item(key = "end_of_page") {
                        Spacer(Modifier.height(8.dp))
                        EndOfPage(withoutCredit = true)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(top = innerPadding.calculateTopPadding() + 8.dp),
                    ) {
                        Spacer(Modifier.height(BACK_BUTTON_STRIP))
                        Text(
                            text = stringResource(Res.string.notification),
                            style = typo().titleLarge.copy(
                                fontSize = 32.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Latest releases from artists you follow",
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 60.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CARD_SHAPE)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)),
                                            CARD_SHAPE
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsNone,
                                        contentDescription = null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                Text(
                                    text = stringResource(Res.string.no_notification),
                                    style = typo().titleLarge.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "When artists you follow release new music, you'll be notified right here.",
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        LiquidGlassIconButton(
            backdrop = backdrop,
            resId = Res.drawable.baseline_arrow_back_ios_new_24,
            tint = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 12.dp, top = 8.dp)
                .size(48.dp),
            onClick = { navController.navigateUp() },
        )

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                shape = CARD_SHAPE,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Clear All Notifications",
                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to clear all notifications? This action cannot be undone.",
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllNotifications()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.delete),
                            style = typo().labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearDialog = false }
                    ) {
                        Text(
                            text = "Cancel",
                            style = typo().labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    navController: NavController,
    enableLiquidGlass: Boolean,
    backdrop: PlatformBackdrop,
    onDelete: () -> Unit,
) {
    val cardModifier = if (enableLiquidGlass) {
        Modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(Color.White.copy(alpha = 0.04f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)), CARD_SHAPE)
            .liquidGlass(backdrop, shape = CARD_SHAPE, interactive = false)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)), CARD_SHAPE)
    }

    Card(
        modifier = cardModifier,
        shape = CARD_SHAPE,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !notification.channelId.startsWith("SYSTEM")) {
                            navController.navigate(
                                ArtistDestination(channelId = notification.channelId)
                            )
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thumb = notification.thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(thumb)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumb)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(Res.drawable.holder),
                        error = painterResource(Res.drawable.holder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.new_release).uppercase(),
                                    style = typo().labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "·",
                                style = typo().labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = notification.time.formatTimeAgo(),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = notification.name,
                            style = typo().titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                ActionGlyph(
                    icon = Icons.Outlined.Close,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onDelete
                )
            }

            val hasSingles = notification.single.isNotEmpty()
            val hasAlbums = notification.album.isNotEmpty()

            if (hasSingles || hasAlbums) {
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notification.single) { single ->
                        ItemAlbumNotification(
                            isAlbum = false,
                            browseId = single["browseId"] ?: "",
                            title = single["title"] ?: "",
                            thumbnail = single["thumbnails"],
                            navController = navController,
                        )
                    }
                    items(notification.album) { album ->
                        ItemAlbumNotification(
                            isAlbum = true,
                            browseId = album["browseId"] ?: "",
                            title = album["title"] ?: "",
                            thumbnail = album["thumbnails"],
                            navController = navController,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemAlbumNotification(
    isAlbum: Boolean,
    browseId: String,
    title: String,
    thumbnail: String?,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(ROW_SHAPE)
            .clickable {
                navController.navigate(
                    AlbumDestination(browseId = browseId)
                )
            }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                    RoundedCornerShape(14.dp)
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumbnail)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isAlbum) stringResource(Res.string.album) else stringResource(Res.string.singles),
                    style = typo().labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = typo().bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    animationMode = MarqueeAnimationMode.Immediately,
                )
                .focusable(),
        )
    }
}

@Composable
private fun ActionGlyph(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Chip(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f), CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = text,
                style = typo().labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
