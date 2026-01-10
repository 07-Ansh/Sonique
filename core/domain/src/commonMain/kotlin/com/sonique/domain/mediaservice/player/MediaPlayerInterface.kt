package com.sonique.domain.mediaservice.player

import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.domain.data.player.GenericPlaybackParameters

 
interface MediaPlayerInterface {
     
    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    )

    fun seekBack()

    fun seekForward()

    fun seekToNext()

    fun seekToPrevious()

    fun prepare()

     
    fun setMediaItem(mediaItem: GenericMediaItem)

    fun addMediaItem(mediaItem: GenericMediaItem)

    fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    )

    fun removeMediaItem(index: Int)

    fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    )

    fun clearMediaItems()

    fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    )

    fun getMediaItemAt(index: Int): GenericMediaItem?

    fun getCurrentMediaTimeLine(): List<GenericMediaItem>

    fun getUnshuffledIndex(shuffledIndex: Int): Int

     
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
    val bufferedPosition: Long
    val bufferedPercentage: Int
    val currentMediaItem: GenericMediaItem?
    val currentMediaItemIndex: Int
    val mediaItemCount: Int
    val contentPosition: Long
    val playbackState: Int

     
    fun hasNextMediaItem(): Boolean

    fun hasPreviousMediaItem(): Boolean

     
    var shuffleModeEnabled: Boolean
    var repeatMode: Int
    var playWhenReady: Boolean
    var playbackParameters: GenericPlaybackParameters

     
    val audioSessionId: Int
    var volume: Float
    var skipSilenceEnabled: Boolean

     
    fun addListener(listener: MediaPlayerListener)

    fun removeListener(listener: MediaPlayerListener)

     
    fun release()
}

