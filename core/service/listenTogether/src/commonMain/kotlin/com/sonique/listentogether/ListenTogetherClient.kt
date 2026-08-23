package com.sonique.listentogether

import com.sonique.ktorext.getEngine
import com.sonique.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val TAG = "ListenTogetherClient"

/**
 * What the socket reports upward.
 *
 * Everything above this class reacts to these and never to frames, which is what lets the state
 * machine in the next layer stay platform-free and testable without a socket.
 */
sealed interface ListenTogetherEvent {
    /**
     * The handshake completed and the connection is usable.
     *
     * [resumed] means a `reconnect` was sent with a stored session token — the room may still be
     * the one we were in. It is NOT a promise: the server answers `reconnected` on success and
     * `error` when the token has expired, and only that answer settles it.
     */
    data class Connected(
        val serverVersion: String,
        val compressionEnabled: Boolean,
        val resumed: Boolean,
    ) : ListenTogetherEvent

    /** One decoded protocol message. [payload] is null for types this client does not model. */
    data class Message(
        val type: String,
        val payload: Any?,
    ) : ListenTogetherEvent

    /** The first pong landed, so [ListenTogetherClient.positionAt] now returns corrected values. */
    data object ClockReady : ListenTogetherEvent

    /**
     * The socket is down. [willRetry] false means this client has given up and the user has to
     * rejoin by hand — the retry budget is spent, or the server refused this client outright.
     */
    data class Disconnected(
        val reason: String?,
        val willRetry: Boolean,
    ) : ListenTogetherEvent
}

/**
 * Owns one WebSocket to a Listen Together server: the capability handshake, the frame loop, the
 * ping loop that calibrates [ServerClock], and reconnection with a stored session token.
 *
 * It deliberately holds no room state — that belongs to the layer above. The one piece of protocol
 * knowledge it does keep is the session token, because reconnection has to be transparent: it is
 * read out of `room_created` / `join_approved` as those pass through, and replayed on the next
 * connection.
 */
class ListenTogetherClient(
    private val clientVersion: String,
    /**
     * Read on every connection attempt rather than captured once, so changing the server in
     * settings takes effect on the next connect instead of needing the screen to be recreated.
     */
    private val serverUrl: () -> String = { DEFAULT_SERVER_URL },
    /**
     * MUST be monotonic — [ServerClock] compares it against itself. The default is Kotlin's own
     * monotonic source, which needs no expect/actual; a platform with a better one (Android's
     * `SystemClock.elapsedRealtime`, which also counts deep sleep) can pass it instead.
     */
    private val elapsedRealtime: () -> Long = {
        PROCESS_START.elapsedNow().inWholeMilliseconds
    },
    dispatcher: CoroutineContext = Dispatchers.Default,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher

    private val client: HttpClient =
        HttpClient(getEngine()) {
            install(WebSockets)
        }

    private val serverClock = ServerClock(elapsedRealtime)

    // SUSPEND rather than a dropping overflow policy, and a buffer far larger than any burst the
    // server can produce: losing one protocol frame is a silent desync, which is strictly worse
    // than briefly slowing the reader. The server's own send buffer is 256.
    private val _events = MutableSharedFlow<ListenTogetherEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    private var codec = MessageCodec(compressionEnabled = true)
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var handshake: CompletableDeferred<ServerCapabilities>? = null

    private var sessionToken: String? = null
    private var pingSequence = 0L
    private var reconnectDelay = INITIAL_RECONNECT_DELAY
    private var reconnectAttempts = 0
    /** Set when the server tells us retrying cannot help — see [MessageTypes.ERROR] handling. */
    private var fatal = false

    /** True between a completed handshake and the socket going down. */
    val isConnected: Boolean
        get() = session?.isActive == true && handshake?.isCompleted == true

    /** Server wall time now, or null until the first pong has landed. */
    fun serverNow(): Long? = serverClock.now()

    /** See [ServerClock.positionAt]. Falls back to [position] whenever the clock cannot help. */
    fun positionAt(
        position: Long,
        effectiveAtServerTime: Long?,
        isPlaying: Boolean,
    ): Long = serverClock.positionAt(position, effectiveAtServerTime, isPlaying)

    /**
     * Opens the connection, or does nothing if one is already open or opening.
     *
     * Reconnection is automatic from here on; callers do not call this again after a drop.
     */
    fun connect() {
        // A deliberate start by the user, so the attempt budget begins again. The retry path
        // deliberately calls [startConnection] instead of this: routing a retry through here would
        // reset the counter every time and [MAX_RECONNECT_ATTEMPTS] would never be reached — the
        // limit would look implemented and quietly never fire.
        reconnectAttempts = 0
        reconnectDelay = INITIAL_RECONNECT_DELAY
        fatal = false
        startConnection()
    }

    private fun startConnection() {
        if (connectionJob?.isActive == true) {
            Logger.i(TAG, "Connect ignored — a connection is already active")
            return
        }
        connectionJob =
            launch {
                try {
                    openAndRun()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, "Connection error: ${e.stackTraceToString()}")
                }
                // Reached on every close, clean or not — openAndRun returns when the reader stops.
                scheduleReconnect()
            }
    }

    /**
     * Sends one message. Returns false when there is no live socket, which the caller must treat
     * as "not sent" rather than retrying blindly: the reconnect path replays nothing.
     */
    suspend fun send(
        msgType: String,
        payload: Any? = null,
    ): Boolean {
        val live = session ?: return false
        if (!live.isActive) return false
        return runCatching {
            live.send(Frame.Binary(true, codec.encode(msgType, payload)))
            true
        }.getOrElse { e ->
            Logger.w(TAG, "Send failed for $msgType: ${e.message}")
            false
        }
    }

    /** Leaves for good: stops retrying, forgets the session token, closes the socket. */
    fun disconnect() {
        Logger.i(TAG, "Disconnecting by request")
        sessionToken = null
        reconnectAttempts = 0
        connectionJob?.cancel()
        connectionJob = null
        launch {
            runCatching { session?.close() }
            session = null
        }
    }

    /** Releases the HTTP client too. The instance is unusable afterwards. */
    fun release() {
        disconnect()
        runCatching { client.close() }
        cancel()
    }

    private suspend fun openAndRun() {
        val url = serverUrl().ifBlank { DEFAULT_SERVER_URL }
        Logger.i(TAG, "Connecting to $url")
        val live = client.webSocketSession(url)
        session = live
        serverClock.reset()
        val pending = CompletableDeferred<ServerCapabilities>()
        handshake = pending

        try {
            coroutineScope {
                val reader = launch { readFrames(live) }

                // The handshake is an ordinary message, so it can only be answered once the reader
                // is running — hence the reader starts first and completes `pending` from there.
                send(
                    MessageTypes.CLIENT_CAPABILITIES,
                    ClientCapabilities(
                        // The server rejects a client that says false, with `unsupported_client`.
                        supportsProtobuf = true,
                        supportsCompression = true,
                        clientVersion = clientVersion,
                    ),
                )
                val caps =
                    withTimeoutOrNull(HANDSHAKE_TIMEOUT) { pending.await() }
                        ?: throw IllegalStateException("Handshake timed out after $HANDSHAKE_TIMEOUT")

                // Only narrows: a server that cannot inflate must not be sent gzip. Widening is
                // pointless because the 100-byte threshold already suppresses useless compression.
                if (!caps.supportsCompression) {
                    Logger.w(TAG, "Server reports no compression support — sending uncompressed")
                    codec = MessageCodec(compressionEnabled = false)
                }

                val token = sessionToken
                if (token != null) {
                    Logger.i(TAG, "Resuming with stored session token")
                    send(MessageTypes.RECONNECT, ReconnectPayload(sessionToken = token))
                }

                // A completed handshake is what "connected" means, so the budget refills here and
                // not merely when the socket opens — a server that accepts TCP but never answers
                // must not buy itself unlimited retries.
                reconnectDelay = INITIAL_RECONNECT_DELAY
                reconnectAttempts = 0
                _events.emit(
                    ListenTogetherEvent.Connected(
                        serverVersion = caps.serverVersion,
                        compressionEnabled = caps.supportsCompression,
                        resumed = token != null,
                    ),
                )

                val pinger = launch { pingLoop() }
                reader.join()
                pinger.cancel()
            }
        } finally {
            handshake = null
            session = null
            // NonCancellable because this runs on the disconnect() path too, where the job has
            // already been cancelled — and close() is itself a suspend call, so without this it
            // would abort immediately and the close frame would never be sent.
            withContext(NonCancellable) { runCatching { live.close() } }
        }
    }

    private suspend fun readFrames(live: DefaultClientWebSocketSession) {
        for (frame in live.incoming) {
            // the room protocol's server writes BinaryMessage exclusively; anything else is not ours.
            if (frame !is Frame.Binary) continue
            // Not `getOrElse { continue }`: Kotlin still forbids break/continue inside a lambda,
            // even an inlined one, so the failure has to be unwrapped before the loop can skip.
            val bytes = frame.readBytes()
            val decoded = runCatching { codec.decode(bytes) }.getOrNull()
            if (decoded == null) {
                Logger.w(TAG, "Undecodable frame dropped (${bytes.size} bytes)")
                continue
            }
            val (type, payloadBytes) = decoded
            val payload = codec.decodePayload(type, payloadBytes)
            handleMessage(type, payload)
        }
        Logger.i(TAG, "Read loop ended; socket closed")
    }

    private suspend fun handleMessage(
        type: String,
        payload: Any?,
    ) {
        when (type) {
            MessageTypes.SERVER_CAPABILITIES -> {
                val caps = payload as? ServerCapabilities
                if (caps == null) {
                    Logger.w(TAG, "Malformed server_capabilities")
                    return
                }
                Logger.i(TAG, "Handshake complete — server version ${caps.serverVersion}")
                handshake?.complete(caps)
                // Internal to the handshake; the layer above has no use for it.
                return
            }

            MessageTypes.PONG -> {
                val pong = payload as? PongPayload ?: return
                val firstSample =
                    serverClock.recordPong(
                        clientTime = pong.clientTime,
                        serverReceiveTime = pong.serverReceiveTime,
                        serverSendTime = pong.serverSendTime,
                    )
                if (firstSample) {
                    Logger.i(TAG, "Server clock calibrated")
                    _events.emit(ListenTogetherEvent.ClockReady)
                }
                return
            }

            // The token is what makes a drop survivable, so it is captured in passing rather than
            // asked of the layer above — which may not have been listening yet.
            MessageTypes.ROOM_CREATED -> (payload as? RoomCreatedPayload)?.let { sessionToken = it.sessionToken }
            MessageTypes.JOIN_APPROVED -> (payload as? JoinApprovedPayload)?.let { sessionToken = it.sessionToken }

            MessageTypes.ERROR -> {
                val error = payload as? ErrorPayload
                Logger.w(TAG, "Server error: ${error?.code} ${error?.message}")
                // `error?.code in set` would not compile — Set<String>.contains takes a non-null
                // String, and a null code is not a rejection anyway.
                if (error != null && error.code in NON_RECOVERABLE_ERROR_CODES) {
                    Logger.e(TAG, "Server rejected this client (${error.code}) — not retrying")
                    fatal = true
                }
            }

            // A room we can no longer return to: the token is spent either way.
            MessageTypes.KICKED -> sessionToken = null
        }
        _events.emit(ListenTogetherEvent.Message(type, payload))
    }

    /**
     * Feeds [ServerClock] and doubles as a liveness signal.
     *
     * The first few pings are close together because the clock is weighted, not averaged: one
     * sample sets the offset and each later one moves it by at most a quarter, so a room joined
     * during the first seconds would otherwise seek against a barely-calibrated clock. After that
     * the interval opens up — the server's read deadline is 60s and any message refreshes it, so
     * [PING_INTERVAL] is also what keeps an idle room's socket alive.
     */
    private suspend fun pingLoop() {
        var sent = 0
        while (true) {
            val ok =
                send(
                    MessageTypes.PING,
                    PingPayload(clientTime = elapsedRealtime(), sequence = ++pingSequence),
                )
            if (!ok) return
            sent++
            delay(if (sent < CALIBRATION_PINGS) CALIBRATION_PING_INTERVAL else PING_INTERVAL)
        }
    }

    private suspend fun scheduleReconnect() {
        if (fatal) {
            _events.emit(ListenTogetherEvent.Disconnected("Server rejected this client", willRetry = false))
            return
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Logger.w(TAG, "Gave up after $MAX_RECONNECT_ATTEMPTS reconnection attempts")
            // The token is dropped with the attempts: a later connect() is a fresh join, not a
            // resume, and replaying a token the server may already have expired would be answered
            // with an error rather than a room.
            sessionToken = null
            _events.emit(
                ListenTogetherEvent.Disconnected(
                    reason = "Could not reconnect after $MAX_RECONNECT_ATTEMPTS attempts",
                    willRetry = false,
                ),
            )
            return
        }

        reconnectAttempts++
        Logger.i(TAG, "Reconnecting in $reconnectDelay (attempt $reconnectAttempts of $MAX_RECONNECT_ATTEMPTS)")
        _events.emit(ListenTogetherEvent.Disconnected("Connection lost", willRetry = true))
        delay(reconnectDelay)
        reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
        connectionJob = null
        startConnection()
    }

    companion object {
        /**
         * The one public server, run by "Nyx" and already shared by several the room protocol forks.
         * Rooms are only shared by clients pointed at the SAME server, so this default is what
         * makes Sonique interoperable out of the box.
         */
        const val DEFAULT_SERVER_URL = "wss://the room serverx.meowery.eu/ws"

        /** Anchors the default monotonic source; see [elapsedRealtime]. */
        private val PROCESS_START = TimeSource.Monotonic.markNow()

        private val HANDSHAKE_TIMEOUT = 10.seconds

        /** the room server `client.go`: the read deadline is 60s and any inbound message refreshes it. */
        private val PING_INTERVAL = 15.seconds
        private val CALIBRATION_PING_INTERVAL = 1.seconds
        private const val CALIBRATION_PINGS = 3

        private val INITIAL_RECONNECT_DELAY: Duration = 1.seconds
        private val MAX_RECONNECT_DELAY: Duration = 60.seconds

        /**
         * How many times a dropped connection is retried before the user is told, in so many words,
         * that it failed. The delay doubles each time — 1s, 2s, 4s, 8s, 16s — so five attempts span
         * a little over half a minute of genuinely bad network before giving up rather than
         * retrying into a flat battery.
         *
         * Counted attempts, not elapsed time: the budget refills only on a COMPLETED handshake, so
         * a server that accepts the socket and then goes quiet still exhausts it.
         */
        private const val MAX_RECONNECT_ATTEMPTS = 5

        /**
         * Errors where reconnecting repeats the same rejection. Everything else the server sends —
         * `rate_limited`, `invalid_payload`, `unknown_message_type` — is about one message, not the
         * connection, and must NOT stop retrying.
         */
        private val NON_RECOVERABLE_ERROR_CODES = setOf("unsupported_client")
    }
}
