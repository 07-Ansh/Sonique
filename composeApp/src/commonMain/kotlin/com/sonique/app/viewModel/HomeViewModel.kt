package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import com.sonique.common.SELECTED_LANGUAGE
import com.sonique.common.SUPPORTED_LANGUAGE
import com.sonique.domain.data.entities.SongEntity
import com.sonique.domain.data.model.home.HomeDataCombine
import com.sonique.domain.data.model.home.HomeItem
import com.sonique.domain.data.model.home.Content
import com.sonique.domain.data.model.home.chart.Chart
import com.sonique.domain.data.model.mood.Mood
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.manager.DataStoreManager.Values.TRUE
import com.sonique.domain.repository.HomeRepository
import com.sonique.domain.utils.Resource
import com.sonique.logger.Logger
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.music_video
import sonique.composeapp.generated.resources.new_release
import sonique.composeapp.generated.resources.song
import sonique.composeapp.generated.resources.view_count

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
) : BaseViewModel() {
    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(cachedHomeItemList ?: listOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private val _speedDialData: MutableStateFlow<HomeItem?> = MutableStateFlow(null)
    val speedDialData: StateFlow<HomeItem?> = _speedDialData

    private var _homeListState = MutableStateFlow<ListState>(if (cachedHomeItemList != null) ListState.IDLE else ListState.LOADING)
    val homeListState: StateFlow<ListState> = _homeListState

    private var _continuation = MutableStateFlow<String?>(cachedContinuation)
    val continuation: StateFlow<String?> = _continuation

    private val _exploreMoodItem: MutableStateFlow<Mood?> = MutableStateFlow(cachedExploreMoodItem)
    val exploreMoodItem: StateFlow<Mood?> = _exploreMoodItem
    private val _accountInfo: MutableStateFlow<Pair<String?, String?>?> = MutableStateFlow(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo

    private var homeJob: Job? = null

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableStateFlow<Chart?> = MutableStateFlow(cachedChart)
    val chart: StateFlow<Chart?> = _chart
    private val _newRelease: MutableStateFlow<List<HomeItem>> = MutableStateFlow(cachedNewRelease ?: arrayListOf())
    val newRelease: StateFlow<List<HomeItem>> = _newRelease
    var regionCodeChart: MutableStateFlow<String?> = MutableStateFlow(null)

    val loading = MutableStateFlow<Boolean>(cachedHomeItemList == null)
    val loadingChart = MutableStateFlow<Boolean>(cachedChart == null)
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableStateFlow<SongEntity?> = MutableStateFlow(null)
    val songEntity: StateFlow<SongEntity?> = _songEntity

    private var _params: MutableStateFlow<String?> = MutableStateFlow(cachedParams)
    val params: StateFlow<String?> = _params

     
    private val _showLogInAlert: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLogInAlert: StateFlow<Boolean> = _showLogInAlert

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    val dataSyncId =
        dataStoreManager
            .dataSyncId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        _speedDialData.value = calculateSpeedDialData(cachedHomeItemList ?: listOf())
        viewModelScope.launch {
            combine(
                dataStoreManager.cookie,
                dataStoreManager.shouldShowLogInRequiredAlert
            ) { cookie, showAlert ->
                cookie.isEmpty() && showAlert == DataStoreManager.Values.TRUE
            }.collect { shouldShow ->
                _showLogInAlert.update { shouldShow }
            }
        }
        homeJob = Job()
        viewModelScope.launch {
            regionCodeChart.value = dataStoreManager.chartKey.first()
            if (cachedChart == null) {
                exploreChart(regionCodeChart.value ?: "ZZ")
            }
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
             
            val job1 =
                launch {
                    val flow = dataStoreManager.location.distinctUntilChanged()
                    (if (cachedHomeItemList != null) flow.drop(1) else flow).collect {
                        regionCode = it
                        getHomeItemList(params.value)
                    }
                }
             
            val job2 =
                launch {
                    val flow = dataStoreManager.language.distinctUntilChanged()
                    (if (cachedHomeItemList != null) flow.drop(1) else flow).collect {
                        language = it
                        getHomeItemList(params.value)
                    }
                }
            val job3 =
                launch {
                    val flow = dataStoreManager.cookie.distinctUntilChanged()
                    (if (cachedHomeItemList != null) flow.drop(1) else flow).collect {
                        getHomeItemList(params.value)
                        _accountInfo.emit(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first(),
                            ),
                        )
                    }
                }
            val job4 =
                launch {
                    val flow = params
                    (if (cachedHomeItemList != null) flow.drop(1) else flow).collectLatest {
                        getHomeItemList(it, forceRefresh = true)
                    }
                }
            val job5 =
                launch {
                    val flow = dataStoreManager.cookie.distinctUntilChanged()
                    (if (cachedHomeItemList != null) flow.drop(1) else flow).collectLatest {
                        if (it.isNotEmpty()) {
                            Logger.w(tag, "Cookie changed, refreshing home")
                            loading.value = true
                            delay(1000)  
                            getHomeItemList(params.value)
                        }
                    }
                }
        }
    }

    fun doneShowLogInAlert(neverShowAgain: Boolean = false) {
        viewModelScope.launch {
            _showLogInAlert.update { false }
            if (neverShowAgain) {
                dataStoreManager.setShouldShowLogInRequiredAlert(false)
            }
        }
    }

    fun getHomeItemList(params: String? = null, forceRefresh: Boolean = false) {
        loading.value = true
        _homeListState.value = ListState.LOADING
        homeJob?.cancel()
        homeJob =
            viewModelScope.launch {
                language =
                    dataStoreManager.getString(SELECTED_LANGUAGE).first()
                        ?: SUPPORTED_LANGUAGE.codes.first()
                regionCode = dataStoreManager.location.first() ?: ""
                combine(
                    homeRepository.getHomeData(
                        params,
                        getString(Res.string.view_count),
                        getString(Res.string.song),
                        forceRefresh = forceRefresh
                    ),
                    homeRepository.getMoodAndMomentsData(forceRefresh = forceRefresh),
                    homeRepository.getChartData(dataStoreManager.chartKey.first(), forceRefresh = forceRefresh),
                    homeRepository.getNewRelease(
                        getString(Res.string.new_release),
                        getString(Res.string.music_video),
                        forceRefresh = forceRefresh
                    ),
                ) { home, exploreMood, exploreChart, newRelease ->
                    HomeDataCombine(home, exploreMood, exploreChart, newRelease)
                }.collect { result ->
                    _isError.value = false
                    val home = result.home
                    Logger.d("home size", "${home.data?.second?.size}")
                    val exploreMoodItem = result.mood
                    val chart = result.chart
                    val newRelease = result.newRelease
                    when (home) {
                        is Resource.Success -> {
                            _continuation.value = home.data?.first
                            val list = home.data?.second ?: listOf()
                            _homeItemList.value = list
                            _speedDialData.value = calculateSpeedDialData(list)
                        }
                        is Resource.Error -> {
                             _isError.value = true
                            _continuation.value = null
                            _homeItemList.value = listOf()
                            _speedDialData.value = null
                        }
                    }
                    if (continuation.value.isNullOrEmpty())
                        _homeListState.value = ListState.PAGINATION_EXHAUST
                    else
                        _homeListState.value = ListState.IDLE
                    when (chart) {
                        is Resource.Success -> {
                            _chart.value = chart.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    when (newRelease) {
                        is Resource.Success -> {
                            _newRelease.value = newRelease.data ?: arrayListOf()
                        }

                        else -> {
                            _newRelease.value = arrayListOf()
                        }
                    }
                    when (exploreMoodItem) {
                        is Resource.Success -> {
                            _exploreMoodItem.value = exploreMoodItem.data
                        }

                        else -> {
                            _exploreMoodItem.value = null
                        }
                    }
                    regionCodeChart.value = dataStoreManager.chartKey.first()
                    Logger.d("HomeViewModel", "getHomeItemList: $result")
                    dataStoreManager.cookie.first().let {
                        if (it != "") {
                            _accountInfo.emit(
                                Pair(
                                    dataStoreManager.getString("AccountName").first(),
                                    dataStoreManager.getString("AccountThumbUrl").first(),
                                ),
                            )
                        }
                    }
                    if (home is Resource.Success && home.data?.second?.isNotEmpty() == true) {
                        cachedHomeItemList = _homeItemList.value
                        cachedExploreMoodItem = _exploreMoodItem.value
                        cachedChart = _chart.value
                        cachedNewRelease = _newRelease.value
                        cachedContinuation = _continuation.value
                        cachedParams = params
                    }
                    when {
                        home is Resource.Error -> home.message
                        exploreMoodItem is Resource.Error -> exploreMoodItem.message
                        chart is Resource.Error -> chart.message
                        else -> null
                    }?.let {
                        showSnackBarErrorState.emit(it)
                        Logger.w("Error", "getHomeItemList: ${home.message}")
                        Logger.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                        Logger.w("Error", "getHomeItemList: ${chart.message}")
                    }
                    loading.value = false
                }
            }
    }

    fun getContinueHomeItem(
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _homeListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                log("Get more home item with continuation: $continuation")
                _homeListState.value = ListState.PAGINATING
                homeRepository.getHomeDataContinue(
                    continuation,
                    getString(Res.string.view_count),
                    getString(Res.string.song),
                ).collect { home ->
                    when (home) {
                        is Resource.Success -> {
                            _continuation.value = home.data?.first
                            val newItems = home.data?.second ?: listOf()
                            _homeItemList.update { it + newItems }
                            if (home.data?.first.isNullOrEmpty()) {
                                _homeListState.value = ListState.PAGINATION_EXHAUST
                            } else {
                                _homeListState.value = ListState.IDLE
                            }
                        }

                        is Resource.Error -> {
                            _continuation.value = null
                            Logger.w(tag, "getContinueHomeItem: ${home.message}")
                            showSnackBarErrorState.emit(home.message ?: "Unknown error")
                            _homeListState.value = ListState.PAGINATION_EXHAUST
                        }
                    }
                }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            homeRepository
                .getChartData(
                    region,
                ).collect { values ->
                    regionCodeChart.value = region
                    dataStoreManager.setChartKey(region)
                    when (values) {
                        is Resource.Success -> {
                            _chart.value = values.data
                        }

                        else -> {
                            _chart.value = null
                        }
                    }
                    loadingChart.value = false
                }
        }
    }

    fun setParams(params: String?) {
        _params.value = params
    }

    private fun calculateSpeedDialData(homeData: List<HomeItem>): HomeItem? {
        if (homeData.isEmpty()) return null
        
        val priorityKeywords = listOf("listen again")
        val generalKeywords = listOf(
            "quick picks", "albums for you", "your daily discover",
            "long listens", "from your library", "featured playlists for you"
        )
        val priorityContents = mutableListOf<Content>()
        val generalContents = mutableListOf<Content>()

        homeData.forEach { section ->
            val titleLower = section.title.lowercase()
            val subtitleLower = section.subtitle?.lowercase() ?: ""
            
            val contents = section.contents.filterNotNull().filter { it.videoId != null && it.videoId != "" }
            if (priorityKeywords.any { it in titleLower || it in subtitleLower }) {
                priorityContents.addAll(contents)
            } else if (generalKeywords.any { it in titleLower || it in subtitleLower }) {
                generalContents.addAll(contents)
            }
        }

        val distinctPriority = priorityContents.distinctBy { it.title }.shuffled()
        val remainingGeneral = generalContents.filter { gen -> distinctPriority.none { pri -> pri.title == gen.title } }.distinctBy { it.title }.shuffled()
        // Prioritize Listen Again items at the very beginning
        val combined = (distinctPriority + remainingGeneral)
        val targetSize = 54
        val finalList = combined.toMutableList()
        if (finalList.size < targetSize) {
            val allOtherContents = homeData.flatMap { it.contents }
                .filterNotNull()
                .filter { it.videoId != null && it.videoId != "" }
                .filter { other -> combined.none { c -> c.title == other.title } }
                .distinctBy { it.title }
                .shuffled()
            finalList.addAll(allOtherContents)
        }

        val finalContents = finalList.distinctBy { it.title }.take(targetSize)

        if (finalContents.isEmpty()) return null
        return HomeItem(
            title = "Continue Listening",
            contents = finalContents
        )
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        private var cachedHomeItemList: List<HomeItem>? = null
        private var cachedChart: Chart? = null
        private var cachedNewRelease: List<HomeItem>? = null
        private var cachedExploreMoodItem: Mood? = null
        private var cachedContinuation: String? = null
        private var cachedParams: String? = null
         
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}

