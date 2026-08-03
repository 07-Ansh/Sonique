package com.sonique.app.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.sonique.app.extension.angledGradientBackground
import com.sonique.app.extension.getColorFromPalette
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.DescriptionView
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.overlayMedium
import com.sonique.app.ui.theme.textHighEmphasis
import com.sonique.app.ui.theme.white
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import sonique.composeapp.generated.resources.baseline_play_circle_24
import sonique.composeapp.generated.resources.baseline_shuffle_24
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.no_description

import com.sonique.app.expect.ui.toImageBitmap

enum class DetailType {
    ALBUM,
    ARTIST,
    LOCAL_PLAYLIST,
    MORE_ALBUMS,
    YOUTUBE_PLAYLIST,
    PODCAST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailContentScreen(
    type: DetailType,
    id: String,
    secondaryId: String? = null,
    isYourYouTubePlaylist: Boolean = false,
    navController: NavController,
    onScrolling: (Boolean) -> Unit = {},
    innerPadding: PaddingValues = PaddingValues()
) {
    // Dynamic switching based on type selected
    when (type) {
        DetailType.ALBUM -> {
            AlbumScreen(
                browseId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.ARTIST -> {
            ArtistScreen(
                channelId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.LOCAL_PLAYLIST -> {
            com.sonique.app.ui.screen.library.LocalPlaylistScreen(
                id = id.toLongOrNull() ?: 0L,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.MORE_ALBUMS -> {
            MoreAlbumsScreen(
                innerPadding = innerPadding,
                navController = navController,
                type = id,
                id = secondaryId ?: ""
            )
        }
        DetailType.YOUTUBE_PLAYLIST -> {
            PlaylistScreen(
                playlistId = id,
                isYourYouTubePlaylist = isYourYouTubePlaylist,
                navController = navController,
                onScrolling = onScrolling
            )
        }
        DetailType.PODCAST -> {
            PodcastScreen(
                podcastId = id,
                navController = navController,
                onScrolling = onScrolling
            )
        }
    }
}

/**
 * A highly polished, single unified design template for all content detail pages.
 * Integrates dynamic cover art color extraction, a collapsing layout, action rows,
 * and a standardized LazyColumn content layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDetailTemplate(
    title: String,
    subtitle: String?,
    description: String?,
    thumbnailUrl: String?,
    isCircleImage: Boolean = false,
    listColors: List<Color>,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    playButtonContent: @Composable () -> Unit,
    onShuffleClick: (() -> Unit)? = null,
    onHeartClick: (@Composable () -> Unit)? = null,
    downloadButtonContent: (@Composable () -> Unit)? = null,
    onPaletteGenerated: (List<Color>) -> Unit = {},
    lazyState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    listModifier: Modifier = Modifier,
    additionalActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val uriHandler = LocalUriHandler.current ?: androidx.compose.ui.platform.LocalUriHandler.current
    val paletteState = rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(bitmap) {
        bitmap?.let { paletteState.generate(it) }
    }

    LaunchedEffect(paletteState.palette) {
        paletteState.palette?.let {
            onPaletteGenerated(listOf(it.getColorFromPalette(), md_theme_dark_background))
        }
    }

    val firstItemVisible by remember {
        derivedStateOf { lazyState.firstVisibleItemIndex == 0 }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    Box(modifier = Modifier.fillMaxSize().background(md_theme_dark_background)) {
        // dynamic ambient background gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .angledGradientBackground(listColors, 25f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, overlayMedium, md_theme_dark_background)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().then(listModifier),
            state = lazyState,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(56.dp)) // Status bar offset
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RippleIconButton(
                            resId = Res.drawable.baseline_arrow_back_ios_new_24,
                            onClick = onBack
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Centered Large Cover Art Image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(thumbnailUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(Res.drawable.holder),
                        error = painterResource(Res.drawable.holder),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { result ->
                            bitmap = result.result.image.toImageBitmap()
                        },
                        modifier = Modifier
                            .size(230.dp)
                            .aspectRatio(1f)
                            .clip(if (isCircleImage) CircleShape else RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Centered Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )

                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            color = Color.LightGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Centered Unified Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (onShuffleClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onShuffleClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.baseline_shuffle_24),
                                    contentDescription = "Shuffle",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Play Button
                        playButtonContent()

                        Spacer(modifier = Modifier.width(16.dp))

                        if (downloadButtonContent != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                downloadButtonContent()
                            }
                        } else if (onHeartClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                onHeartClick()
                            }
                        }

                        additionalActions()
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description if any
                    if (!description.isNullOrEmpty() && description != "null") {
                        DescriptionView(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            text = description,
                            onTimeClicked = {},
                            onURLClicked = { url ->
                                uriHandler.openUri(url)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CenterLoadingBox(modifier = Modifier)
                    }
                }
            } else {
                content()
            }
        }

        // Top sticky app bar when scrolled
        AnimatedVisibility(
            visible = shouldHideTopBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    RippleIconButton(
                        resId = Res.drawable.baseline_arrow_back_ios_new_24,
                        onClick = onBack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = md_theme_dark_background
                )
            )
        }
    }
}
