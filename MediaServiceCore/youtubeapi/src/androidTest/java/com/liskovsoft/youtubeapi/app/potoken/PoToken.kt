package com.liskovsoft.youtubeapi.app.potoken

import com.google.gson.JsonElement
import com.liskovsoft.sharedutils.TestHelpers
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper
import com.liskovsoft.googlecommon.common.js.V8Runtime

internal class PoToken {
    data class Arguments(val privateScript: String?,
                         val program: String?,
                         val globalName: String?,
                         val bgConfig: BotGuardConfig
    )
    data class Result(val poToken: String?,
                      val integrityTokenData: Data?)
    data class Data(val integrityToken: String?,
                    val estimatedTtlSecs: Int?,
                    val mintRefreshThreshold: Int?,
                    val webSafeFallbackToken: String?)

    private data class BotGuardResult(val integrityTokenData: Data?, val postProcessFunction: String?)

     
    fun generate(args: Arguments): Result? {
        val bgResult = invokeBotGuard(args.privateScript, args.program, args.globalName, args.bgConfig) ?: return null

        if (bgResult.postProcessFunction?.isEmpty() == true) throw IllegalStateException("postProcessFunction cannot be empty")

        val script = listOf(
            DOM_WRAPPER.trimIndent(),
            args.privateScript,
            """
                const acquirePo = ${bgResult.postProcessFunction}(base64ToU8((_b = (_a = '${bgResult.integrityTokenData?.integrityToken}') !== null && _a !== void 0 ? _a : '${bgResult.integrityTokenData?.webSafeFallbackToken}') !== null && _b !== void 0 ? _b : ''));
                const result = acquirePo(new TextEncoder().encode('${args.bgConfig.identifier}'));
                u8ToBase64(result, true)
            """.trimIndent()
        )

        val poToken = V8Runtime.instance().evaluate(script.joinToString(""))

        return Result(poToken, bgResult.integrityTokenData)
    }

     
    fun generatePlaceholder(identifier: String, clientState: Int? = null): String? {
        return null
    }

     
    private fun invokeBotGuard(privateScript: String?, program: String?, globalName: String?, bgConfig: BotGuardConfig): BotGuardResult? {
        val script = listOf(
             

            TestHelpers.readResource("potoken/jsdom_browserify.js"),
            "var mydom = new jsdom.JSDOM(); window = mydom.window; document = mydom.window.document; " +
                    "addEventListener = function(type, listener, options) {document.addEventListener(type, listener, options);};" +
                    "removeEventListener = function(type, listener, options) {document.removeEventListener(type, listener, options);};" +
                    "dispatchEvent = function(event) {document.dispatchEvent(event);};",

             
             
             
             

            privateScript,
            """
                var result = null;
                (async function getPoToken() {
                    var vm = $globalName;
                    const attFunctions = {};
                    const setAttFunctions = (fn1, fn2, fn3, fn4) => {
                       Object.assign(attFunctions, { fn1, fn2, fn3, fn4 });
                    };
                    
                    await vm.a('$program', setAttFunctions, true, undefined, () => {});
                    
                    var botguardResponse;
                    const postProcessFunctions = [];
                    
                    await attFunctions.fn1((response) => (botguardResponse = response), [, , postProcessFunctions]);
                    
                    const payload = ['${bgConfig.requestKey}', botguardResponse];
                    // TODO: handle multiple postProcessFunctions?
                    var second = JSON.stringify(payload);
                    var first = '';
                    if (postProcessFunctions.length > 0)
                        first = postProcessFunctions[0].name;
                    result = first + "$RESULT_DELIM" + second;
                })();
            """.trimIndent(),
            "result.toString();"
        )

        val result = V8Runtime.instance().evaluate(script) ?: return null
         

        val (postProcessFunction, payload) = result.split(RESULT_DELIM)

        if (payload.isEmpty())
            throw IllegalArgumentException("No response")

        if (postProcessFunction.isEmpty())
            throw IllegalArgumentException("Got response but no post-process functions")

        val wrapper = bgConfig.api.generateIntegrityToken(payload)

        val response = RetrofitHelper.get(wrapper) ?: return null

        val (integrityToken, estimatedTtlSecs, mintRefreshThreshold, websafeFallbackToken) = response

        return BotGuardResult(
            Data(asString(integrityToken), asInt(estimatedTtlSecs), asInt(mintRefreshThreshold), asString(websafeFallbackToken)), postProcessFunction
        )
    }

    private fun asInt(intElem: JsonElement?) = if (intElem?.isJsonNull == true) null else intElem?.asInt

    private fun asString(strElem: JsonElement?) = if (strElem?.isJsonNull == true) null else strElem?.asString
}

