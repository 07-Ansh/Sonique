package com.liskovsoft.youtubeapi.app.models;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.googlecommon.common.converters.FieldNullable;
import com.liskovsoft.googlecommon.common.converters.regexp.RegExp;

import java.util.regex.Pattern;

 
public class PlayerData {
     

    private static final String FUNCTION_RANDOM_BYTES =
            "var window={};window.crypto={getRandomValues:function(arr){for(var i=0;i<arr.length;i++){arr[i]=Math.floor(Math.random()*Math.floor(Math.pow(2,8*arr.BYTES_PER_ELEMENT)))}}};";

    private static final Pattern SIGNATURE_CLIENT_PLAYBACK_NONCE_V1 = Pattern.compile("function [$\\w]+\\(\\)");
    private static final Pattern SIGNATURE_CLIENT_PLAYBACK_NONCE_V2 =
            Pattern.compile("(function [$\\w]+\\(([\\w])\\)[\\S\\s]*)(function [$\\w]+\\([\\w]\\))([\\S\\s]*)");

     
    @FieldNullable
    @RegExp(";function [$\\w]+\\([\\w]?\\)\\{if\\(window\\.crypto&&window\\.crypto\\.getRandomValues[\\S\\s]*?" +
            "function [$\\w]+\\(\\)\\{for\\(var .*[\\w]\\.push\\(\".*\"\\.charAt\\(.*\\)\\);return [\\w]\\.join\\(\"\"\\)\\}")
    private String mClientPlaybackNonceFunctionV1;

     
    @RegExp("function [$\\w]+\\([\\w]?\\)\\{if\\(window\\.crypto&&window\\.crypto\\.getRandomValues[\\S\\s]*?" +
            "function [$\\w]+\\([\\w]?\\)\\{.*for\\(\\w+ .*[\\w]\\.push\\(\".*\"\\.charAt\\(.*\\)\\);return [\\w]\\.join\\(\"\"\\)\\}")
    private String mClientPlaybackNonceFunctionV2;

    public String getClientPlaybackNonceFunction() {
        String cpn = getRawClientPlaybackNonceFunction();

        return cpn != null ? FUNCTION_RANDOM_BYTES + cpn + "getClientPlaybackNonce();" : null;
    }

    public String getRawClientPlaybackNonceFunction() {
        String cpn = getClientPlaybackNonceFunctionV2();

        if (cpn == null) {
            cpn = getClientPlaybackNonceFunctionV1();
        }

        return cpn;
    }

    private String getClientPlaybackNonceFunctionV1() {
        return Helpers.replace(mClientPlaybackNonceFunctionV1, SIGNATURE_CLIENT_PLAYBACK_NONCE_V1, "function getClientPlaybackNonce()");
    }

    private String getClientPlaybackNonceFunctionV2() {
        return Helpers.replace(mClientPlaybackNonceFunctionV2, SIGNATURE_CLIENT_PLAYBACK_NONCE_V2,
                "$1function getCPN($2)$4function getClientPlaybackNonce(){return getCPN(16)}");
    }

     

     

    private static final Pattern SIGNATURE_DECIPHER = Pattern.compile("function [$\\w]+\\(([\\w])\\)");

     
    @RegExp({
        ";\\w+ [$\\w]+=\\{[\\S\\s]{10,200}?[\\w]\\.reverse\\(\\)[\\S\\s]*?function [$\\w]+\\([\\w]\\)\\{.*[\\w]\\.split\\(\"\"\\).*;return [\\w]\\.join\\(\"\"\\)\\}",
    })
    private String mDecipherFunction;

    @RegExp({
            ";\\w+ [$\\w]+=\\{[\\S\\s]{10,200}?[\\w]\\.reverse\\(\\)[\\S\\s]*?function [$\\w]+\\([\\w]\\)\\{.*[\\w]\\.split\\(.+\\).*;return [\\w]\\.join\\([$\\w]+\\[\\d+\\]\\)\\}",
    })
    private String mDecipherFunctionPart1;

    @RegExp({
            "'use strict';(var [$\\w]+=[.\\S\\s]+?\\.split\\(.+?\\);)",
    })
    private String mDecipherFunctionPart2;

    public String getDecipherFunction() {
        String deFunc = null;

        if (mDecipherFunction != null) {
            deFunc = Helpers.replace(mDecipherFunction, SIGNATURE_DECIPHER, "function decipherSignature($1)");
        } else if (mDecipherFunctionPart1 != null && mDecipherFunctionPart2 != null) {
            deFunc = Helpers.replace(mDecipherFunctionPart1, SIGNATURE_DECIPHER, "function decipherSignature($1)") + ";" + mDecipherFunctionPart2;
        }

        return deFunc;
    }

     

     

    @RegExp("signatureTimestamp:(\\d+)")
    private String mSignatureTimestamp;

    public String getSignatureTimestamp() {
        return mSignatureTimestamp;
    }

     
}
