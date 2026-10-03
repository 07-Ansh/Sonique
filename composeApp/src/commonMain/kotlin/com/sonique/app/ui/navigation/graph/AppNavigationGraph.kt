package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sonique.app.ui.navigation.destination.home.HomeDestination
import com.sonique.app.ui.navigation.destination.home.SettingsDestination
import com.sonique.app.ui.screen.home.SettingScreen
import androidx.navigation.toRoute
import com.sonique.app.ui.navigation.destination.library.LibraryDestination
import com.sonique.app.ui.navigation.destination.player.FullscreenDestination
import com.sonique.app.ui.navigation.destination.search.SearchDestination
import com.sonique.app.ui.screen.home.HomeScreen
import com.sonique.app.ui.screen.library.LibraryScreen
import com.sonique.app.ui.screen.other.SearchScreen
import com.sonique.app.ui.navigation.destination.library.AlbumsDestination
import com.sonique.app.ui.screen.player.FullscreenPlayer
import com.sonique.common.LibraryChipType

private const val TRANSITION_DURATION = 300
private val transitionEasing = FastOutSlowInEasing

private fun isTabRoute(route: String?): Boolean {
    if (route == null) return false
    return route.contains("HomeDestination") ||
        route.contains("SearchDestination") ||
        route.contains("AlbumsDestination") ||
        route.contains("LibraryDestination")
}

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    enablePageTransitions: Boolean = false,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            if (!enablePageTransitions) {
                fadeIn(animationSpec = tween(TRANSITION_DURATION))
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (targetRoute?.contains("FullscreenDestination") == true) {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                } else if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
                    val initialIndex = getTabExtensionIndex(initialRoute)
                    val targetIndex = getTabExtensionIndex(targetRoute)
                    if (targetIndex > initialIndex) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                    }
                } else {
                    // Forward navigation (Push): new screen slides in from right to left
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                }
            }
        },
        exitTransition = {
            if (!enablePageTransitions) {
                fadeOut(animationSpec = tween(TRANSITION_DURATION))
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (targetRoute?.contains("FullscreenDestination") == true) {
                    // Screen underneath player fades out cleanly without horizontal slide
                    fadeOut(animationSpec = tween(TRANSITION_DURATION))
                } else if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
                    val initialIndex = getTabExtensionIndex(initialRoute)
                    val targetIndex = getTabExtensionIndex(targetRoute)
                    if (targetIndex > initialIndex) {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                    }
                } else {
                    // Forward navigation: current screen slides out to the left with subtle parallax
                    slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                }
            }
        },
        popEnterTransition = {
            if (!enablePageTransitions) {
                fadeIn(animationSpec = tween(TRANSITION_DURATION))
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (initialRoute?.contains("FullscreenDestination") == true) {
                    // Returning from Fullscreen player: underneath screen simply fades in
                    fadeIn(animationSpec = tween(TRANSITION_DURATION))
                } else if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
                    val initialIndex = getTabExtensionIndex(initialRoute)
                    val targetIndex = getTabExtensionIndex(targetRoute)
                    if (targetIndex > initialIndex) {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                    }
                } else {
                    // Back navigation: parent screen slides in from the left
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
                }
            }
        },
        popExitTransition = {
            if (!enablePageTransitions) {
                fadeOut(animationSpec = tween(TRANSITION_DURATION))
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                if (initialRoute?.contains("FullscreenDestination") == true) {
                    // Fullscreen player slides down to the bottom
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                } else if (isTabRoute(initialRoute) && isTabRoute(targetRoute)) {
                    val initialIndex = getTabExtensionIndex(initialRoute)
                    val targetIndex = getTabExtensionIndex(targetRoute)
                    if (targetIndex > initialIndex) {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                        ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                    }
                } else {
                    // Back navigation: popping screen slides out to the right (left-to-right exit)
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
                }
            }
        },
    ) {
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        composable<LibraryDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<LibraryDestination>()
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
                openDownloads = destination.openDownloads,
            )
        }
        composable<AlbumsDestination> {
            com.sonique.app.ui.screen.other.AlbumsScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        composable<FullscreenDestination>(
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(TRANSITION_DURATION, easing = transitionEasing),
                ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
            },
        ) {
            FullscreenPlayer(
                navController,
                hideNavBar = hideNavBar,
                showNavBar = {
                    showNavBar.invoke(true)
                    showNowPlayingSheet.invoke()
                },
            )
        }
        composable<SettingsDestination> { entry ->
            val destination = entry.toRoute<SettingsDestination>()
            SettingScreen(
                innerPadding = innerPadding,
                navController = navController,
                startCategory = destination.startCategory,
            )
        }
         
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            enablePageTransitions = enablePageTransitions,
            hideNavBar = hideNavBar,
            showNavBar = { showNavBar(true) },
            onScrolling = onScrolling,
        )
         
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
         
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            onScrolling = onScrolling,
        )
         
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}

private fun getTabExtensionIndex(route: String?): Int {
    if (route == null) return 0
    return when {
        route.contains("HomeDestination") -> 0
        route.contains("SearchDestination") -> 1
        route.contains("AlbumsDestination") -> 2
        route.contains("LibraryDestination") -> 3
        else -> 0
    }
}
