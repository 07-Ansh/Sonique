package com.sonique.data.repository

import com.sonique.data.db.LocalDataSource
import com.sonique.data.db.MusicDatabase
import com.sonique.data.io.fileSystem
import com.sonique.domain.data.entities.NotificationEntity
import com.sonique.domain.data.model.cookie.CookieItem
import com.sonique.domain.data.type.RecentlyType
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.CommonRepository
import com.sonique.kotlinytmusicscraper.YouTube
import com.sonique.kotlinytmusicscraper.models.YouTubeLocale
import com.sonique.logger.Logger
import com.sonique.spotify.Spotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class CommonRepositoryImpl(
    private val coroutineScope: CoroutineScope,
    private val database: MusicDatabase,
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val spotify: Spotify,
) : CommonRepository {
    @OptIn(ExperimentalTime::class)
    override fun init(cookiePath: String, dataStoreManager: DataStoreManager) {
        youTube.cookiePath = cookiePath.toPath()
        coroutineScope.launch {
            val resetSpotifyToken =
                launch {
                    dataStoreManager.setSpotifyClientToken("")
                    dataStoreManager.setSpotifyPersonalToken("")
                    dataStoreManager.setSpotifyClientTokenExpires(Clock.System.now().epochSeconds)
                    dataStoreManager.setSpotifyPersonalTokenExpires(Clock.System.now().epochSeconds)
                }
            val localeJob =
                launch {
                    combine(dataStoreManager.location, dataStoreManager.language) { location, language ->
                        Pair(location, language)
                    }.collectLatest { (location, language) ->
                        youTube.locale =
                            YouTubeLocale(
                                location,
                                try {
                                    language.substring(0..1)
                                } catch (e: Exception) {
                                    "en"
                                },
                            )
                    }
                }
            val ytCookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collectLatest { cookie ->
                        if (cookie.isNotEmpty()) {
                            youTube.cookie = cookie
                            youTube.visitorData()?.let {
                                youTube.visitorData = it
                            }
                        } else {
                            youTube.cookie = null
                        }
                        Logger.d("YouTube", "New cookie")
                        localDataSource.getUsedGoogleAccount()?.netscapeCookie?.let {
                            writeTextToFile(it, cookiePath)
                            Logger.w("YouTube", "Wrote cookie to file")
                        }
                    }
                }
            val pageIdJob =
                launch {
                    dataStoreManager.pageId.distinctUntilChanged().collectLatest { pageId ->
                        youTube.pageId = pageId.ifEmpty { null }
                        Logger.d("YouTube", "New pageId")
                        localDataSource.getUsedGoogleAccount()?.netscapeCookie?.let {
                            writeTextToFile(it, cookiePath)
                            Logger.w("YouTube", "Wrote cookie to file")
                        }
                    }
                }
            val usingProxy =
                launch {
                    combine(
                        dataStoreManager.usingProxy,
                        dataStoreManager.proxyType,
                        dataStoreManager.proxyHost,
                        dataStoreManager.proxyPort,
                    ) { usingProxy, proxyType, proxyHost, proxyPort ->
                        Pair(usingProxy == DataStoreManager.TRUE, Triple(proxyType, proxyHost, proxyPort))
                    }.collectLatest { (usingProxy, data) ->
                        if (usingProxy) {
                            withContext(Dispatchers.IO) {
                                youTube.setProxy(
                                    data.first == DataStoreManager.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                                spotify.setProxy(
                                    data.first == DataStoreManager.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                            }
                        } else {
                            youTube.removeProxy()
                            spotify.removeProxy()
                        }
                    }
                }
            val dataSyncIdJob =
                launch {
                    dataStoreManager.dataSyncId.collectLatest { dataSyncId ->
                        youTube.dataSyncId = dataSyncId
                    }
                }
            val visitorDataJob =
                launch {
                    dataStoreManager.visitorData.collectLatest { visitorData ->
                        youTube.visitorData = visitorData
                    }
                }

            localeJob.join()
            ytCookieJob.join()
            pageIdJob.join()
            usingProxy.join()
            dataSyncIdJob.join()
            visitorDataJob.join()
            resetSpotifyToken.join()
        }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                youTube.updateYtdlp()
            }
        }
    }

    // Database
    override fun closeDatabase() {
        database.close()
    }

    override fun getDatabasePath() = com.sonique.data.db.getDatabasePath()

    override suspend fun databaseDaoCheckpoint() = localDataSource.checkpoint()

    // Recently data
    override fun getAllRecentData(): Flow<List<RecentlyType>> =
        flow {
            emit(localDataSource.getAllRecentData())
        }.flowOn(Dispatchers.IO)

    // Notifications
    override suspend fun insertNotification(notificationEntity: NotificationEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNotification(notificationEntity)
        }

    override suspend fun getAllNotifications(): Flow<List<NotificationEntity>?> =
        flow {
            emit(localDataSource.getAllNotification())
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteNotification(id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteNotification(id)
        }

    override suspend fun writeTextToFile(
        text: String,
        filePath: String,
    ): Boolean {
        try {
            fileSystem().sink(filePath.toPath()).buffer().use { sink ->
                sink.writeUtf8(text)
                sink.close()
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Original from YTDLnis com.sonique.app
     */
    override suspend fun getCookiesFromInternalDatabase(
        url: String,
        packageName: String
    ): CookieItem =
        withContext(Dispatchers.IO) {
            return@withContext getCookies(
                url,
                packageName,
            )
        }
}

expect fun getCookies(
    url: String,
    packageName: String
): CookieItem

