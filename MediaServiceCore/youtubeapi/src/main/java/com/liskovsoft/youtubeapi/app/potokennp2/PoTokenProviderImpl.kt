package com.liskovsoft.youtubeapi.app.potokennp2

import android.os.Build.VERSION
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenProvider
import com.liskovsoft.youtubeapi.app.potokennp2.misc.PoTokenResult
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.liskovsoft.sharedutils.helpers.DeviceHelpers
import com.liskovsoft.sharedutils.mylogger.Log
import com.liskovsoft.youtubeapi.app.AppService
import com.liskovsoft.youtubeapi.app.potokennp2.visitor.VisitorService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object PoTokenProviderImpl : PoTokenProvider {
    val TAG = PoTokenProviderImpl::class.simpleName
    private val webViewSupported by lazy { DeviceHelpers.isWebViewSupported() }
    private var webViewBadImpl = false  

    private object WebPoTokenGenLock
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null
    
    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (VERSION.SDK_INT < 19 || !isWebPotSupported) {
            return null
        }

        try {
            return getWebClientPoToken(videoId = videoId, forceRecreate = false)
        } catch (e: RuntimeException) {
             
            when (val cause = e.cause) {
                is BadWebViewException -> {
                    Log.e(TAG, "Could not obtain poToken because WebView is broken", e)
                    webViewBadImpl = true
                    return null
                }
                null -> throw e
                else -> throw cause  
            }
        }
    }

     
    @RequiresApi(19)
    private fun getWebClientPoToken(videoId: String, forceRecreate: Boolean): PoTokenResult {
         
        data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

        val (poTokenGenerator, visitorData, streamingPot, hasBeenRecreated) =
            synchronized(WebPoTokenGenLock) {
                val shouldRecreate = webPoTokenGenerator == null || webPoTokenVisitorData == null || webPoTokenStreamingPot == null ||
                   forceRecreate || webPoTokenGenerator!!.isExpired()

                if (shouldRecreate) {
                     
                     
                    webPoTokenVisitorData = VisitorService.getVisitorData()

                    val latch = if (webPoTokenGenerator != null) CountDownLatch(1) else null

                     
                    webPoTokenGenerator?.let {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                it.close()
                            } finally {
                                latch?.countDown()
                            }
                        }
                    }

                    latch?.await(3, TimeUnit.SECONDS)

                     
                    webPoTokenGenerator = PoTokenWebView
                        .newPoTokenGenerator(AppService.instance().context)

                     
                     
                    webPoTokenStreamingPot = webPoTokenGenerator!!
                        .generatePoToken(webPoTokenVisitorData!!)
                }

                return@synchronized Quadruple(
                    webPoTokenGenerator!!,
                    webPoTokenVisitorData!!,
                    webPoTokenStreamingPot!!,
                    shouldRecreate
                )
            }

        val playerPot = try {
             
             
             
            if (videoId.isEmpty()) "" else poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                 
                 
                throw throwable
            } else {
                 
                 
                 
                Log.e(TAG, "Failed to obtain poToken, retrying", throwable)
                return getWebClientPoToken(videoId = videoId, forceRecreate = true)
            }
        }

        Log.d(
            TAG,
            "poToken for $videoId: playerPot=$playerPot, " +
                    "streamingPot=$streamingPot, visitor_data=$visitorData"
        )

        return PoTokenResult(videoId, visitorData, playerPot, streamingPot)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null

    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null

    override fun isWebPotExpired() = isWebPotSupported && webPoTokenGenerator?.isExpired() ?: true

    override fun isWebPotSupported() = VERSION.SDK_INT >= 19 && webViewSupported && !webViewBadImpl

    fun resetCache() {
        webPoTokenVisitorData = null
        webPoTokenStreamingPot = null
    }
}


