package com.sonique.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sonique.common.CHART_SUPPORTED_COUNTRY
import com.sonique.common.Config
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.data.model.home.chart.Chart
import com.sonique.domain.data.model.mood.Mood
import com.sonique.domain.extension.now
import com.sonique.domain.mediaservice.handler.PlaylistType
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.utils.toTrack
import com.sonique.logger.Logger
import com.sonique.app.extension.isScrollingUp
import com.sonique.app.ui.component.CenterLoadingBox
import com.sonique.app.ui.component.Chip
import com.sonique.app.ui.component.DropdownButton
import com.sonique.app.ui.component.EndOfPage
import com.sonique.app.ui.component.HomeItem
import com.sonique.app.ui.component.HomeItemContentPlaylist
import com.sonique.app.ui.component.HomeShimmer
import com.sonique.app.ui.component.ItemArtistChart
import com.sonique.app.ui.component.MoodMomentAndGenreHomeItem
import com.sonique.app.ui.component.QuickPicksItem
import com.sonique.app.ui.component.RippleIconButton
import com.sonique.app.ui.component.InAppNotification
import com.sonique.app.ui.component.OfflineScreen
import com.sonique.app.ui.navigation.destination.home.HomeDestination
import com.sonique.app.ui.navigation.destination.home.MoodDestination
import com.sonique.app.ui.navigation.destination.home.NotificationDestination
import com.sonique.app.ui.navigation.destination.home.SettingsDestination
import com.sonique.app.ui.navigation.destination.library.LibraryDestination
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.navigation.destination.list.PlaylistDestination
import com.sonique.app.ui.navigation.destination.login.LoginDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.typo
import com.sonique.app.ui.theme.white
import com.sonique.app.viewModel.HomeViewModel
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import com.sonique.app.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import com.sonique.app.viewModel.ListState
import com.sonique.app.viewModel.SharedViewModel

import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.app_name
import sonique.composeapp.generated.resources.baseline_settings_24
import com.sonique.app.expect.ui.rememberNotificationPermissionLauncher
import sonique.composeapp.generated.resources.energize
import sonique.composeapp.generated.resources.feel_good
import sonique.composeapp.generated.resources.focus
import sonique.composeapp.generated.resources.genre
import sonique.composeapp.generated.resources.good_afternoon
import sonique.composeapp.generated.resources.good_evening
import sonique.composeapp.generated.resources.good_morning
import sonique.composeapp.generated.resources.good_night
import sonique.composeapp.generated.resources.holder
import sonique.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import sonique.composeapp.generated.resources.log_in
import sonique.composeapp.generated.resources.log_in_warning
import sonique.composeapp.generated.resources.moods_amp_moment
import sonique.composeapp.generated.resources.outline_notifications_24
import sonique.composeapp.generated.resources.party
import sonique.composeapp.generated.resources.quick_picks
import sonique.composeapp.generated.resources.relax
import sonique.composeapp.generated.resources.romance
import sonique.composeapp.generated.resources.sad
import sonique.composeapp.generated.resources.sleep
import sonique.composeapp.generated.resources.top_artists
import sonique.composeapp.generated.resources.welcome_back
import sonique.composeapp.generated.resources.workout
import sonique.composeapp.generated.resources.travel
import sonique.composeapp.generated.resources.for_you

private val listOfHomeChip =
    listOf(
        Res.string.for_you,
        Res.string.relax,
        Res.string.sleep,
        Res.string.energize,
        Res.string.sad,
        Res.string.romance,
        Res.string.feel_good,
        Res.string.workout,
        Res.string.party,
        Res.string.travel,
        Res.string.focus,
    )

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var accountShow by rememberSaveable {
        mutableStateOf(false)
    }
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()

    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()



    var topAppBarHeightPx by rememberSaveable {
        mutableIntStateOf(0)
    }



    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.getHomeItemList(params)
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }


    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                        ) >= (scrollState.layoutInfo.totalItemsCount - 1)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {
            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

 
 
 
 
 
 
 
 




    Box {
        InAppNotification(
            visible = shouldShowLogInAlert,
            message = stringResource(Res.string.log_in_warning),
            actionLabel = stringResource(Res.string.log_in),
            onActionClick = {
                viewModel.doneShowLogInAlert(false)
                navController.navigate(LoginDestination)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(LocalDensity.current) { topAppBarHeightPx.toDp() })
                .zIndex(1f)
        )
        PullToRefreshBox(
            modifier =
                Modifier,
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top =
                                    with(LocalDensity.current) {
                                        topAppBarHeightPx.toDp()
                                    },
                            ),
                    containerColor = PullToRefreshDefaults.indicatorContainerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    maxDistance = PullToRefreshDefaults.PositionalThreshold,
                )
            },
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    if (isError) {
                        OfflineScreen(
                            onExploreDownloads = {
                                navController.navigate(LibraryDestination(openDownloads = true))
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .padding(horizontal = 15.dp),
                            contentPadding =
                                PaddingValues(
                                    top = with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                                ),
                            state = scrollState,
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                        ) {
                            item {
                                Column {
                                    if (accountInfo != null && accountShow) {
                                        AccountLayout(
                                            accountName = accountInfo?.first ?: "",
                                            url = accountInfo?.second ?: "",
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                            items(homeData, key = { it.hashCode() }) { item ->
                                if (item.title == stringResource(Res.string.quick_picks)) {
                                    AnimatedVisibility(
                                        visible =
                                            homeData.find {
                                                it.title ==
                                                    stringResource(
                                                        Res.string.quick_picks,
                                                    )
                                            } != null,
                                    ) {
                                        QuickPicks(
                                            homeItem =
                                                (
                                                    homeData.find {
                                                        it.title ==
                                                            stringResource(
                                                                Res.string.quick_picks,
                                                            )
                                                    } ?: return@AnimatedVisibility
                                                    ).let { content ->
                                                        content.copy(
                                                            contents =
                                                                content.contents.mapNotNull { ct ->
                                                                    ct?.copy(
                                                                        artists =
                                                                            ct.artists?.let { art ->
                                                                                if (art.size > 1) {
                                                                                    art.dropLast(1)
                                                                                } else {
                                                                                    art
                                                                                }
                                                                            },
                                                                    )
                                                                },
                                                        )
                                                    },
                                            viewModel = viewModel,
                                        )
                                    }
                                } else {
                                    HomeItem(
                                        navController = navController,
                                        data = item,
                                    )
                                }
                            }
                        item {
                            AnimatedVisibility(
                                homeListState == ListState.PAGINATING,
                                enter = expandVertically() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                CenterLoadingBox(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                )
                            }
                        }
                        if (homeListState == ListState.PAGINATION_EXHAUST) {
                            items(newRelease, key = { it.hashCode() }) {
                                AnimatedVisibility(
                                    visible = newRelease.isNotEmpty(),
                                ) {
                                    HomeItem(
                                        navController = navController,
                                        data = it,
                                    )
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = moodMomentAndGenre != null,
                                ) {
                                    moodMomentAndGenre?.let {
                                        MoodMomentAndGenre(
                                            mood = it,
                                            navController = navController,
                                        )
                                    }
                                }
                            }
                            item {
                                Column(
                                    Modifier
                                        .padding(vertical = 10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ChartTitle()
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(targetState = regionChart) {
                                        Logger.w("HomeScreen", "regionChart: $it")
                                        if (it != null) {
                                            DropdownButton(
                                                items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                                defaultSelected =
                                                    CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                        CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                    )
                                                        ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                            ) {
                                                viewModel.exploreChart(
                                                    CHART_SUPPORTED_COUNTRY.items[
                                                        CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                            it,
                                                        ),
                                                    ],
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(
                                        targetState = chartLoading,
                                        label = "Chart",
                                    ) { loading ->
                                        if (!loading) {
                                            chart?.let {
                                                ChartData(
                                                    chart = it,
                                                    navController = navController,
                                                )
                                            }
                                        } else {
                                            CenterLoadingBox(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(400.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                }
            } else {
                Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) {
                                    topAppBarHeightPx.toDp()
                                },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .background(md_theme_dark_background.copy(alpha = 0.8f))  
                    .onGloballyPositioned { coordinates ->
                        topAppBarHeightPx = coordinates.size.height
                    },
        ) {
            AnimatedVisibility(
                visible = isScrollingUp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                HomeTopAppBar(navController)
            }
            AnimatedVisibility(
                visible = !isScrollingUp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.statusBars,
                            ),
                )
            }
            AnimatedVisibility(
                visible = isScrollingUp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier =
                    Modifier
                        .horizontalScroll(chipRowState)
                        .padding(bottom = 2.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOfHomeChip.forEach { id ->
                        val isSelected =
                            when (params) {
                                HOME_PARAMS_RELAX -> id == Res.string.relax
                                HOME_PARAMS_SLEEP -> id == Res.string.sleep
                                HOME_PARAMS_ENERGIZE -> id == Res.string.energize
                                HOME_PARAMS_SAD -> id == Res.string.sad
                                HOME_PARAMS_ROMANCE -> id == Res.string.romance
                                HOME_PARAMS_FEEL_GOOD -> id == Res.string.feel_good
                                HOME_PARAMS_WORKOUT -> id == Res.string.workout
                                HOME_PARAMS_PARTY -> id == Res.string.party
                                HOME_PARAMS_COMMUTE -> id == Res.string.travel
                                HOME_PARAMS_FOCUS -> id == Res.string.focus
                                else -> id == Res.string.for_you
                            }
                        Chip(
                            isAnimated = loading,
                            isSelected = isSelected,
                            text = stringResource(id),
                        ) {
                            when (id) {
                                Res.string.for_you -> viewModel.setParams(null)
                                Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                Res.string.travel -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController, accountInfo: Pair<String, String>? = null) {
    val hour =
        remember {
            val date = now().time
            date.hour
        }
    val greeting = when (hour) {
        in 6..12 -> stringResource(Res.string.good_morning)
        in 13..17 -> stringResource(Res.string.good_afternoon)
        in 18..23 -> stringResource(Res.string.good_evening)
        else -> stringResource(Res.string.good_night)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-12).dp)
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(Res.string.app_name),
                style = typo().headlineMedium,
                color = Color.White
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
             val notificationPermissionLauncher = rememberNotificationPermissionLauncher {
                 if (it) {
                     navController.navigate(NotificationDestination)
                 }
             }
             RippleIconButton(resId = Res.drawable.outline_notifications_24) {
                notificationPermissionLauncher()
            }
             Spacer(modifier = Modifier.width(8.dp))
             RippleIconButton(resId = Res.drawable.baseline_settings_24) {
                 navController.navigate(SettingsDestination)
             }
        }
    }
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = typo().bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Text(
                text = accountName,
                style = typo().headlineMedium,
                color = Color.White,
                modifier =
                    Modifier
                        .padding(start = 8.dp),
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Text(
            text = "Mixes for you",
            style = typo().headlineMedium,
            color = Color.White,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(256.dp),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        data = it,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    val lazyListState1 = rememberLazyGridState()
    val snapperFlingBehavior1 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState1))

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        Text(
            text = stringResource(Res.string.moods_amp_moment),
            style = typo().headlineMedium,
            color = white,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(210.dp),
            state = lazyListState1,
            flingBehavior = snapperFlingBehavior1,
        ) {
            items(mood.moodsMoments, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigate(
                        MoodDestination(
                            it.params,
                        ),
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.genre),
            style = typo().headlineMedium,
            maxLines = 1,
            color = white,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(210.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(mood.genres, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigate(
                        MoodDestination(
                            it.params,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = "Featuring Today",
            style = typo().headlineMedium,
            color = white,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = white,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = white,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = data.browseId,
                            ),
                        )
                    },
                    data = data,
                    widthDp = gridWidthDp,
                )
            }
        }
    }
}

