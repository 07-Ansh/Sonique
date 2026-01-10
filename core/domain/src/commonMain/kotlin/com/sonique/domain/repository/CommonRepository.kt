package com.sonique.domain.repository

import com.sonique.domain.data.entities.NotificationEntity
import com.sonique.domain.data.model.cookie.CookieItem
import com.sonique.domain.data.type.RecentlyType
import com.sonique.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.Flow

interface CommonRepository {
    fun init(cookiePath: String, dataStoreManager: DataStoreManager)

     
    fun closeDatabase()

    fun getDatabasePath(): String?

    suspend fun databaseDaoCheckpoint()

     
    fun getAllRecentData(): Flow<List<RecentlyType>>

     
    suspend fun insertNotification(notificationEntity: NotificationEntity)

    suspend fun getAllNotifications(): Flow<List<NotificationEntity>?>

    suspend fun deleteNotification(id: Long)

    suspend fun writeTextToFile(text: String, filePath: String): Boolean

    suspend fun getCookiesFromInternalDatabase(url: String, packageName: String): CookieItem
}

