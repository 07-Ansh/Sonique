package com.sonique.data.repository

import com.sonique.domain.repository.UpdateRepository
import com.sonique.domain.repository.UpdateStatus
import com.sonique.domain.repository.ReleaseInfo
import com.sonique.kotlinytmusicscraper.YouTube
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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

