package com.sonique.app.ui.navigation.graph

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.sonique.app.ui.navigation.destination.home.*
import com.sonique.app.ui.screen.home.MoodScreen
import com.sonique.app.ui.screen.home.NotificationScreen
import com.sonique.app.ui.screen.home.RecentlySongsScreen
import com.sonique.app.ui.screen.home.SettingScreen
import com.sonique.app.ui.screen.other.CreditScreen
import com.sonique.app.viewModel.SharedViewModel
import org.koin.compose.koinInject

fun NavGraphBuilder.homeScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    enablePageTransitions: Boolean = false,
    hideNavBar: () -> Unit = { },
    showNavBar: () -> Unit = { },
    onScrolling: (Boolean) -> Unit = {},
) {
    composable<CreditDestination> {
        CreditScreen(
            paddingValues = innerPadding,
            navController = navController,
        )
    }
    composable<MoodDestination> { entry ->
        val params = entry.toRoute<MoodDestination>().params
        MoodScreen(
            navController = navController,
            params = params,
            onScrolling = onScrolling,
        )
    }
    composable<NotificationDestination> {
        NotificationScreen(
            navController = navController,
        )
    }
    composable<RecentlySongsDestination> {
        RecentlySongsScreen(
            navController = navController,
            innerPadding = innerPadding,
            onScrolling = onScrolling,
        )
    }

}

