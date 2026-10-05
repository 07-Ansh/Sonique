package com.sonique.app.expect

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.sonique.logger.Logger
import org.koin.mp.KoinPlatform.getKoin

actual fun preloadImage(url: String) {
    if (url.isBlank()) return
    try {
        val context: Context = getKoin().get()
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        SingletonImageLoader.get(context).enqueue(request)
    } catch (e: Exception) {
        Logger.e("ImagePreloader", "Failed to preload $url: ${e.message}")
    }
}
