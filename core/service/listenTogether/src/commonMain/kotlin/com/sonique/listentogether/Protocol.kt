@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.sonique.listentogether

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Envelope `type` values. Client → server first, then server → client. */
object MessageTypes {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LEAVE_ROOM = "leave_room"
    const val APPROVE_JOIN = "approve_join"
    const val REJECT_JOIN = "reject_join"
    const val PLAYBACK_ACTION = "playback_action"
    const val BUFFER_READY = "buffer_ready"
    const val KICK_USER = "kick_user"
    const val TRANSFER_HOST = "transfer_host"
    const val PING = "ping"
    const val CHAT = "chat"
    const val REQUEST_SYNC = "request_sync"
    const val RECONNECT = "reconnect"
    const val SUGGEST_TRACK = "suggest_track"
    const val APPROVE_SUGGESTION = "approve_suggestion"
    const val REJECT_SUGGESTION = "reject_suggestion"

    /**
     * Capability negotiation rides in an ordinary [Envelope] like everything else — the `.proto`
     * only says "first message from client" and never names the type, so these two strings come
     * from the server itself: the room server `internal/server/protocol.go`, `MsgTypeClientCapabilities`
     * / `MsgTypeServerCapabilities`. Getting either one wrong is answered with `unknown_message_type`
     * and the handshake simply never completes.
     */
    const val CLIENT_CAPABILITIES = "client_capabilities"

    const val ROOM_CREATED = "room_created"
    const val JOIN_REQUEST = "join_request"
    const val JOIN_APPROVED = "join_approved"
    const val JOIN_REJECTED = "join_rejected"
    const val USER_JOINED = "user_joined"
    const val USER_LEFT = "user_left"
    const val SYNC_PLAYBACK = "sync_playback"
    const val BUFFER_WAIT = "buffer_wait"
    const val BUFFER_COMPLETE = "buffer_complete"
    const val ERROR = "error"
    const val PONG = "pong"
    const val HOST_CHANGED = "host_changed"
    const val KICKED = "kicked"
    const val SYNC_STATE = "sync_state"
    const val RECONNECTED = "reconnected"
    const val USER_RECONNECTED = "user_reconnected"
    const val USER_DISCONNECTED = "user_disconnected"
    const val SUGGESTION_RECEIVED = "suggestion_received"
    const val SUGGESTION_APPROVED = "suggestion_approved"
    const val SUGGESTION_REJECTED = "suggestion_rejected"

    /** The server's half of the handshake — see [CLIENT_CAPABILITIES]. */
    const val SERVER_CAPABILITIES = "server_capabilities"
}

/** Values of [PlaybackActionPayload.action]. */
object PlaybackActions {
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val SKIP_NEXT = "skip_next"
    const val SKIP_PREV = "skip_prev"
    const val CHANGE_TRACK = "change_track"
    const val QUEUE_ADD = "queue_add"
    const val QUEUE_REMOVE = "queue_remove"
    const val QUEUE_CLEAR = "queue_clear"
    const val SYNC_QUEUE = "sync_queue"
    const val SET_VOLUME = "set_volume"
}

/**
 * The frame every message travels in.
 *
 * [compressed] says whether [payload] was gzipped before being placed here — see
 * [MessageCodec.COMPRESSION_THRESHOLD] for when that happens.
 */
@Serializable
data class Envelope(
    @ProtoNumber(1) val type: String = "",
    @ProtoNumber(2) val payload: ByteArray = ByteArray(0),
    @ProtoNumber(3) val compressed: Boolean = false,
) {
    // ByteArray gives identity equality by default, which would make two envelopes carrying the
    // same bytes compare unequal — and the round-trip tests compare envelopes.
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is Envelope &&
                    type == other.type &&
                    compressed == other.compressed &&
                    payload.contentEquals(other.payload)
            )

    override fun hashCode(): Int = (type.hashCode() * 31 + payload.contentHashCode()) * 31 + compressed.hashCode()
}

/**
 * One track, as the room sees it.
 *
 * [id] is the YouTube videoId, which is why two different clients resolve the same row: both read
 * the same catalogue. Everything else is display metadata.
 */
@Serializable
data class TrackInfo(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val title: String = "",
    @ProtoNumber(3) val artist: String = "",
    @ProtoNumber(4) val album: String = "",
    /** Milliseconds. */
    @ProtoNumber(5) val duration: Long = 0L,
    @ProtoNumber(6) val thumbnail: String = "",
    @ProtoNumber(7) val suggestedBy: String = "",
)

@Serializable
data class UserInfo(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
    @ProtoNumber(3) val isHost: Boolean = false,
    @ProtoNumber(4) val isConnected: Boolean = false,
)

@Serializable
data class RoomState(
    @ProtoNumber(1) val roomCode: String = "",
    @ProtoNumber(2) val hostId: String = "",
    @ProtoNumber(3) val users: List<UserInfo> = emptyList(),
    @ProtoNumber(4) val currentTrack: TrackInfo? = null,
    @ProtoNumber(5) val isPlaying: Boolean = false,
    @ProtoNumber(6) val position: Long = 0L,
    @ProtoNumber(7) val lastUpdate: Long = 0L,
    @ProtoNumber(8) val volume: Float = 0f,
    @ProtoNumber(9) val queue: List<TrackInfo> = emptyList(),
    @ProtoNumber(10) val revision: Long = 0L,
)


@Serializable
data class CreateRoomPayload(
    @ProtoNumber(1) val username: String = "",
)

@Serializable
data class JoinRoomPayload(
    @ProtoNumber(1) val roomCode: String = "",
    @ProtoNumber(2) val username: String = "",
)

@Serializable
class LeaveRoomPayload

@Serializable
data class ApproveJoinPayload(
    @ProtoNumber(1) val userId: String = "",
)

@Serializable
data class RejectJoinPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val reason: String = "",
)

/**
 * Every transport command, and the only message whose timing fields matter.
 *
 * [serverTime] and [capturedAtServerTime] are what let a late-arriving PLAY still land on the right
 * position: the receiver advances [position] by however long the frame spent in flight, measured on
 * the SERVER's clock rather than its own. See [ServerClock.positionAt].
 */
@Serializable
data class PlaybackActionPayload(
    @ProtoNumber(1) val action: String = "",
    @ProtoNumber(2) val trackId: String = "",
    /** Milliseconds. */
    @ProtoNumber(3) val position: Long = 0L,
    @ProtoNumber(4) val trackInfo: TrackInfo? = null,
    @ProtoNumber(5) val insertNext: Boolean = false,
    @ProtoNumber(6) val queue: List<TrackInfo> = emptyList(),
    @ProtoNumber(7) val queueTitle: String = "",
    @ProtoNumber(8) val volume: Float = 0f,
    @ProtoNumber(9) val serverTime: Long = 0L,
    @ProtoNumber(10) val revision: Long = 0L,
    @ProtoNumber(11) val capturedAtServerTime: Long = 0L,
)

@Serializable
data class PingPayload(
    @ProtoNumber(1) val clientTime: Long = 0L,
    @ProtoNumber(2) val sequence: Long = 0L,
)

@Serializable
data class BufferReadyPayload(
    @ProtoNumber(1) val trackId: String = "",
)

@Serializable
data class KickUserPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val reason: String = "",
)

@Serializable
data class TransferHostPayload(
    @ProtoNumber(1) val newHostId: String = "",
)

@Serializable
data class SuggestTrackPayload(
    @ProtoNumber(1) val trackInfo: TrackInfo? = null,
)

@Serializable
data class ApproveSuggestionPayload(
    @ProtoNumber(1) val suggestionId: String = "",
)

@Serializable
data class RejectSuggestionPayload(
    @ProtoNumber(1) val suggestionId: String = "",
    @ProtoNumber(2) val reason: String = "",
)

@Serializable
data class ReconnectPayload(
    @ProtoNumber(1) val sessionToken: String = "",
)


@Serializable
data class RoomCreatedPayload(
    @ProtoNumber(1) val roomCode: String = "",
    @ProtoNumber(2) val userId: String = "",
    @ProtoNumber(3) val sessionToken: String = "",
)

@Serializable
data class JoinRequestPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
)

@Serializable
data class JoinApprovedPayload(
    @ProtoNumber(1) val roomCode: String = "",
    @ProtoNumber(2) val userId: String = "",
    @ProtoNumber(3) val sessionToken: String = "",
    @ProtoNumber(4) val state: RoomState? = null,
)

@Serializable
data class JoinRejectedPayload(
    @ProtoNumber(1) val reason: String = "",
)

@Serializable
data class UserJoinedPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
)

@Serializable
data class UserLeftPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
)

/**
 * Nobody hears anything until everyone in [waitingFor] has answered [BufferReadyPayload].
 *
 * This is the whole synchronisation model, and the reason the UI has to name who it is waiting for:
 * playback genuinely stops for the slowest device, and silence with no explanation reads as a hang.
 */
@Serializable
data class BufferWaitPayload(
    @ProtoNumber(1) val trackId: String = "",
    @ProtoNumber(2) val waitingFor: List<String> = emptyList(),
)

@Serializable
data class BufferCompletePayload(
    @ProtoNumber(1) val trackId: String = "",
)

@Serializable
data class ErrorPayload(
    @ProtoNumber(1) val code: String = "",
    @ProtoNumber(2) val message: String = "",
)

@Serializable
data class HostChangedPayload(
    @ProtoNumber(1) val newHostId: String = "",
    @ProtoNumber(2) val newHostName: String = "",
)

@Serializable
data class KickedPayload(
    @ProtoNumber(1) val reason: String = "",
)

@Serializable
data class SyncStatePayload(
    @ProtoNumber(1) val currentTrack: TrackInfo? = null,
    @ProtoNumber(2) val isPlaying: Boolean = false,
    @ProtoNumber(3) val position: Long = 0L,
    @ProtoNumber(4) val lastUpdate: Long = 0L,
    @ProtoNumber(5) val queue: List<TrackInfo> = emptyList(),
    @ProtoNumber(6) val volume: Float = 0f,
    @ProtoNumber(7) val revision: Long = 0L,
)

@Serializable
data class PongPayload(
    @ProtoNumber(1) val clientTime: Long = 0L,
    @ProtoNumber(2) val serverReceiveTime: Long = 0L,
    @ProtoNumber(3) val serverSendTime: Long = 0L,
    @ProtoNumber(4) val sequence: Long = 0L,
)

@Serializable
data class ReconnectedPayload(
    @ProtoNumber(1) val roomCode: String = "",
    @ProtoNumber(2) val userId: String = "",
    @ProtoNumber(3) val state: RoomState? = null,
    @ProtoNumber(4) val isHost: Boolean = false,
)

@Serializable
data class UserReconnectedPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
)

@Serializable
data class UserDisconnectedPayload(
    @ProtoNumber(1) val userId: String = "",
    @ProtoNumber(2) val username: String = "",
)

@Serializable
data class SuggestionReceivedPayload(
    @ProtoNumber(1) val suggestionId: String = "",
    @ProtoNumber(2) val fromUserId: String = "",
    @ProtoNumber(3) val fromUsername: String = "",
    @ProtoNumber(4) val trackInfo: TrackInfo? = null,
)

@Serializable
data class SuggestionApprovedPayload(
    @ProtoNumber(1) val suggestionId: String = "",
    @ProtoNumber(2) val trackInfo: TrackInfo? = null,
)

@Serializable
data class SuggestionRejectedPayload(
    @ProtoNumber(1) val suggestionId: String = "",
    @ProtoNumber(2) val reason: String = "",
)


/**
 * Sent first, before anything else.
 *
 * This is the protocol's only version signal, so it is also the only place a future the room protocol
 * change can be detected rather than merely suffered.
 */
@Serializable
data class ClientCapabilities(
    @ProtoNumber(1) val supportsProtobuf: Boolean = false,
    @ProtoNumber(2) val supportsCompression: Boolean = false,
    @ProtoNumber(3) val clientVersion: String = "",
)

@Serializable
data class ServerCapabilities(
    @ProtoNumber(1) val supportsProtobuf: Boolean = false,
    @ProtoNumber(2) val supportsCompression: Boolean = false,
    @ProtoNumber(3) val serverVersion: String = "",
)
