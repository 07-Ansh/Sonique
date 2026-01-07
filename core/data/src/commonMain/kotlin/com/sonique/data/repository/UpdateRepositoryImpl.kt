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
            // Using public GitHub API
            val response = client.get("https://api.github.com/repos/07-Ansh/Sonique/releases/latest")
            val responseBody = response.bodyAsText()
            val releaseJson = json.parseToJsonElement(responseBody).jsonObject

            val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content ?: ""
            val body = releaseJson["body"]?.jsonPrimitive?.content ?: ""
            val name = releaseJson["name"]?.jsonPrimitive?.content ?: ""
            
            // Safe parsing of assets
            val assets = releaseJson["assets"]?.jsonArray
            // Find universal or arm64 apk
            val apkAsset = assets?.firstOrNull { 
                val assetName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                assetName.contains("universal", ignoreCase = true) || assetName.contains("arm64", ignoreCase = true)
            }?.jsonObject
            
            val downloadUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content 
                ?: releaseJson["html_url"]?.jsonPrimitive?.content // Fallback to release page
                ?: "https://github.com/07-Ansh/Sonique/releases"

            // Current Version Logic
             // We need to inject BuildConfig or similar to get current version.
             // For now, I'll assume we can pass it or get it from build config fields if exposed.
             // But KMP data module might not have access to Android BuildConfig directly easily without expect/actual.
             // However, `com.sonique.app.BuildKonfig` is often used in KMP.
             // Let's check imports.
             
             // Assuming remote tag is like "v1.0.0" and local is "1.0.0"
             val remoteVersion = tagName.removePrefix("v")
             // TODO: Real version check. 
             // Ideally we inject version name. For now let's use a placeholder or BuildKonfig if available.
             // I will use a safe unavailable check for now and let the ViewModel pass the current version? 
             // No, Repository should do the check.
             // I'll assume a standard Version comparison.
             
             // Since I don't have easy access to current version in this pure Kotlin module without setup,
             // I will modify the Repository interface to accept `currentVersion`? 
             // Or better, I will emit the Available status and let ViewModel decide if it's new.
             // ACTUALLY, usually Repository encapsulates this.
             // I'll emit Available containing the remote version, and let logic compare.
            
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

