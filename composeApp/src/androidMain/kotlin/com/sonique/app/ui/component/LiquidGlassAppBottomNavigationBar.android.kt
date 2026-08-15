package com.sonique.app.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.ChainStyle
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.logger.Logger
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.ui.navigation.destination.home.HomeDestination
import com.sonique.app.ui.navigation.destination.library.LibraryDestination
import com.sonique.app.ui.navigation.destination.search.SearchDestination
import com.sonique.app.ui.navigation.destination.library.AlbumsDestination
import com.sonique.app.ui.screen.MiniPlayer
import com.sonique.app.viewModel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.graphics.Color

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

private const val TAG = "LiquidGlassAppBottomNavigationBar"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    onOpenNowPlaying: () -> Unit,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit
) {
    val layer = rememberGraphicsLayer()
    val toolbarInteraction = rememberGlassInteraction()
    val searchFabInteraction = rememberGlassInteraction()
    val luminanceAnimation = 0.35f


    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val liquidGlassGlassiness by viewModel.liquidGlassGlassiness.collectAsStateWithLifecycle()
    val isShowMiniPlayer = remember(nowPlayingData) {
        nowPlayingData?.isNotEmpty() == true
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val bottomNavScreens =
        listOf(
            BottomNavScreen.Home,
            BottomNavScreen.Search,
            BottomNavScreen.Albums,
            BottomNavScreen.Library,
        )
    val barTabs =
        listOf(
            BottomNavScreen.Home,
            BottomNavScreen.Albums,
            BottomNavScreen.Library,
        )
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(
            when (startDestination) {
                is HomeDestination -> BottomNavScreen.Home.ordinal
                is SearchDestination -> BottomNavScreen.Search.ordinal
                is AlbumsDestination -> BottomNavScreen.Albums.ordinal
                is LibraryDestination -> BottomNavScreen.Library.ordinal
                else -> BottomNavScreen.Home.ordinal
            },
        )
    }
    var isExpanded by rememberSaveable {
        mutableStateOf(true)
    }
    val searchButtonSize by animateDpAsState(if (isExpanded) 56.dp else 48.dp, tween(300))

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxDockWidth = (screenWidthDp - searchButtonSize - 32.dp - 8.dp).coerceAtMost(288.dp)
    val tabWidth = (maxDockWidth / 3).coerceAtLeast(70.dp)

    var isInSearchDestination by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.destination?.let { current ->
            isInSearchDestination = current.hasRoute(SearchDestination::class)
        }
    }

    var updateConstraints by remember {
        mutableStateOf(true)
    }

    var constraintSet by remember {
        mutableStateOf(
            decoupledConstraints(isShowMiniPlayer, isExpanded),
        )
    }

    LaunchedEffect(isShowMiniPlayer, isExpanded) {
        constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
        updateConstraints = false
    }

    LaunchedEffect(updateConstraints) {
        if (updateConstraints) {
            constraintSet = decoupledConstraints(isShowMiniPlayer, isExpanded)
            updateConstraints = false
        }
    }

    LaunchedEffect(isScrolledToTop, isInSearchDestination) {
        isExpanded = isScrolledToTop && !isInSearchDestination
    }

    fun selectTab(index: Int) {
        val screen = bottomNavScreens.find { it.ordinal == index } ?: return
        if (selectedIndex == index) {
            if (currentBackStackEntry?.destination?.hierarchy?.any {
                    it.hasRoute(screen.destination::class)
                } == true
            ) {
                reloadDestinationIfNeeded(screen.destination::class)
            } else {
                navController.navigate(screen.destination) {
                    popUpTo(screen.destination) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        } else {
            selectedIndex = index
            navController.navigate(screen.destination) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val searchButtonContent = @Composable {
        Box(
            modifier = Modifier
                .size(searchButtonSize)
                .drawInteractiveGlass(
                    backdrop = backdrop,
                    layer = layer,
                    luminanceAnimation = luminanceAnimation,
                    shape = CircleShape,
                    interaction = searchFabInteraction,
                    glassiness = liquidGlassGlassiness,
                )
                .clickable { selectTab(BottomNavScreen.Search.ordinal) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = BottomNavScreen.Search.icon,
                contentDescription = null,
                tint = Color.White
            )
        }
    }

    val searchButtonPadding by animateDpAsState(
        targetValue = if (isExpanded) 16.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
    )

    ConstraintLayout(
        constraintSet = constraintSet,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    WindowInsets.navigationBars.asPaddingValues(),
                )
                .padding(
                    bottom = 8.dp,
                )
                .imePadding(),
        animateChangesSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
    ) {
        LiquidGlassTabBar(
            tabs = barTabs,
            selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex },
            backdrop = backdrop,
            layer = layer,
            luminance = luminanceAnimation,
            isExpanded = isExpanded,
            tabWidth = tabWidth,
            glassiness = liquidGlassGlassiness,
            modifier = Modifier
                .layoutId("toolbar")
                .onGloballyPositioned { updateConstraints = true },
            onTabSelected = { position -> selectTab(barTabs[position].ordinal) },
        )

        MiniPlayer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(56.dp)
                .layoutId("miniPlayer"),
            backdrop = backdrop,
            enableLiquidGlass = true,
            onClick = {
                onOpenNowPlaying()
            },
            onClose = {
                viewModel.stopPlayer()
                viewModel.isServiceRunning = false
            },
            onDrag = {},
            onDragEnd = {},
        )

        Box(
            modifier = Modifier
                .layoutId("searchButton")
                .padding(start = searchButtonPadding.coerceAtLeast(0.dp))
        ) {
            searchButtonContent()
        }
    }
}

private fun decoupledConstraints(
    isMiniplayerShow: Boolean = true,
    isExpanded: Boolean,
): ConstraintSet =
    ConstraintSet {
        val searchButton = createRefFor("searchButton")
        val toolbar = createRefFor("toolbar")
        val miniPlayer = createRefFor("miniPlayer")

        if (isExpanded) {
            constrain(toolbar) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(searchButton.start, margin = 20.dp)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }
            constrain(searchButton) {
                start.linkTo(toolbar.end, margin = 20.dp)
                end.linkTo(parent.end)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                visibility = Visibility.Visible
            }
            createHorizontalChain(toolbar, searchButton, chainStyle = ChainStyle.Packed)

            constrain(miniPlayer) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(toolbar.top, margin = 6.dp)
                width = if (isMiniplayerShow) Dimension.matchParent else Dimension.wrapContent
                visibility = if (isMiniplayerShow) Visibility.Visible else Visibility.Gone
            }
        } else {
            constrain(toolbar) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, margin = 16.dp)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }
            constrain(searchButton) {
                end.linkTo(parent.end, margin = 16.dp)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = Dimension.wrapContent
                height = Dimension.wrapContent
                visibility = Visibility.Visible
            }
            constrain(miniPlayer) {
                start.linkTo(toolbar.end)
                end.linkTo(searchButton.start)
                top.linkTo(toolbar.top)
                bottom.linkTo(toolbar.bottom)
                width = if (isMiniplayerShow) Dimension.fillToConstraints else Dimension.wrapContent
                visibility = if (isMiniplayerShow) Visibility.Visible else Visibility.Gone
            }
        }
    }
