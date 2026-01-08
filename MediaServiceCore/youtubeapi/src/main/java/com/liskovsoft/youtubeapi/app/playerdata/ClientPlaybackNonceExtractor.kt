package com.liskovsoft.youtubeapi.app.playerdata

import com.liskovsoft.sharedutils.helpers.Helpers
import com.liskovsoft.googlecommon.common.js.V8Runtime
import java.util.regex.Pattern

internal object ClientPlaybackNonceExtractor {
    private const val FUNCTION_RANDOM_BYTES: String =
        "var window={};window.crypto={getRandomValues:function(arr){for(var i=0;i<arr.length;i++){arr[i]=Math.floor(Math.random()*Math.floor(Math.pow(2,8*arr.BYTES_PER_ELEMENT)))}}};"

     
    private val mClientPlaybackNonceFunctionV1: Pattern = Pattern.compile(";function [$\\w]+\\([\\w]?\\)\\{if\\(window\\.crypto&&window\\.crypto\\.getRandomValues[\\S\\s]*?" +
            "function [$\\w]+\\(\\)\\{for\\(var .*[\\w]\\.push\\(\".*\"\\.charAt\\(.*\\)\\);return [\\w]\\.join\\(\"\"\\)\\}")
     
    private val mClientPlaybackNonceFunctionV2: Pattern = Pattern.compile("function [$\\w]+\\([\\w]?\\)\\{if\\(window\\.crypto&&window\\.crypto\\.getRandomValues[\\S\\s]*?" +
            "function [$\\w]+\\([\\w]?\\)\\{.*for\\(\\w+ .*[\\w]\\.push\\(\".*\"\\.charAt\\(.*\\)\\);return [\\w]\\.join\\(\"\"\\)\\}")
    private val SIGNATURE_CLIENT_PLAYBACK_NONCE_V1: Pattern = Pattern.compile("function [$\\w]+\\(\\)")
    private val SIGNATURE_CLIENT_PLAYBACK_NONCE_V2: Pattern = Pattern.compile("(function [$\\w]+\\(([\\w])\\)[\\S\\s]*)(function [$\\w]+\\([\\w]\\))([\\S\\s]*)")

    fun extractClientPlaybackNonceCode(jsCode: String): String? {
        return extractRawClientPlaybackNonceFunction(jsCode)?.let { "${FUNCTION_RANDOM_BYTES}${it}getClientPlaybackNonce();" }
    }

    fun createClientPlaybackNonce(clientPlaybackNonceCode: String): String? {
        return V8Runtime.instance().evaluate(clientPlaybackNonceCode)
    }

    private fun extractRawClientPlaybackNonceFunction(jsCode: String): String? {
        var matcher = mClientPlaybackNonceFunctionV2.matcher(jsCode)

        if (matcher.find()) {
            return Helpers.replace(matcher.group(), SIGNATURE_CLIENT_PLAYBACK_NONCE_V2,
                "$1function getCPN($2)$4function getClientPlaybackNonce(){return getCPN(16)}")
        }

        matcher = mClientPlaybackNonceFunctionV1.matcher(jsCode)

        if (matcher.find()) {
            return Helpers.replace(matcher.group(), SIGNATURE_CLIENT_PLAYBACK_NONCE_V1, "function getClientPlaybackNonce()")
        }

        return null
    }
}

