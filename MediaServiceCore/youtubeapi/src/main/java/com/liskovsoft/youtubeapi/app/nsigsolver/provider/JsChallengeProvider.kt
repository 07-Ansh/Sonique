package com.liskovsoft.youtubeapi.app.nsigsolver.provider

import com.liskovsoft.youtubeapi.app.nsigsolver.common.YouTubeInfoExtractor

internal abstract class JsChallengeProvider {
    protected val ie = YouTubeInfoExtractor
    protected abstract val supportedTypes: List<JsChallengeType>

    private fun validateRequest(request: JsChallengeRequest) {
         
        if (request.type !in supportedTypes) {
            throw JsChallengeProviderRejectedRequest("JS Challenge type ${request.type} is not supported by the provider ${this::class.simpleName}")
        }
    }

     
    fun bulkSolve(requests: List<JsChallengeRequest>): Sequence<JsChallengeProviderResponse> = sequence {
        val validatedRequests: MutableList<JsChallengeRequest> = mutableListOf()
        for (request in requests) {
            try {
                validateRequest(request)
                validatedRequests.add(request)
            } catch (e: JsChallengeProviderRejectedRequest) {
                yield(JsChallengeProviderResponse(request=request, error=e))
            }
        }
        yieldAll(realBulkSolve(validatedRequests))
    }

     
    protected abstract fun realBulkSolve(requests: List<JsChallengeRequest>): Sequence<JsChallengeProviderResponse>

    protected fun getPlayer(playerUrl: String): String {
        return try {
            ie.loadPlayer(playerUrl)
        } catch (e: Exception) {
            throw JsChallengeProviderError("Failed to load player for JS challenge: $playerUrl", e)
        }
    }
}

