package com.sonique.app.ui.screen.other

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HomeItem
import com.sonique.app.ui.component.HomeShimmer
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.AlbumsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.youtube_albums

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: AlbumsViewModel = koinViewModel(),
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val albumSections by viewModel.albumSections.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()

    LaunchedEffect(scrollState, isScrollingUp) {
        snapshotFlow { scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0 }
            .collect { isAtTop ->
                val shouldBeVisible = if (isAtTop) true else isScrollingUp
                onScrolling(shouldBeVisible)
            }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            onRefresh = { viewModel.fetchAlbumsData(forceRefresh = true) },
            isRefreshing = isLoading,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isLoading,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                )
            },
        ) {
            if (isLoading && albumSections.isEmpty()) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HomeShimmer()
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Text(
                            text = "Albums",
                            style = typo().headlineLarge,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(albumSections, key = { it.hashCode() }) { item ->
                        HomeItem(
                            navController = navController,
                            data = item,
                        )
                    }

                    item {
                        EndOfPage()
                    }
                }
            }
        }
    }
}
