package com.sonique.data.lyrics

import com.sonique.domain.lyrics.LyricsUtils
import com.sonique.data.lyrics.providers.*
import kotlinx.coroutines.*

private const val MAX_LYRICS_FETCH_MS = 25000L
private const val PER_PROVIDER_TIMEOUT_MS = 8000L

class LyricsHelper {

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        providerOrder: List<String> = LyricsProviderRegistry.getDefaultProviderOrder()
    ): LyricsWithProvider {
        val enabledProviders = providerOrder.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(title)

            for (provider in enabledProviders) {
                val providerResult = try {
                    withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            id = id,
                            title = cleanedTitle,
                            artist = artist,
                            duration = duration,
                            album = album
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }

                if (providerResult != null && providerResult.isSuccess) {
                    val filtered = LyricsUtils.filterLyricsCreditLines(providerResult.getOrNull()!!)
                    return@withTimeoutOrNull LyricsWithProvider(filtered, provider.name)
                }
            }
            LyricsWithProvider("", "")
        }

        return result ?: LyricsWithProvider("", "")
    }

    suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        providerOrder: List<String> = LyricsProviderRegistry.getDefaultProviderOrder(),
        callback: (LyricsResult) -> Unit
    ) {
        val cleanedTitle = LyricsUtils.cleanTitleForSearch(title)
        val enabledProviders = providerOrder.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }

        coroutineScope {
            val jobs = enabledProviders.map { provider ->
                launch {
                    try {
                        provider.getAllLyrics(id, cleanedTitle, artist, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            callback(LyricsResult(provider.name, filteredLyrics))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // ignore and continue
                    }
                }
            }
            jobs.forEach { it.join() }
        }
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
