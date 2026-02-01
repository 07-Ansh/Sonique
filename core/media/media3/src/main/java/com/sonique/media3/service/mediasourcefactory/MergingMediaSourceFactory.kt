package com.sonique.media3.service.mediasourcefactory

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy


@UnstableApi
internal class MergingMediaSourceFactory(
    private val defaultMediaSourceFactory: DefaultMediaSourceFactory,
) : MediaSource.Factory {
    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray = defaultMediaSourceFactory.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        return defaultMediaSourceFactory.createMediaSource(mediaItem)

 
    }
}

