package com.sonique.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sonique.app.expect.ui.layerBackdrop
import com.sonique.app.expect.ui.LocalLiquidGlassEnabled
import com.sonique.app.extension.copy
import com.sonique.app.ui.component.AppBottomNavigationBar
import com.sonique.app.ui.component.AppNavigationRail
import com.sonique.app.ui.component.LiquidGlassAppBottomNavigationBar

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
import kotlin.reflect.KClass
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()
    
    val updateViewModel: UpdateViewModel = koinInject()
    val updateAvailable by updateViewModel.updateAvailable.collectAsStateWithLifecycle()

     
    val hasMedia = remember(nowPlayingData) {
        nowPlayingData?.isNotEmpty() == true
    }
    val isSettings = navBackStackEntry?.destination?.route?.contains("Settings") == true
    val isShowMiniPlayer = hasMedia && !isSettings

    var isShowNowPlaylistScreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isInFullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isNavBarVisible by rememberSaveable {
        mutableStateOf(true)
    }

    val currentRoute = navBackStackEntry?.destination?.route
    val isAtHome = currentRoute?.contains("HomeDestination") == true

    com.sonique.app.expect.ui.BackHandler(
        enabled = isShowNowPlaylistScreen || !isAtHome
    ) {
        try {
            if (isShowNowPlaylistScreen) {
                isShowNowPlaylistScreen = false
            } else {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    navController.navigate(HomeDestination) {
                        navController.graph.startDestinationId.let { startId ->
                            if (startId != 0) {
                                popUpTo(startId) {
                                    inclusive = false
                                }
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        } catch (e: Exception) {
            com.sonique.logger.Logger.e("AppNavigation", "Navigation error handled: ${e.message}")
        }
    }

    val reloadDestination by viewModel.reloadDestination.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val enablePageTransitions by viewModel.enablePageTransitions.collectAsStateWithLifecycle()

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

    LaunchedEffect(navBackStackEntry) {
        if (isSettings) {
            isNavBarVisible = false
        } else {
            val route = navBackStackEntry?.destination?.route
            val isMainRoute = route?.contains("Home") == true || 
                             route?.contains("Search") == true || 
                             route?.contains("Library") == true || 
                             route?.contains("Downloads") == true
            if (isMainRoute) {
                isNavBarVisible = true
            }
        }
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

    var isScrolledToTop by rememberSaveable {
        mutableStateOf(true)
    }

    LaunchedEffect(navBackStackEntry) {
        Logger.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
        if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
            isShowNowPlaylistScreen = false
        }
        isInFullscreen = navBackStackEntry?.destination?.hierarchy?.any {
            it.hasRoute(FullscreenDestination::class)
        } == true
        isScrolledToTop = true
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLiquidGlassEnabled provides (enableLiquidGlass && getPlatform() == Platform.Android)
    ) {
        val backdrop = rememberBackdrop()

        val coreScope = androidx.compose.runtime.rememberCoroutineScope()
        androidx.compose.foundation.layout.BoxWithConstraints {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val playerOffsetY = androidx.compose.runtime.remember {
            androidx.compose.animation.core.Animatable(screenHeightPx)
        }
        var isPlayerExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        LaunchedEffect(screenHeightPx) {
            if (!isShowNowPlaylistScreen) {
                playerOffsetY.snapTo(screenHeightPx)
            }
        }

        LaunchedEffect(isShowNowPlaylistScreen, screenHeightPx) {
             val target = if (isShowNowPlaylistScreen) 0f else screenHeightPx
             if (kotlin.math.abs(playerOffsetY.value - target) > 0.5f) {
                 playerOffsetY.animateTo(
                     target,
                     animationSpec = androidx.compose.animation.core.tween(
                         durationMillis = 280,
                         easing = androidx.compose.animation.core.FastOutSlowInEasing
                     )
                 )
             }
             isPlayerExpanded = isShowNowPlaylistScreen
        }

        AppTheme(enableLiquidGlass = enableLiquidGlass && getPlatform() == Platform.Android) {
            if (updateAvailable != null) {
                UpdateDialog(
                    releaseInfo = updateAvailable!!,
                    onDismiss = { updateViewModel.dismissUpdate() },
                    onDownload = {
                        navController.navigate(com.sonique.app.ui.navigation.destination.home.SettingsDestination(startCategory = "UPDATES"))
                        updateViewModel.dismissUpdate()
                    }
                )
            }

            val showInstallPrompt by viewModel.showInstallPrompt.collectAsStateWithLifecycle()
            val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()

            if (showInstallPrompt && downloadStatus is SharedViewModel.DownloadStatus.Downloaded) {
                val path = (downloadStatus as SharedViewModel.DownloadStatus.Downloaded).path
                AlertDialog(
                    onDismissRequest = { viewModel.dismissInstallPrompt() },
                    title = { Text("Update Ready") },
                    text = { Text("The update has been downloaded. Do you want to install it now?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.installUpdate(path)
                                viewModel.dismissInstallPrompt()
                            }
                        ) {
                            Text("Install Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissInstallPrompt() }) {
                            Text("Later")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }

            val showGitHubPopup by viewModel.showGitHubPopup.collectAsStateWithLifecycle()
            if (showGitHubPopup) {
                com.sonique.app.ui.component.GitHubStarDialog(
                    onDismiss = { viewModel.dismissGitHubPopup() },
                    onStar = { viewModel.neverShowGitHubPopupAgain() },
                    onNeverShowAgain = { viewModel.neverShowGitHubPopupAgain() }
                )
            }

            val showChangelog by viewModel.showChangelog.collectAsStateWithLifecycle()
            if (showChangelog) {
                val changelogText by viewModel.changelogText.collectAsStateWithLifecycle()
                com.sonique.app.ui.component.ChangelogDialog(
                    changelog = changelogText,
                    onDismiss = { viewModel.dismissChangelog() }
                )
            }

            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = if (enableLiquidGlass && getPlatform() == Platform.Android) Color.Black else MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (!isTablet) {
                            AnimatedVisibility(
                                isNavBarVisible,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                if (enableLiquidGlass && getPlatform() == Platform.Android) {
                                    LiquidGlassAppBottomNavigationBar(
                                        startDestination = HomeDestination,
                                        navController = navController,
                                        backdrop = backdrop,
                                        viewModel = viewModel,
                                        onOpenNowPlaying = { isShowNowPlaylistScreen = true },
                                        isScrolledToTop = isScrolledToTop,
                                    ) { klass ->
                                        viewModel.reloadDestination(klass)
                                    }
                                } else {
                                    Column {
                                        AnimatedVisibility(
                                            isShowMiniPlayer,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically(),
                                        ) {
                                            MiniPlayer(
                                                modifier = Modifier
                                                    .height(64.dp)
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 12.dp,
                                                    ).padding(
                                                        bottom = 4.dp,
                                                    ),
                                                backdrop = backdrop,
                                                enableLiquidGlass = false,
                                                onClick = {
                                                    isShowNowPlaylistScreen = true
                                                },
                                                onClose = {
                                                    viewModel.stopPlayer()
                                                    viewModel.isServiceRunning = false
                                                },
                                                onDrag = { delta ->
                                                    coreScope.launch {
                                                        playerOffsetY.snapTo((playerOffsetY.value + delta).coerceIn(0f, screenHeightPx))
                                                    }
                                                },
                                                onDragEnd = {
                                                    coreScope.launch {
                                                        val expandThreshold = screenHeightPx * 0.7f
                                                        val collapseThreshold = screenHeightPx * 0.3f
                                                        
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
                                                                playerOffsetY.animateTo(screenHeightPx, spec)
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
                        }
                    },
                    content = { innerPadding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (enableLiquidGlass && !isTablet) {
                                        Modifier.layerBackdrop(backdrop)
                                    } else {
                                        Modifier
                                    }
                                ),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (enableLiquidGlass) {
                                            Modifier.padding(top = 0.dp)
                                        } else {
                                            Modifier.padding(
                                                start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                                end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                                top = 0.dp,
                                                bottom = innerPadding.calculateBottomPadding(),
                                            )
                                        }
                                    ),
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
                                              .fillMaxSize()
                                              .then(
                                                  if (enableLiquidGlass && isTablet && !isInFullscreen) {
                                                      Modifier.layerBackdrop(backdrop)
                                                  } else {
                                                      Modifier
                                                  }
                                              ),
                                      ) {
                                         AppNavigationGraph(
                                             innerPadding = innerPadding,
                                             navController = navController,
                                             enablePageTransitions = enablePageTransitions,
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
                                            enableLiquidGlass = false,
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                textContentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                    },
                )
                if (!isTabletLandscape) {
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

