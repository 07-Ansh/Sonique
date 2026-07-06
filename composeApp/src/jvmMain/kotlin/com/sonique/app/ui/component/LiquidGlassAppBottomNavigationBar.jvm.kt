package com.sonique.app.ui.component

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.viewModel.SharedViewModel
import kotlin.reflect.KClass

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
    // LiquidGlassAppBottomNavigationBar is a no-op on desktop targets
}
