package com.sonique.listentogether

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.max

/**
 * Maps the server's wall clock onto this device's monotonic clock, from ping/pong round trips.
 *
 * Every device in a room runs its own clock with its own drift, so "play at position 1234" is
 * meaningless until both ends agree what time it is. Each pong carries the client's send time and
 * the server's receive/send times, which is enough to estimate one-way latency and therefore the
 * offset between the two clocks.
 *
 * [elapsedRealtime] must be MONOTONIC — a wall clock that an NTP correction can step backwards
 * would make [positionAt] jump. Callers inject it so each platform passes its own source.
 */
internal class ServerClock(
    private val elapsedRealtime: () -> Long,
) {
    // the room protocol uses @Synchronized, which is JVM-only; atomicfu's lock is the multiplatform
    // equivalent and compiles to the same monitor on JVM/Android.
    private val lock = SynchronizedObject()

    private var serverOffsetMs: Double? = null
    private var bestRoundTripMs = Long.MAX_VALUE

    fun reset() =
        synchronized(lock) {
            serverOffsetMs = null
            bestRoundTripMs = Long.MAX_VALUE
        }

    /**
     * Folds one pong into the estimate. Returns true only for the FIRST accepted sample, which is
     * the moment the clock becomes usable at all.
     *
     * Samples are weighted by quality rather than averaged flat: a round trip close to the best one
     * seen gets 0.25, a slow one only 0.05, so a single congested frame cannot drag the offset.
     */
    fun recordPong(
        clientTime: Long,
        serverReceiveTime: Long,
        serverSendTime: Long,
    ): Boolean =
        synchronized(lock) {
            val receivedAt = elapsedRealtime()
            if (clientTime <= 0L || clientTime > receivedAt || receivedAt - clientTime > MAX_SAMPLE_AGE_MS) {
                return@synchronized false
            }
            if (serverReceiveTime <= 0L || serverSendTime < serverReceiveTime) return@synchronized false

            val roundTrip = receivedAt - clientTime
            val serverProcessing = serverSendTime - serverReceiveTime
            // Time the server spent thinking is not time on the wire, so it must come out before
            // the remainder is halved into a one-way estimate.
            val networkRoundTrip = max(0L, roundTrip - serverProcessing)
            val sampleOffset = serverSendTime + networkRoundTrip / 2.0 - receivedAt
            val previousOffset = serverOffsetMs

            if (networkRoundTrip < bestRoundTripMs) bestRoundTripMs = networkRoundTrip
            val weight = if (networkRoundTrip <= bestRoundTripMs + GOOD_SAMPLE_MARGIN_MS) 0.25 else 0.05
            serverOffsetMs = previousOffset?.let { it + weight * (sampleOffset - it) } ?: sampleOffset
            previousOffset == null
        }

    /** Server wall time now, or null until a first pong has landed. */
    fun now(): Long? = synchronized(lock) { serverOffsetMs?.let { (elapsedRealtime() + it).toLong() } }

    /**
     * Where a track actually is, given where it was at [effectiveAtServerTime].
     *
     * Advances [position] by however long the command spent in flight — but only while playing, and
     * only once the clock is calibrated. Every unknown falls back to the raw position rather than
     * to a guess, because a wrong seek is worse than a slightly late one.
     */
    fun positionAt(
        position: Long,
        effectiveAtServerTime: Long?,
        isPlaying: Boolean,
    ): Long {
        if (!isPlaying || effectiveAtServerTime == null || effectiveAtServerTime <= 0L) return position
        val serverNow = now() ?: return position
        return position + max(0L, serverNow - effectiveAtServerTime)
    }

    private companion object {
        const val MAX_SAMPLE_AGE_MS = 60_000L
        const val GOOD_SAMPLE_MARGIN_MS = 50L
    }
}
