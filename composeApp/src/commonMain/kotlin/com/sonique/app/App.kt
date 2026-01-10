package com.sonique.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.toUri
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.logger.Logger
import com.sonique.app.expect.Orientation
import com.sonique.app.expect.currentOrientation

import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.extension.copy
import com.sonique.app.ui.component.AppBottomNavigationBar
import com.sonique.app.ui.component.AppNavigationRail

import com.sonique.app.ui.navigation.destination.home.HomeDestination
import com.sonique.app.ui.navigation.destination.home.NotificationDestination
import com.sonique.app.ui.navigation.destination.library.LibraryDestination
import com.sonique.app.ui.navigation.destination.list.AlbumDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.navigation.destination.player.FullscreenDestination
import com.sonique.app.ui.navigation.graph.AppNavigationGraph
import com.sonique.app.ui.screen.MiniPlayer
import com.sonique.app.ui.screen.player.NowPlayingScreen
import com.sonique.app.ui.screen.player.NowPlayingScreenContent
import com.sonique.app.ui.theme.AppTheme
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SharedViewModel
import com.sonique.app.viewModel.UpdateViewModel
import com.sonique.app.ui.component.UpdateDialog

import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.good_night
import sonique.composeapp.generated.resources.sleep_timer_off
import sonique.composeapp.generated.resources.this_link_is_not_supported
import sonique.composeapp.generated.resources.yes
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import kotlin.math.roundToInt



@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: SharedViewModel = koinInject(),
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val navController = rememberNavController()

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()
    
    val updateViewModel: UpdateViewModel = koinInject()
    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()

    if (updateAvailable != null) {
        UpdateDialog(
            releaseInfo = updateAvailable!!,
            onDismiss = { updateViewModel.dismissUpdate() }
        )
    }
    


     
    var isShowMiniPlayer by rememberSaveable {
        mutableStateOf(true)
    }

     
    var isShowNowPlaylistScreen by rememberSaveable {
        mutableStateOf(false)
    }

     
    var isInFullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isNavBarVisible by rememberSaveable {
        mutableStateOf(true)
    }





    val reloadDestination by viewModel.reloadDestination.collectAsStateWithLifecycle()

    LaunchedEffect(reloadDestination) {
        val destination = reloadDestination
        if (destination != null) {
            try {
                navController.popBackStack(destination, false)
            } catch (e: Exception) {
                Logger.e("App", "Error reloading destination", e)
            }
            viewModel.reloadDestinationDone()
        }
    }

    LaunchedEffect(nowPlayingData) {
        isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
    }

    LaunchedEffect(intent) {
        val intent = intent ?: return@LaunchedEffect
        val data = intent.data
        Logger.d("MainActivity", "onCreate: $data")
        if (data != null) {
            if (data == "com.sonique.com.sonique.app://notification".toUri()) {
                viewModel.setIntent(null)
                navController.navigate(
                    NotificationDestination,
                )
            } else if (data == "com.sonique.com.sonique.app://downloads".toUri()) {
                viewModel.setIntent(null)
                navController.navigate(
                    LibraryDestination(
                        openDownloads = true,
                    ),
                )
            } else {
                Logger.d("MainActivity", "onCreate: $data")
                when (val path = data.pathSegments.firstOrNull()) {
                    "playlist" ->
                        data
                            .getQueryParameter("list")
                            ?.let { playlistId ->
                                viewModel.setIntent(null)
                                if (playlistId.startsWith("OLAK5uy_")) {
                                    navController.navigate(
                                        AlbumDestination(
                                            browseId = playlistId,
                                        ),
                                    )
                                } else if (playlistId.startsWith("VL")) {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = playlistId,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = "VL$playlistId",
                                        ),
                                    )
                                }
                            }

                    "channel", "c" ->
                        data.lastPathSegment?.let { artistId ->
                            if (artistId.startsWith("UC")) {
                                viewModel.setIntent(null)
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = artistId,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(
                                    getString(
                                        Res.string.this_link_is_not_supported,
                                    ),
                                )
                            }
                        }

                    else ->
                        when {
                            path == "watch" -> data.getQueryParameter("v")
                            data.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                }
            }
        }
    }



    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        Logger.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
        if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
            isShowNowPlaylistScreen = false
        }
        isInFullscreen = navBackStackEntry?.destination?.hierarchy?.any {
            it.hasRoute(FullscreenDestination::class)
        } == true
    }
    var isScrolledToTop by rememberSaveable {
        mutableStateOf(false)
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE

    val backdrop = rememberBackdrop()

    val coreScope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.foundation.layout.BoxWithConstraints {
        val screenHeight = maxHeight
        val playerOffsetY = androidx.compose.runtime.remember {
            androidx.compose.animation.core.Animatable(screenHeight.value)
        }
        var isPlayerExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        LaunchedEffect(isShowNowPlaylistScreen) {
             val target = if (isShowNowPlaylistScreen) 0f else screenHeight.value
             if (kotlin.math.abs(playerOffsetY.value - target) > 0.5f) {
                 playerOffsetY.animateTo(
                     target,
                     animationSpec = androidx.compose.animation.core.tween(
                         durationMillis = 500,
                         easing = androidx.compose.animation.core.EaseInOut
                     )
                 )
             }
             isPlayerExpanded = isShowNowPlaylistScreen
        }

        AppTheme {
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    bottomBar = {
                        if (!isTablet) {
                            AnimatedVisibility(
                                isNavBarVisible,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                Column {
                                    AnimatedVisibility(
                                        isShowMiniPlayer,
                                        enter = fadeIn() + slideInHorizontally(),
                                        exit = fadeOut(),
                                    ) {
                                        MiniPlayer(
                                            Modifier
                                                .height(64.dp)
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 12.dp,
                                                ).padding(
                                                    bottom = 4.dp,
                                                ),
                                            backdrop = backdrop,
                                            onClick = {
                                                isShowNowPlaylistScreen = true
                                            },
                                            onClose = {
                                                viewModel.stopPlayer()
                                                viewModel.isServiceRunning = false
                                            },
                                            onDrag = { delta ->
                                                coreScope.launch {
                                                    playerOffsetY.snapTo((playerOffsetY.value + delta).coerceIn(0f, screenHeight.value))
                                                }
                                            },
                                            onDragEnd = {
                                                coreScope.launch {
                                                    val expandThreshold = screenHeight.value * 0.7f
                                                    val collapseThreshold = screenHeight.value * 0.3f
                                                    
                                                    val shouldExpand = if (playerOffsetY.value < expandThreshold) true else isShowNowPlaylistScreen
                                                    
                                                     
                                                    val spec = androidx.compose.animation.core.tween<Float>(
                                                        durationMillis = 500,
                                                        easing = androidx.compose.animation.core.EaseInOut
                                                    )

                                                    if (shouldExpand) {
                                                        if (!isShowNowPlaylistScreen) {
                                                            isShowNowPlaylistScreen = true
                                                        } else {
                                                            playerOffsetY.animateTo(0f, spec)
                                                        }
                                                    } else {
                                                        if (isShowNowPlaylistScreen) {
                                                            isShowNowPlaylistScreen = false
                                                        } else {
                                                            playerOffsetY.animateTo(screenHeight.value, spec)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    AppBottomNavigationBar(
                                        navController = navController,
                                        isTranslucentBackground = false,
                                    ) { klass ->
                                        viewModel.reloadDestination(klass)
                                    }
                                }
                            }
                        }
                    },
                    content = { innerPadding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        ) {
                            Row(
                                Modifier.fillMaxSize(),
                            ) {
                                 
                                if (isTablet && !isInFullscreen) {
                                    AppNavigationRail(
                                        navController = navController,
                                    ) { klass ->
                                        viewModel.reloadDestination(klass)
                                    }
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxSize(),
                                    ) {
                                        AppNavigationGraph(
                                            innerPadding = innerPadding,
                                            navController = navController,
                                            hideNavBar = {
                                                isNavBarVisible = false
                                            },
                                            showNavBar = {
                                                isNavBarVisible = true
                                            },
                                            showNowPlayingSheet = {
                                                isShowNowPlaylistScreen = true
                                            },
                                            onScrolling = {
                                                isScrolledToTop = it
                                            },
                                        )
                                    }
                                    this@Row.AnimatedVisibility(
                                        modifier =
                                        Modifier
                                            .padding(innerPadding)
                                            .align(Alignment.BottomCenter),
                                        visible = isShowMiniPlayer && isTablet && !isInFullscreen,
                                        enter = fadeIn() + slideInHorizontally(),
                                        exit = fadeOut(),
                                    ) {
                                        MiniPlayer(
                                            if (getPlatform() == Platform.Android) {
                                                Modifier
                                                    .height(56.dp)
                                                    .fillMaxWidth(0.8f)
                                                    .padding(
                                                        horizontal = 12.dp,
                                                    ).padding(
                                                        bottom = 4.dp,
                                                    )
                                            } else {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(84.dp)
                                                    .background(Color.Transparent)
                                            },
                                            backdrop = backdrop,
                                            onClick = {
                                                isShowNowPlaylistScreen = true
                                            },
                                            onClose = {
                                                viewModel.stopPlayer()
                                                viewModel.isServiceRunning = false
                                            },
                                            onDrag = {},
                                            onDragEnd = {}
                                        )
                                    }
                                }
                                 
                                if (isTablet && isTabletLandscape && !isInFullscreen) {
                                    AnimatedVisibility(
                                        isShowNowPlaylistScreen,
                                        enter = expandHorizontally() + fadeIn(),
                                        exit = fadeOut() + shrinkHorizontally(),
                                    ) {
                                        Row(
                                            Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.35f),
                                        ) {
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                Modifier
                                                    .padding(
                                                        innerPadding.copy(
                                                            start = 0.dp,
                                                            top = 0.dp,
                                                            bottom = 0.dp,
                                                        ),
                                                    ).clip(
                                                        RoundedCornerShape(12.dp),
                                                    ),
                                            ) {
                                                NowPlayingScreenContent(
                                                    navController = navController,
                                                    sharedViewModel = viewModel,
                                                    isExpanded = true,
                                                    dismissIcon = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                                    onSwipeEnabledChange = {},
                                                ) {
                                                    isShowNowPlaylistScreen = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (sleepTimerState.isDone) {
                            Logger.w("MainActivity", "Sleep Timer Done: $sleepTimerState")
                            AlertDialog(
                                properties =
                                DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false,
                                ),
                                onDismissRequest = {
                                    viewModel.stopSleepTimer()
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.stopSleepTimer()
                                    }) {
                                        Text(
                                            stringResource(Res.string.yes),
                                            style = typo().bodySmall,
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        stringResource(Res.string.sleep_timer_off),
                                        style = typo().labelSmall,
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(Res.string.good_night),
                                        style = typo().bodySmall,
                                    )
                                },
                            )
                        }


                    },
                )
                if (!isTabletLandscape) {
                    if (playerOffsetY.value < screenHeight.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { androidx.compose.ui.unit.IntOffset(0, playerOffsetY.value.roundToInt()) }
                        ) {
                            NowPlayingScreen(
                                navController = navController,
                            ) {
                                isShowNowPlaylistScreen = false
                            }
                        }
                    }
                }
            }
        }
    }
}

