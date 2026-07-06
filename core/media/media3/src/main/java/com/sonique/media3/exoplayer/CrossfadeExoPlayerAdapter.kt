package com.sonique.media3.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.domain.data.player.GenericPlaybackParameters
import com.sonique.domain.data.player.PlayerConstants
import com.sonique.domain.data.player.PlayerError
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.mediaservice.player.MediaPlayerInterface
import com.sonique.domain.mediaservice.player.MediaPlayerListener
import com.sonique.domain.repository.StreamRepository
import com.sonique.logger.Logger
import com.sonique.media3.audio.BiquadFilter
import com.sonique.media3.audio.CrossfadeFilterAudioProcessor
import com.sonique.media3.exoplayer.CrossfadeExoPlayerAdapter.Companion.SPEED_PITCH_STEP
import com.sonique.media3.service.mediasourcefactory.MergingMediaSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

private const val TAG = "CrossfadeExoPlayerAdapter"

/**
 * ExoPlayer implementation of [MediaPlayerInterface] with crossfade support.
 *
 * Architecture mirrors [com.simpmusic.media_jvm.GstreamerPlayerAdapter]:
 * - Internal playlist management (not ExoPlayer's playlist)
 * - Multi-player instance model: each track gets its own ExoPlayer
 * - Precaching system for smooth transitions
 * - Crossfade with listener swap pattern
 * - [DelegatingForwardingPlayer] for MediaSession integration
 *
 * Key difference from GstreamerPlayerAdapter:
 * - Uses [MergingMediaSourceFactory] + ResolvingDataSource for URL resolution
 *   (instead of manually extracting URLs via StreamRepository)
 * - Each ExoPlayer gets a single [MediaItem] and auto-resolves the stream URL
 */
@SuppressLint("UnsafeOptInUsageError")
@OptIn(UnstableApi::class)
internal class CrossfadeExoPlayerAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val dataStoreManager: DataStoreManager,
    private val mediaSourceFactory: MergingMediaSourceFactory,
    private val audioAttributes: AudioAttributes,
    private val streamRepository: StreamRepository,
) : MediaPlayerInterface {
    // ========== Internal State Enum (same as GstreamerPlayerAdapter) ==========

    private enum class InternalState {
        IDLE, // No media loaded
        PREPARING, // Loading media
        READY, // Ready to play/paused
        PLAYING, // Currently playing
        PAUSED,
        ENDED, // Playback ended
        ERROR, // Error state
    }

    private fun InternalState.isInReadyState(): Boolean = this == InternalState.READY || this == InternalState.PLAYING || this == InternalState.PAUSED

    // ========== Crossfade Settings (loaded from DataStore) ==========

    init {
        coroutineScope.launch {
            dataStoreManager.crossfadeEnabled.collect { enabled ->
                crossfadeEnabled = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "Crossfade enabled: $crossfadeEnabled")
            }
        }
        coroutineScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                crossfadeDurationMs = duration
                Logger.d(TAG, "Crossfade duration: $crossfadeDurationMs ms")
            }
        }
        coroutineScope.launch {
            dataStoreManager.crossfadeDjMode.collect { enabled ->
                djCrossfadeEnabled = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "DJ crossfade mode: $djCrossfadeEnabled")
            }
        }
    }

    // ========== State Management ==========

    private val listeners = mutableListOf<MediaPlayerListener>()

    @Volatile
    private var currentPlayer: ExoPlayer? = null

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

    @Volatile
    private var internalPlaybackPitch = 1.0f

    @Volatile
    private var internalSkipSilence = false

    // Position tracking - updated periodically, not on every query
    @Volatile
    private var cachedPosition = 0L

    @Volatile
    private var cachedDuration = 0L

    @Volatile
    private var cachedBufferedPosition = 0L

    @Volatile
    private var cachedIsLoading = false

    private var positionUpdateJob: Job? = null

    // Active Player.Listener (equivalent to BusListeners in GstreamerPlayerAdapter)
    // Only ONE listener instance, attached to ONE ExoPlayer at a time.
    // Swapped between players during crossfade.
    private var activePlayerListener: Player.Listener? = null

    // ========== Audio Focus (manual, session-scoped) â€” #2155 ==========
    // The multi-player swap model means audio focus must NOT be tied to any single
    // ExoPlayer: releasing the outgoing player would abandon focus, and the incoming
    // player (built with handleAudioFocus=false) never re-requests it â€” the root cause
    // of "music stops between tracks" (see androidx/media#2100). Instead we hold one
    // app-level AudioFocusRequest at the adapter level so focus survives every swap.

    private val duckVolumeFactor = 0.2f

    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    @Volatile
    private var hasAudioFocus = false

    /** True when focus was lost transiently so playback should auto-resume on regain. */
    @Volatile
    private var resumeOnFocusGain = false

    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Don't fight the crossfade ramp; while crossfading it owns the volume.
                    if (!isCrossfading) currentPlayer?.volume = internalVolume
                    if (resumeOnFocusGain) {
                        resumeOnFocusGain = false
                        play()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    // Permanent loss (another app took over): pause and stop tracking focus.
                    resumeOnFocusGain = false
                    hasAudioFocus = false
                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Temporary loss (e.g. an incoming call): pause and remember to resume.
                    resumeOnFocusGain = internalState == InternalState.PLAYING
                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Lower the volume instead of pausing (e.g. a navigation prompt).
                    // Skip during crossfade â€” the ramp owns volume and would override this.
                    if (!isCrossfading) currentPlayer?.volume = internalVolume * duckVolumeFactor
                }
            }
        }

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes
                    .Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setOnAudioFocusChangeListener(audioFocusListener)
            .setWillPauseWhenDucked(false)
            .build()
    }

    /** Request app-level audio focus once; idempotent while focus is held. */
    private fun requestAudioFocusInternal(): Boolean {
        val am = audioManager ?: return true
        if (hasAudioFocus) return true
        val granted = am.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasAudioFocus = granted
        Logger.d(TAG, "requestAudioFocus -> granted=$granted")
        return granted
    }

    private fun abandonAudioFocusInternal() {
        val am = audioManager ?: return
        if (!hasAudioFocus) return
        am.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
        resumeOnFocusGain = false
        Logger.d(TAG, "abandonAudioFocus")
    }

    // ========== Precaching System ==========

    private data class PrecachedPlayer(
        val player: ExoPlayer,
        val mediaItem: GenericMediaItem,
        val filter: CrossfadeFilterAudioProcessor? = null,
    )

    // VideoId -> PrecachedPlayer
    private val precachedPlayers = ConcurrentHashMap<String, PrecachedPlayer>()
    private var precacheEnabled = true
    private val maxPrecacheCount = 2
    private var precacheJob: Job? = null

    // ========== Crossfade System ==========

    @Volatile
    private var crossfadeEnabled = false

    @Volatile
    private var crossfadeDurationMs = 5000

    @Volatile
    private var djCrossfadeEnabled = true

    @Volatile
    private var secondaryPlayer: ExoPlayer? = null

    @Volatile
    private var crossfadeJob: Job? = null

    @Volatile
    private var isCrossfading = false

    // Per-player filter references for DJ-style crossfade
    @Volatile
    private var currentPlayerFilter: CrossfadeFilterAudioProcessor? = null

    @Volatile
    private var secondaryPlayerFilter: CrossfadeFilterAudioProcessor? = null

    /** Index we're crossfading from; used when cancelling to revert localCurrentMediaItemIndex. */
    @Volatile
    private var crossfadeFromIndex = -1

    // ========== Retry on Source Error ==========
    // Track retry attempts per media item to avoid infinite retry loops
    private var retryCount = 0
    private var retryVideoId: String? = null
    private val maxRetryCount = 2

    // ========== AutoMix Metadata Cache ==========
    // videoId -> audio analysis data from Tidal (populated externally when 320kbps stream is fetched)
    private val audioMetaCache = ConcurrentHashMap<String, SongAudioMeta>()

    /**
     * Update crossfade state and notify listeners when it changes.
     */
    private fun setCrossfading(value: Boolean) {
        if (isCrossfading != value) {
            isCrossfading = value
            listeners.forEach { it.onCrossfadeStateChanged(value) }
        }
    }

    // ========== Playlist Management ==========

    private val playlist = mutableListOf<GenericMediaItem>()
    private var localCurrentMediaItemIndex = -1

    // Shuffle management
    private var shuffleIndices = mutableListOf<Int>()
    private var shuffleOrder = mutableListOf<Int>()

    // Loading management
    private var currentLoadJob: Job? = null

    // ========== ForwardingPlayer for MediaSession ==========

    // Create an initial idle ExoPlayer for MediaSession to hold
    private val initialPlayerWithFilter = createExoPlayerInstance()

    /**
     * Stable [Player] reference for MediaSession.
     * Delegates all calls to the currently active [ExoPlayer] instance.
     * Updated via [DelegatingForwardingPlayer.swapDelegate] when the active player changes.
     */
    val forwardingPlayer: DelegatingForwardingPlayer = DelegatingForwardingPlayer(initialPlayerWithFilter.player)

    init {
        currentPlayer = initialPlayerWithFilter.player
        currentPlayerFilter = initialPlayerWithFilter.filter

        // Wire up playlist navigation so ForwardingPlayer (and thus MediaSession)
        // can see the full playlist state instead of the single-item ExoPlayer state.
        // Only navigation commands are overridden â€” NOT getMediaItemCount/getCurrentMediaItemIndex
        // which must stay consistent with ExoPlayer's internal Timeline to avoid crashes.
        forwardingPlayer.playlistNavigationProvider =
            object : DelegatingForwardingPlayer.PlaylistNavigationProvider {
                override fun hasNextMediaItem(): Boolean = this@CrossfadeExoPlayerAdapter.hasNextMediaItem()

                override fun hasPreviousMediaItem(): Boolean = this@CrossfadeExoPlayerAdapter.hasPreviousMediaItem()

                override fun seekToNext(): Unit = this@CrossfadeExoPlayerAdapter.seekToNext()

                override fun seekToPrevious(): Unit = this@CrossfadeExoPlayerAdapter.seekToPrevious()

                override fun seekToPreviousMediaItem(): Unit = this@CrossfadeExoPlayerAdapter.seekToPreviousMediaItem()
            }
    }

    // ========== ExoPlayer Instance Factory ==========

    /**
     * Result of creating an ExoPlayer instance, bundled with its per-player crossfade filter.
     */
    private data class PlayerWithFilter(
        val player: ExoPlayer,
        val filter: CrossfadeFilterAudioProcessor,
    )

    /**
     * Create a new ExoPlayer instance with a per-player [CrossfadeFilterAudioProcessor].
     *
     * Each player gets its own filter instance so the fade-out player can have
     * an independent low-pass filter while the fade-in player has a high-pass filter.
     *
     * Audio focus is NOT handled per-player: it is managed once at the adapter level
     * (see the Audio Focus section) so it survives every player swap (#2155).
     */
    private fun createExoPlayerInstance(): PlayerWithFilter {
        val crossfadeFilter = CrossfadeFilterAudioProcessor()

        val perPlayerRenderers =
            object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink =
                    DefaultAudioSink
                        .Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                        .setAudioProcessorChain(
                            DefaultAudioSink.DefaultAudioProcessorChain(
                                arrayOf(crossfadeFilter),
                                SilenceSkippingAudioProcessor(
                                    2_000_000,
                                    (20_000 / 2_000_000).toFloat(),
                                    2_000_000,
                                    0,
                                    256,
                                ),
                                SonicAudioProcessor(),
                            ),
                        ).build()
            }

        val player =
            ExoPlayer
                .Builder(context)
                .setAudioAttributes(audioAttributes, false)
                .setLoadControl(
                    DefaultLoadControl
                        .Builder()
                        .setBufferDurationsMs(
                            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 4,
                            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 4,
                            0,
                            0,
                        ).build(),
                ).setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setSeekForwardIncrementMs(5000)
                .setSeekBackIncrementMs(5000)
                .setMediaSourceFactory(mediaSourceFactory)
                .setRenderersFactory(perPlayerRenderers)
                .build()

        return PlayerWithFilter(player, crossfadeFilter)
    }

    // ========== Playback Control ==========

    override fun play() {
        Logger.d(TAG, "play() called (state: $internalState, playWhenReady: $internalPlayWhenReady)")
        coroutineScope.launch {
            when (internalState) {
                InternalState.READY, InternalState.ENDED, InternalState.PAUSED -> {
                    currentPlayer?.let { player ->
                        requestAudioFocusInternal()
                        player.play()
                        transitionToState(InternalState.PLAYING)
                        internalPlayWhenReady = true
                    } ?: Logger.w(TAG, "Play called but currentPlayer is null")
                }

                InternalState.PREPARING -> {
                    internalPlayWhenReady = true
                    Logger.d(TAG, "Play: During PREPARING - will auto-play when ready")
                }

                InternalState.PLAYING -> {
                    internalPlayWhenReady = true
                    cachedIsLoading = false
                }

                else -> {
                    Logger.w(TAG, "Play: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun pause() {
        Logger.d(TAG, "pause() called (state: $internalState, playWhenReady: $internalPlayWhenReady)")
        coroutineScope.launch {
            forwardingPlayer.suppressPlaybackEnded = false
            // Cancel any ongoing crossfade by committing the incoming track (A+1) as current.
            // Direction 1: pausing during a crossfade stays on A+1 (the track the UI already
            // shows) and freezes it in place via the when(internalState) block below â€” it does
            // NOT jump back to A.
            if (isCrossfading) {
                Logger.d(TAG, "Pause: committing incoming (A+1) and pausing in place")
                commitIncomingAsCurrentInternal()
            }

            when (internalState) {
                InternalState.PLAYING, InternalState.READY -> {
                    currentPlayer?.let { player ->
                        player.pause()
                        transitionToState(InternalState.PAUSED)
                        internalPlayWhenReady = false
                    }
                }

                InternalState.PREPARING -> {
                    internalPlayWhenReady = false
                }

                else -> {
                    Logger.w(TAG, "Pause: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            forwardingPlayer.suppressPlaybackEnded = false
            currentPlayer?.let { player ->
                Logger.d(TAG, "Stop called")
                player.stop()
                transitionToState(InternalState.IDLE)
                stopPositionUpdates()
                abandonAudioFocusInternal()
                notifyEqualizerIntent(false)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        currentPlayer?.let { player ->
            try {
                player.seekTo(positionMs)
                cachedPosition = positionMs
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

            // Cancel any ongoing crossfade
            if (isCrossfading) {
                Logger.d(TAG, "seekTo: Cancelling crossfade")
                crossfadeJob?.cancel()
                crossfadeJob = null
                currentPlayerFilter?.enabled = false
                secondaryPlayerFilter?.enabled = false
                secondaryPlayer?.release()
                secondaryPlayer = null
                secondaryPlayerFilter = null
                setCrossfading(false)
            }

            // Cancel any ongoing load
            currentLoadJob?.cancel()

            // Load the new track
            localCurrentMediaItemIndex = mediaItemIndex
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
        coroutineScope.launch {
            // During crossfade Aâ†’A+1, "next" commits A+1 as current (Direction 1: the UI already
            // shows A+1) and then advances to A+2. Outside crossfade it advances normally.
            val wasCrossfading = isCrossfading
            if (wasCrossfading) {
                Logger.d(TAG, "seekToNext: committing incoming (A+1), then advancing to A+2")
                commitIncomingAsCurrentInternal()
            }
            if (hasNextMediaItem()) {
                seekTo(getNextMediaItemIndex(), 0)
            } else if (wasCrossfading) {
                // A+1 was the last track â€” stay on it (already promoted), just refresh metadata.
                forwardingPlayer.notifyMediaItemChanged()
            }
        }
    }

    override fun seekToPrevious() {
        coroutineScope.launch {
            // During a crossfade, commit the incoming track (A+1) as current FIRST, so the
            // 3-second rule below evaluates against A+1's position/index (Direction 1).
            if (isCrossfading) {
                Logger.d(TAG, "seekToPrevious: committing incoming (A+1) first")
                commitIncomingAsCurrentInternal()
            }

            // Standard music player behavior:
            // - Position > 3s  â†’ seek to start of current track
            // - Position <= 3s â†’ go to previous track
            val positionThresholdMs = 3000L
            if (cachedPosition > positionThresholdMs) {
                Logger.d(TAG, "seekToPrevious: pos=${cachedPosition}ms > ${positionThresholdMs}ms â€” seeking to start")
                currentPlayer?.seekTo(0)
                cachedPosition = 0
            } else if (hasPreviousMediaItem()) {
                Logger.d(TAG, "seekToPrevious: pos=${cachedPosition}ms <= ${positionThresholdMs}ms â€” going to previous track")
                val prevIndex = getPreviousMediaItemIndex()
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPrevious: No previous item, seeking to start")
                currentPlayer?.seekTo(0)
                cachedPosition = 0
            }
        }
    }

    override fun seekToPreviousMediaItem() {
        coroutineScope.launch {
            // Mirror seekToPrevious(): commit the incoming track (A+1) as current first.
            if (isCrossfading) {
                Logger.d(TAG, "seekToPreviousMediaItem: committing incoming (A+1) first")
                commitIncomingAsCurrentInternal()
            }

            // Always advance to the previous track regardless of `cachedPosition` â€”
            // skips the 3-second "seek to start" rule used by seekToPrevious().
            if (hasPreviousMediaItem()) {
                val prevIndex = getPreviousMediaItemIndex()
                Logger.d(TAG, "seekToPreviousMediaItem: jumping to previous index=$prevIndex")
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPreviousMediaItem: No previous item â€” no-op")
            }
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

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            loadAndPlayTrackInternal(0, 0, internalPlayWhenReady)
        }
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)

        if (internalShuffleModeEnabled) {
            createShuffleOrder()
        }

        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

        if (playlist.size - 1 - currentMediaItemIndex <= maxPrecacheCount) {
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
            val currentIndexBeforeInsert = localCurrentMediaItemIndex

            playlist.add(index, mediaItem)

            // Adjust current index if needed
            if (index <= localCurrentMediaItemIndex) {
                localCurrentMediaItemIndex++
            }

            // Update shuffle order if enabled
            if (internalShuffleModeEnabled) {
                if (currentIndexBeforeInsert >= 0 && index == currentIndexBeforeInsert + 1) {
                    val currentShufflePos = shuffleIndices.getOrNull(currentIndexBeforeInsert) ?: 0
                    insertIntoShuffleOrder(index, currentShufflePos)
                } else {
                    createShuffleOrder()
                }
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index - 1 - currentMediaItemIndex <= maxPrecacheCount) {
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
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

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
                    localCurrentMediaItemIndex == fromIndex -> {
                        toIndex
                    }
                    fromIndex < localCurrentMediaItemIndex && toIndex >= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex - 1
                    }
                    fromIndex > localCurrentMediaItemIndex && toIndex <= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex + 1
                    }
                    else -> {
                        localCurrentMediaItemIndex
                    }
                }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            clearPrecacheExceptCurrentInternal()
            triggerPrecachingInternal()
        }
    }

    override fun clearMediaItems() {
        coroutineScope.launch {
            playlist.clear()
            localCurrentMediaItemIndex = -1
            clearShuffleOrder()
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

            precachedPlayers.remove(mediaItem.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index == localCurrentMediaItemIndex) {
                loadAndPlayTrackInternal(index, 0, internalPlayWhenReady)
            } else {
                triggerPrecachingInternal()
            }
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> =
        if (internalShuffleModeEnabled) {
            shuffleOrder.mapNotNull { shuffledIndex -> playlist.getOrNull(shuffledIndex) }
        } else {
            playlist.toList()
        }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int =
        if (internalShuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }

    // ========== Playback State Properties ==========

    override val isPlaying: Boolean
        get() = internalState == InternalState.PLAYING

    override val currentPosition: Long
        get() = cachedPosition

    override val duration: Long
        get() = currentPlayer?.duration ?: cachedDuration

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
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
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
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = currentShufflePos + 1
                    if (nextShufflePos < shuffleOrder.size) {
                        shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
                    }
                } else {
                    (localCurrentMediaItemIndex + 1).coerceAtMost(playlist.size - 1)
                }
            }
        }

    private fun getPreviousMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos =
                        if (currentShufflePos > 0) {
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
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos = currentShufflePos - 1
                    if (prevShufflePos >= 0) {
                        shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
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
                createShuffleOrder()
            } else {
                clearShuffleOrder()
            }

            val mediaItemList = getShuffledMediaItemList()
            listeners.forEach { it.onShuffleModeEnabledChanged(value, mediaItemList) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            Logger.d(TAG, "Shuffle mode ${if (value) "enabled" else "disabled"}")
        }

    override var repeatMode: Int
        get() = internalRepeatMode
        set(value) {
            if (internalRepeatMode == value) return
            internalRepeatMode = value
            listeners.forEach { it.onRepeatModeChanged(value) }
        }

    override var playWhenReady: Boolean
        get() = internalPlayWhenReady
        set(value) {
            internalPlayWhenReady = value
            if (value) play() else pause()
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = GenericPlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
        set(value) {
            internalPlaybackSpeed = value.speed
            internalPlaybackPitch = value.pitch
            val params = PlaybackParameters(value.speed, value.pitch)
            currentPlayer?.playbackParameters = params
            // Also apply to secondary player during crossfade
            secondaryPlayer?.playbackParameters = params
        }

    // ========== Audio Settings ==========

    override val audioSessionId: Int
        get() = currentPlayer?.audioSessionId ?: 0

    override var volume: Float
        get() = internalVolume
        set(value) {
            Logger.w(TAG, "Setting volume to $value")
            internalVolume = value.coerceIn(0f, 1f)
            currentPlayer?.volume = internalVolume
            listeners.forEach { it.onVolumeChanged(internalVolume) }
        }

    override var skipSilenceEnabled: Boolean
        get() = internalSkipSilence
        set(value) {
            internalSkipSilence = value
            currentPlayer?.skipSilenceEnabled = value
            // Also apply to secondary player during crossfade
            secondaryPlayer?.skipSilenceEnabled = value
        }

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

        // Cancel crossfade
        crossfadeJob?.cancel()
        secondaryPlayer?.release()
        secondaryPlayer = null
        secondaryPlayerFilter = null
        currentPlayerFilter = null
        isCrossfading = false

        abandonAudioFocusInternal()
        coroutineScope.cancel()
        cleanupCurrentPlayerInternal()
        clearAllPrecacheInternal()
        listeners.clear()
    }

    // ========== Internal: State Transition ==========

    /**
     * State transition helper - mirrors GstreamerPlayerAdapter.transitionToState()
     */
    private fun propagatePlayerError(error: PlaybackException) {
        val genericError =
            PlayerError(
                errorCode =
                    when (error.errorCode) {
                        PlaybackException.ERROR_CODE_TIMEOUT -> PlayerConstants.ERROR_CODE_TIMEOUT
                        else -> error.errorCode
                    },
                errorCodeName = error.errorCodeName,
                message = error.message,
            )
        Logger.e(TAG, "Playback error: ${error.message}")
        listeners.forEach { it.onPlayerError(genericError) }
        transitionToState(InternalState.ERROR)
    }

    private fun transitionToState(newState: InternalState) {
        if (internalState == newState) {
            Logger.d(TAG, "State transition ignored: already in $newState")
            return
        }

        val oldState = internalState
        internalState = newState

        Logger.d(TAG, "State: $oldState -> $newState (playWhenReady=$internalPlayWhenReady)")

        // Update cached duration from player
        currentPlayer?.let {
            val dur = it.duration
            if (dur > 0L) {
                cachedDuration = dur
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
            }

            InternalState.READY -> {
                if (internalPlayWhenReady) {
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

    // ========== Internal: Load and Play Track ==========

    /**
     * Load and play track - mirrors GstreamerPlayerAdapter.loadAndPlayTrackInternal()
     *
     * Key difference: instead of extracting URL + creating PlayBin,
     * we create an ExoPlayer, set a MediaItem, and let MediaSourceFactory resolve the URL.
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
                    val cachedPlayerEntry = precachedPlayers.remove(videoId)
                    val player: ExoPlayer
                    val playerFilter: CrossfadeFilterAudioProcessor?
                    if (cachedPlayerEntry?.player != null) {
                        Logger.d(TAG, "Using precached player for $videoId")
                        player = cachedPlayerEntry.player
                        playerFilter = cachedPlayerEntry.filter
                    } else {
                        Logger.d(TAG, "Creating new player for $videoId")
                        val pwf = createExoPlayerInstance()
                        player = pwf.player
                        playerFilter = pwf.filter
                        player.setMediaItem(mediaItem.toMedia3MediaItem())
                        player.prepare()
                    }

                    // === CAREFUL ORDER for ForwardingPlayer integration ===

                    // 1. Remove our active listener from old player
                    cleanupPlayerListenerInternal()
                    stopPositionUpdates()
                    crossfadeJob?.cancel()
                    crossfadeJob = null
                    setCrossfading(false)

                    // 2. Save old player reference
                    val oldPlayer = currentPlayer

                    // 3. Set new player as current
                    currentPlayer = player
                    currentPlayerFilter = playerFilter

                    // 4. Setup our listener on new player
                    setupPlayerListenerInternal(player)

                    // 5. Swap ForwardingPlayer delegate (moves MediaSession's listeners from old to new)
                    forwardingPlayer.swapDelegate(player)

                    // 5b. Notify MediaSession about the new media item
                    // The MediaItem was set before the swap (either during precache or above),
                    // so MediaSession's listener missed the onMediaItemTransition event.
                    // play() below will trigger onIsPlayingChanged which causes MediaSession
                    // to re-query metadata, but this explicit notify is safer and ensures
                    // the notification updates immediately even if play() is delayed.
                    forwardingPlayer.notifyMediaItemChanged()

                    // 6. NOW release old player (it has no listeners anymore)
                    if (oldPlayer != null && oldPlayer !== player) {
                        try {
                            oldPlayer.stop()
                            oldPlayer.release()
                        } catch (e: Exception) {
                            Logger.w(TAG, "Error releasing old player: ${e.message}")
                        }
                    }

                    // Audio focus is held at the adapter level (see Audio Focus section),
                    // not per-player, so it survives this swap (#2155).

                    // Apply settings
                    player.volume = internalVolume
                    player.playbackParameters = PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
                    player.skipSilenceEnabled = internalSkipSilence

                    // Seek if needed
                    if (startPositionMs > 0) {
                        player.seekTo(startPositionMs)
                        cachedPosition = startPositionMs
                    }

                    // Auto-play if requested
                    if (shouldPlay) {
                        requestAudioFocusInternal()
                        player.play()
                        transitionToState(InternalState.PLAYING)
                    } else {
                        player.pause()
                        transitionToState(InternalState.READY)
                    }

                    forwardingPlayer.suppressPlaybackEnded = false

                    // Start position updates
                    startPositionUpdates()

                    // Eagerly load audio metadata for auto crossfade calculations
                    // so it's available when position updates check the trigger threshold
                    if (crossfadeEnabled && crossfadeDurationMs == DataStoreManager.CROSSFADE_DURATION_AUTO) {
                        loadAudioMetaIfNeeded(videoId)
                    }

                    // Trigger precaching
                    triggerPrecachingInternal()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(TAG, "Load track error: ${e.message}", e)
                    forwardingPlayer.suppressPlaybackEnded = false
                    transitionToState(InternalState.ERROR)
                }
            }
    }

    // ========== Internal: Player Listener Management ==========
    // Equivalent to setupPlayerListenersInternal / cleanupBusListenersInternal

    /**
     * Setup Player.Listener on the given player.
     * First removes any existing listener from the old player (like cleanupBusListenersInternal),
     * then creates and attaches a new listener to the given player.
     *
     * This is the KEY crossfade mechanism:
     * When crossfade starts, the listener is moved from old player to new player.
     * The old player's STATE_ENDED event is then ignored (no listener to handle it).
     */
    private fun setupPlayerListenerInternal(player: ExoPlayer) {
        // Clean up old listener first
        cleanupPlayerListenerInternal()

        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            Logger.d(TAG, "End of stream reached")
                            if (hasNextMediaItem()) {
                                forwardingPlayer.suppressPlaybackEnded = true
                                transitionToState(InternalState.PREPARING)
                            } else {
                                transitionToState(InternalState.ENDED)
                            }
                            handleTrackEndInternal()
                        }

                        Player.STATE_READY -> {
                            // Always clear loading state when ExoPlayer is ready
                            // (handles both initial load AND mid-playback rebuffer)
                            if (cachedIsLoading && player == currentPlayer) {
                                cachedIsLoading = false
                                listeners.forEach { it.onIsLoadingChanged(false) }
                            }
                            // Duration should be available now
                            val dur = player.duration
                            if (dur > 0) cachedDuration = dur
                            // Reset retry counter on successful playback
                            retryCount = 0
                            retryVideoId = null
                        }

                        Player.STATE_BUFFERING -> {
                            // Playback is stalled waiting for data â€” report buffering
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    if (isPlaying) {
                        if (internalState != InternalState.PLAYING) {
                            transitionToState(InternalState.PLAYING)
                            notifyEqualizerIntent(true)
                        }
                    } else {
                        if (internalState == InternalState.PLAYING) {
                            if (!player.playWhenReady) {
                                transitionToState(InternalState.PAUSED)
                                notifyEqualizerIntent(false)
                            }
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlayerError from non-current player")
                        return
                    }

                    // Retry for source errors (expired/invalid stream URL)
                    // ERROR_CODE_PARSING_CONTAINER_MALFORMED (3001) = server returned non-media response (e.g. HTML error page)
                    // ERROR_CODE_IO_BAD_HTTP_STATUS (2004) = HTTP 403/410 from expired URL
                    // ERROR_CODE_IO_NETWORK_CONNECTION_FAILED (2001) = connection refused
                    val isRetryableSourceError =
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS

                    val currentVideoId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId
                    if (isRetryableSourceError && currentVideoId != null) {
                        // Reset retry count if this is a different track
                        if (retryVideoId != currentVideoId) {
                            retryVideoId = currentVideoId
                            retryCount = 0
                        }
                        if (retryCount < maxRetryCount) {
                            retryCount++
                            Logger.w(TAG, "Retryable source error (attempt $retryCount/$maxRetryCount) for $currentVideoId: ${error.errorCodeName}")
                            // Snapshot the current position so the retry resumes where playback
                            // failed instead of restarting the track from the beginning.
                            val resumePositionMs = cachedPosition.coerceAtLeast(0L)
                            coroutineScope.launch {
                                try {
                                    // Invalidate cached format so ResolvingDataSource fetches a fresh URL
                                    streamRepository.invalidateFormat(currentVideoId)
                                    streamRepository.invalidateFormat("${com.sonique.common.MERGING_DATA_TYPE.VIDEO}$currentVideoId")
                                    // Evict from precache (it may hold a stale player)
                                    precachedPlayers.remove(currentVideoId)?.player?.release()
                                    // Reload the track at the saved position
                                    loadAndPlayTrackInternal(localCurrentMediaItemIndex, resumePositionMs, shouldPlay = true)
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Logger.e(TAG, "Retry failed: ${e.message}", e)
                                    propagatePlayerError(error)
                                }
                            }
                            return
                        }
                        Logger.e(TAG, "Max retries ($maxRetryCount) exhausted for $currentVideoId")
                    }

                    propagatePlayerError(error)
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    // ExoPlayer reports isLoading=true during normal background buffer refill,
                    // not just when playback is stalled. Only propagate loading=true when
                    // playback is actually stalled (STATE_BUFFERING) AND the player intends
                    // to play. Ignore buffering events while paused to avoid showing
                    // a loading spinner when the user has explicitly paused.
                    val isPlaybackStalled = isLoading && player.playbackState == Player.STATE_BUFFERING && player.playWhenReady
                    val isCurrentPlayer = player == currentPlayer
                    Logger.d(TAG, "onIsLoadingChanged: isLoading=$isLoading, isPlaybackStalled=$isPlaybackStalled, isCurrentPlayer=$isCurrentPlayer")
                    if (cachedIsLoading != isPlaybackStalled && isCurrentPlayer) {
                        cachedIsLoading = isPlaybackStalled
                        listeners.forEach { it.onIsLoadingChanged(isPlaybackStalled) }
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    val genericTracks = tracks.toGenericTracks()
                    listeners.forEach { it.onTracksChanged(genericTracks) }
                }

                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    if (player != currentPlayer) {
                        Logger.d(TAG, "Ignoring onPlaybackStateChanged from non-current player")
                        return
                    }
                    val shouldBePlaying =
                        !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
                    if (events.containsAny(
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                            Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_IS_PLAYING_CHANGED,
                            Player.EVENT_POSITION_DISCONTINUITY,
                        )
                    ) {
                        if (shouldBePlaying) {
                            listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(true) }
                        } else {
                            listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(false) }
                        }
                    }
                }
            }

        player.addListener(listener)
        activePlayerListener = listener
    }

    /**
     * Clean up active player listener from whichever player has it.
     * During crossfade the listener is on secondaryPlayer, not currentPlayer.
     */
    private fun cleanupPlayerListenerInternal() {
        activePlayerListener?.let { listener ->
            currentPlayer?.removeListener(listener)
            secondaryPlayer?.removeListener(listener)
        }
        activePlayerListener = null
    }

    // ========== Internal: Player Cleanup ==========

    private fun cleanupPlayerInternal(player: ExoPlayer) {
        try {
            player.stop()
            player.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error cleaning up player: ${e.message}")
        }
    }

    private fun cleanupCurrentPlayerInternal() {
        stopPositionUpdates()
        cleanupPlayerListenerInternal()

        // Cancel any ongoing crossfade
        crossfadeJob?.cancel()
        crossfadeJob = null
        setCrossfading(false)

        currentPlayer?.let { cleanupPlayerInternal(it) }
        currentPlayer = null
    }

    /**
     * Abort an in-progress crossfade by committing the INCOMING track (A+1) as the new
     * current player â€” the mid-fade counterpart of [finalizeCrossfade]. Invoked when the
     * user interacts during a crossfade (next/prev/pause). Direction: "crossfade means we
     * have moved to A+1", so we keep A+1 and drop A.
     *
     * Mirrors [finalizeCrossfade]'s player swap: release the outgoing player (A), promote
     * the secondary player (A+1) to current. The active listener and the ForwardingPlayer
     * delegate are intentionally NOT touched â€” both already point at A+1 (set in
     * [triggerCrossfadeTransition]); touching them would lose the listener / detach
     * MediaSession. [localCurrentMediaItemIndex] already equals A+1, so it is kept.
     *
     * Does NOT change [internalState], restart position updates, or seek â€” the caller
     * decides what to do next (pause in place, advance to A+2, go to previous, ...).
     */
    private fun commitIncomingAsCurrentInternal() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        stopPositionUpdates()

        // Release the outgoing player (A). Do NOT remove listeners â€” the active listener
        // is on the incoming player (A+1), which we are keeping (same as finalizeCrossfade).
        currentPlayer?.let { cleanupPlayerInternal(it) }

        // Promote incoming (A+1) to current.
        currentPlayer = secondaryPlayer
        currentPlayerFilter = secondaryPlayerFilter
        secondaryPlayer = null
        secondaryPlayerFilter = null

        // The incoming player was fading in: reduced volume + DJ filter on (and the
        // outgoing one carried any tempo/pitch match). Restore normal playback on A+1.
        currentPlayerFilter?.enabled = false
        currentPlayer?.volume = internalVolume
        currentPlayer?.playbackParameters = PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
        currentPlayer?.skipSilenceEnabled = internalSkipSilence

        setCrossfading(false)
        crossfadeFromIndex = -1
    }

    // ========== Internal: Track End ==========

    /**
     * Handle track end - mirrors GstreamerPlayerAdapter.handleTrackEndInternal()
     */
    private fun handleTrackEndInternal() {
        // Check if crossfade should be used
        val shouldCrossfade =
            crossfadeEnabled &&
                hasNextMediaItem() &&
                !isCrossfading

        if (shouldCrossfade) {
            val nextIndex = getNextMediaItemIndex()
            triggerCrossfadeTransition(nextIndex)
        } else {
            // Original behavior
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
    }

    // ========== Internal: Crossfade ==========

    /**
     * Trigger crossfade to next track.
     * Mirrors GstreamerPlayerAdapter.triggerCrossfadeTransition()
     *
     * Key mechanism: [setupPlayerListenerInternal] moves the active listener
     * from the current player to the next player. The old player's STATE_ENDED
     * event is then ignored (no listener to fire).
     */
    private fun triggerCrossfadeTransition(nextIndex: Int) {
        if (nextIndex !in playlist.indices || isCrossfading) return

        coroutineScope.launch {
            try {
                setCrossfading(true)
                val nextMediaItem = playlist[nextIndex]
                val nextVideoId = nextMediaItem.mediaId

                Logger.d(TAG, "Starting crossfade to track $nextIndex")

                // Get or create secondary player
                val cachedPlayerEntry = precachedPlayers.remove(nextVideoId)
                val nextPlayer: ExoPlayer
                val nextFilter: CrossfadeFilterAudioProcessor?
                if (cachedPlayerEntry?.player != null) {
                    nextPlayer = cachedPlayerEntry.player
                    nextFilter = cachedPlayerEntry.filter
                } else {
                    val pwf = createExoPlayerInstance()
                    nextPlayer = pwf.player
                    nextFilter = pwf.filter
                    nextPlayer.setMediaItem(nextMediaItem.toMedia3MediaItem())
                    nextPlayer.prepare()
                }

                // Setup secondary player
                secondaryPlayer = nextPlayer
                secondaryPlayerFilter = nextFilter
                // *** KEY: Move our custom listener from current to next player ***
                setupPlayerListenerInternal(nextPlayer)
                // Playback parameters applied below after AutoMix ratios are calculated
                nextPlayer.skipSilenceEnabled = internalSkipSilence
                nextPlayer.volume = 0f

                // === CRITICAL ORDER for MediaSession notification ===
                // 1. Swap ForwardingPlayer BEFORE play()
                //    This moves MediaSession's Player.Listener to nextPlayer.
                //    If we play() first, MediaSession misses onIsPlayingChanged and
                //    onMediaItemTransition events (they fire before its listener is attached).
                forwardingPlayer.swapDelegate(nextPlayer)

                // 2. Now play - MediaSession's listener is attached and receives state change events
                requestAudioFocusInternal()
                nextPlayer.play()

                forwardingPlayer.suppressPlaybackEnded = false

                // 3. Force MediaSession to update notification metadata
                //    Even though play() triggers onIsPlayingChanged (which causes MediaSession
                //    to re-query player state), the onMediaItemTransition event was missed
                //    (MediaItem was set during precache, before the swap).
                //    This explicitly notifies MediaSession about the new track metadata.
                forwardingPlayer.notifyMediaItemChanged()

                // Capture current video ID BEFORE advancing localCurrentMediaItemIndex
                val currentVideoId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId ?: ""

                // Lazily load AutoMix metadata from NewFormatEntity if not in cache
                val isAutoMode = crossfadeDurationMs == DataStoreManager.CROSSFADE_DURATION_AUTO
                if (isAutoMode) {
                    loadAudioMetaIfNeeded(currentVideoId)
                    loadAudioMetaIfNeeded(nextVideoId)
                }
                val resolvedConfigDurationMs =
                    if (isAutoMode) {
                        resolveAutoCrossfadeDurationMs(currentVideoId, nextVideoId)
                    } else {
                        crossfadeDurationMs
                    }
                val bpmSpeedRatio = if (isAutoMode) calculateBpmSpeedRatio(currentVideoId, nextVideoId) else 1.0f
                val keyPitchRatio = if (isAutoMode) calculateKeyPitchRatio(currentVideoId, nextVideoId) else 1.0f

                // Incoming player plays at natural speed/pitch â€” only the outgoing
                // player is adjusted to match the incoming track during crossfade.
                nextPlayer.playbackParameters =
                    PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)

                // Update now playing IMMEDIATELY (store from-index for cancel scenarios)
                crossfadeFromIndex = localCurrentMediaItemIndex
                localCurrentMediaItemIndex = nextIndex

                // Notify our custom listeners IMMEDIATELY (UI updates to new track)
                listeners.forEach {
                    it.onMediaItemTransition(
                        nextMediaItem,
                        PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                    )
                }

                Logger.d(TAG, "Now playing updated to track $nextIndex during crossfade")

                // Calculate effective crossfade duration based on ACTUAL remaining time.
                // If the secondary player wasn't precached, URL resolution + buffering may
                // have consumed part of the crossfade window. Use the lesser of configured
                // duration and actual remaining time so the animation ends when the old track does.
                val actualTimeRemaining =
                    currentPlayer?.let { player ->
                        val dur = player.duration
                        val pos = player.currentPosition
                        // Divide by playback speed: at 2x speed, remaining wall-clock time is halved
                        val speed = internalPlaybackSpeed.coerceAtLeast(0.1f)
                        if (dur > 0 && pos >= 0) ((dur - pos) / speed).toLong() else resolvedConfigDurationMs.toLong()
                    } ?: resolvedConfigDurationMs.toLong()

                val effectiveCrossfadeDurationMs =
                    minOf(resolvedConfigDurationMs.toLong(), actualTimeRemaining)
                        .coerceAtLeast(1000L)
                        .toInt()

                Logger.d(
                    TAG,
                    "Crossfade duration: configured=${resolvedConfigDurationMs}ms (auto=$isAutoMode), " +
                        "bpmRatio=$bpmSpeedRatio, pitchRatio=$keyPitchRatio, " +
                        "actualRemaining=${actualTimeRemaining}ms, effective=${effectiveCrossfadeDurationMs}ms",
                )

                // Perform crossfade animation with effective duration and AutoMix parameters
                performCrossfade(nextIndex, nextPlayer, effectiveCrossfadeDurationMs, bpmSpeedRatio, keyPitchRatio)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(TAG, "Crossfade error: ${e.message}", e)
                setCrossfading(false)
                // Fallback to normal transition
                seekTo(nextIndex, 0)
            }
        }
    }

    /**
     * S-curve (sigmoid) function for DJ filter crossfade timing.
     * Keeps both tracks near full spectrum at the start and end,
     * with a steep transition in the middle â€” like a real DJ mixer crossfader.
     *
     * k controls steepness: higher = sharper transition.
     * k=6 gives a gentle S-curve: ~8-92% of duration covers the sweep,
     * keeping a subtle onset/ending without harsh "slam" in the middle.
     */
    private fun sigmoid(
        t: Float,
        k: Float = DJ_FILTER_SIGMOID_K,
    ): Float = 1.0f / (1.0f + exp(-k * (t - 0.5f)))

    /**
     * Exponential interpolation between two values.
     * Frequency perception is logarithmic, so this produces a natural-sounding sweep.
     */
    private fun exponentialInterpolate(
        start: Float,
        end: Float,
        t: Float,
    ): Float {
        if (start <= 0f || end <= 0f) return end
        return exp(ln(start) + (ln(end) - ln(start)) * t).toFloat()
    }

    /**
     * Perform the actual crossfade animation.
     * Mirrors GstreamerPlayerAdapter.performCrossfade()
     *
     * @param effectiveDurationMs The actual crossfade duration to use. May be shorter than
     *   the configured [crossfadeDurationMs] if URL resolution / buffering consumed
     *   part of the crossfade window.
     * @param targetSpeedRatio BPM-based speed ratio for incoming track (1.0 = no adjustment).
     *   Ramps from this value to 1.0 during crossfade so the track plays at natural speed after.
     * @param targetPitchRatio Key-based pitch ratio for incoming track (1.0 = no adjustment).
     *   Ramps from this value to 1.0 during crossfade.
     */
    private suspend fun performCrossfade(
        nextIndex: Int,
        nextPlayer: ExoPlayer,
        effectiveDurationMs: Int,
        targetSpeedRatio: Float = 1.0f,
        targetPitchRatio: Float = 1.0f,
    ) {
        val steps = 50 // 50 steps for smooth transition
        val delayPerStep = (effectiveDurationMs / steps).coerceAtLeast(20) // min 20ms per step
        val targetVolume = internalVolume
        val useDjFilter = djCrossfadeEnabled
        val useAutoMixRamp = targetSpeedRatio != 1.0f || targetPitchRatio != 1.0f
        Logger.d(
            TAG,
            "Crossfade animation: ${effectiveDurationMs}ms, $steps steps, ${delayPerStep}ms/step, " +
                "dj=$useDjFilter, autoMix=$useAutoMixRamp (speed=$targetSpeedRatio, pitch=$targetPitchRatio)",
        )

        // Setup DJ filters before starting animation
        if (useDjFilter) {
            currentPlayerFilter?.let { filter ->
                filter.filterType = BiquadFilter.FilterType.LOW_PASS
                filter.cutoffFrequencyHz = LPF_START_HZ
                filter.enabled = true
            }
            secondaryPlayerFilter?.let { filter ->
                filter.filterType = BiquadFilter.FilterType.HIGH_PASS
                filter.cutoffFrequencyHz = HPF_START_HZ
                filter.enabled = true
            }
        }

        // Track last quantized speed/pitch to avoid redundant PlaybackParameters updates
        var lastOutgoingSpeed = -1f
        var lastOutgoingPitch = -1f

        // Front-loaded BPM/pitch ramp portion (fraction of crossfade duration).
        // Speed/pitch reach target within the first [BPM_RAMP_PORTION] of crossfade,
        // then HOLD for the remainder so both tracks share the same effective BPM
        // throughout the audible overlap.
        val bpmRampPortion = BPM_RAMP_PORTION

        crossfadeJob?.cancel()
        crossfadeJob =
            coroutineScope.launch {
                try {
                    for (step in 0..steps) {
                        if (!isActive) break

                        val progress = step.toFloat() / steps

                        // Equal-power crossfade (cos/sin curves) instead of linear.
                        // Human loudness perception is logarithmic â€” linear volume fade makes
                        // the outgoing track "die" perceptually around the midpoint while
                        // incoming hasn't filled in yet, leaving an audible gap.
                        // cosÂ²(Î¸) + sinÂ²(Î¸) = 1 â†’ total acoustic power stays constant across
                        // the blend, so the listener perceives a smooth handoff with the
                        // outgoing remaining audible long enough for the DJ filter sweep
                        // to register.
                        val fadeAngle = (progress * PI / 2).toFloat()

                        // Fade out current player (old track): cos curve, slow at start, fast at end
                        val fadeOutVolume = targetVolume * cos(fadeAngle)
                        currentPlayer?.volume = fadeOutVolume

                        // Fade in next player (new track): sin curve, fast at start, slow at end
                        val fadeInVolume = targetVolume * sin(fadeAngle)
                        nextPlayer.volume = fadeInVolume

                        // DJ-style filter sweep (alongside volume)
                        // S-curve (sigmoid) on time axis: holds flat at start/end,
                        // transitions steeply in the middle â€” mimics a real DJ mixer
                        // crossfader where both tracks briefly overlap at full spectrum.
                        // Exponential interpolation on frequency axis preserves
                        // logarithmic hearing perception for a natural-sounding sweep.
                        if (useDjFilter) {
                            val filterProgress = sigmoid(progress)

                            // Outgoing: LPF sweeps 20kHz â†’ 200Hz
                            currentPlayerFilter?.cutoffFrequencyHz =
                                exponentialInterpolate(LPF_START_HZ, LPF_END_HZ, filterProgress)

                            // Incoming: HPF sweeps 8kHz â†’ 20Hz
                            secondaryPlayerFilter?.cutoffFrequencyHz =
                                exponentialInterpolate(HPF_START_HZ, HPF_END_HZ, filterProgress)
                        }

                        // AutoMix: only adjust the OUTGOING (previous) player to match
                        // the incoming track. The incoming player stays at natural speed/pitch.
                        //
                        // Front-loaded ramp: outgoing speed/pitch reach target within the
                        // first [bpmRampPortion] of crossfade, then HOLD at target for the
                        // remainder. This way the bulk of the audible blend plays at matched
                        // BPM (DJ-style beat alignment) instead of catching up only at the
                        // very end when outgoing volume is already 0.
                        //
                        // Quantize to SPEED_PITCH_STEP to avoid SonicAudioProcessor popping
                        // from too-frequent micro-adjustments.
                        if (useAutoMixRamp) {
                            val linearRamp =
                                if (bpmRampPortion <= 0f) {
                                    1f
                                } else {
                                    (progress / bpmRampPortion).coerceAtMost(1f)
                                }
                            // Smoothstep S-curve: slowâ†’fastâ†’slow (3tÂ²âˆ’2tÂ³)
                            val rampProgress = linearRamp * linearRamp * (3f - 2f * linearRamp)
                            val rawOutSpeed = lerp(1.0f, targetSpeedRatio, rampProgress)
                            val rawOutPitch = lerp(1.0f, targetPitchRatio, rampProgress)
                            val qOutSpeed = quantize(rawOutSpeed * internalPlaybackSpeed)
                            val qOutPitch = quantize(rawOutPitch * internalPlaybackPitch)

                            if (qOutSpeed != lastOutgoingSpeed || qOutPitch != lastOutgoingPitch) {
                                currentPlayer?.playbackParameters = PlaybackParameters(qOutSpeed, qOutPitch)
                                lastOutgoingSpeed = qOutSpeed
                                lastOutgoingPitch = qOutPitch
                            }
                        }

                        delay(delayPerStep.toLong())
                    }

                    // Transition complete
                    finalizeCrossfade(nextIndex, nextPlayer)
                } catch (e: CancellationException) {
                    Logger.d(TAG, "Crossfade cancelled")
                    // Cleanup DJ filters
                    currentPlayerFilter?.enabled = false
                    secondaryPlayerFilter?.enabled = false
                    // Restore outgoing player's speed/pitch to natural
                    currentPlayer?.playbackParameters =
                        PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
                    // Cleanup player
                    nextPlayer.release()
                    secondaryPlayer = null
                    secondaryPlayerFilter = null
                    setCrossfading(false)
                }
            }
    }

    // ========== AutoMix Public API ==========

    /**
     * Audio analysis metadata for a song, populated from Tidal search response.
     */
    data class SongAudioMeta(
        val bpm: Int?,
        val key: String?,
        val keyScale: String?, // "MAJOR" or "MINOR"
    )

    /**
     * Update the audio analysis metadata cache for a song.
     * Called externally when Tidal 320kbps stream is fetched and BPM/key data is available.
     * Data is used by Auto crossfade mode for beat-quantized duration, BPM matching, and key matching.
     */
    fun updateSongAudioMeta(
        videoId: String,
        bpm: Int?,
        key: String?,
        keyScale: String?,
    ) {
        if (bpm != null || key != null) {
            audioMetaCache[videoId] = SongAudioMeta(bpm, key, keyScale)
            Logger.d(TAG, "AutoMix meta updated: videoId=$videoId, bpm=$bpm, key=$key $keyScale")
        }
    }

    // ========== AutoMix Internal Logic ==========

    /**
     * Lazily load audio analysis metadata from NewFormatEntity if not already in cache.
     * Called before AutoMix calculations to ensure metadata is available.
     */
    private suspend fun loadAudioMetaIfNeeded(videoId: String) {
        if (videoId.isBlank() || audioMetaCache.containsKey(videoId)) return
        try {
            val format = streamRepository.getNewFormat(videoId).firstOrNull()
            if (format == null) {
                Logger.d(TAG, "AutoMix meta: no NewFormatEntity found for videoId=$videoId")
                return
            }
            if (format.bpm != null || format.musicKey != null) {
                audioMetaCache[videoId] = SongAudioMeta(format.bpm, format.musicKey, format.keyScale)
                Logger.d(TAG, "AutoMix meta loaded: videoId=$videoId, bpm=${format.bpm}, key=${format.musicKey} ${format.keyScale}")
            } else {
                Logger.d(TAG, "AutoMix meta: format exists but no bpm/key data for videoId=$videoId")
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to load AutoMix meta for $videoId: ${e.message}")
        }
    }

    /**
     * Linear interpolation between two values.
     */
    private fun lerp(
        start: Float,
        end: Float,
        t: Float,
    ): Float = start + (end - start) * t

    /**
     * Quantize a speed/pitch value to the nearest [SPEED_PITCH_STEP] (2%).
     * Prevents SonicAudioProcessor from resetting on micro-adjustments that
     * cause audible popping/clicking artifacts.
     */
    private fun quantize(value: Float): Float = (Math.round(value / SPEED_PITCH_STEP) * SPEED_PITCH_STEP)

    /**
     * Get BPM-adaptive target duration for auto crossfade.
     * Linear interpolation: BPM 70 â†’ 35s, BPM 170 â†’ 12s.
     * Slower songs get longer crossfade (like Apple Music AutoMix ~30s average).
     */
    private fun getAutoTargetDurationMs(bpm: Int): Double {
        val clampedBpm = bpm.coerceIn(70, 170)
        // Linear interpolation: BPM 70 â†’ 30s, BPM 170 â†’ 7s
        return 30000.0 - (clampedBpm - 70) * 230.0
    }

    /**
     * Resolve the crossfade duration for Auto mode based on BPM, BPM gap, and key gap.
     *
     * Algorithm:
     * 1. Base duration from current BPM (slow â†’ longer, fast â†’ shorter)
     * 2. BPM gap factor: larger tempo difference â†’ longer crossfade to smooth transition
     * 3. Key gap factor: larger Camelot distance â†’ longer crossfade to mask harmonic clash
     * 4. Quantize to beat boundaries, clamp to safe range
     */
    private fun resolveAutoCrossfadeDurationMs(
        currentVideoId: String,
        nextVideoId: String,
    ): Int {
        val currentBpm = audioMetaCache[currentVideoId]?.bpm
        val nextBpm = audioMetaCache[nextVideoId]?.bpm
        if (currentBpm == null || nextBpm == null) return AUTO_FALLBACK_DURATION_MS
        if (currentBpm <= 0 || nextBpm <= 0) return AUTO_FALLBACK_DURATION_MS

        val beatMs = 60_000.0 / currentBpm
        val baseTargetMs = getAutoTargetDurationMs(currentBpm)

        val bpmGapFactor = calculateBpmGapDurationFactor(currentBpm, nextBpm)
        val keyGapFactor = calculateKeyGapDurationFactor(currentVideoId, nextVideoId)
        val adjustedTargetMs = baseTargetMs * bpmGapFactor * keyGapFactor

        val bestBeatCount =
            BEAT_COUNT_OPTIONS.minByOrNull { abs(it * beatMs - adjustedTargetMs) }
                ?: DEFAULT_BEAT_COUNT
        val duration = (bestBeatCount * beatMs).toInt()

        Logger.d(
            TAG,
            "AutoMix duration: bpm=$currentBpm -> $nextBpm, base=${baseTargetMs.toInt()}ms, " +
                "bpmGap=${"%.2f".format(bpmGapFactor)}, keyGap=${"%.2f".format(keyGapFactor)}, " +
                "adjusted=${adjustedTargetMs.toInt()}ms, beats=$bestBeatCount, final=${duration}ms",
        )

        return duration.coerceIn(AUTO_MIN_DURATION_MS, AUTO_MAX_DURATION_MS)
    }

    /**
     * BPM gap â†’ duration multiplier.
     * Normalizes halftime/doubletime (80 vs 160 â†’ effectively same tempo).
     * Linear: 0% gap â†’ 1.0x, 25% gap â†’ 1.5x.
     */
    private fun calculateBpmGapDurationFactor(
        currentBpm: Int,
        nextBpm: Int,
    ): Double {
        if (currentBpm <= 0 || nextBpm <= 0) return 1.0
        var ratio = nextBpm.toDouble() / currentBpm.toDouble()
        while (ratio > 1.5) ratio /= 2.0
        while (ratio < 0.67) ratio *= 2.0
        val gapPercent = abs(1.0 - ratio)
        return 1.0 + gapPercent * BPM_GAP_DURATION_SCALE
    }

    /**
     * Key gap (Camelot distance) â†’ duration multiplier.
     * Compatible keys (dist â‰¤ 1) need no extension; far keys need longer blend.
     */
    private fun calculateKeyGapDurationFactor(
        currentVideoId: String,
        nextVideoId: String,
    ): Double {
        val currentMeta = audioMetaCache[currentVideoId]
        val nextMeta = audioMetaCache[nextVideoId]
        val currentKey = currentMeta?.key ?: return UNKNOWN_GAP_DEFAULT_FACTOR
        val nextKey = nextMeta?.key ?: return UNKNOWN_GAP_DEFAULT_FACTOR

        val currentCamelot = keyToCamelot(currentKey, currentMeta.keyScale) ?: return 1.0
        val nextCamelot = keyToCamelot(nextKey, nextMeta.keyScale) ?: return 1.0

        val dist = camelotDistance(currentCamelot, nextCamelot)
        return when {
            dist <= 1 -> 1.0
            dist == 2 -> 1.1
            dist <= 4 -> 1.25
            else -> 1.4
        }
    }

    /**
     * Calculate the playback speed ratio to match the incoming track's BPM to the current track's BPM.
     * Returns a ratio to apply to the incoming track's speed (e.g., 1.05 = speed up 5%).
     * Handles halftime/doubletime harmonic BPM relationships (e.g., 70 BPM â‰ˆ 140 BPM / 2).
     * Returns 1.0 if no BPM data or the ratio exceeds the safe adjustment range.
     */
    private fun calculateBpmSpeedRatio(
        currentVideoId: String,
        nextVideoId: String,
    ): Float {
        val currentMeta = audioMetaCache[currentVideoId]
        val nextMeta = audioMetaCache[nextVideoId]
        val currentBpm = currentMeta?.bpm
        val nextBpm = nextMeta?.bpm

        if (currentBpm == null || nextBpm == null) {
            Logger.d(
                TAG,
                "AutoMix BPM: missing data - current=$currentBpm (cached=${currentMeta != null}), " +
                    "next=$nextBpm (cached=${nextMeta != null})",
            )
            return 1.0f
        }
        if (currentBpm <= 0 || nextBpm <= 0) return 1.0f

        // Ratio is APPLIED to outgoing player so its effective BPM matches next track.
        //   outgoing_effective_BPM = currentBpm Ã— ratio
        //   want outgoing_effective_BPM = nextBpm
        //   â†’ ratio = nextBpm / currentBpm
        var ratio = nextBpm.toFloat() / currentBpm.toFloat()

        // Normalize halftime/doubletime relationships (e.g., 140/70 â†’ 1.0, 70/140 â†’ 1.0)
        while (ratio > 1.5f) ratio /= 2f
        while (ratio < 0.67f) ratio *= 2f

        Logger.d(TAG, "AutoMix BPM: current=$currentBpm, next=$nextBpm, ratio=${"%.4f".format(ratio)}")

        // Only apply if adjustment is within safe range (avoids unnatural artifacts)
        // Quantize to SPEED_PITCH_STEP to avoid SonicAudioProcessor artifacts
        return if (ratio in BPM_RATIO_MIN..BPM_RATIO_MAX) {
            quantize(ratio)
        } else {
            Logger.d(TAG, "AutoMix BPM: ratio ${"%.4f".format(ratio)} outside safe range [$BPM_RATIO_MIN..$BPM_RATIO_MAX], skipping")
            1.0f
        }
    }

    // ========== Camelot Wheel Key Matching ==========

    /**
     * Camelot Wheel position: number (1-12) + type (minor=A, major=B).
     * Standard DJ key compatibility system â€” adjacent codes are harmonically compatible.
     */
    private data class CamelotCode(
        val number: Int,
        val isMinor: Boolean,
    ) {
        override fun toString(): String = "$number${if (isMinor) "A" else "B"}"
    }

    /**
     * Map a musical key + scale to its Camelot Wheel code.
     *
     * Camelot Wheel (minor = A, major = B):
     *  1A=Abm  1B=B     |  7A=Dm   7B=F
     *  2A=Ebm  2B=F#    |  8A=Am   8B=C
     *  3A=Bbm  3B=Db    |  9A=Em   9B=G
     *  4A=Fm   4B=Ab    | 10A=Bm  10B=D
     *  5A=Cm   5B=Eb    | 11A=F#m 11B=A
     *  6A=Gm   6B=Bb    | 12A=C#m 12B=E
     */
    private fun keyToCamelot(
        key: String,
        keyScale: String?,
    ): CamelotCode? {
        val semitone = keyToSemitone(key)
        if (semitone < 0) return null

        val isMinor = keyScale?.uppercase()?.contains("MIN") == true

        // Camelot number lookup by chromatic semitone (C=0, C#=1, D=2, ... B=11)
        //                                   C  C# D  D# E  F  F# G  G# A  A# B
        val minorCamelotByPitch = intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)
        val majorCamelotByPitch = intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)

        val number = if (isMinor) minorCamelotByPitch[semitone] else majorCamelotByPitch[semitone]
        return CamelotCode(number, isMinor)
    }

    /**
     * Calculate the Camelot distance between two keys.
     * Considers both the circular number distance (0-6) and type difference (A/B).
     *
     * Compatible transitions (distance <= 1):
     * - Same code (8A->8A): distance 0
     * - Adjacent number, same type (8A->7A, 8A->9A): distance 1
     * - Same number, different type / relative key (8A->8B): distance 1
     *
     * Near-compatible (distance 2, same type):
     * - +-2 same type (8A->10A): 2 semitones apart (whole tone) â€” safe to shift
     */
    private fun camelotDistance(
        a: CamelotCode,
        b: CamelotCode,
    ): Int {
        val numberDiff = abs(a.number - b.number)
        val circularDist = minOf(numberDiff, 12 - numberDiff)
        val typeDiff = if (a.isMinor != b.isMinor) 1 else 0
        return circularDist + typeDiff
    }

    /**
     * Calculate pitch ratio using Camelot Wheel for musically correct key matching.
     *
     * Tries Â±1 then Â±2 semitone shifts on the outgoing track and picks the
     * smallest shift that brings Camelot distance â‰¤ 1 (harmonically compatible).
     * Â±1 semitone is nearly imperceptible during crossfade; Â±2 is a whole tone.
     * If no small shift achieves compatibility, returns 1.0 (no shift).
     */
    private fun calculateKeyPitchRatio(
        currentVideoId: String,
        nextVideoId: String,
    ): Float {
        val currentMeta = audioMetaCache[currentVideoId]
        val nextMeta = audioMetaCache[nextVideoId]
        val currentKey = currentMeta?.key
        val nextKey = nextMeta?.key

        if (currentKey == null || nextKey == null) {
            Logger.d(
                TAG,
                "AutoMix Key: missing data - currentKey=$currentKey (cached=${currentMeta != null}), " +
                    "nextKey=$nextKey (cached=${nextMeta != null})",
            )
            return 1.0f
        }

        val currentCamelot = keyToCamelot(currentKey, currentMeta.keyScale)
        val nextCamelot = keyToCamelot(nextKey, nextMeta.keyScale)

        if (currentCamelot == null || nextCamelot == null) {
            Logger.d(
                TAG,
                "AutoMix Key: unknown key format - currentKey='$currentKey' ${currentMeta.keyScale}, " +
                    "nextKey='$nextKey' ${nextMeta.keyScale}",
            )
            return 1.0f
        }

        val dist = camelotDistance(currentCamelot, nextCamelot)

        Logger.d(
            TAG,
            "AutoMix Key: current=$currentKey ${currentMeta.keyScale} ($currentCamelot), " +
                "next=$nextKey ${nextMeta.keyScale} ($nextCamelot), camelotDist=$dist",
        )

        if (dist <= 1) {
            Logger.d(TAG, "AutoMix Key: compatible (dist=$dist), no shift")
            return 1.0f
        }

        val currentSemitone = keyToSemitone(currentKey)
        if (currentSemitone < 0) return 1.0f

        val isMinor = currentCamelot.isMinor
        //                                   C  C# D  D# E  F  F# G  G# A  A# B
        val minorCamelotByPitch = intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)
        val majorCamelotByPitch = intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)

        for (shift in intArrayOf(-1, 1, -2, 2)) {
            val shiftedSemitone = (currentSemitone + shift + 12) % 12
            val shiftedNumber =
                if (isMinor) minorCamelotByPitch[shiftedSemitone] else majorCamelotByPitch[shiftedSemitone]
            val shiftedCamelot = CamelotCode(shiftedNumber, isMinor)
            if (camelotDistance(shiftedCamelot, nextCamelot) <= 1) {
                val pitchRatio = exp(ln(2.0) * shift.toDouble() / 12.0).toFloat()
                Logger.d(
                    TAG,
                    "AutoMix Key: shift $shift semitones ($currentCamelot -> $shiftedCamelot), " +
                        "ratio=${"%.4f".format(pitchRatio)}",
                )
                return pitchRatio
            }
        }

        Logger.d(TAG, "AutoMix Key: dist=$dist, no safe shift within Â±2 semitones")
        return 1.0f
    }

    /**
     * Map a musical key name to its chromatic semitone number (0-11).
     * C=0, C#/Db=1, D=2, ..., B=11. Returns -1 for unknown keys.
     */
    private fun keyToSemitone(key: String): Int =
        when (key.trim()) {
            "C" -> 0
            "C#", "Db" -> 1
            "D" -> 2
            "D#", "Eb" -> 3
            "E" -> 4
            "F" -> 5
            "F#", "Gb" -> 6
            "G" -> 7
            "G#", "Ab" -> 8
            "A" -> 9
            "A#", "Bb" -> 10
            "B" -> 11
            else -> -1
        }

    companion object {
        // DJ crossfade sigmoid steepness (higher = sharper S-curve transition)
        private const val DJ_FILTER_SIGMOID_K = 6f

        // DJ crossfade filter frequency bounds
        private const val LPF_START_HZ = 20000f // Low-pass starts wide open
        private const val LPF_END_HZ = 200f // Low-pass ends muffled (keeps bass thump like Pioneer DJM)
        private const val HPF_START_HZ = 2000f // High-pass starts lower â€” incoming track fills in faster
        private const val HPF_END_HZ = 20f // High-pass ends wide open

        // AutoMix constants
        private const val AUTO_FALLBACK_DURATION_MS = 30000 // Default when no BPM data
        private const val AUTO_MIN_DURATION_MS = 20000
        private const val AUTO_MAX_DURATION_MS = 45000
        private val BEAT_COUNT_OPTIONS = intArrayOf(8, 16, 24, 32, 40, 48, 64, 80, 96)
        private const val DEFAULT_BEAT_COUNT = 32
        private const val BPM_RATIO_MIN = 0.75f // Max 25% slower
        private const val BPM_RATIO_MAX = 1.25f // Max 25% faster

        // BPM gap â†’ duration scale: 0% gap â†’ 1.0x, 25% gap â†’ 1.5x
        private const val BPM_GAP_DURATION_SCALE = 2.0

        // Default gap factor when BPM or key data is missing â€” assume moderate incompatibility
        private const val UNKNOWN_GAP_DEFAULT_FACTOR = 1.25

        // Quantization step for speed/pitch ramp (0.5% = 0.005)
        // Prevents SonicAudioProcessor from popping on micro-adjustments
        private const val SPEED_PITCH_STEP = 0.02f

        // Front-loaded BPM/pitch ramp portion (fraction of crossfade duration).
        // Outgoing tempo reaches target within the first [BPM_RAMP_PORTION] of the
        // crossfade (smoothstep S-curve) and then holds for the remainder.
        private const val BPM_RAMP_PORTION = 0.6f
    }

    /**
     * Finalize crossfade: swap players and cleanup.
     * Mirrors GstreamerPlayerAdapter.finalizeCrossfade()
     */
    private fun finalizeCrossfade(
        nextIndex: Int,
        nextPlayer: ExoPlayer,
    ) {
        Logger.d(TAG, "Crossfade complete, swapping players")

        // Cleanup old current player WITHOUT touching listeners
        // (listeners are already setup for nextPlayer via setupPlayerListenerInternal)
        stopPositionUpdates()

        // Cleanup the old current player manually
        currentPlayer?.let { oldPlayer ->
            try {
                oldPlayer.stop()
                oldPlayer.release()
            } catch (e: Exception) {
                Logger.w(TAG, "Error cleaning up old player: ${e.message}")
            }
        }

        // Disable and reset DJ filters on the new current player (no overhead during normal playback)
        secondaryPlayerFilter?.let { filter ->
            filter.enabled = false
        }
        // Old player's filter is released with the old player (GC'd)

        // Promote secondary to current
        currentPlayer = nextPlayer
        currentPlayerFilter = secondaryPlayerFilter
        secondaryPlayer = null
        secondaryPlayerFilter = null
        // localCurrentMediaItemIndex already updated in triggerCrossfadeTransition()

        // Audio focus is held at the adapter level (see Audio Focus section),
        // not per-player, so it survives this crossfade swap (#2155).

        // Ensure correct volume and playback parameters
        currentPlayer?.volume = internalVolume
        currentPlayer?.playbackParameters = PlaybackParameters(internalPlaybackSpeed, internalPlaybackPitch)
        currentPlayer?.skipSilenceEnabled = internalSkipSilence

        // Reset state
        setCrossfading(false)
        crossfadeFromIndex = -1
        transitionToState(InternalState.PLAYING)

        // Ensure MediaSession notification has correct metadata after crossfade completes.
        // setAudioAttributes() above may cause a brief audio session reset, so re-notify
        // MediaSession to guarantee the notification shows the correct track info.
        forwardingPlayer.notifyMediaItemChanged()

        // Start position tracking
        startPositionUpdates()

        // Trigger next precache
        triggerPrecachingInternal()
    }

    // ========== Internal: Position Updates ==========

    /**
     * Start position updates (periodic background task).
     * Also handles crossfade detection when position approaches end.
     */
    private fun startPositionUpdates() {
        stopPositionUpdates()

        positionUpdateJob =
            coroutineScope.launch {
                while (isActive && currentPlayer != null) {
                    try {
                        currentPlayer?.let { player ->
                            if (internalState == InternalState.PLAYING ||
                                internalState == InternalState.READY ||
                                internalState == InternalState.PAUSED
                            ) {
                                // During crossfade, show the incoming track's timeline
                                val timelinePlayer = if (isCrossfading) secondaryPlayer ?: player else player
                                val pos = timelinePlayer.currentPosition
                                val dur = timelinePlayer.duration
                                val buf = timelinePlayer.bufferedPosition

                                if (pos >= 0) cachedPosition = pos
                                if (dur > 0) cachedDuration = dur
                                if (buf >= 0) cachedBufferedPosition = buf

                                // Check if should trigger crossfade.
                                // Add a preparation buffer (3s) so that if the next track
                                // is NOT precached, URL resolution + buffering time doesn't
                                // eat into the audible crossfade window.
                                if (crossfadeEnabled &&
                                    !isCrossfading &&
                                    player.isPlaying &&
                                    dur > 0 &&
                                    pos > 0
                                ) {
                                    // Account for playback speed: at higher speed, media time
                                    // is consumed faster, so wall-clock remaining is shorter
                                    val speed = internalPlaybackSpeed.coerceAtLeast(0.1f)
                                    val timeRemaining = ((dur - pos) / speed).toLong()
                                    val nextVideoId = playlist.getOrNull(getNextMediaItemIndex())?.mediaId
                                    val isPrecached = nextVideoId != null && precachedPlayers.containsKey(nextVideoId)
                                    // If next track is precached, trigger at exactly crossfadeDurationMs.
                                    // If NOT precached, trigger 3s earlier to allow preparation time.
                                    val preparationBufferMs = if (isPrecached) 0L else 3000L
                                    // In Auto mode, calculate beat-quantized duration for trigger threshold.
                                    val resolvedDurationMs =
                                        if (crossfadeDurationMs == DataStoreManager.CROSSFADE_DURATION_AUTO) {
                                            val currentVideoId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId ?: ""
                                            resolveAutoCrossfadeDurationMs(currentVideoId, nextVideoId ?: "")
                                        } else {
                                            crossfadeDurationMs
                                        }
                                    val triggerThreshold = resolvedDurationMs.toLong() + preparationBufferMs
                                    if (timeRemaining in 1..triggerThreshold) {
                                        if (hasNextMediaItem()) {
                                            val nextIndex = getNextMediaItemIndex()
                                            triggerCrossfadeTransition(nextIndex)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore query errors - don't log to avoid spam
                    }

                    delay(200) // Update every 200ms
                }
            }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // ========== Internal: Precaching ==========

    /**
     * Trigger precaching - mirrors GstreamerPlayerAdapter.triggerPrecachingInternal()
     *
     * Creates ExoPlayer instances for upcoming tracks. Each player gets a MediaItem
     * and calls prepare(), which triggers URL resolution and buffering via MediaSourceFactory.
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
                                PlayerConstants.REPEAT_MODE_ALL -> {
                                    (index + i) % playlist.size
                                }
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

                        try {
                            val pwf = createExoPlayerInstance()
                            pwf.player.setMediaItem(mediaItem.toMedia3MediaItem())
                            pwf.player.prepare()
                            precachedPlayers[mediaItem.mediaId] = PrecachedPlayer(pwf.player, mediaItem, pwf.filter)
                            Logger.d(TAG, "Precached player for index $idx")
                        } catch (e: Exception) {
                            Logger.e(TAG, "Precaching error for $idx: ${e.message}")
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

    private fun cancelPrecaching() {
        precacheJob?.cancel()
        precacheJob = null
    }

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

    private fun clearAllPrecacheInternal() {
        Logger.d(TAG, "Clearing all precache")
        precachedPlayers.values.forEach { cleanupPlayerInternal(it.player) }
        precachedPlayers.clear()
    }

    // ========== Internal: Notifications ==========

    private fun notifyEqualizerIntent(shouldOpen: Boolean) {
        listeners.forEach { it.shouldOpenOrCloseEqualizerIntent(shouldOpen) }
    }

    // ========== Internal: Shuffle Management ==========
    // Mirrors GstreamerPlayerAdapter shuffle management exactly

    private fun createShuffleOrder() {
        if (playlist.isEmpty()) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        val indices = playlist.indices.toMutableList()

        val currentIndex = localCurrentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        indices.shuffle()

        if (currentIndex in playlist.indices) {
            indices.add(0, currentIndex)
        }

        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
        Logger.d(TAG, "Cleared shuffle order")
    }

    private fun insertIntoShuffleOrder(
        insertedOriginalIndex: Int,
        afterShufflePos: Int,
    ) {
        if (playlist.isEmpty() || insertedOriginalIndex !in playlist.indices) {
            return
        }

        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] >= insertedOriginalIndex) {
                shuffleOrder[i]++
            }
        }

        val insertPos = (afterShufflePos + 1).coerceIn(0, shuffleOrder.size)
        shuffleOrder.add(insertPos, insertedOriginalIndex)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }

        Logger.d(
            TAG,
            "Inserted index $insertedOriginalIndex into shuffle at position $insertPos (after shuffle pos $afterShufflePos)",
        )
    }

    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        if (!internalShuffleModeEnabled || shuffleOrder.isEmpty()) {
            return playlist.toList()
        }
        return shuffleOrder.mapNotNull { playlist.getOrNull(it) }
    }

    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        listeners.forEach { it.onTimelineChanged(list, reason) }
    }
}
