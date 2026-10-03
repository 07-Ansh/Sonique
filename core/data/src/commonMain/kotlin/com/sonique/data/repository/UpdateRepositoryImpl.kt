package com.sonique.data.repository

import com.sonique.domain.repository.UpdateRepository
import com.sonique.domain.repository.UpdateStatus
import com.sonique.domain.repository.ReleaseInfo
import com.sonique.kotlinytmusicscraper.YouTube
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import com.sonique.logger.Logger

internal class UpdateRepositoryImpl(
    private val youTube: YouTube,
) : UpdateRepository {

    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchChangelog(version: String): String? {
        return try {
            val response = client.get("https://api.github.com/repos/07-Ansh/Sonique/releases/tags/v$version")
            val releaseJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
            releaseJson["body"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun fetchAllReleases(): Result<List<ReleaseInfo>> {
        return try {
            val response = client.get("https://api.github.com/repos/07-Ansh/Sonique/releases?per_page=50") {
                header("User-Agent", "Sonique")
                header("Accept", "application/vnd.github.v3+json")
            }
            val responseBody = response.bodyAsText()
            val releaseArray = json.parseToJsonElement(responseBody).jsonArray
            val releases = releaseArray.mapNotNull { element ->
                val releaseJson = element.jsonObject
                val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val body = releaseJson["body"]?.jsonPrimitive?.content ?: ""
                val name = releaseJson["name"]?.jsonPrimitive?.content ?: tagName
                val publishedAt = releaseJson["published_at"]?.jsonPrimitive?.content ?: ""
                val htmlUrl = releaseJson["html_url"]?.jsonPrimitive?.content ?: "https://github.com/07-Ansh/Sonique/releases"
                val assets = releaseJson["assets"]?.jsonArray
                val apkAsset = assets?.firstOrNull { 
                    val assetName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                    assetName.endsWith(".apk", ignoreCase = true)
                }?.jsonObject ?: assets?.firstOrNull { 
                    val assetName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                    assetName.contains("universal", ignoreCase = true) || assetName.contains("arm64", ignoreCase = true)
                }?.jsonObject
                val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content ?: htmlUrl

                ReleaseInfo(
                    version = tagName,
                    changelog = body,
                    downloadUrl = downloadUrl,
                    title = name,
                    publishedAt = publishedAt,
                    htmlUrl = htmlUrl,
                )
            }
            Result.success(releases)
        } catch (e: Exception) {
            Logger.e("UpdateRepositoryImpl", "Failed to fetch all releases: ${e.message}")
            Result.failure(e)
        }
    }


    override fun checkForUpdate(): Flow<UpdateStatus> = flow {
        emit(UpdateStatus.Loading)
        try {
             
            val response = client.get("https://api.github.com/repos/07-Ansh/Sonique/releases/latest")
            val responseBody = response.bodyAsText()
            val releaseJson = json.parseToJsonElement(responseBody).jsonObject

            val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content ?: ""
            val body = releaseJson["body"]?.jsonPrimitive?.content ?: ""
            val name = releaseJson["name"]?.jsonPrimitive?.content ?: ""
            
             
            val assets = releaseJson["assets"]?.jsonArray
             
            val apkAsset = assets?.firstOrNull { 
                val assetName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                assetName.endsWith(".apk", ignoreCase = true)
            }?.jsonObject ?: assets?.firstOrNull { 
                val assetName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                assetName.contains("universal", ignoreCase = true) || assetName.contains("arm64", ignoreCase = true)
            }?.jsonObject
            
            val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content 
                ?: releaseJson["html_url"]?.jsonPrimitive?.content  
                ?: "https://github.com/07-Ansh/Sonique/releases"

             
              
              
              
              
              
             
              
             val remoteVersion = tagName.removePrefix("v")
              
              
              
              
              
             
              
              
              
              
              
            
             if (tagName.isNotEmpty()) {
                 emit(UpdateStatus.Available(
                     ReleaseInfo(
                         version = tagName,
                         changelog = body,
                         downloadUrl = downloadUrl,
                         title = name
                     )
                 ))
             } else {
                 emit(UpdateStatus.Error("Empty tag name"))
             }

        } catch (e: Exception) {
            e.printStackTrace()
            emit(UpdateStatus.Error(e.message ?: "Unknown error"))
        }
    }
}

