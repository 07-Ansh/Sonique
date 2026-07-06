package com.sonique.kotlinytmusicscraper.extractor

import com.sonique.kotlinytmusicscraper.models.YouTubeClient
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.Proxy

private val REQUIRED_AUDIO_ITAGS = setOf(250, 251, 774, 141)
private val REQUIRED_VIDEO_ITAGS = setOf(137, 136, 134)

internal fun List<Pair<Int, String>>.hasRequiredItags(): Boolean {
    val itags = this.mapTo(HashSet()) { it.first }
    val hasAudio = REQUIRED_AUDIO_ITAGS.any { it in itags }
    val hasVideo = REQUIRED_VIDEO_ITAGS.any { it in itags }
    return hasAudio && hasVideo
}

private val streamHealthCheckClient: OkHttpClient by lazy {
    OkHttpClient
        .Builder()
        .build()
}

/**
 * Pick one random URL whose itag belongs to [REQUIRED_AUDIO_ITAGS] or [REQUIRED_VIDEO_ITAGS] and
 * HEAD it. Returns true only when the response code is in the 200..299 range. We only health-check
 * required-itag URLs because those are the ones the player will actually use; lower-quality
 * extras can stay unverified.
 */
internal fun List<Pair<Int, String>>.headCheckRandomStream(): Boolean {
    val required = REQUIRED_AUDIO_ITAGS + REQUIRED_VIDEO_ITAGS
    val candidate = this.filter { it.first in required }.randomOrNull() ?: return false
    return runCatching {
        val request =
            okhttp3.Request
                .Builder()
                .head()
                .url(candidate.second)
                .build()
        streamHealthCheckClient.newCall(request).execute().use { response ->
            response.code in 200..299
        }
    }.getOrDefault(false)
}

class BraveNewPipeDownloaderImpl(
    proxy: Proxy?,
) : Downloader() {
    private val client =
        OkHttpClient
            .Builder()
            .proxy(proxy)
            .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request
                .Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body?.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }
}
