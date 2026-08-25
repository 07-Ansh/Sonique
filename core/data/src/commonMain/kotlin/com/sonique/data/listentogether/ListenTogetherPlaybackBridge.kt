package com.sonique.data.listentogether

import com.sonique.common.MERGING_DATA_TYPE
import com.sonique.domain.data.model.browse.album.Track
import com.sonique.domain.mediaservice.handler.MediaPlayerHandler
import com.sonique.domain.mediaservice.handler.QueueData
import com.sonique.domain.mediaservice.handler.SimpleMediaState
import com.sonique.domain.data.model.listentogether.RoomTrack
import com.sonique.domain.data.player.GenericMediaItem
import com.sonique.domain.data.player.GenericMediaMetadata
import com.sonique.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.sonique.domain.repository.ListenTogetherRepository
import com.sonique.listentogether.ListenTogetherSession
import com.sonique.listentogether.PlaybackActions
import com.sonique.listentogether.TrackInfo

private const val TAG = "ListenTogetherBridge"

/** What the guest reacts to. A data class so `distinctUntilChanged` compares every field. */
private data class RoomSnapshot(
    val track: RoomTrack?,
    val isPlaying: Boolean,
    val position: Long,
    val queueIds: List<String>,
)

/**
 * Joins a Listen Together room to the local player.
 *
 * Lives in `commonMain` and talks only to [MediaPlayerHandler], which is the one interface both
 * platforms implement — Android's `MediaServiceHandlerImpl` and Desktop's
 * `JvmMediaPlayerHandlerImpl` share nothing else, so anything written against a concrete player
 * would have to be written twice and would drift.
 *
 * Direction of travel is decided entirely by who hosts:
 * - **Host** watches the local player and publishes what it does.
 * - **Guest** watches the room and applies what the host did, and publishes nothing.
 *
 * The [applyingRemote] guard is what stops those two from feeding each other: applying a remote
 * pause makes the local player report "paused", which would otherwise be published straight back
 * to the server as a fresh command.
 */
class ListenTogetherPlaybackBridge(
    /**
     * Read through the repository (domain types), written through the session (protocol types).
     *
     * The split is deliberate: reacting to a room is app logic and belongs on our own model, while
     * publishing a command is protocol detail that must not leak upward — putting
     * `sendPlaybackAction` on the domain interface would drag ListenTogether's schema into domain.
     */
    private val repository: ListenTogetherRepository,
    private val session: ListenTogetherSession,
    private val handler: MediaPlayerHandler,
    private val scope: CoroutineScope,
) {
    private var started = false
    private var applyingRemote = false
    private var lastPublishedTrackId: String? = null
    private var lastAppliedTrackId: String? = null
    private var lastAppliedQueueIds: List<String> = emptyList()

    /**
     * Whether the room was playing before the command currently being applied.
     *
     * Needed because change_track always says "not playing" — see [watchRoomForGuests].
     */
    private var lastRoomPlaying = false

    /** Idempotent: callers cannot know whether something else already started it. */
    fun start() {
        if (started) return
        started = true
        Logger.i(TAG, "Playback bridge started")
        scope.launch { suppressCrossfadeWhileInRoom() }
        scope.launch { watchRoomForGuests() }
        scope.launch { resyncGuestOnResume() }
        scope.launch { publishCurrentStateOnJoin() }
        scope.launch { publishStateWhenSomeoneArrives() }
        scope.launch { publishQueueAsHost() }
        scope.launch { publishTrackChangesAsHost() }
        scope.launch { publishPlayPauseAsHost() }
        scope.launch { publishSeeksAsHost() }
        scope.launch { answerBufferBarrier() }
    }

    /**
     * Crossfade overlaps two tracks for seconds, which drifts a room apart at every transition.
     *
     * The user's own setting is left alone — this is a separate override on the player, the same
     * shape as the sleep-timer fade, so a process death mid-room cannot lose their preference.
     */
    private suspend fun suppressCrossfadeWhileInRoom() {
        repository.room
            .map { it.inRoom }
            .distinctUntilChanged()
            .collect { inRoom ->
                withContext(Dispatchers.Main) { handler.player.crossfadeSuppressed = inRoom }
                if (!inRoom) {
                    lastRoomPlaying = false
                } else if (!repository.room.value.isHost) {
                    // Ask for the live position the moment we are in. The state pushed on join
                    // carries the position as of the host's LAST command, which can be minutes old
                    // — obeying it drops a joiner at the start of a song everyone else is halfway
                    // through. sync_state answers with where the room actually is.
                    repository.requestSync()
                }
                Logger.i(TAG, if (inRoom) "Crossfade suppressed for the room" else "Crossfade restored")
            }
    }

    /**
     * A guest may pause, and stays paused.
     *
     * An earlier version forced the guest straight back to the room's state, which made pause
     * impossible — press it and playback resumed instantly. The room is something you listen along
     * with, not something that holds your transport hostage: pausing is local and silent, and
     * pressing play again asks the server where the room actually is now, so resuming lands in the
     * right place instead of wherever this device stopped. This is what ListenTogether's manager does
     * (`requestSync` — "call this when a guest presses play/pause").
     */
    private suspend fun resyncGuestOnResume() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { locallyPlaying ->
                val room = repository.room.value
                if (!room.inRoom || room.isHost || applyingRemote) return@collect
                if (!locallyPlaying) {
                    Logger.i(TAG, "Guest paused locally — leaving the room running")
                    return@collect
                }
                Logger.i(TAG, "Guest resumed — asking the server where the room is")
                repository.requestSync()
            }
    }


    private suspend fun watchRoomForGuests() {
        repository.room
            // The queue is part of the key: without it a room state that changed ONLY its queue
            // compares equal here and is dropped, so the guest never builds the host's queue at all.
            .map {
                RoomSnapshot(
                    track = it.currentTrack,
                    isPlaying = it.isPlaying,
                    position = it.position,
                    queueIds = it.queue.map { t -> t.id },
                ) to (it.inRoom && !it.isHost)
            }
            .distinctUntilChanged()
            .collect { (snapshot, shouldFollow) ->
                if (!shouldFollow) return@collect
                val (track, isPlaying, position, queueIds) = snapshot
                Logger.i(TAG, "Room says: track=${track?.id} playing=$isPlaying pos=$position queue=${queueIds.size}")
                applyingRemote = true
                try {
                    // Rebuild when the track changes OR when the queue behind it does — the queue
                    // can legitimately arrive while the same track keeps playing.
                    val queueChanged = queueIds != lastAppliedQueueIds
                    val trackChanged =
                        track != null && track.id.isNotBlank() && (track.id != lastAppliedTrackId || queueChanged)
                    // The server forces IsPlaying=false on EVERY change_track — protocol default,
                    // not the host pausing. Obeying it pauses the track just loaded, which is why
                    // the guest sat silent on next, prev AND end-of-song alike. Carry the room's
                    // previous intent across the change; a real pause arrives as its own command,
                    // with the track unchanged, and is applied normally.
                    val playing = if (trackChanged && !isPlaying) lastRoomPlaying else isPlaying
                    lastRoomPlaying = playing
                    if (trackChanged && track != null) {
                        val sameTrack = track.id == lastAppliedTrackId
                        lastAppliedTrackId = track.id
                        lastAppliedQueueIds = queueIds
                        // Decided BEFORE loading, not corrected afterwards: loading with a hardcoded
                        // playWhenReady=true and letting applyTransport pause it is a race, and the
                        // guest wins it by starting to play in a room the host has paused.
                        // A NEW track still starts where the room is, not at zero: someone joining
                        // a room mid-song must land next to everyone else. Only the same track
                        // being rebuilt (the queue arrived late) keeps the local playhead.
                        val startAt =
                            if (sameTrack) {
                                handler.player.currentPosition
                            } else {
                                session.positionAt(position, playing)
                            }
                        playTrack(track, keepPosition = startAt, playWhenReady = playing)
                    }
                    applyTransport(playing, position)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to apply remote state: ${e.message}")
                } finally {
                    applyingRemote = false
                }
            }
    }

    private suspend fun applyTransport(
        isPlaying: Boolean,
        position: Long,
    ) = withContext(Dispatchers.Main) {
        // Correct the position for however long the command spent in flight; ServerClock falls back
        // to the raw value whenever it is not calibrated yet.
        val corrected = session.positionAt(position, isPlaying)
        // A small drift is normal and seeking on every tick would stutter; only a real gap is worth
        // a seek, which is also why the host publishes position with each command.
        if (abs(handler.player.currentPosition - corrected) > SEEK_TOLERANCE_MS) {
            handler.player.seekTo(corrected)
        }
        // playWhenReady, not isPlaying: a track that is still buffering reports isPlaying=false
        // while already committed to playing, so comparing against it re-issues play() every tick
        // and, worse, lets a stale pause land on a track that was about to start.
        if (isPlaying && !handler.player.playWhenReady) {
            handler.player.play()
        } else if (!isPlaying && handler.player.playWhenReady) {
            handler.player.pause()
        }
    }

    /**
     * Loads the host's track locally.
     *
     * The room only carries a videoId, so the guest resolves its own metadata and stream — which is
     * exactly why two clients on different platforms, or a ListenTogether and a ListenTogether client, can
     * share a room at all: both read the same catalogue rather than shipping audio to each other.
     */
    /**
     * Loads the host's track and queue.
     *
     * Built straight from the room's own [TrackInfo], the way ListenTogether's manager does
     * (`TrackInfo.toMediaMetadata().toMediaItem()`) — deliberately NOT by resolving metadata from
     * the catalogue first. Two reasons, both learned the hard way:
     *
     * 1. `Track.toGenericMediaItem()` GUESSES song-vs-video from the artwork aspect ratio and
     *    treats the `maxresdefault.jpg` fallback as video. A guest resolving its own metadata lands
     *    on that branch almost every time, ends up on the merged audio+video path, and gets video
     *    with no sound where the host has plain audio.
     * 2. A network round trip per track can hang; `first { it.data != null }` on a flow that never
     *    carries data blocks the whole collector, and with it every later room command.
     *
     * The stream itself is still resolved locally by the player — the room only ever carries ids.
     */
    private suspend fun playTrack(
        info: RoomTrack,
        keepPosition: Long = 0L,
        playWhenReady: Boolean,
    ) {
        val roomQueue = repository.room.value.queue
        // ListenTogether's canonicalPlaybackQueue: the current track leads, upcoming follows, no dupes.
        val ordered =
            (listOf(info) + roomQueue.filter { it.id != info.id })
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
        Logger.i(TAG, "Guest loading ${info.id} (${info.title}) + ${ordered.size - 1} upcoming")

        // Dispatchers.Main is mandatory, not tidiness: Media3 throws if the player is touched off
        // the main thread, and the bridge runs on the service scope (Default).
        withContext(Dispatchers.Main) {
            handler.clearMediaItems()
            handler.addMediaItem(ordered.first().toRoomMediaItem(), playWhenReady = playWhenReady)
            val rest = ordered.drop(1)
            if (rest.isNotEmpty()) handler.addMediaItemList(rest.map { it.toRoomMediaItem() })
            // Rebuilding restarts the track; if only the queue changed, put the playhead back.
            if (keepPosition > 0L) handler.player.seekTo(keepPosition)
        }
    }

    /**
     * A room track as a media item.
     *
     * `MERGING_DATA_TYPE.SONG` is set explicitly: inside a room every client must be on the same
     * rendition, and for listening together that rendition is audio. Leaving it to be inferred is
     * what produced video-with-no-sound on the guest.
     */
    private fun RoomTrack.toRoomMediaItem(): GenericMediaItem =
        GenericMediaItem(
            mediaId = id,
            uri = id,
            metadata =
                GenericMediaMetadata(
                    title = title,
                    artist = artist.ifBlank { null },
                    albumTitle = album.ifBlank { null },
                    artworkUri = thumbnail.ifBlank { null },
                    description = MERGING_DATA_TYPE.SONG,
                ),
            customCacheKey = id,
        )


    /**
     * Publishes what is ALREADY playing the moment we become host.
     *
     * Everything else here reacts to a *change* — a track transition, a play/pause. Someone who
     * was already listening and then opens a room produces neither, so without this the room has
     * no state at all and every guest sits in silence waiting for a command that only arrives if
     * the host happens to touch the transport.
     */
    private suspend fun publishCurrentStateOnJoin() {
        repository.room
            .map { it.inRoom && it.isHost }
            .distinctUntilChanged()
            .collect { isHosting -> if (isHosting) publishSnapshot() }
    }

    /**
     * Re-publishes for a guest who arrives later.
     *
     * The server keeps the room's last known state, but only what the host has told it; a guest
     * approved before the host's first command would otherwise join an empty room.
     */
    private suspend fun publishStateWhenSomeoneArrives() {
        repository.room
            .map { it.members.size }
            .distinctUntilChanged()
            .collect { count ->
                val state = repository.room.value
                if (state.inRoom && state.isHost && count > 1) publishSnapshot()
            }
    }

    /** Republishes the queue whenever the host's own queue changes. */
    private suspend fun publishQueueAsHost() {
        handler.queueData
            .map { (it as? QueueData.Data)?.listTracks?.map { t -> t.videoId }.orEmpty() }
            .distinctUntilChanged()
            .collect { ids ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote || ids.isEmpty()) return@collect
                publishQueue()
            }
    }

    private fun publishQueue() {
        val data = handler.queueData.value as? QueueData.Data ?: return
        val tracks = data.listTracks.map { it.toTrackInfo() }
        if (tracks.isEmpty()) return
        session.sendQueue(tracks, data.playlistName.orEmpty())
        Logger.i(TAG, "Published queue of ${tracks.size} track(s)")
    }

    private fun publishSnapshot() {
        val item = handler.nowPlaying.value ?: return
        if (item.mediaId.isBlank()) return
        lastPublishedTrackId = item.mediaId
        val data = handler.queueData.value as? QueueData.Data
        session.sendPlaybackAction(
            action = PlaybackActions.CHANGE_TRACK,
            trackId = item.mediaId,
            position = handler.player.currentPosition,
            trackInfo = item.toTrackInfo(),
            queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
            queueTitle = data?.playlistName.orEmpty(),
        )
        // A second command, because change_track alone does not say whether it is running —
        // the server explicitly sets IsPlaying=false on a track change.
        session.sendPlaybackAction(
            action = if (handler.player.playWhenReady) PlaybackActions.PLAY else PlaybackActions.PAUSE,
            trackId = "",
            position = handler.player.currentPosition,
            trackInfo = null,
        )
        Logger.i(TAG, "Published current state to the room: ${item.mediaId}")
    }

    private suspend fun publishTrackChangesAsHost() {
        handler.nowPlaying
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.mediaId == new.mediaId }
            .collect { item ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                if (item.mediaId == lastPublishedTrackId) return@collect
                lastPublishedTrackId = item.mediaId
                Logger.i(TAG, "Host publishing track change: ${item.mediaId}")
                val data = handler.queueData.value as? QueueData.Data
                session.sendPlaybackAction(
                    action = PlaybackActions.CHANGE_TRACK,
                    trackId = item.mediaId,
                    position = 0L,
                    trackInfo = item.toTrackInfo(),
                    queue = data?.listTracks.orEmpty().map { it.toTrackInfo() },
                    queueTitle = data?.playlistName.orEmpty(),
                )
                // change_track alone leaves the room paused: the server sets IsPlaying=false on
                // every track change. The host's own controlState does NOT change when one playing
                // track follows another, so nothing else would ever send this and guests would load
                // each new track and sit there stopped.
                //
                // playWhenReady, NOT isPlaying. isPlaying means "audio is coming out right now" and
                // is false while the new track buffers — which is exactly when this runs. Asking
                // isPlaying here made the PLAY never get sent on precisely the transitions it
                // exists for. playWhenReady is the intent to play and stays true across the gap.
                if (handler.player.playWhenReady) {
                    session.sendPlaybackAction(
                        action = PlaybackActions.PLAY,
                        trackId = "",
                        position = 0L,
                        trackInfo = null,
                    )
                }
            }
    }

    private suspend fun publishPlayPauseAsHost() {
        handler.controlState
            .map { it.isPlaying }
            .distinctUntilChanged()
            .collect { isPlaying ->
                val state = repository.room.value
                if (!state.inRoom || !state.isHost || applyingRemote) return@collect
                // A host that merely buffers reports isPlaying=false, indistinguishable from a
                // user pause — and publishing it stops the WHOLE room on one device's hiccup.
                // playWhenReady carries the intent, so a dip where the two disagree is not news.
                val intent = handler.player.playWhenReady
                if (isPlaying != intent) return@collect
                Logger.i(TAG, "Host publishing ${if (intent) "PLAY" else "PAUSE"}")
                session.sendPlaybackAction(
                    action = if (intent) PlaybackActions.PLAY else PlaybackActions.PAUSE,
                    // Deliberately EMPTY. The server rejects a play/pause whose trackId does not
                    // match the track it is holding ("stale_track") and drops it silently; sending
                    // nothing makes it fill in its own current track, which is always right.
                    trackId = "",
                    position = handler.player.currentPosition,
                    trackInfo = null,
                )
            }
    }

    /**
     * Publishes a seek.
     *
     * Neither `nowPlaying` nor `controlState` changes when the host drags the scrubber, so without
     * this a seek is simply never sent and guests keep playing from wherever they were.
     *
     * Detected off `SimpleMediaState.Progress`, which both platforms already emit — ListenTogether uses
     * Media3's `onPositionDiscontinuity(DISCONTINUITY_REASON_SEEK)`, but that is an Android-only
     * API and Desktop runs mpv. A seek is a position that moved further than wall-clock time could
     * account for; ordinary playback advances roughly in step with it.
     */
    private suspend fun publishSeeksAsHost() {
        var lastProgress = 0L
        var lastAt = 0L
        handler.simpleMediaState.collect { mediaState ->
            val progress = (mediaState as? SimpleMediaState.Progress)?.progress ?: return@collect
            val now = PROCESS_START.elapsedNow().inWholeMilliseconds
            val previous = lastProgress
            val previousAt = lastAt
            lastProgress = progress
            lastAt = now

            val state = repository.room.value
            if (!state.inRoom || !state.isHost || applyingRemote) return@collect
            if (previousAt == 0L) return@collect

            val elapsed = now - previousAt
            val expected = previous + if (handler.player.isPlaying) elapsed else 0L
            if (kotlin.math.abs(progress - expected) < SEEK_DETECT_MS) return@collect

            Logger.i(TAG, "Host publishing SEEK to $progress (expected ~$expected)")
            session.sendPlaybackAction(
                action = PlaybackActions.SEEK,
                trackId = "",
                position = progress,
                trackInfo = null,
            )
        }
    }


    /**
     * Answers `buffer_ready` once the local player has the track the room is waiting on.
     *
     * Nobody in the room hears anything until every member answers, so a client that never sends
     * this silently freezes playback for everyone — including the host.
     */
    private suspend fun answerBufferBarrier() {
        repository.room
            .map { it.waitingFor to it.currentTrack?.id }
            .distinctUntilChanged()
            .collect { (waitingFor, trackId) ->
                val state = repository.room.value
                if (trackId.isNullOrBlank() || !state.inRoom) return@collect
                if (state.selfUserId !in waitingFor) return@collect
                // bufferedPercentage, not isPlaying: the barrier asks whether the track is loaded,
                // and playback is exactly what it is holding back.
                if (handler.player.bufferedPercentage >= READY_BUFFER_PERCENT) {
                    session.reportBufferReady(trackId)
                }
            }
    }

    private fun RoomTrack.toProtocol(): TrackInfo =
        TrackInfo(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = durationMs,
            thumbnail = thumbnail,
        )

    private fun Track.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = videoId,
            title = title,
            artist = artists?.joinToString(", ") { it.name }.orEmpty(),
            album = album?.name.orEmpty(),
            duration = (durationSeconds?.toLong() ?: 0L) * 1000L,
            thumbnail = thumbnails?.lastOrNull()?.url.orEmpty(),
        )

    /** The room carries a videoId plus display metadata; the guest resolves its own stream. */
    private fun GenericMediaItem.toTrackInfo(): TrackInfo =
        TrackInfo(
            id = mediaId,
            title = metadata.title.orEmpty(),
            artist = metadata.artist.orEmpty(),
            album = metadata.albumTitle.orEmpty(),
            duration = handler.player.duration.coerceAtLeast(0L),
            thumbnail = metadata.artworkUri.orEmpty(),
        )

    private companion object {
        /** Monotonic reference for telling a seek apart from ordinary playback advancing. */
        val PROCESS_START = TimeSource.Monotonic.markNow()

        /**
         * ListenTogether's own hard-sync threshold (`HARD_SYNC_THRESHOLD_MS`). Below it a seek costs
         * more in stutter than it buys in sync; above it the room is audibly apart.
         */
        const val SEEK_TOLERANCE_MS = 750L

        /**
         * A jump larger than this is a seek rather than playback advancing. Comfortably above the
         * progress tick interval so ordinary drift never trips it.
         */
        const val SEEK_DETECT_MS = 2_500L
        const val READY_BUFFER_PERCENT = 5
    }
}
