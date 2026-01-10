package com.liskovsoft.youtubeapi.app.nsigsolver.provider

internal class InfoExtractorError(message: String, cause: Exception? = null): Exception(message, cause)

internal open class ContentProviderError(message: String, cause: Exception? = null): Exception(message, cause)

 
internal class JsChallengeProviderRejectedRequest(message: String, cause: Exception? = null): ContentProviderError(message, cause)

 
internal class JsChallengeProviderError(message: String, cause: Exception? = null): ContentProviderError(message, cause)

