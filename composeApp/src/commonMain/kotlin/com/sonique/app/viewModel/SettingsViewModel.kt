package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import com.eygraber.uri.Uri
import com.sonique.common.Config
import com.sonique.common.QUALITY
import com.sonique.common.SELECTED_LANGUAGE
import com.sonique.common.VIDEO_QUALITY
import com.sonique.domain.data.entities.DownloadState
import com.sonique.domain.data.entities.GoogleAccountEntity
import com.sonique.domain.extension.toNetScapeString
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.handler.DownloadHandler
import com.sonique.domain.repository.AccountRepository
import com.sonique.domain.repository.ArtistRepository
import com.sonique.domain.repository.CacheRepository
import com.sonique.domain.repository.CommonRepository
import com.sonique.domain.repository.PlaylistRepository
import com.sonique.domain.repository.SongRepository
import com.sonique.domain.utils.LocalResource
import com.sonique.logger.LogLevel
import com.sonique.logger.Logger
import com.sonique.app.Platform
import com.sonique.app.expect.checkYtdlp
import com.sonique.app.getPlatform
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.backup_create_failed
import sonique.composeapp.generated.resources.backup_create_success
import sonique.composeapp.generated.resources.backup_in_progress
import sonique.composeapp.generated.resources.clear_canvas_cache
import sonique.composeapp.generated.resources.clear_downloaded_cache
import sonique.composeapp.generated.resources.clear_player_cache
import sonique.composeapp.generated.resources.clear_thumbnail_cache
import sonique.composeapp.generated.resources.restore_failed
import sonique.composeapp.generated.resources.restore_in_progress

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val commonRepository: CommonRepository,
    private val songRepository: SongRepository,
    private val accountRepository: AccountRepository,
    private val cacheRepository: CacheRepository,
    private val playlistRepository: PlaylistRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {
    private val databasePath: String? = commonRepository.getDatabasePath()
    private val downloadUtils: DownloadHandler by inject()

    private var _location: MutableStateFlow<String?> = MutableStateFlow(null)
    val location: StateFlow<String?> = _location
    private var _language: MutableStateFlow<String?> = MutableStateFlow(null)
    val language: StateFlow<String?> = _language
    private var _loggedIn: MutableStateFlow<String?> = MutableStateFlow(null)
    val loggedIn: StateFlow<String?> = _loggedIn
    private var _normalizeVolume: MutableStateFlow<String?> = MutableStateFlow(null)
    val normalizeVolume: StateFlow<String?> = _normalizeVolume
    private var _skipSilent: MutableStateFlow<String?> = MutableStateFlow(null)
    val skipSilent: StateFlow<String?> = _skipSilent
    private var _savedPlaybackState: MutableStateFlow<String?> = MutableStateFlow(null)
    val savedPlaybackState: StateFlow<String?> = _savedPlaybackState
    private var _saveRecentSongAndQueue: MutableStateFlow<String?> = MutableStateFlow(null)
    val saveRecentSongAndQueue: StateFlow<String?> = _saveRecentSongAndQueue

    private var _sponsorBlockEnabled: MutableStateFlow<String?> = MutableStateFlow(null)
    val sponsorBlockEnabled: StateFlow<String?> = _sponsorBlockEnabled
    private var _sponsorBlockCategories: MutableStateFlow<ArrayList<String>?> =
        MutableStateFlow(null)
    val sponsorBlockCategories: StateFlow<ArrayList<String>?> = _sponsorBlockCategories
    private var _sendBackToGoogle: MutableStateFlow<String?> = MutableStateFlow(null)
    val sendBackToGoogle: StateFlow<String?> = _sendBackToGoogle


    private var _translationLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val translationLanguage: StateFlow<String?> = _translationLanguage
    private var _useTranslation: MutableStateFlow<String?> = MutableStateFlow(null)
    val useTranslation: StateFlow<String?> = _useTranslation
    private var _playerCacheLimit: MutableStateFlow<Int?> = MutableStateFlow(null)
    val playerCacheLimit: StateFlow<Int?> = _playerCacheLimit

    private var _thumbCacheSize = MutableStateFlow<Long?>(null)
    val thumbCacheSize: StateFlow<Long?> = _thumbCacheSize
    private var _canvasCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    val canvasCacheSize: StateFlow<Long?> = _canvasCacheSize

    private var _usingProxy = MutableStateFlow(false)
    val usingProxy: StateFlow<Boolean> = _usingProxy
    private var _proxyType = MutableStateFlow(DataStoreManager.ProxyType.PROXY_TYPE_HTTP)
    val proxyType: StateFlow<DataStoreManager.ProxyType> = _proxyType
    private var _proxyHost = MutableStateFlow("")
    val proxyHost: StateFlow<String> = _proxyHost
    private var _proxyPort = MutableStateFlow(8000)
    val proxyPort: StateFlow<Int> = _proxyPort


    private val _crossfadeEnabled = MutableStateFlow<Boolean>(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled
    private val _crossfadeDuration = MutableStateFlow<Int>(5000)
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration
    private val _youtubeSubtitleLanguage = MutableStateFlow<String>("")
    val youtubeSubtitleLanguage: StateFlow<String> = _youtubeSubtitleLanguage



    private var _backupDownloaded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val backupDownloaded: StateFlow<Boolean> = _backupDownloaded



    private val _explicitContentEnabled = MutableStateFlow(false)
    val explicitContentEnabled: StateFlow<Boolean> = _explicitContentEnabled





    private val _likedSongsCount = MutableStateFlow(0)
    val likedSongsCount: StateFlow<Int> = _likedSongsCount

    private val _likedPlaylistsCount = MutableStateFlow(0)
    val likedPlaylistsCount: StateFlow<Int> = _likedPlaylistsCount

    private val _followedArtistsCount = MutableStateFlow(0)
    val followedArtistsCount: StateFlow<Int> = _followedArtistsCount

    private val _usedAccount = MutableStateFlow<GoogleAccountEntity?>(null)
    val usedAccount: StateFlow<GoogleAccountEntity?> = _usedAccount

    private val _keepServiceAlive = MutableStateFlow<Boolean>(false)
    val keepServiceAlive: StateFlow<Boolean> = _keepServiceAlive

    private val _keepYouTubePlaylistOffline = MutableStateFlow<Boolean>(false)
    val keepYouTubePlaylistOffline: StateFlow<Boolean> = _keepYouTubePlaylistOffline

    private val _combineLocalAndYouTubeLiked = MutableStateFlow<Boolean>(false)
    val combineLocalAndYouTubeLiked: StateFlow<Boolean> = _combineLocalAndYouTubeLiked

    private val _downloadQuality = MutableStateFlow<String?>(null)
    val downloadQuality: StateFlow<String?> = _downloadQuality



    private var _alertData: MutableStateFlow<SettingAlertState?> = MutableStateFlow(null)
    val alertData: StateFlow<SettingAlertState?> = _alertData

    private var _basicAlertData: MutableStateFlow<SettingBasicAlertState?> = MutableStateFlow(null)
    val basicAlertData: StateFlow<SettingBasicAlertState?> = _basicAlertData

     
    private var _fraction: MutableStateFlow<SettingsStorageSectionFraction> =
        MutableStateFlow(
            SettingsStorageSectionFraction(),
        )
    val fraction: StateFlow<SettingsStorageSectionFraction> = _fraction

     
    private var _killServiceOnExit: MutableStateFlow<String?> = MutableStateFlow(null)
    val killServiceOnExit: StateFlow<String?> = _killServiceOnExit

    private var _spotifyLogIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLogIn: StateFlow<Boolean> = _spotifyLogIn

    private var _spotifyLyrics: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLyrics: StateFlow<Boolean> = _spotifyLyrics

    private var _spotifyCanvas: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyCanvas: StateFlow<Boolean> = _spotifyCanvas



    init {
        getYoutubeSubtitleLanguage()

        viewModelScope.launch {
            songRepository.getLikedSongs().collect { songs ->
                _likedSongsCount.value = songs.size
            }
        }
        viewModelScope.launch {
            playlistRepository.getLikedPlaylists().collect { playlists ->
                _likedPlaylistsCount.value = playlists.size
            }
        }
        viewModelScope.launch {
            artistRepository.getFollowedArtists().collect { artists ->
                _followedArtistsCount.value = artists.size
            }
        }
        viewModelScope.launch {
            accountRepository.getUsedGoogleAccount().collect { account ->
                _usedAccount.value = account
            }
        }
        getSpotifyLogIn()
        getSpotifyLyrics()
        getSpotifyCanvas()
    }

    fun getAudioSessionId() = mediaPlayerHandler.player.audioSessionId

    fun getData() {
        getLocation()
        getLanguage()
        getQuality()
        getPlayerCacheSize()
        getDownloadedCacheSize()
        getPlayerCacheLimit()
        getDownloadQuality()

        getSendBackToGoogle()
        getNormalizeVolume()
        getSkipSilent()
        getKillServiceOnExit()
        getKeepServiceAlive()
        getExplicitContentEnabled()
        getCombineLocalAndYouTubeLiked()
        getKeepYouTubePlaylistOffline()
        getUseTranslation()
        getTranslationLanguage()
        getCanvasCache()
        getYoutubeSubtitleLanguage()
        getSponsorBlockEnabled()
        getSponsorBlockCategories()

        getSaveRecentSongAndQueue()
        getSavedPlaybackState()
        getCrossfadeEnabled()
        getCrossfadeDuration()
        getBackupDownloaded()

        getSpotifyLogIn()
        getSpotifyLyrics()
        getSpotifyCanvas()

        viewModelScope.launch {
            calculateDataFraction(
                cacheRepository,
            )?.let {
                _fraction.value = it
            }
        }
    }

    private fun getDownloadQuality() {
        viewModelScope.launch {
            dataStoreManager.downloadQuality.collect { quality ->
                when (quality) {
                    QUALITY.items[0].toString() -> _downloadQuality.emit(QUALITY.items[0].toString())
                    QUALITY.items[1].toString() -> _downloadQuality.emit(QUALITY.items[1].toString())
                    QUALITY.items[2].toString() -> _downloadQuality.emit(QUALITY.items[2].toString())
                    "High - 256kps (YT Premium) (Slow loading)" -> {
                        _downloadQuality.emit(QUALITY.items[2].toString())
                        dataStoreManager.setDownloadQuality(QUALITY.items[2].toString())
                    }
                }
            }
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            if (getPlatform() == Platform.Android) {
                dataStoreManager.setDownloadQuality(quality)
                getQuality()
            } else if (getPlatform() == Platform.Desktop) {
                val installed = checkYtdlp()
                if (installed) {
                    dataStoreManager.setDownloadQuality(quality)
                    getQuality()
                } else {
                    makeToast("Your device does not have yt-dlp installed. Please install it to use the best quality.")
                }
            }
            getDownloadQuality()
        }
    }



    private fun getKeepYouTubePlaylistOffline() {
        viewModelScope.launch {
            dataStoreManager.keepYouTubePlaylistOffline.collect { keep ->
                _keepYouTubePlaylistOffline.value = keep == DataStoreManager.TRUE
            }
        }
    }

    fun setKeepYouTubePlaylistOffline(keep: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKeepYouTubePlaylistOffline(keep)
            getKeepYouTubePlaylistOffline()
        }
    }

    private fun getCombineLocalAndYouTubeLiked() {
        viewModelScope.launch {
            dataStoreManager.combineLocalAndYouTubeLiked.collect { combine ->
                _combineLocalAndYouTubeLiked.value = combine == DataStoreManager.TRUE
            }
        }
    }

    fun setCombineLocalAndYouTubeLiked(combine: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCombineLocalAndYouTubeLiked(combine)
            getCombineLocalAndYouTubeLiked()
        }
    }

    private fun getKeepServiceAlive() {
        viewModelScope.launch {
            dataStoreManager.keepServiceAlive.collect { keepServiceAlive ->
                _keepServiceAlive.value = keepServiceAlive == DataStoreManager.TRUE
            }
        }
    }

    fun setKeepServiceAlive(keepServiceAlive: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKeepServiceAlive(keepServiceAlive)
            getKeepServiceAlive()
        }
    }



    private fun getExplicitContentEnabled() {
        viewModelScope.launch {
            dataStoreManager.explicitContentEnabled.collect { enabled ->
                _explicitContentEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setExplicitContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setExplicitContentEnabled(enabled)
            getExplicitContentEnabled()
        }
    }





    private fun getBackupDownloaded() {
        viewModelScope.launch {
            dataStoreManager.backupDownloaded.collect { backupDownloaded ->
                _backupDownloaded.value = backupDownloaded == DataStoreManager.TRUE
            }
        }
    }

    fun setBackupDownloaded(backupDownloaded: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBackupDownloaded(backupDownloaded)
            getBackupDownloaded()
        }
    }









    private fun getCanvasCache() {
        viewModelScope.launch {
            _canvasCacheSize.value = cacheRepository.getCacheSize(Config.CANVAS_CACHE)
        }
    }

    fun setAlertData(alertData: SettingAlertState?) {
        _alertData.value = alertData
    }

    fun setBasicAlertData(alertData: SettingBasicAlertState?) {
        _basicAlertData.value = alertData
    }

    private fun getUsingProxy() {
        viewModelScope.launch {
            dataStoreManager.usingProxy.collectLatest { usingProxy ->
                if (usingProxy == DataStoreManager.TRUE) {
                    getProxy()
                }
                _usingProxy.value = usingProxy == DataStoreManager.TRUE
            }
        }
    }

    fun setUsingProxy(usingProxy: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setUsingProxy(usingProxy)
            getUsingProxy()
            getProxy()
        }
    }

    private fun getProxy() {
        viewModelScope.launch {
            val host =
                launch {
                    dataStoreManager.proxyHost.collect {
                        _proxyHost.value = it
                    }
                }
            val port =
                launch {
                    dataStoreManager.proxyPort.collect {
                        _proxyPort.value = it
                    }
                }
            val type =
                launch {
                    dataStoreManager.proxyType.collect {
                        _proxyType.value = it
                        log("getProxy: $it")
                    }
                }
            host.join()
            port.join()
            type.join()
        }
    }

    fun setProxy(
        proxyType: DataStoreManager.ProxyType,
        host: String,
        port: Int,
    ) {
        log("setProxy: $proxyType, $host, $port")
        viewModelScope.launch {
            dataStoreManager.setProxyType(proxyType)
            dataStoreManager.setProxyHost(host)
            dataStoreManager.setProxyPort(port)
        }
    }



    fun getThumbCacheSize(context: PlatformContext) {
        viewModelScope.launch {
            val diskCache = SingletonImageLoader.get(context).diskCache
            _thumbCacheSize.emit(diskCache?.size)
        }
    }



    fun getTranslationLanguage() {
        viewModelScope.launch {
            dataStoreManager.translationLanguage.collect { translationLanguage ->
                _translationLanguage.emit(translationLanguage)
            }
        }
    }

    fun setTranslationLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setTranslationLanguage(language)
            getTranslationLanguage()
        }
    }

    fun getUseTranslation() {
        viewModelScope.launch {
            dataStoreManager.enableTranslateLyric.collect { useTranslation ->
                _useTranslation.emit(useTranslation)
            }
        }
    }

    fun setUseTranslation(useTranslation: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setEnableTranslateLyric(useTranslation)
            getUseTranslation()
        }
    }



    fun getLocation() {
        viewModelScope.launch {
            dataStoreManager.location.collect { location ->
                _location.emit(location)
            }
        }
    }

    fun getLoggedIn() {
        viewModelScope.launch {
            dataStoreManager.loggedIn.collect { loggedIn ->
                _loggedIn.emit(loggedIn)
            }
        }
    }

    fun changeLocation(location: String) {
        viewModelScope.launch {
            dataStoreManager.setLocation(location)
            getLocation()
        }
    }

    fun getSaveRecentSongAndQueue() {
        viewModelScope.launch {
            dataStoreManager.saveRecentSongAndQueue.collect { saved ->
                _saveRecentSongAndQueue.emit(saved)
            }
        }
    }



    fun getSponsorBlockEnabled() {
        viewModelScope.launch {
            dataStoreManager.sponsorBlockEnabled.first().let { enabled ->
                _sponsorBlockEnabled.emit(enabled)
            }
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSponsorBlockEnabled(enabled)
            getSponsorBlockEnabled()
        }
    }



    fun getSponsorBlockCategories() {
        viewModelScope.launch {
            dataStoreManager.getSponsorBlockCategories().let {
                log("getSponsorBlockCategories: $it", LogLevel.WARN)
                _sponsorBlockCategories.emit(it)
            }
        }
    }

    fun setSponsorBlockCategories(list: ArrayList<String>) {
        log("setSponsorBlockCategories: $list", LogLevel.WARN)
        viewModelScope.launch {
            runBlocking(Dispatchers.IO) {
                dataStoreManager.setSponsorBlockCategories(list)
            }
            getSponsorBlockCategories()
        }
    }



    private var _quality: MutableStateFlow<String?> = MutableStateFlow(null)
    val quality: StateFlow<String?> = _quality

    fun getQuality() {
        viewModelScope.launch {
            dataStoreManager.quality.collect { quality ->
                when (quality) {
                    QUALITY.items[0].toString() -> _quality.emit(QUALITY.items[0].toString())
                    QUALITY.items[1].toString() -> _quality.emit(QUALITY.items[1].toString())
                    QUALITY.items[2].toString() -> _quality.emit(QUALITY.items[2].toString())
                    "High - 256kps (YT Premium) (Slow loading)" -> {
                        _quality.emit(QUALITY.items[2].toString())
                        dataStoreManager.setQuality(QUALITY.items[2].toString())
                    }
                }
            }
        }
    }



    fun changeQuality(qualityItem: String?) {
        viewModelScope.launch {
            log("changeQuality: $qualityItem")
            if (getPlatform() == Platform.Android) {
                dataStoreManager.setQuality(qualityItem ?: QUALITY.items.first().toString())
                getQuality()
            } else if (getPlatform() == Platform.Desktop) {
                val installed = checkYtdlp()
                if (installed) {
                    dataStoreManager.setQuality(qualityItem ?: QUALITY.items.first().toString())
                    getQuality()
                } else {
                    makeToast("Your device does not have yt-dlp installed. Please install it to use the best quality.")
                }
            }
        }
    }

    private val _cacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var cacheSize: StateFlow<Long?> = _cacheSize

    fun getPlayerCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = cacheRepository.getCacheSize(Config.PLAYER_CACHE)
        }
    }

    fun clearPlayerCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.PLAYER_CACHE)
            makeToast(getString(Res.string.clear_player_cache))
            getPlayerCacheSize()
        }
    }

    private val _downloadedCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var downloadedCacheSize: StateFlow<Long?> = _downloadedCacheSize

    fun getDownloadedCacheSize() {
        viewModelScope.launch {
            _downloadedCacheSize.value = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
        }
    }

    fun clearDownloadedCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().singleOrNull()?.let { songs ->
                songs.forEach { song ->
                    songRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            makeToast(getString(Res.string.clear_downloaded_cache))
            getDownloadedCacheSize()
            downloadUtils.removeAllDownloads()
        }
    }

    fun clearCanvasCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.CANVAS_CACHE)
            makeToast(getString(Res.string.clear_canvas_cache))
            getCanvasCache()
        }
    }

    fun backup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                makeToast(getString(Res.string.backup_in_progress))
                withContext(Dispatchers.IO) {
                    backupNative(commonRepository, uri, backupDownloaded.value)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    makeToast(getString(Res.string.backup_create_success))
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    it.printStackTrace()
                    makeToast(getString(Res.string.backup_create_failed))
                }
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            makeToast(getString(Res.string.restore_in_progress))
            withContext(Dispatchers.IO) {
                runCatching {
                    restoreNative(commonRepository, uri) {
                        getData()
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        makeToast(getString(Res.string.restore_failed))
                    }
                }
            }
        }
    }

    fun getLanguage() {
        viewModelScope.launch {
            dataStoreManager.getString(SELECTED_LANGUAGE).collect { language ->
                _language.emit(language)
            }
        }
    }

    fun changeLanguage(code: String) {
        viewModelScope.launch {
            dataStoreManager.putString(SELECTED_LANGUAGE, code)
            Logger.w("SettingsViewModel", "changeLanguage: $code")
            getLanguage()
            changeLanguageNative(code)
        }
    }

    fun getNormalizeVolume() {
        viewModelScope.launch {
            dataStoreManager.normalizeVolume.collect { normalizeVolume ->
                _normalizeVolume.emit(normalizeVolume)
            }
        }
    }

    fun setNormalizeVolume(normalizeVolume: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNormalizeVolume(normalizeVolume)
            getNormalizeVolume()
        }
    }

    fun getSendBackToGoogle() {
        viewModelScope.launch {
            dataStoreManager.sendBackToGoogle.collect { sendBackToGoogle ->
                _sendBackToGoogle.emit(sendBackToGoogle)
            }
        }
    }

    fun setSendBackToGoogle(sendBackToGoogle: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSendBackToGoogle(sendBackToGoogle)
            getSendBackToGoogle()
        }
    }

    fun getSkipSilent() {
        viewModelScope.launch {
            dataStoreManager.skipSilent.collect { skipSilent ->
                _skipSilent.emit(skipSilent)
            }
        }
    }

    fun setSkipSilent(skip: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSkipSilent(skip)
            getSkipSilent()
        }
    }

    fun getSavedPlaybackState() {
        viewModelScope.launch {
            dataStoreManager.saveStateOfPlayback.collect { savedPlaybackState ->
                _savedPlaybackState.emit(savedPlaybackState)
            }
        }
    }

    fun setSavedPlaybackState(savedPlaybackState: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveStateOfPlayback(savedPlaybackState)
            getSavedPlaybackState()
        }
    }

    fun setSaveLastPlayed(b: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveRecentSongAndQueue(b)
            getSaveRecentSongAndQueue()
        }
    }

    fun getPlayerCacheLimit() {
        viewModelScope.launch {
            dataStoreManager.maxSongCacheSize.collect {
                _playerCacheLimit.emit(it)
            }
        }
    }

    fun setPlayerCacheLimit(size: Int) {
        viewModelScope.launch {
            dataStoreManager.setMaxSongCacheSize(size)
            getPlayerCacheLimit()
        }
    }

    private var _googleAccounts: MutableStateFlow<LocalResource<List<GoogleAccountEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val googleAccounts: StateFlow<LocalResource<List<GoogleAccountEntity>>> = _googleAccounts

    fun getAllGoogleAccount() {
        Logger.w("getAllGoogleAccount", "getAllGoogleAccount: Go to function")
        viewModelScope.launch {
            _googleAccounts.emit(LocalResource.Loading())
            accountRepository.getGoogleAccounts().collectLatest { accounts ->
                Logger.w("getAllGoogleAccount", "getAllGoogleAccount: $accounts")
                if (!accounts.isNullOrEmpty()) {
                    _googleAccounts.emit(LocalResource.Success(accounts))
                } else {
                    if (loggedIn.value == DataStoreManager.TRUE) {
                        accountRepository
                            .getAccountInfo(
                                dataStoreManager.cookie.first(),
                            ).collect {
                                Logger.w("getAllGoogleAccount", "getAllGoogleAccount: $it")
                                if (it.isNotEmpty()) {
                                    dataStoreManager.putString("AccountName", it.first().name)
                                    dataStoreManager.putString(
                                        "AccountThumbUrl",
                                        it
                                            .first()
                                            .thumbnails
                                            .lastOrNull()
                                            ?.url ?: "",
                                    )
                                    accountRepository
                                        .insertGoogleAccount(
                                            GoogleAccountEntity(
                                                email = it.first().email,
                                                name = it.first().name,
                                                thumbnailUrl =
                                                    it
                                                        .first()
                                                        .thumbnails
                                                        .lastOrNull()
                                                        ?.url ?: "",
                                                cache = accountRepository.getYouTubeCookie(),
                                                pageId = it.first().pageId,
                                                isUsed = true,
                                            ),
                                        ).singleOrNull()
                                        ?.let { account ->
                                            Logger.w("getAllGoogleAccount", "inserted: $account")
                                        }
                                    getAllGoogleAccount()
                                } else {
                                    _googleAccounts.emit(LocalResource.Success(emptyList()))
                                }
                            }
                    } else {
                        _googleAccounts.emit(LocalResource.Success(emptyList()))
                    }
                }
            }
        }
    }

    suspend fun addAccount(
        cookie: String,
        netscapeCookie: String? = null,
    ): Boolean {
        val currentCookie = dataStoreManager.cookie.first()
        val currentPageId = dataStoreManager.pageId.first()
        val currentLoggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
        try {
            dataStoreManager.setCookie(cookie, "")
            dataStoreManager.setLoggedIn(true)
            return accountRepository
                .getAccountInfo(
                    cookie,
                ).lastOrNull()
                ?.takeIf {
                    it.isNotEmpty()
                }?.let { accountInfoList ->
                    Logger.d("getAllGoogleAccount", "addAccount: $accountInfoList")
                    accountRepository.getGoogleAccounts().lastOrNull()?.forEach {
                        Logger.d("getAllGoogleAccount", "set used: $it start")
                        accountRepository
                            .updateGoogleAccountUsed(it.email, false)
                            .singleOrNull()
                            ?.let {
                                Logger.w("getAllGoogleAccount", "set used: $it")
                            }
                    }
                    dataStoreManager.putString("AccountName", accountInfoList.first().name)
                    dataStoreManager.putString(
                        "AccountThumbUrl",
                        accountInfoList
                            .first()
                            .thumbnails
                            .lastOrNull()
                            ?.url ?: "",
                    )
                    val cookieItem =
                        netscapeCookie ?: commonRepository
                            .getCookiesFromInternalDatabase(Config.YOUTUBE_MUSIC_MAIN_URL, getPackageName())
                            .toNetScapeString()
                    commonRepository.writeTextToFile(cookieItem, (getFileDir() + "/ytdlp-cookie.txt")).let {
                        Logger.d("getAllGoogleAccount", "addAccount: write cookie file: $it")
                    }
                    accountInfoList.forEachIndexed { index, account ->
                        accountRepository
                            .insertGoogleAccount(
                                GoogleAccountEntity(
                                    email = account.email,
                                    name = account.name,
                                    thumbnailUrl =
                                        account
                                            .thumbnails
                                            .lastOrNull()
                                            ?.url ?: "",
                                    cache = cookie,
                                    isUsed = index == 0,
                                    netscapeCookie = cookieItem,
                                    pageId = account.pageId,
                                ),
                            ).firstOrNull()
                            ?.let {
                                log("addAccount: $it", LogLevel.WARN)
                            }
                    }
                    dataStoreManager.setLoggedIn(true)
                    dataStoreManager.setCookie(cookie, accountInfoList.first().pageId)
                    getAllGoogleAccount()
                    getLoggedIn()
                    true
                } ?: run {
                Logger.w("getAllGoogleAccount", "addAccount: Account info is null")
                dataStoreManager.setCookie(currentCookie, currentPageId)
                dataStoreManager.setLoggedIn(currentLoggedIn)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("getAllGoogleAccount", "addAccount: ${e.message}")
            dataStoreManager.setCookie(currentCookie, currentPageId)
            dataStoreManager.setLoggedIn(currentLoggedIn)
            return false
        }
    }

    fun setUsedAccount(acc: GoogleAccountEntity?) {
        viewModelScope.launch {
            if (acc != null) {
                googleAccounts.value.data?.forEach {
                    accountRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Logger.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", acc.name)
                dataStoreManager.putString("AccountThumbUrl", acc.thumbnailUrl)
                accountRepository
                    .updateGoogleAccountUsed(acc.email, true)
                    .singleOrNull()
                    ?.let {
                        Logger.w("getAllGoogleAccount", "set used: $it")
                    }
                acc.netscapeCookie?.let { commonRepository.writeTextToFile(it, (getFileDir() + "/ytdlp-cookie.txt")) }.let {
                    Logger.d("getAllGoogleAccount", "addAccount: write cookie file: $it")
                }
                dataStoreManager.setCookie(acc.cache ?: "", acc.pageId)
                dataStoreManager.setLoggedIn(true)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            } else {
                googleAccounts.value.data?.forEach {
                    accountRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Logger.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", "")
                dataStoreManager.putString("AccountThumbUrl", "")
                dataStoreManager.setLoggedIn(false)
                dataStoreManager.setCookie("", null)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            }
        }
    }

    fun logOutAllYouTube() {
        viewModelScope.launch {
            googleAccounts.value.data?.forEach { account ->
                accountRepository.deleteGoogleAccount(account.email)
            }
            dataStoreManager.putString("AccountName", "")
            dataStoreManager.putString("AccountThumbUrl", "")
            dataStoreManager.setLoggedIn(false)
            dataStoreManager.setCookie("", null)
            delay(500)
            getAllGoogleAccount()
            getLoggedIn()
        }
    }

    @ExperimentalCoilApi
    fun clearThumbnailCache(platformContext: PlatformContext) {
        viewModelScope.launch {
            SingletonImageLoader.get(platformContext).diskCache?.clear()
            makeToast(getString(Res.string.clear_thumbnail_cache))
            getThumbCacheSize(platformContext)
        }
    }


    fun getSpotifyLogIn() {
        viewModelScope.launch {
            dataStoreManager.spdc.collect { loggedIn ->
                if (loggedIn.isNotEmpty()) {
                    _spotifyLogIn.emit(true)
                } else {
                    _spotifyLogIn.emit(false)
                }
            }
        }
    }

    fun setSpotifyLogIn(loggedIn: Boolean) {
        viewModelScope.launch {
            _spotifyLogIn.emit(loggedIn)
            if (!loggedIn) {
                dataStoreManager.setSpdc("")
                delay(500)
            }
            getSpotifyLogIn()
        }
    }



    fun getSpotifyLyrics() {
        viewModelScope.launch {
            dataStoreManager.spotifyLyrics.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyLyrics.emit(true)
                } else {
                    _spotifyLyrics.emit(false)
                }
            }
        }
    }

    fun setSpotifyLyrics(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyLyrics(loggedIn)
            getSpotifyLyrics()
        }
    }

    fun getSpotifyCanvas() {
        viewModelScope.launch {
            dataStoreManager.spotifyCanvas.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyCanvas.emit(true)
                } else {
                    _spotifyCanvas.emit(false)
                }
            }
        }
    }

    fun setSpotifyCanvas(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyCanvas(loggedIn)
            getSpotifyCanvas()
        }
    }

     
    fun getKillServiceOnExit() {
        viewModelScope.launch {
            dataStoreManager.killServiceOnExit.collect { killServiceOnExit ->
                _killServiceOnExit.emit(killServiceOnExit)
            }
        }
    }

     
    fun setKillServiceOnExit(kill: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKillServiceOnExit(kill)
            getKillServiceOnExit()
        }
    }

    private fun getCrossfadeEnabled() {
        viewModelScope.launch {
            dataStoreManager.crossfadeEnabled.collect { crossfadeEnabled ->
                _crossfadeEnabled.value = crossfadeEnabled == DataStoreManager.TRUE
            }
        }
    }

    fun setCrossfadeEnabled(crossfadeEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeEnabled(crossfadeEnabled)
            getCrossfadeEnabled()
        }
    }

    private fun getCrossfadeDuration() {
        viewModelScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                _crossfadeDuration.value = duration
            }
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeDuration(duration)
            getCrossfadeDuration()
        }
    }

    fun getYoutubeSubtitleLanguage() {
        viewModelScope.launch {
            dataStoreManager.youtubeSubtitleLanguage.collect { language ->
                _youtubeSubtitleLanguage.emit(language)
            }
        }
    }

    fun setYoutubeSubtitleLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setYoutubeSubtitleLanguage(language)
            getYoutubeSubtitleLanguage()
        }
    }


}

data class SettingsStorageSectionFraction(
    val otherApp: Float = 0f,
    val downloadCache: Float = 0f,
    val playerCache: Float = 0f,
    val canvasCache: Float = 0f,
    val thumbCache: Float = 0f,
    val appDatabase: Float = 0f,
    val freeSpace: Float = 0f,
) {
    fun combine(): Float = otherApp + downloadCache + playerCache + canvasCache + thumbCache + appDatabase + freeSpace
}

data class SettingAlertState(
    val title: String,
    val message: String? = null,
    val textField: TextFieldData? = null,
    val selectOne: SelectData? = null,
    val multipleSelect: SelectData? = null,
    val confirm: Pair<String, (SettingAlertState) -> Unit>,
    val dismiss: String,
) {
    data class TextFieldData(
        val label: String,
        val value: String = "",
         
        val verifyCodeBlock: ((String) -> Pair<Boolean, String?>)? = null,
    )

    data class SelectData(
         
        val listSelect: List<Pair<Boolean, String>>,
    ) {
        fun getSelected(): String = listSelect.firstOrNull { it.first }?.second ?: ""

        fun getListSelected(): List<String> = listSelect.filter { it.first }.map { it.second }
    }
}

data class SettingBasicAlertState(
    val title: String,
    val message: String? = null,
    val confirm: Pair<String, () -> Unit>,
    val dismiss: String,
)

expect suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction?

expect suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit = {},
)

expect suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
)

expect fun getPackageName(): String

expect fun getFileDir(): String

expect fun changeLanguageNative(code: String)

