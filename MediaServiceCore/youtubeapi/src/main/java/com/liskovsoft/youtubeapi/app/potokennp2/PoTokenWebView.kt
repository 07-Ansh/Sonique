package com.liskovsoft.youtubeapi.app.potokennp2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.liskovsoft.sharedutils.mylogger.Log
import com.liskovsoft.sharedutils.okhttp.OkHttpManager
import io.reactivex.SingleEmitter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(19)
internal class PoTokenWebView private constructor(
    context: Context,
    private var onInitDone: () -> Unit
) : PoTokenGenerator {
    private val webView = WebView(context)
    private val poTokenEmitters = mutableListOf<Pair<String, (String) -> Unit>>()
    private var expirationMs: Long = -1
    var initError: Throwable? = null

     
    init {
        val webViewSettings = webView.settings
         
        webViewSettings.javaScriptEnabled = true
         
         
         
         
        setSafeBrowsingEnabled(webViewSettings, false)

        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true  

         
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                     
                     
                     
                     

                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    Log.e(TAG, "This WebView implementation is broken: $fmt")

                     
                     

                     
                    onInitializationErrorCloseAndCancel(BadWebViewException(fmt))
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    private fun setSafeBrowsingEnabled(settings: WebSettings, enabled: Boolean) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            try {
                WebSettingsCompat.setSafeBrowsingEnabled(settings, enabled)
            } catch (e: AbstractMethodError) {  
                e.printStackTrace()
                 
            }
        }
    }

     
    private fun loadHtmlAndObtainBotguard(context: Context) {
        Log.d(TAG, "loadHtmlAndObtainBotguard() called")

        val html = context.assets.open("po_token.html").bufferedReader()
            .use { it.readText() }

        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html.replaceFirst(
                "</script>",
                 
                "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
            ),
            "text/html",
            "utf-8",
            null,
        )
    }

     
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Log.d(TAG, "downloadAndRunBotguard() called")

        val responseBody = makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/Create",
            "[ \"$REQUEST_KEY\" ]",
        ) ?: return

        val parsedChallengeData = parseChallengeData(responseBody)

        runOnMainThread {
            webView.evaluateJavascript(
                """try {
                    data = $parsedChallengeData
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                }""",
                null
            )
        }
    }

     
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        val msg = "onJsInitializationError: $error"
        Log.e(TAG, msg)
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(msg))
    }

     
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Log.d(TAG, "botguardResponse: $botguardResponse")

        val responseBody = makeBotguardServiceRequest(
            "https://www.youtube.com/api/jnn/v1/GenerateIT",
            "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]",
        ) ?: return

        Log.d(TAG, "GenerateIT response: $responseBody")
        val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)

         
         
         
        expirationMs = System.currentTimeMillis() + ((expirationTimeInSeconds - 600) * 1_000)

        runOnMainThread {
            webView.evaluateJavascript(
                "this.integrityToken = $integrityToken"
            ) {
                Log.d(TAG, "initialization finished, expiration=${expirationTimeInSeconds}s")
                onInitDone()
            }
        }
    }
     

     
    override fun generatePoToken(identifier: String): String {
        Log.d(TAG, "generatePoToken() called with identifier $identifier")
        val latch = CountDownLatch(1)
        lateinit var pot: String

        addPoTokenEmitter(identifier) {
            pot = it
        }

        val u8Identifier = stringToU8(identifier)

        runOnMainThread {
            webView.evaluateJavascript(
                """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
            ) { latch.countDown() }
        }

        latch.await()

        initError?.let { throw it }

        return pot
    }

     
    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        val msg = "onObtainPoTokenError: identifier=$identifier error=$error"
        Log.e(TAG, msg)
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(msg))
    }

     
    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        Log.d(TAG, "Generated poToken (before decoding): identifier=$identifier poTokenU8=$poTokenU8")
        val poToken = u8ToBase64(poTokenU8)

        Log.d(TAG, "Generated poToken: identifier=$identifier poToken=$poToken")
        popPoTokenEmitter(identifier)?.invoke(poToken)
    }

    override fun isExpired(): Boolean {
         
         
        return System.currentTimeMillis() > expirationMs
    }

     

     
     
    private fun addPoTokenEmitter(identifier: String, emitter: (String) -> Unit) {
        synchronized(poTokenEmitters) {
            poTokenEmitters.add(Pair(identifier, emitter))
        }
    }

     
    private fun popPoTokenEmitter(identifier: String): ((String) -> Unit)? {
        return synchronized(poTokenEmitters) {
            poTokenEmitters.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenEmitters.removeAt(it).second
            }
        }
    }

     
    private fun popAllPoTokenEmitters(): List<Pair<String, (String) -> Unit>> {
        return synchronized(poTokenEmitters) {
            val result = poTokenEmitters.toList()
            poTokenEmitters.clear()
            result
        }
    }
     

     
     
    private fun makeBotguardServiceRequest(
        url: String,
        data: String
    ): String? {
        val response = OkHttpManager.instance().doPostRequest(
            url,
            mapOf(
                 
                "User-Agent" to USER_AGENT,
                "Accept" to "application/json",
                "Content-Type" to "application/json+protobuf",
                "x-goog-api-key" to GOOGLE_API_KEY,
                "x-user-agent" to "grpc-web-javascript/0.1",
            ),
            data,
            null
        )

        val httpCode = response.code

        if (httpCode != 200) {
            onInitializationErrorCloseAndCancel(PoTokenException("Invalid response code: $httpCode"))
        }

        if (response.body == null) {
            onInitializationErrorCloseAndCancel(PoTokenException("Response body is empty. Response code: $httpCode"))
        }

        return response.body?.string()
    }

     
    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        initError = error
        popAllPoTokenEmitters()
        runOnMainThread {
            close()
             
            onInitDone()
        }
    }

     
    @MainThread
    override fun close() {
        webView.clearHistory()
         
        webView.clearCache(true)

         
        webView.loadUrl("about:blank")

        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }
     

    companion object : PoTokenGenerator.Factory {
        private val TAG = PoTokenWebView::class.simpleName
         
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"  
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"

        override fun newPoTokenGenerator(context: Context): PoTokenGenerator {
            if (hasThermalServiceBug(context)) {
                throw BadWebViewException("ThermalService isn't available")
            }

            if (hasUsbServiceBug(context)) {
                throw BadWebViewException("Usb service isn't available")
            }

            val latch = CountDownLatch(1)

            lateinit var potWv: PoTokenWebView
            var initError: Throwable? = null

            runOnMainThread {
                potWv = try {
                    PoTokenWebView(context) { latch.countDown() }
                } catch (e: Throwable) {
                    initError = BadWebViewException("${e::class.simpleName}: ${e.message}")
                    latch.countDown()
                    return@runOnMainThread
                }
                potWv.loadHtmlAndObtainBotguard(context)
            }

            latch.await(20, TimeUnit.SECONDS)

            initError?.let { throw it }
            potWv.initError?.let { throw it }

            return potWv
        }

         
        private fun runOnMainThread(
            runnable: Runnable
        ) {
            if (!Handler(Looper.getMainLooper()).post(runnable)) {
                throw PoTokenException("Could not run on main thread")
            }
        }
    }
}


