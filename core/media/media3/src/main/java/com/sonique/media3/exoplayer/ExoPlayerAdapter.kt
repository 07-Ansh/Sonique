package com.sonique.media3.exoplayer

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.domain.data.player.GenericMediaMetadata
import com.sonique.domain.data.player.GenericPlaybackParameters
import com.sonique.domain.data.player.GenericTracks
import com.sonique.domain.data.player.PlayerConstants
import com.sonique.domain.data.player.PlayerError
import com.sonique.domain.mediaservice.player.MediaPlayerInterface
import com.sonique.domain.mediaservice.player.MediaPlayerListener
import com.sonique.logger.Logger
import com.sonique.media3.utils.BetterShuffleOrder

/**
 * ExoPlayer implementation of MediaPlayerInterface
 * Handles all Media3-specific logic and conversions
 */
private const val TAG = "ExoPlayerAdapter"

@SuppressLint("UnsafeOptInUsageError")
@UnstableApi
class ExoPlayerAdapter(
    private val exoPlayer: ExoPlayer,
) : MediaPlayerInterface {
    private val listeners = mutableListOf<MediaPlayerListener>()
    private val exoPlayerListener = ExoPlayerListenerImpl()

    // Shuffle management
    // Maps original playlist index -> shuffled position
    private var shuffleIndices = mutableListOf<Int>()

    // Maps shuffled position -> original playlist index
    private var shuffleOrder = mutableListOf<Int>()

    init {
        exoPlayer.addListener(exoPlayerListener)
    }

    // Playback control
    override fun play() = exoPlayer.play()

    override fun pause() = exoPlayer.pause()

    override fun stop() = exoPlayer.stop()

    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) = exoPlayer.seekTo(mediaItemIndex, positionMs)

    override fun seekBack() = exoPlayer.seekBack()

    override fun seekForward() = exoPlayer.seekForward()

    override fun seekToNext() = exoPlayer.seekToNext()

    override fun seekToPrevious() = exoPlayer.seekToPrevious()

    override fun prepare() = exoPlayer.prepare()

    // Media item management
    override fun setMediaItem(mediaItem: GenericMediaItem) {
        exoPlayer.setMediaItem(mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        exoPlayer.addMediaItem(mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.addMediaItem(index, mediaItem.toMedia3MediaItem())
        val currentIndexBeforeInsert = currentMediaItemIndex
        // Update shuffle order if enabled
        if (shuffleModeEnabled) {
            // Check if this is "play next" (inserting right after current playing song)
            if (currentIndexBeforeInsert >= 0 && index == currentIndexBeforeInsert + 1) {
                // This is "play next" - insert into shuffle order right after current song
                val currentShufflePos = shuffleIndices.getOrNull(currentIndexBeforeInsert) ?: 0
                insertIntoShuffleOrder(index, currentShufflePos)
            } else {
                // Not "play next" - recreate entire shuffle order
                createShuffleOrder()
            }
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun removeMediaItem(index: Int) {
        exoPlayer.removeMediaItem(index)
        if (shuffleModeEnabled) {
            removeFromShuffleOrder(index)
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (shuffleModeEnabled) {
            moveShuffleOrder(fromIndex, toIndex)
        } else {
            exoPlayer.moveMediaItem(fromIndex, toIndex)
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun clearMediaItems() {
        exoPlayer.clearMediaItems()
        clearShuffleOrder()
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.replaceMediaItem(index, mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? =
        if (index in 0..<exoPlayer.mediaItemCount) {
            exoPlayer.getMediaItemAt(index).toGenericMediaItem()
        } else {
            null
        }

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> =
        if (shuffleModeEnabled) {
            shuffleOrder.map { shuffledIndex -> exoPlayer.getMediaItemAt(shuffledIndex).toGenericMediaItem() }
        } else {
            List(exoPlayer.mediaItemCount) { i -> exoPlayer.getMediaItemAt(i).toGenericMediaItem() }
        }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int =
        if (shuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }

    // Playback state properties
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
    override val currentPosition: Long get() = exoPlayer.currentPosition
    override val duration: Long get() = exoPlayer.duration
    override val bufferedPosition: Long get() = exoPlayer.bufferedPosition
    override val bufferedPercentage: Int get() = exoPlayer.bufferedPercentage
    override val currentMediaItem: GenericMediaItem? get() = exoPlayer.currentMediaItem?.toGenericMediaItem()
    override val currentMediaItemIndex: Int get() = exoPlayer.currentMediaItemIndex
    override val mediaItemCount: Int get() = exoPlayer.mediaItemCount
    override val contentPosition: Long get() = exoPlayer.contentPosition
    override val playbackState: Int get() = exoPlayer.playbackState

    // Navigation
    override fun hasNextMediaItem(): Boolean = exoPlayer.hasNextMediaItem()

    override fun hasPreviousMediaItem(): Boolean = exoPlayer.hasPreviousMediaItem()

    // Playback modes
    override var shuffleModeEnabled: Boolean
        get() = exoPlayer.shuffleModeEnabled
        set(value) {
            exoPlayer.shuffleModeEnabled = value
        }

    override var repeatMode: Int
        get() = exoPlayer.repeatMode
        set(value) {
            exoPlayer.repeatMode = value
        }

    override var playWhenReady: Boolean
        get() = exoPlayer.playWhenReady
        set(value) {
            exoPlayer.playWhenReady = value
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = exoPlayer.playbackParameters.toGenericPlaybackParameters()
        set(value) {
            exoPlayer.playbackParameters = value.toMedia3PlaybackParameters()
        }

    // Audio settings
    override val audioSessionId: Int get() = exoPlayer.audioSessionId
    override var volume: Float
        get() = exoPlayer.volume
        set(value) {
            exoPlayer.volume = value
        }

    override var skipSilenceEnabled: Boolean
        get() = exoPlayer.skipSilenceEnabled
        set(value) {
            exoPlayer.skipSilenceEnabled = value
        }

    // Listener management
    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    // Release resources
    override fun release() {
        exoPlayer.removeListener(exoPlayerListener)
        listeners.clear()
        exoPlayer.release()
    }

    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        val list = mutableListOf<GenericMediaItem>()
        val s = exoPlayer.shuffleModeEnabled
        val timeline = exoPlayer.currentTimeline
        var i = timeline.getFirstWindowIndex(s)
        while (i != C.INDEX_UNSET) {
            getMediaItemAt(i)?.let { list.add(it) }
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, s)
        }
        return list
    }

    /**
     * Notify timeline changed with current order (shuffled or not)
     */
    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        listeners.forEach { it.onTimelineChanged(list, reason) }
    }

    // Internal ExoPlayer listener that converts events to generic events
    private inner class ExoPlayerListenerImpl : Player.Listener {
//        override fun onTimelineChanged(
//            timeline: Timeline,
//            reason: Int,
//        ) {
//            super.onTimelineChanged(timeline, reason)
//            val list = mutableListOf<GenericMediaItem>()
//            val s = exoPlayer.shuffleModeEnabled
//            var i = timeline.getFirstWindowIndex(s)
//            while (i != C.INDEX_UNSET) {
//                getMediaItemAt(i)?.let { list.add(it) }
//                i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, s)
//            }
//            listeners.forEach {
//                it.onTimelineChanged(
//                    list,
//                    when (reason) {
//                        Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> "TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED"
//                        Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> "TIMELINE_CHANGE_REASON_SOURCE_UPDATE"
//                        else -> "Unknown"
//                    },
//                )
//            }
//        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val domainState =
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        PlayerConstants.STATE_IDLE
                    }

                    Player.STATE_ENDED -> {
                        PlayerConstants.STATE_ENDED
                    }

                    Player.STATE_READY -> {
                        PlayerConstants.STATE_READY
                    }

                    else -> {
                        playbackState
                    }
                }
            listeners.forEach { it.onPlaybackStateChanged(domainState) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listeners.forEach { it.onIsPlayingChanged(isPlaying) }
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            val genericMediaItem = mediaItem?.toGenericMediaItem()
            val domainReason =
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_SEEK
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                    else -> reason
                }
            listeners.forEach { it.onMediaItemTransition(genericMediaItem, domainReason) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val genericTracks = tracks.toGenericTracks()
            listeners.forEach { it.onTracksChanged(genericTracks) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val domainErrorCode =
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_TIMEOUT -> PlayerConstants.ERROR_CODE_TIMEOUT
                    else -> error.errorCode
                }
            val genericError =
                PlayerError(
                    errorCode = domainErrorCode,
                    errorCodeName = error.errorCodeName,
                    message = error.message,
                )
            listeners.forEach { it.onPlayerError(genericError) }
        }

        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            val shouldBePlaying = !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                if (shouldBePlaying) {
                    listeners.forEach {
                        it.shouldOpenOrCloseEqualizerIntent(true)
                    }
                } else {
                    listeners.forEach {
                        it.shouldOpenOrCloseEqualizerIntent(false)
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) {
                createShuffleOrder()
            } else {
                clearShuffleOrder()
            }
            val list = getShuffledMediaItemList()
            listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled, list) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val domainRepeatMode =
                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> PlayerConstants.REPEAT_MODE_OFF
                    Player.REPEAT_MODE_ONE -> PlayerConstants.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ALL -> PlayerConstants.REPEAT_MODE_ALL
                    else -> repeatMode
                }
            listeners.forEach { it.onRepeatModeChanged(domainRepeatMode) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            listeners.forEach { it.onIsLoadingChanged(isLoading) }
        }
    }

    /**
     * Create shuffle order for current playlist
     * Keeps the current track at its position and shuffles the rest
     */
    private fun createShuffleOrder() {
        if (mediaItemCount == 0) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        // Create list of all indices
        val indices = (0..<mediaItemCount).toMutableList()

        // If we have a current track, keep it at current position
        val currentIndex = currentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        // Shuffle the remaining indices
        indices.shuffle()

        // If we have a current track, insert it at the beginning
        if (currentIndex in (0..<mediaItemCount)) {
            indices.add(0, currentIndex)
        }

        // Store the shuffle order
        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        // Create reverse mapping (original index -> shuffled position)
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    /**
     * Clear shuffle order
     */
    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
        exoPlayer.shuffleOrder.cloneAndClear()
        Logger.d(TAG, "Cleared shuffle order")
    }

    /**
     * Insert item into shuffle order at specific position
     * Used for "play next" functionality when shuffle is enabled
     *
     * @param insertedOriginalIndex The index in the original playlist where item was inserted
     * @param afterShufflePos The shuffle position after which to insert (typically current song's position)
     */
    private fun insertIntoShuffleOrder(
        insertedOriginalIndex: Int,
        afterShufflePos: Int,
    ) {
        if (mediaItemCount == 0 || insertedOriginalIndex !in 0..<mediaItemCount) {
            return
        }

        // Step 1: Adjust all existing shuffle order indices that are >= insertedOriginalIndex
        // (because we inserted a new item, all indices after it shift up by 1)
        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] >= insertedOriginalIndex) {
                shuffleOrder[i]++
            }
        }

        // Step 2: Insert the new item right after the specified shuffle position
        val insertPos = (afterShufflePos + 1).coerceIn(0, shuffleOrder.size)
        shuffleOrder.add(insertPos, insertedOriginalIndex)

        // Step 3: Rebuild the reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Inserted index $insertedOriginalIndex into shuffle at position $insertPos (after shuffle pos $afterShufflePos)")
    }

    private fun moveShuffleOrder(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex !in shuffleOrder.indices || toIndex !in shuffleOrder.indices) {
            return
        }

        val item = shuffleOrder.removeAt(fromIndex)
        shuffleOrder.add(toIndex, item)

        // Rebuild reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Moved shuffle order item from $fromIndex to $toIndex")
    }

    private fun removeFromShuffleOrder(originalIndex: Int) {
        val shufflePos = shuffleIndices.getOrNull(originalIndex) ?: return
        shuffleOrder.removeAt(shufflePos)

        // Rebuild reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Removed original index $originalIndex from shuffle order")
    }
}

// Extension functions for conversions between Media3 and Generic types

@UnstableApi
private fun GenericMediaItem.toMedia3MediaItem(): MediaItem {
    val builder =
        MediaItem
            .Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata.toMedia3MediaMetadata())

    uri?.let { builder.setUri(it) }
    customCacheKey?.let { builder.setCustomCacheKey(it) }

    return builder.build()
}

@UnstableApi
private fun MediaItem.toGenericMediaItem(): GenericMediaItem =
    GenericMediaItem(
        mediaId = mediaId,
        uri = localConfiguration?.uri.toString(),
        metadata = mediaMetadata.toGenericMediaMetadata(),
        customCacheKey = localConfiguration?.customCacheKey,
    )

private fun GenericMediaMetadata.toMedia3MediaMetadata(): MediaMetadata =
    MediaMetadata
        .Builder()
        .apply {
            title?.let { setTitle(it) }
            artist?.let { setArtist(it) }
            albumTitle?.let { setAlbumTitle(it) }
            artworkUri?.let { setArtworkUri(it.toUri()) }
            description?.let { setDescription(it) }
        }.build()

private fun MediaMetadata.toGenericMediaMetadata(): GenericMediaMetadata =
    GenericMediaMetadata(
        title = title?.toString(),
        artist = artist?.toString(),
        albumTitle = albumTitle?.toString(),
        artworkUri = artworkUri.toString(),
        description = description?.toString(),
    )

private fun GenericPlaybackParameters.toMedia3PlaybackParameters(): PlaybackParameters = PlaybackParameters(speed, pitch)

private fun PlaybackParameters.toGenericPlaybackParameters(): GenericPlaybackParameters = GenericPlaybackParameters(speed, pitch)

private fun Tracks.toGenericTracks(): GenericTracks {
    val genericGroups =
        groups.map { group ->
            GenericTracks.GenericTrackGroup(trackCount = group.length)
        }
    return GenericTracks(groups = genericGroups)
}

