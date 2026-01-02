package com.sonique.media_jvm

import com.sonique.common.MERGING_DATA_TYPE
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.domain.data.player.GenericPlaybackParameters
import com.sonique.domain.data.player.PlayerConstants
import com.sonique.domain.data.player.PlayerError
import com.sonique.domain.extension.isVideo
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.player.MediaPlayerInterface
import com.sonique.domain.mediaservice.player.MediaPlayerListener
import com.sonique.domain.repository.StreamRepository
import com.sonique.logger.Logger
import com.sonique.media_jvm.download.getDownloadPath
import com.sun.jna.Platform
import com.sun.jna.platform.win32.Kernel32
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.freedesktop.gstreamer.Bin
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.Format
import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Pipeline
import org.freedesktop.gstreamer.State
import org.freedesktop.gstreamer.Version
import org.freedesktop.gstreamer.elements.PlayBin
import org.freedesktop.gstreamer.event.SeekFlags
import org.freedesktop.gstreamer.event.SeekType
import org.freedesktop.gstreamer.swing.GstVideoComponent
import java.io.File
import java.net.URI
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

private const val TAG = "GstreamerPlayerAdapter"

/**
 * GStreamer implementation of MediaPlayerInterface
 * Features:
 * - Queue management with auto-load for next track
 * - Precaching system for smooth transitions
 * - Thread-safe operations with dedicated GStreamer thread
 * - Hardware acceleration support
 * - Advanced audio pipeline
 * - Proper state machine like ExoPlayer
 */
class GstreamerPlayerAdapter(
    private val coroutineScope: CoroutineScope,
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
) : MediaPlayerInterface {
    // Internal state enum for proper state machine
    private enum class InternalState {
        IDLE, // No media loaded
        PREPARING, // Loading media
        READY, // Ready to play/paused
        PLAYING, // Currently playing
        PAUSED,
        ENDED, // Playback ended
        ERROR, // Error state
    }

    private fun InternalState.isInReadyState(): Boolean =
        this == InternalState.READY || this == InternalState.PLAYING || this == InternalState.PAUSED

    init {
        /**
         * Set up paths to native GStreamer libraries - see adjacent file.
         */
        configurePaths()

        /**
         * Initialize GStreamer. Always pass the lowest version you require -
         * Version.BASELINE is GStreamer 1.8. Use Version.of() for higher.
         * Features requiring later versions of GStreamer than passed here will
         * throw an exception in the bindings even if the actual native library
         * is a higher version.
         */
        Gst.init(Version.of(1, 20), "FXPlayer", "--gapless")
    }

    // ========== Threading Model ==========
    // Single-threaded executor for ALL GStreamer operations (like ExoPlayer's internal playback thread
    // ========== State Management ==========
    private val listeners = mutableListOf<MediaPlayerListener>()

    @Volatile
    private var currentPlayer: GstreamerPlayer? = null

    @Volatile
    private var internalState = InternalState.IDLE

    @Volatile
    private var internalPlayWhenReady = true

    @Volatile
    private var internalVolume = 1.0f

    @Volatile
    private var internalRepeatMode = PlayerConstants.REPEAT_MODE_OFF

    @Volatile
    private var internalShuffleModeEnabled = false

    @Volatile
    private var internalPlaybackSpeed = 1.0f

    // Position tracking - updated periodically, not on every query
    @Volatile
    private var cachedPosition = 0L

    @Volatile
    private var cachedDuration = 0L

    @Volatile
    private var cachedBufferedPosition = 0L

    @Volatile
    private var cachedIsLoading = false

    // Buffering state for dual-stream playback (audio + video)
    @Volatile
    private var audioBufferingPercent = 100

    @Volatile
    private var videoBufferingPercent = 100

    @Volatile
    private var wasPlayingBeforeBuffering = false

    private var positionUpdateJob: Job? = null

    // State transition debouncing to prevent flickering
    @Volatile
    private var lastStateChangeTime = 0L
    private val stateChangeDebounceMs = 100L

    // Bus listener management
    private data class BusListeners(
        val eos: Bus.EOS,
        val durationChanged: Bus.DURATION_CHANGED,
        val error: Bus.ERROR,
        val warning: Bus.WARNING,
        val stateChanged: Bus.STATE_CHANGED,
        val buffering: Bus.BUFFERING,
        val asyncDone: Bus.ASYNC_DONE,
    )

    private var activeBusListeners: BusListeners? = null

    // Simplified listener for video player (only buffering needed)
    private var activeVideoBufferingListener: Bus.BUFFERING? = null

    // Precaching system
    private data class PrecachedPlayer(
        val player: GstreamerPlayer,
        val mediaItem: GenericMediaItem,
        val url: String,
    )

    // VideoId -> Player
    private val precachedPlayers = ConcurrentHashMap<String, PrecachedPlayer>()
    private var precacheEnabled = true
    private val maxPrecacheCount = 2
    private var precacheJob: Job? = null

    // Playlist management
    private val playlist = mutableListOf<GenericMediaItem>()
    private var localCurrentMediaItemIndex = -1

    // Shuffle management
    // Maps original playlist index -> shuffled position
    private var shuffleIndices = mutableListOf<Int>()
    // Maps shuffled position -> original playlist index
    private var shuffleOrder = mutableListOf<Int>()

    // Loading management
    private var currentLoadJob: Job? = null

    fun getCurrentPlayer(): GstreamerPlayer? = currentPlayer

    // ========== Playback Control ==========

    override fun play() {
        Logger.d(TAG, "▶️ play() called (current state: $internalState, playWhenReady: $internalPlayWhenReady)")
        coroutineScope.launch {
            when (internalState) {
                InternalState.READY, InternalState.ENDED, InternalState.PAUSED -> {
                    currentPlayer?.let { player ->
                        Logger.d(TAG, "▶️ Play: Setting GStreamer state to PLAYING")
                        player.setState(State.PLAYING)
                        transitionToState(InternalState.PLAYING)
                        internalPlayWhenReady = true
                        // State change will be handled by stateChangedListener
                    } ?: Logger.w(TAG, "Play called but currentPlayer is null")
                }

                InternalState.PREPARING -> {
                    // Just set playWhenReady, will auto-play when ready
                    if (!cachedIsLoading) {
                        cachedIsLoading = true
                        listeners.forEach { it.onIsLoadingChanged(true) }
                    }
                    Logger.d(TAG, "▶️ Play: During PREPARING - will auto-play when ready")
                }

                InternalState.PLAYING -> {
                    // Already playing, update flag
                    internalPlayWhenReady = true
                    cachedIsLoading = false
                    Logger.d(TAG, "▶️ Play: Already playing")
                }

                else -> {
                    Logger.w(TAG, "▶️ Play: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun pause() {
        Logger.d(TAG, "⏸️ pause() called (current state: $internalState, playWhenReady: $internalPlayWhenReady)")
        coroutineScope.launch {
            currentPlayer?.pause()
            when (internalState) {
                InternalState.PLAYING, InternalState.READY -> {
                    currentPlayer?.let { player ->
                        Logger.d(TAG, "⏸️ Pause: Setting GStreamer state to PAUSED")
                        player.setState(State.PAUSED)
                        transitionToState(InternalState.PAUSED)
                        internalPlayWhenReady = false
                        // State change will be handled by stateChangedListener
                    }
                }

                InternalState.PREPARING -> {
                    // Just set playWhenReady to false
                    internalPlayWhenReady = false
                    Logger.d(TAG, "⏸️ Pause: During PREPARING - will not auto-play")
                }

                else -> {
                    Logger.w(TAG, "⏸️ Pause: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            currentPlayer?.let { player ->
                Logger.d(TAG, "Stop called")
                player.setState(State.NULL)
                transitionToState(InternalState.IDLE)
                stopPositionUpdates()
                notifyEqualizerIntent(false)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        currentPlayer?.let { player ->
            try {
                val seekResult = player.seek(positionMs, TimeUnit.MILLISECONDS)
                if (seekResult) {
                    cachedPosition = positionMs
                    Logger.d(TAG, "Seeked to position: $positionMs")
                } else {
                    Logger.w(TAG, "Seek failed to position: $positionMs")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Seek exception: ${e.message}", e)
            }
        }
    }

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
        if (mediaItemIndex !in playlist.indices) return

        coroutineScope.launch {
            val shouldPlay = internalPlayWhenReady

            // Cancel any ongoing load
            currentLoadJob?.cancel()

            // Load the new track
            localCurrentMediaItemIndex = mediaItemIndex
            currentPlayer?.pause()
            currentPlayer?.release()
            currentPlayer = null
            loadAndPlayTrackInternal(mediaItemIndex, positionMs, shouldPlay)
        }
    }

    override fun seekBack() {
        val newPosition = (cachedPosition - 5000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun seekForward() {
        val newPosition = (cachedPosition + 5000).coerceAtMost(cachedDuration)
        seekTo(newPosition)
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            val nextIndex = getNextMediaItemIndex()
            seekTo(nextIndex, 0)
        }
    }

    override fun seekToPrevious() {
        if (hasPreviousMediaItem()) {
            val prevIndex = getPreviousMediaItemIndex()
            seekTo(prevIndex, 0)
        }
    }

    override fun prepare() {
        if (playlist.isNotEmpty() && localCurrentMediaItemIndex >= 0) {
            coroutineScope.launch {
                loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, false)
            }
        }
    }

    // ========== Media Item Management ==========

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        coroutineScope.launch {
            // Cancel ongoing operations
            currentLoadJob?.cancel()
            cancelPrecaching()

            playlist.clear()
            clearAllPrecacheInternal()
            playlist.add(mediaItem)
            localCurrentMediaItemIndex = 0

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            loadAndPlayTrackInternal(0, 0, internalPlayWhenReady)
        }
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)

        // Update shuffle order if enabled
        if (internalShuffleModeEnabled) {
            createShuffleOrder()
        }

        // Notify timeline changed
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

        if (playlist.size - 1 - currentMediaItemIndex <= maxPrecacheCount) {
            // If added item is within precache range, trigger precaching
            coroutineScope.launch {
                clearPrecacheExceptCurrentInternal()
                triggerPrecachingInternal()
            }
        }
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        if (index in 0..playlist.size) {
            // Store current index before modifications for shuffle logic
            val currentIndexBeforeInsert = localCurrentMediaItemIndex

            playlist.add(index, mediaItem)

            // Adjust current index if needed
            if (index <= localCurrentMediaItemIndex) {
                localCurrentMediaItemIndex++
            }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
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

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index - 1 - currentMediaItemIndex <= maxPrecacheCount) {
                // If added item is within precache range, trigger precaching
                coroutineScope.launch {
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }
        }
    }

    override fun removeMediaItem(index: Int) {
        if (index !in playlist.indices) return

        coroutineScope.launch {
            val track = playlist.removeAt(index)

            // Remove from precache
            precachedPlayers.remove(track.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            when {
                index < localCurrentMediaItemIndex -> {
                    localCurrentMediaItemIndex--
                    // Rekey precache
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }

                index == localCurrentMediaItemIndex -> {
                    if (localCurrentMediaItemIndex >= playlist.size) {
                        localCurrentMediaItemIndex = playlist.size - 1
                    }
                    if (localCurrentMediaItemIndex >= 0) {
                        loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, internalPlayWhenReady)
                    } else {
                        cleanupCurrentPlayerInternal()
                    }
                }

                else -> {
                    // Index after current, just update precache
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }
    }

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex !in playlist.indices || toIndex !in playlist.indices) return

        coroutineScope.launch {
            val item = playlist.removeAt(fromIndex)
            playlist.add(toIndex, item)

            // Update current index
            localCurrentMediaItemIndex =
                when {
                    localCurrentMediaItemIndex == fromIndex -> toIndex
                    fromIndex < localCurrentMediaItemIndex && toIndex >= localCurrentMediaItemIndex ->
                        localCurrentMediaItemIndex - 1

                    fromIndex > localCurrentMediaItemIndex && toIndex <= localCurrentMediaItemIndex ->
                        localCurrentMediaItemIndex + 1

                    else -> localCurrentMediaItemIndex
                }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            // Clear and rebuild precache
            clearPrecacheExceptCurrentInternal()
            triggerPrecachingInternal()
        }
    }

    override fun clearMediaItems() {
        coroutineScope.launch {
            playlist.clear()
            localCurrentMediaItemIndex = -1

            // Clear shuffle order
            clearShuffleOrder()

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            cleanupCurrentPlayerInternal()
            clearAllPrecacheInternal()
        }
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        if (index !in playlist.indices) return

        coroutineScope.launch {
            playlist[index] = mediaItem

            // Remove from precache
            precachedPlayers.remove(mediaItem.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            // Notify timeline changed
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index == localCurrentMediaItemIndex) {
                loadAndPlayTrackInternal(index, 0, internalPlayWhenReady)
            } else {
                triggerPrecachingInternal()
            }
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> {
        return if (internalShuffleModeEnabled) {
            shuffleOrder.mapNotNull { shuffledIndex -> playlist.getOrNull(shuffledIndex) }
        } else {
            playlist.toList()
        }
    }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int {
        return if (internalShuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }
    }

    // ========== Playback State Properties ==========

    override val isPlaying: Boolean
        get() = internalState == InternalState.PLAYING

    override val currentPosition: Long
        get() = cachedPosition

    override val duration: Long
        get() = cachedDuration

    override val bufferedPosition: Long
        get() = cachedBufferedPosition

    override val bufferedPercentage: Int
        get() {
            val dur = duration
            if (dur <= 0) return 0
            return ((bufferedPosition * 100) / dur).toInt().coerceIn(0, 100)
        }

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(localCurrentMediaItemIndex)

    override val currentMediaItemIndex: Int
        get() = localCurrentMediaItemIndex

    override val mediaItemCount: Int
        get() = playlist.size

    override val contentPosition: Long
        get() = cachedPosition

    override val playbackState: Int
        get() =
            when (internalState) {
                InternalState.IDLE -> PlayerConstants.STATE_IDLE
                InternalState.PREPARING -> PlayerConstants.STATE_BUFFERING
                InternalState.READY -> PlayerConstants.STATE_READY
                InternalState.PLAYING -> PlayerConstants.STATE_READY
                InternalState.ENDED -> PlayerConstants.STATE_ENDED
                InternalState.ERROR -> PlayerConstants.STATE_IDLE
                InternalState.PAUSED -> PlayerConstants.STATE_READY
            }

    // ========== Navigation ==========

    override fun hasNextMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else -> localCurrentMediaItemIndex < playlist.size - 1
        }

    override fun hasPreviousMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else -> localCurrentMediaItemIndex > 0
        }

    private fun getNextMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> localCurrentMediaItemIndex
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    // Find current position in shuffle order
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = (currentShufflePos + 1) % shuffleOrder.size
                    shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex < playlist.size - 1) {
                        localCurrentMediaItemIndex + 1
                    } else {
                        0
                    }
                }
            }

            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    // Find current position in shuffle order
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = currentShufflePos + 1
                    if (nextShufflePos < shuffleOrder.size) {
                        shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex // No next item
                    }
                } else {
                    (localCurrentMediaItemIndex + 1).coerceAtMost(playlist.size - 1)
                }
            }
        }

    private fun getPreviousMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> localCurrentMediaItemIndex
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    // Find current position in shuffle order
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos = if (currentShufflePos > 0) {
                        currentShufflePos - 1
                    } else {
                        shuffleOrder.size - 1
                    }
                    shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex > 0) {
                        localCurrentMediaItemIndex - 1
                    } else {
                        playlist.size - 1
                    }
                }
            }

            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    // Find current position in shuffle order
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos = currentShufflePos - 1
                    if (prevShufflePos >= 0) {
                        shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex // No previous item
                    }
                } else {
                    (localCurrentMediaItemIndex - 1).coerceAtLeast(0)
                }
            }
        }

    // ========== Playback Modes ==========

    override var shuffleModeEnabled: Boolean
        get() = internalShuffleModeEnabled
        set(value) {
            if (internalShuffleModeEnabled == value) return

            internalShuffleModeEnabled = value

            if (value) {
                // Enable shuffle - create shuffle order
                createShuffleOrder()
            } else {
                // Disable shuffle - clear shuffle order
                clearShuffleOrder()
            }

            // Notify listeners with the current order
            val mediaItemList = getShuffledMediaItemList()
            listeners.forEach { it.onShuffleModeEnabledChanged(value, mediaItemList) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            Logger.d(TAG, "Shuffle mode ${if (value) "enabled" else "disabled"}")
        }

    override var repeatMode: Int
        get() = internalRepeatMode
        set(value) {
            internalRepeatMode = value
        }

    override var playWhenReady: Boolean
        get() = internalPlayWhenReady
        set(value) {
            internalPlayWhenReady = value
            if (value) play() else pause()
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = GenericPlaybackParameters(internalPlaybackSpeed, internalPlaybackSpeed)
        set(value) {
            internalPlaybackSpeed = value.speed
            currentPlayer?.let { player ->
                // GStreamer playback rate control via seek event with rate
                try {
                    val currentPos = currentPosition * 1000000 // Convert to nanoseconds
                    val rate = value.speed.toDouble()

                    // Use seek with rate parameter for playback speed control
                    val seekFlags =
                        EnumSet.of(
                            SeekFlags.FLUSH,
                            SeekFlags.ACCURATE,
                        )

                    player.seek(
                        rate,
                        Format.TIME,
                        seekFlags,
                        SeekType.SET,
                        currentPos,
                        SeekType.NONE,
                        -1,
                    )
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to set playback speed: ${e.message}")
                }
            }
        }

    // ========== Audio Settings ==========

    override val audioSessionId: Int
        get() = 0 // GStreamer doesn't provide audio session ID in the same way

    override var volume: Float
        get() = internalVolume
        set(value) {
            Logger.w(TAG, "Setting volume to $value")
            internalVolume = value.coerceIn(0f, 1f)
            currentPlayer?.setVolume(internalVolume.toDouble())
            listeners.forEach { it.onVolumeChanged(internalVolume) }
        }

    override var skipSilenceEnabled: Boolean = false
    // GStreamer doesn't natively support skip silence, would need custom pipeline

    // ========== Listener Management ==========

    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    // ========== Release Resources ==========

    override fun release() {
        // Cancel all ongoing jobs
        currentLoadJob?.cancel()
        precacheJob?.cancel()
        positionUpdateJob?.cancel()

        coroutineScope.cancel()
        cleanupCurrentPlayerInternal()
        clearAllPrecacheInternal()
        listeners.clear()
    }

    // ========== Internal Methods ==========
    // NOTE: All internal methods MUST be called from coroutineScope unless otherwise noted

    /**
     * State transition helper - MUST be called within stateLock
     */
    private fun transitionToState(newState: InternalState) {
        if (internalState == newState) {
            Logger.d(TAG, "State transition ignored: already in $newState")
            return
        }

        val oldState = internalState
        internalState = newState

        Logger.d(TAG, "⚡ State transition: $oldState -> $newState (playWhenReady=$internalPlayWhenReady)")

        currentPlayer?.playerBin?.queryDuration(TimeUnit.MILLISECONDS)?.let {
            if (it > 0L) {
                Logger.d(TAG, "Current duration updated: $it ms")
                cachedDuration = it
            }
        }

        // Notify listeners
        when (newState) {
            InternalState.PAUSED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.IDLE -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.PREPARING -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_BUFFERING) }
                listeners.forEach { it.onIsLoadingChanged(true) }
            }

            InternalState.READY -> {
                if (internalPlayWhenReady && currentPlayer?.playerBin?.state != State.PAUSED) {
                    play()
                } else {
                    listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                    listeners.forEach { it.onIsPlayingChanged(false) }
                }
            }

            InternalState.PLAYING -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsLoadingChanged(false) }
                listeners.forEach { it.onIsPlayingChanged(true) }
            }

            InternalState.ENDED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_ENDED) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.ERROR -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
                listeners.forEach {
                    it.onPlayerError(
                        PlayerError(
                            errorCode = 403,
                            errorCodeName = "ERROR_UNKNOWN",
                            message = "Can not extract playable URL or playback error",
                        ),
                    )
                }
            }
        }
    }

    /**
     * Load and play track - MUST run on coroutineScope
     */
    private fun loadAndPlayTrackInternal(
        index: Int,
        startPositionMs: Long,
        shouldPlay: Boolean,
    ) {
        if (index !in playlist.indices) return

        val mediaItem = playlist[index]
        val videoId = mediaItem.mediaId

        // Cancel previous load
        currentLoadJob?.cancel()

        currentLoadJob =
            coroutineScope.launch {
                try {
                    transitionToState(InternalState.PREPARING)

                    // Notify media item transition
                    listeners.forEach {
                        it.onMediaItemTransition(
                            mediaItem,
                            PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                        )
                    }
                    // Use precached player if available
                    val cachedPlayer = precachedPlayers.remove(videoId)
                    val player =
                        if (cachedPlayer?.player != null) {
                            cachedPlayer.player
                        } else {
                            // Extract URL outside GStreamer thread
                            val uri = extractPlayableUrl(mediaItem)

                            if (uri == null || uri.second.isEmpty()) {
                                Logger.e(TAG, "Failed to extract playable URL for $videoId")
                                transitionToState(InternalState.ERROR)
                                return@launch
                            }
                            createMediaPlayerInternal(uri.first, uri.second)
                        }

                    // Cleanup current
                    cleanupCurrentPlayerInternal()

                    // Set as current
                    currentPlayer = player
                    setupPlayerListenersInternal(player.playerBin)

                    // Apply settings
                    player.setVolume(internalVolume.toDouble())

                    // Set to PAUSED to load pipeline
                    player.setState(State.PAUSED)

                    // Seek if needed
                    if (startPositionMs > 0) {
                        player.seek(startPositionMs, TimeUnit.MILLISECONDS)
                        cachedPosition = startPositionMs
                    }

                    // Auto-play if requested
                    if (shouldPlay) {
                        player.setState(State.READY)
                        transitionToState(InternalState.READY)
                        player.setState(State.PLAYING)
                        transitionToState(InternalState.PLAYING)
                    } else {
                        player.setState(State.READY)
                        transitionToState(InternalState.READY)
                    }

                    // Start position updates
                    startPositionUpdates()

                    // Trigger precaching
                    triggerPrecachingInternal()
                } catch (e: Exception) {
                    Logger.e(TAG, "Load track error: ${e.message}", e)
                    transitionToState(InternalState.ERROR)
                }
            }
    }

    /**
     * Create player for separate audio and video streams (DASH format)
     *
     * Strategy: TWO COMPLETELY INDEPENDENT PLAYBINS
     * - Audio PlayBin: plays audio stream (unmuted)
     * - Video PlayBin: plays video stream (muted, for display only)
     * - NO muxing, NO wrapping in pipeline
     * - Manual synchronization: explicitly control both players together
     * - Buffering: wait for BOTH to be ready before playing
     *
     * When only audio URI is provided:
     * - Uses a single PlayBin for audio-only playback
     */
    private suspend fun createMediaPlayerInternal(
        isVideo: Boolean,
        uri: String,
    ): GstreamerPlayer {
        // Case 1: Audio + Video (TWO SEPARATE PLAYBINS)
        if (isVideo) {
            val videoComponent = GstVideoComponent()

            val videoPlayBin =
                PlayBin("videoPlayer-${System.currentTimeMillis()}").apply {
                    setURI(URI(uri))
                    setVideoSink(videoComponent.element)
                }

            videoPlayBin.set("buffer-size", 5242880) // 5 MB
            videoPlayBin.set("buffer-duration", 5000) // 5 seconds

            return GstreamerPlayer(
                playerBin = videoPlayBin,
                videoComponent = videoComponent,
            )
        }

        // Case 2: Audio only (single PlayBin)
        Logger.d(TAG, "Creating audio-only player: $uri")
        val audioPlayer =
            PlayBin("audioPlayer-${System.currentTimeMillis()}").apply {
                setURI(URI(uri))
            }

        return GstreamerPlayer(
            playerBin = audioPlayer,
            videoComponent = null,
        )
    }

    /**
     * Setup bus listeners - MUST be called on gstreamerDispatcher
     */
    private fun setupPlayerListenersInternal(player: Bin) {
        // Clean up old listeners first
        cleanupBusListenersInternal()

        val bus = player.bus

        // Create new listeners
        val eosListener =
            Bus.EOS { _ ->
                player.state = State.PAUSED
                Logger.d(TAG, "End of stream reached")
                transitionToState(InternalState.ENDED)
                runBlocking { pause() }
                handleTrackEndInternal()
            }

        val durationListener =
            Bus.DURATION_CHANGED { _ ->
                currentPlayer?.let { player ->
                    if (duration > 0L) {
                        val dur = player.playerBin.queryDuration(TimeUnit.MILLISECONDS)
                        cachedDuration = if (dur != -1L) dur / 1000000 else cachedDuration
//                        Logger.d(TAG, "Duration updated: $cachedDuration ms")
                    }
                }
            }

        val errorListener =
            Bus.ERROR { _, code, message ->
                val error =
                    PlayerError(
                        errorCode = PlayerConstants.ERROR_CODE_TIMEOUT,
                        errorCodeName = "GSTREAMER_ERROR",
                        message = message ?: "Playback error (code: $code)",
                    )
                Logger.e(TAG, "Playback error: $message")
                listeners.forEach { it.onPlayerError(error) }
                transitionToState(InternalState.ERROR)
            }

        val warningListener =
            Bus.WARNING { _, code, message ->
                Logger.w(TAG, "Warning (code: $code): $message")
            }

        val stateChangedListener =
            Bus.STATE_CHANGED { _, oldState, newState, pending ->
                // Filter out intermediate state transitions to prevent flickering
                // Only react to meaningful PAUSED <-> PLAYING transitions
                if (oldState == newState) return@STATE_CHANGED

                // Ignore transitions to/from READY state (intermediate)
                if ((newState == State.READY || oldState == State.READY) && !internalState.isInReadyState()) {
                    transitionToState(InternalState.READY)
                    return@STATE_CHANGED
                }

                // Debounce rapid state changes
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastStateChangeTime < stateChangeDebounceMs) {
                    Logger.d(TAG, "State change debounced: $oldState -> $newState")
                    return@STATE_CHANGED
                }
                lastStateChangeTime = currentTime

                Logger.d(TAG, "State changed: $oldState -> $newState (internal: $internalState)")

                when (newState) {
                    State.PLAYING -> {
                        if (internalState != InternalState.PLAYING) {
                            transitionToState(InternalState.PLAYING)
                            notifyEqualizerIntent(true)
                        }
                    }

                    State.PAUSED -> {
                        // Only transition to READY if we were actually playing
                        if (internalState == InternalState.PLAYING) {
                            transitionToState(InternalState.READY)
                            notifyEqualizerIntent(false)
                        }
                    }

                    State.NULL -> {
                        notifyEqualizerIntent(false)
                        transitionToState(InternalState.IDLE)
                    }

                    else -> {
                    }
                }
            }

        val bufferingListener =
            Bus.BUFFERING { _, percent ->

            }

        val asyncDoneListener =
            Bus.ASYNC_DONE { _ ->
                // Pipeline is ready, only auto-play if:
                // 1. We're in READY state (not already playing)
                // 2. playWhenReady is true
                // 3. We're not already transitioning
                if (internalState == InternalState.READY && internalPlayWhenReady) {
                    Logger.d(TAG, "ASYNC_DONE: Auto-starting playback")
                    currentPlayer?.setState(State.PLAYING)
                }
            }

        // Connect listeners
        bus.connect(eosListener)
        bus.connect(errorListener)
        bus.connect(warningListener)
        bus.connect(stateChangedListener)
        bus.connect(bufferingListener)
        bus.connect(asyncDoneListener)
        bus.connect(durationListener)

        // Store references
        activeBusListeners =
            BusListeners(
                eos = eosListener,
                durationChanged = durationListener,
                error = errorListener,
                warning = warningListener,
                stateChanged = stateChangedListener,
                buffering = bufferingListener,
                asyncDone = asyncDoneListener,
            )
    }

    /**
     * Clean up bus listeners
     */
    private fun cleanupBusListenersInternal() {
        activeBusListeners?.let { listeners ->
            currentPlayer?.playerBin?.bus?.let { bus ->
                try {
                    bus.disconnect(Bus.EOS::class.java, listeners.eos)
                    bus.disconnect(Bus.DURATION_CHANGED::class.java, listeners.durationChanged)
                    bus.disconnect(Bus.ERROR::class.java, listeners.error)
                    bus.disconnect(Bus.WARNING::class.java, listeners.warning)
                    bus.disconnect(Bus.STATE_CHANGED::class.java, listeners.stateChanged)
                    bus.disconnect(Bus.BUFFERING::class.java, listeners.buffering)
                    bus.disconnect(Bus.ASYNC_DONE::class.java, listeners.asyncDone)
                } catch (e: Exception) {
                    Logger.w(TAG, "Error disconnecting listeners: ${e.message}")
                }
            }
        }
        activeBusListeners = null
    }

    /**
     * Cleanup a player instance
     */
    private fun cleanupPlayerInternal(player: GstreamerPlayer) {
        try {
            player.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error cleaning up player: ${e.message}")
        }
    }

    /**
     * Cleanup current player
     */
    private fun cleanupCurrentPlayerInternal() {
        stopPositionUpdates()
        cleanupBusListenersInternal()
        currentPlayer?.let { cleanupPlayerInternal(it) }
        currentPlayer = null
    }

    /**
     * Handle track end
     */
    private fun handleTrackEndInternal() {
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                seekTo(localCurrentMediaItemIndex, 0)
            }

            PlayerConstants.REPEAT_MODE_ALL -> {
                if (hasNextMediaItem()) {
                    seekToNext()
                }
            }

            else -> {
                if (localCurrentMediaItemIndex < playlist.size - 1) {
                    seekToNext()
                } else {
                    notifyEqualizerIntent(false)
                }
            }
        }
    }

    /**
     * Start position updates (periodic background task)
     */
    private fun startPositionUpdates() {
        stopPositionUpdates()

        positionUpdateJob =
            coroutineScope.launch {
                while (isActive && currentPlayer != null) {
                    try {
                        // Skip position queries during transitions to prevent flicker
                        currentPlayer?.playerBin?.let { player ->
                            // Only query position when in PLAYING or READY states
                            if (internalState == InternalState.PLAYING ||
                                internalState == InternalState.READY
                            ) {
                                val pos = player.queryPosition(TimeUnit.MILLISECONDS)
                                val dur = player.queryDuration(TimeUnit.MILLISECONDS)

                                if (pos > 0) cachedPosition = pos
                                if (dur > 0) cachedDuration = dur
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore query errors - don't log to avoid spam
                    }

                    delay(200) // Update every 200ms
                }
            }
    }

    /**
     * Stop position updates
     */
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    /**
     * Trigger precaching - with proper cancellation
     */
    private fun triggerPrecachingInternal() {
        if (!precacheEnabled || playlist.isEmpty()) return

        cancelPrecaching()
        Logger.d(TAG, "Trigger precache")
        precacheJob =
            coroutineScope.launch {
                try {
                    val indicesToPrecache = mutableListOf<Int>()

                    val index = localCurrentMediaItemIndex
                    for (i in 1..maxPrecacheCount) {
                        val nextIndex =
                            when (internalRepeatMode) {
                                PlayerConstants.REPEAT_MODE_ALL -> (index + i) % playlist.size
                                else -> {
                                    val next = index + i
                                    if (next < playlist.size) next else break
                                }
                            }

                        if (nextIndex != localCurrentMediaItemIndex &&
                            !precachedPlayers.containsKey(playlist.getOrNull(nextIndex)?.mediaId)
                        ) {
                            indicesToPrecache.add(nextIndex)
                        }
                    }

                    for (idx in indicesToPrecache) {
                        if (!isActive) break

                        val mediaItem = playlist.getOrNull(idx) ?: continue

                        val uri =
                            withContext(coroutineScope.coroutineContext) {
                                extractPlayableUrl(mediaItem)
                            }

                        if (uri != null && uri.second.isNotEmpty()) {
                            try {
                                val player = createMediaPlayerInternal(uri.first, uri.second)
                                player.setState(State.READY)
                                precachedPlayers[mediaItem.mediaId] = PrecachedPlayer(player, mediaItem, uri.second)
                                Logger.d(TAG, "Precached player for index $idx")
                            } catch (e: Exception) {
                                Logger.e(TAG, "Precaching error for $idx: ${e.message}")
                            }
                        }

                        delay(100)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.e(TAG, "Precaching error: ${e.message}")
                    }
                }
            }
    }

    /**
     * Cancel precaching
     */
    private fun cancelPrecaching() {
        precacheJob?.cancel()
        precacheJob = null
    }

    /**
     * Clear precache except current
     */
    private fun clearPrecacheExceptCurrentInternal() {
        Logger.d(TAG, "Clearing precache")
        precachedPlayers.entries.removeIf { (videoId, cached) ->
            if (videoId != currentMediaItem?.mediaId) {
                cleanupPlayerInternal(cached.player)
                true
            } else {
                false
            }
        }
    }

    /**
     * Clear all precache
     */
    private fun clearAllPrecacheInternal() {
        Logger.d(TAG, "Clearing all precache")
        precachedPlayers.values.forEach { cleanupPlayerInternal(it.player) }
        precachedPlayers.clear()
    }

    /**
     * Notify equalizer intent
     */
    private fun notifyEqualizerIntent(shouldOpen: Boolean) {
        listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(shouldOpen) }
    }

    /**
     * Create shuffle order for current playlist
     * Keeps the current track at its position and shuffles the rest
     */
    private fun createShuffleOrder() {
        if (playlist.isEmpty()) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        // Create list of all indices
        val indices = playlist.indices.toMutableList()

        // If we have a current track, keep it at current position
        val currentIndex = localCurrentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        // Shuffle the remaining indices
        indices.shuffle()

        // If we have a current track, insert it at the beginning
        if (currentIndex in playlist.indices) {
            indices.add(0, currentIndex)
        }

        // Store the shuffle order
        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        // Create reverse mapping (original index -> shuffled position)
        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    /**
     * Clear shuffle order
     */
    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
        Logger.d(TAG, "Cleared shuffle order")
    }

    /**
     * Insert item into shuffle order at specific position
     * Used for "play next" functionality when shuffle is enabled
     *
     * @param insertedOriginalIndex The index in the original playlist where item was inserted
     * @param afterShufflePos The shuffle position after which to insert (typically current song's position)
     */
    private fun insertIntoShuffleOrder(insertedOriginalIndex: Int, afterShufflePos: Int) {
        if (playlist.isEmpty() || insertedOriginalIndex !in playlist.indices) {
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
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }

        Logger.d(TAG, "Inserted index $insertedOriginalIndex into shuffle at position $insertPos (after shuffle pos $afterShufflePos)")
    }

    /**
     * Get shuffled list of media items
     */
    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        if (!internalShuffleModeEnabled || shuffleOrder.isEmpty()) {
            return playlist.toList()
        }
        return shuffleOrder.mapNotNull { playlist.getOrNull(it) }
    }

    /**
     * Notify timeline changed with current order (shuffled or not)
     */
    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        listeners.forEach { it.onTimelineChanged(list, reason) }
    }

    /**
     * Enable or disable precaching
     */
    fun setPrecachingEnabled(enabled: Boolean) {
        precacheEnabled = enabled
        if (!enabled) {
            clearPrecacheExceptCurrentInternal()
        } else {
            triggerPrecachingInternal()
        }
    }

    /**
     * Set maximum number of tracks to precache
     */
    fun setMaxPrecacheCount(count: Int) {
        // maxPrecacheCount = count.coerceIn(0, 5)
        // Note: maxPrecacheCount is now val, but you can make it var if needed
    }

    /**
     * Extract playable URL for a video ID
     * isVideo: Boolean -> uri: String
     */
    private suspend fun extractPlayableUrl(mediaItem: GenericMediaItem): Pair<Boolean, String>? {
        Logger.w(TAG, "Extracting playable URL for ${mediaItem.mediaId}")
        val shouldFindVideo =
            mediaItem.isVideo() &&
                dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE
        val videoId = mediaItem.mediaId
        if (File(getDownloadPath()).listFiles().takeIf { it != null }?.any {
                it.name.contains(videoId)
            } ?: false
        ) {
            val files =
                File(getDownloadPath()).listFiles().filter {
                    it.name.contains(videoId)
                }
            val audioFile = files.firstOrNull { !it.name.contains(MERGING_DATA_TYPE.VIDEO) }
            return false to audioFile?.toURI().toString()
        } else {
            streamRepository.getNewFormat(videoId).lastOrNull()?.let {
                val audioUrl = it.audioUrl
                val videoUrl = it.videoUrl
                if (!shouldFindVideo && !audioUrl.isNullOrEmpty()) {
                    val is403Url = streamRepository.is403Url(audioUrl).firstOrNull() != false
                    Logger.d("Stream", "is 403 $is403Url")
                    if (!is403Url) {
                        Logger.w("Stream", "Audio from format")
                        return false to audioUrl
                    }
                } else if (shouldFindVideo && !videoUrl.isNullOrEmpty()) {
                    val is403Url = streamRepository.is403Url(videoUrl).firstOrNull() != false
                    Logger.d("Stream", "is 403 $is403Url")
                    if (!is403Url) {
                        Logger.w("Stream", "Video from format")
                        return true to videoUrl
                    }
                }
            }

            if (shouldFindVideo) {
                val videoUrl =
                    streamRepository
                        .getStream(
                            dataStoreManager,
                            videoId,
                            isDownloading = false,
                            isVideo = true,
                            muxed = true,
                        ).lastOrNull()
                        ?.let {
                            Logger.d(TAG, "Stream Video $it")
                            it
                        }
                return true to (videoUrl ?: return null)
            } else {
                val audioUrl =
                    streamRepository
                        .getStream(
                            dataStoreManager,
                            videoId,
                            isDownloading = false,
                            isVideo = false,
                        ).lastOrNull()
                        ?.let {
                            Logger.d(TAG, "Stream Audio $it")
                            it
                        }
                return true to (audioUrl ?: return null)
            }
        }
    }

    private fun configurePaths() {
        if (Platform.isWindows()) {
            val gstPath = System.getProperty("gstreamer.path", findWindowsLocation())
            if (!gstPath!!.isEmpty()) {
                val systemPath = System.getenv("PATH")
                if (systemPath == null || systemPath.trim { it <= ' ' }.isEmpty()) {
                    Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath)
                } else {
                    Kernel32.INSTANCE.SetEnvironmentVariable(
                        "PATH",
                        (
                            gstPath +
                                File.pathSeparator + systemPath
                        ),
                    )
                }
            }
        } else if (Platform.isMac()) {
            val gstPath =
                System.getProperty(
                    "gstreamer.path",
                    "/Library/Frameworks/GStreamer.framework/Libraries/",
                )
            if (!gstPath!!.isEmpty()) {
                val jnaPath = System.getProperty("jna.library.path", "").trim { it <= ' ' }
                if (jnaPath.isEmpty()) {
                    System.setProperty("jna.library.path", gstPath)
                } else {
                    System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath)
                }
            }
        }
    }

    /**
     * Query over a stream of possible environment variables for GStreamer
     * location, filtering on the first non-null result, and adding \bin\ to the
     * value.
     *
     * @return location or empty string
     */
    private fun findWindowsLocation(): String? {
        if (Platform.is64Bit()) {
            return Stream
                .of<String?>(
                    "GSTREAMER_1_0_ROOT_MSVC_X86_64",
                    "GSTREAMER_1_0_ROOT_MINGW_X86_64",
                    "GSTREAMER_1_0_ROOT_X86_64",
                ).map<String?> { name: String? -> System.getenv(name) }
                .filter { p: String? -> p != null }
                .map<String?> { p: String? -> if (p!!.endsWith("\\")) p + "bin\\" else p + "\\bin\\" }
                .findFirst()
                .orElse("")
        } else {
            return ""
        }
    }
}

data class GstreamerPlayer(
    val playerBin: Pipeline,
    val videoComponent: GstVideoComponent? = null,
) {
    companion object {
        private const val TAG = "GstreamerPlayer"
    }

    fun setState(state: State) {
        playerBin.state = state
    }

    fun seek(
        position: Long,
        unit: TimeUnit,
    ): Boolean = playerBin.seek(1.0, Format.TIME, EnumSet.of(SeekFlags.FLUSH,
        SeekFlags.ACCURATE), SeekType.SET, TimeUnit.NANOSECONDS.convert(position,
        unit), SeekType.NONE, -1)

    fun seek(
        rate: Double,
        format: Format,
        flags: EnumSet<SeekFlags>,
        startType: SeekType,
        start: Long,
        stopType: SeekType,
        stop: Long,
    ): Boolean = playerBin.seek(rate, format, flags, startType, start, stopType, stop)

    fun pause() {
        playerBin.state = State.PAUSED
    }

    fun stop() {
        playerBin.stop()
    }

    fun setVolume(volume: Double) {
        // Set volume on the playerBin (which is the audio PlayBin in dual-stream case)
        (playerBin as? PlayBin)?.volume = volume
    }

    fun release() {
        try {
            stop()
            playerBin.state = State.NULL
            playerBin.dispose()
        } catch (e: Exception) {
            Logger.w(TAG, "Error releasing player: ${e.message}")
        }
    }
}

