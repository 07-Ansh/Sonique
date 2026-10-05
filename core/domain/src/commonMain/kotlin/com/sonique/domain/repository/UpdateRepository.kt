package com.sonique.domain.repository

import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForUpdate(): Flow<UpdateStatus>
    suspend fun fetchChangelog(version: String): String?
    suspend fun fetchAllReleases(): Result<List<ReleaseInfo>>
}

sealed class UpdateStatus {
    data object Loading : UpdateStatus()
    data object NoUpdate : UpdateStatus()
    data class Available(val release: ReleaseInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

data class ReleaseInfo(
    val version: String,
    val changelog: String,
    val downloadUrl: String,
    val title: String,
    val publishedAt: String = "",
    val htmlUrl: String = "",
)


