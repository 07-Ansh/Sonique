package com.sonique.listentogether

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.GzipSink
import okio.GzipSource
import okio.buffer

/**
 * Turns a payload into the bytes that go on the socket, and back.
 *
 * Wire format, exactly as the room protocol and the servers speak it: the payload is protobuf-encoded,
 * gzipped when it is worth it, then wrapped in an [Envelope] carrying the message type and a
 * `compressed` flag — and the envelope itself is protobuf.
 *
 * The 250-odd lines of `toProtoMessage` / `protoToX` in the original are absent here on purpose:
 * they exist to bridge hand-written Kotlin data classes to protoc-generated Java builders, and
 * `Protocol.kt`'s `@ProtoNumber` annotations remove that gap entirely. Same bytes, one model.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class MessageCodec(
    private val compressionEnabled: Boolean = true,
) {
    /**
     * `encodeDefaults = false` is not a preference — it is what proto3 means.
     *
     * A proto3 field holding its default value is simply absent from the wire, which is what
     * `protoc` emits and what Go's `proto.Marshal` emits, so this is the setting that makes the
     * "same bytes as protoc" claim in Protocol.kt actually true. With `true`, encoding any payload
     * carrying a null message field — `PlaybackActionPayload.trackInfo` on every play, pause, seek
     * and volume command — threw `'null' is not supported for optional properties in ProtoBuf`
     * instead, so no transport command could be sent at all.
     */
    private val proto = ProtoBuf { encodeDefaults = false }

    /** Encodes one message into a complete frame. */
    fun encode(
        msgType: String,
        payload: Any?,
    ): ByteArray {
        val payloadBytes = payload?.let { encodePayload(it) } ?: ByteArray(0)
        // Below the threshold gzip reliably makes the frame LARGER — its header alone is 10 bytes,
        // and most of these messages are a handful of fields.
        val compress = compressionEnabled && payloadBytes.size > COMPRESSION_THRESHOLD
        val body = if (compress) gzip(payloadBytes) else payloadBytes
        return proto.encodeToByteArray(
            Envelope.serializer(),
            Envelope(type = msgType, payload = body, compressed = compress),
        )
    }

    /**
     * Unwraps a frame into its type and its still-encoded payload.
     *
     * Decompression failure returns the payload untouched rather than throwing: the flag is set by
     * the sender, and a frame we cannot inflate is more likely mislabelled than fatal.
     */
    fun decode(data: ByteArray): Pair<String, ByteArray> {
        val envelope = proto.decodeFromByteArray(Envelope.serializer(), data)
        val body = if (envelope.compressed) gunzip(envelope.payload) ?: envelope.payload else envelope.payload
        return envelope.type to body
    }

    /**
     * Decodes a payload once its type is known.
     *
     * An unknown type returns null instead of throwing — the room protocol may add message types before we
     * do, and a client that dies on an unrecognised frame cannot share a room with a newer one.
     * `chat` is deliberately in this list even though Sonique draws no chat UI: a other user
     * in the room WILL send it.
     */
    fun decodePayload(
        msgType: String,
        payloadBytes: ByteArray,
    ): Any? =
        when (msgType) {
            MessageTypes.CREATE_ROOM -> decode(CreateRoomPayload.serializer(), payloadBytes)
            MessageTypes.JOIN_ROOM -> decode(JoinRoomPayload.serializer(), payloadBytes)
            MessageTypes.APPROVE_JOIN -> decode(ApproveJoinPayload.serializer(), payloadBytes)
            MessageTypes.REJECT_JOIN -> decode(RejectJoinPayload.serializer(), payloadBytes)
            MessageTypes.PLAYBACK_ACTION, MessageTypes.SYNC_PLAYBACK ->
                decode(PlaybackActionPayload.serializer(), payloadBytes)
            MessageTypes.BUFFER_READY -> decode(BufferReadyPayload.serializer(), payloadBytes)
            MessageTypes.KICK_USER -> decode(KickUserPayload.serializer(), payloadBytes)
            MessageTypes.TRANSFER_HOST -> decode(TransferHostPayload.serializer(), payloadBytes)
            MessageTypes.PING -> decode(PingPayload.serializer(), payloadBytes)
            MessageTypes.PONG -> decode(PongPayload.serializer(), payloadBytes)
            MessageTypes.RECONNECT -> decode(ReconnectPayload.serializer(), payloadBytes)
            MessageTypes.SUGGEST_TRACK -> decode(SuggestTrackPayload.serializer(), payloadBytes)
            MessageTypes.APPROVE_SUGGESTION -> decode(ApproveSuggestionPayload.serializer(), payloadBytes)
            MessageTypes.REJECT_SUGGESTION -> decode(RejectSuggestionPayload.serializer(), payloadBytes)

            MessageTypes.ROOM_CREATED -> decode(RoomCreatedPayload.serializer(), payloadBytes)
            MessageTypes.JOIN_REQUEST -> decode(JoinRequestPayload.serializer(), payloadBytes)
            MessageTypes.JOIN_APPROVED -> decode(JoinApprovedPayload.serializer(), payloadBytes)
            MessageTypes.JOIN_REJECTED -> decode(JoinRejectedPayload.serializer(), payloadBytes)
            MessageTypes.USER_JOINED -> decode(UserJoinedPayload.serializer(), payloadBytes)
            MessageTypes.USER_LEFT -> decode(UserLeftPayload.serializer(), payloadBytes)
            MessageTypes.BUFFER_WAIT -> decode(BufferWaitPayload.serializer(), payloadBytes)
            MessageTypes.BUFFER_COMPLETE -> decode(BufferCompletePayload.serializer(), payloadBytes)
            MessageTypes.ERROR -> decode(ErrorPayload.serializer(), payloadBytes)
            MessageTypes.HOST_CHANGED -> decode(HostChangedPayload.serializer(), payloadBytes)
            MessageTypes.KICKED -> decode(KickedPayload.serializer(), payloadBytes)
            MessageTypes.SYNC_STATE -> decode(SyncStatePayload.serializer(), payloadBytes)
            MessageTypes.RECONNECTED -> decode(ReconnectedPayload.serializer(), payloadBytes)
            MessageTypes.USER_RECONNECTED -> decode(UserReconnectedPayload.serializer(), payloadBytes)
            MessageTypes.USER_DISCONNECTED -> decode(UserDisconnectedPayload.serializer(), payloadBytes)
            MessageTypes.SUGGESTION_RECEIVED -> decode(SuggestionReceivedPayload.serializer(), payloadBytes)
            MessageTypes.SUGGESTION_APPROVED -> decode(SuggestionApprovedPayload.serializer(), payloadBytes)
            MessageTypes.SUGGESTION_REJECTED -> decode(SuggestionRejectedPayload.serializer(), payloadBytes)

            MessageTypes.SERVER_CAPABILITIES -> decode(ServerCapabilities.serializer(), payloadBytes)

            else -> null
        }

    private fun encodePayload(payload: Any): ByteArray =
        when (payload) {
            is CreateRoomPayload -> proto.encodeToByteArray(CreateRoomPayload.serializer(), payload)
            is JoinRoomPayload -> proto.encodeToByteArray(JoinRoomPayload.serializer(), payload)
            is LeaveRoomPayload -> ByteArray(0)
            is ApproveJoinPayload -> proto.encodeToByteArray(ApproveJoinPayload.serializer(), payload)
            is RejectJoinPayload -> proto.encodeToByteArray(RejectJoinPayload.serializer(), payload)
            is PlaybackActionPayload -> proto.encodeToByteArray(PlaybackActionPayload.serializer(), payload)
            is PingPayload -> proto.encodeToByteArray(PingPayload.serializer(), payload)
            is BufferReadyPayload -> proto.encodeToByteArray(BufferReadyPayload.serializer(), payload)
            is KickUserPayload -> proto.encodeToByteArray(KickUserPayload.serializer(), payload)
            is TransferHostPayload -> proto.encodeToByteArray(TransferHostPayload.serializer(), payload)
            is SuggestTrackPayload -> proto.encodeToByteArray(SuggestTrackPayload.serializer(), payload)
            is ApproveSuggestionPayload -> proto.encodeToByteArray(ApproveSuggestionPayload.serializer(), payload)
            is RejectSuggestionPayload -> proto.encodeToByteArray(RejectSuggestionPayload.serializer(), payload)
            is ReconnectPayload -> proto.encodeToByteArray(ReconnectPayload.serializer(), payload)
            is ClientCapabilities -> proto.encodeToByteArray(ClientCapabilities.serializer(), payload)
            else -> ByteArray(0)
        }

    private fun <T> decode(
        serializer: kotlinx.serialization.DeserializationStrategy<T>,
        bytes: ByteArray,
    ): T? =
        runCatching { proto.decodeFromByteArray(serializer, bytes) }.getOrNull()

    private fun gzip(data: ByteArray): ByteArray {
        val sink = Buffer()
        val buffered = GzipSink(sink).buffer()
        try {
            buffered.write(data)
        } finally {
            buffered.close()
        }
        return sink.readByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray? =
        runCatching {
            val buffered = GzipSource(Buffer().apply { write(data) }).buffer()
            try {
                buffered.readByteArray()
            } finally {
                buffered.close()
            }
        }.getOrNull()

    companion object {
        /** Gzip below this size costs more bytes than it saves. */
        const val COMPRESSION_THRESHOLD = 100
    }
}
