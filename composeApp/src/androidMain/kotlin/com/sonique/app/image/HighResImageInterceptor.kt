package com.sonique.app.image

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.sonique.domain.utils.toHighResThumbnailUrl

/**
 * Coil Interceptor that upgrades Google User Content and YouTube image requests
 * to high-resolution formats, with automatic fallback for older YouTube videos
 * that lack maxresdefault thumbnails (404 fallback chain: maxresdefault -> sddefault -> hqdefault).
 */
class HighResImageInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data !is String) {
            return chain.proceed()
        }

        val upgradedUrl = data.toHighResThumbnailUrl()
        val upgradedRequest = if (upgradedUrl != data) {
            request.newBuilder().data(upgradedUrl).build()
        } else {
            request
        }

        val result = chain.withRequest(upgradedRequest).proceed()

        // If a YouTube maxresdefault thumbnail fails (e.g. HTTP 404 on low-res uploads),
        // fall back gracefully to sddefault.jpg or hqdefault.jpg
        if (result is ErrorResult && upgradedUrl.contains("/maxresdefault.jpg")) {
            val sdUrl = upgradedUrl.replace("/maxresdefault.jpg", "/sddefault.jpg")
            val sdResult = chain.withRequest(request.newBuilder().data(sdUrl).build()).proceed()
            if (sdResult is SuccessResult) {
                return sdResult
            }

            val hqUrl = upgradedUrl.replace("/maxresdefault.jpg", "/hqdefault.jpg")
            return chain.withRequest(request.newBuilder().data(hqUrl).build()).proceed()
        }

        return result
    }
}
