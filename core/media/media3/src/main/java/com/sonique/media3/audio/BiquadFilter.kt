package com.sonique.media3.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Biquad IIR filter supporting low-pass (LPF) and high-pass (HPF) filter types.
 * Based on Robert Bristow-Johnson's Audio EQ Cookbook.
 *
 * Designed for real-time DJ-style crossfade transitions:
 * - Outgoing track: LPF sweeps cutoff from 20 kHz -> ~300 Hz (treble fades, becomes muffled)
 * - Incoming track: HPF sweeps cutoff from ~300 Hz -> 20 Hz (bass fills in gradually)
 *
 * Supports stereo with independent per-channel state.
 * Thread-safe coefficient updates via [updateCoefficients].
 */
class BiquadFilter {

    enum class FilterType {
        LOW_PASS,
        HIGH_PASS,
    }

    // Two cascaded biquad stages → 4th order (24 dB/octave)
    // Stage 1 coefficients
    @Volatile private var b0_1 = 1.0
    @Volatile private var b1_1 = 0.0
    @Volatile private var b2_1 = 0.0
    @Volatile private var a1_1 = 0.0
    @Volatile private var a2_1 = 0.0

    // Stage 2 coefficients
    @Volatile private var b0_2 = 1.0
    @Volatile private var b1_2 = 0.0
    @Volatile private var b2_2 = 0.0
    @Volatile private var a1_2 = 0.0
    @Volatile private var a2_2 = 0.0

    // Stage 1 per-channel state (left)
    private var s1_x1L = 0.0; private var s1_x2L = 0.0
    private var s1_y1L = 0.0; private var s1_y2L = 0.0
    // Stage 1 per-channel state (right)
    private var s1_x1R = 0.0; private var s1_x2R = 0.0
    private var s1_y1R = 0.0; private var s1_y2R = 0.0

    // Stage 2 per-channel state (left)
    private var s2_x1L = 0.0; private var s2_x2L = 0.0
    private var s2_y1L = 0.0; private var s2_y2L = 0.0
    // Stage 2 per-channel state (right)
    private var s2_x1R = 0.0; private var s2_x2R = 0.0
    private var s2_y1R = 0.0; private var s2_y2R = 0.0

    /**
     * Recalculate filter coefficients for the given parameters.
     * Uses two cascaded Butterworth stages for 4th-order (24 dB/octave) rolloff,
     * matching professional DJ mixer filter steepness.
     *
     * @param cutoffHz Cutoff frequency in Hz.
     * @param sampleRate Sample rate in Hz (e.g. 44100, 48000).
     * @param q Quality factor per stage. 0.707 (Butterworth) gives maximally flat passband.
     * @param type LOW_PASS or HIGH_PASS.
     */
    fun updateCoefficients(
        cutoffHz: Float,
        sampleRate: Int,
        q: Float = BUTTERWORTH_Q,
        type: FilterType,
    ) {
        val clampedCutoff = cutoffHz.coerceIn(20f, (sampleRate / 2f) - 1f)
        val omega = 2.0 * PI * clampedCutoff / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / (2.0 * q)

        // Both stages use identical coefficients (cascaded identical Butterworth)
        val rawA0: Double
        when (type) {
            FilterType.LOW_PASS -> {
                val v = (1.0 - cosOmega) / 2.0
                b0_1 = v; b0_2 = v
                b1_1 = 1.0 - cosOmega; b1_2 = b1_1
                b2_1 = v; b2_2 = v
                rawA0 = 1.0 + alpha
                a1_1 = -2.0 * cosOmega; a1_2 = a1_1
                a2_1 = 1.0 - alpha; a2_2 = a2_1
            }

            FilterType.HIGH_PASS -> {
                val v = (1.0 + cosOmega) / 2.0
                b0_1 = v; b0_2 = v
                b1_1 = -(1.0 + cosOmega); b1_2 = b1_1
                b2_1 = v; b2_2 = v
                rawA0 = 1.0 + alpha
                a1_1 = -2.0 * cosOmega; a1_2 = a1_1
                a2_1 = 1.0 - alpha; a2_2 = a2_1
            }
        }

        // Normalize both stages
        b0_1 /= rawA0; b1_1 /= rawA0; b2_1 /= rawA0; a1_1 /= rawA0; a2_1 /= rawA0
        b0_2 /= rawA0; b1_2 /= rawA0; b2_2 /= rawA0; a1_2 /= rawA0; a2_2 /= rawA0
    }

    /**
     * Process a single mono sample through both cascaded stages.
     */
    fun processSampleMono(input: Double): Double {
        // Stage 1
        val mid = b0_1 * input + b1_1 * s1_x1L + b2_1 * s1_x2L - a1_1 * s1_y1L - a2_1 * s1_y2L
        s1_x2L = s1_x1L; s1_x1L = input
        s1_y2L = s1_y1L; s1_y1L = mid

        // Stage 2
        val output = b0_2 * mid + b1_2 * s2_x1L + b2_2 * s2_x2L - a1_2 * s2_y1L - a2_2 * s2_y2L
        s2_x2L = s2_x1L; s2_x1L = mid
        s2_y2L = s2_y1L; s2_y1L = output

        return output
    }

    /**
     * Process a stereo sample pair through both cascaded stages.
     * Each channel maintains independent filter state.
     */
    fun processStereo(inputLeft: Double, inputRight: Double): Pair<Double, Double> {
        // Left: Stage 1
        val midL = b0_1 * inputLeft + b1_1 * s1_x1L + b2_1 * s1_x2L - a1_1 * s1_y1L - a2_1 * s1_y2L
        s1_x2L = s1_x1L; s1_x1L = inputLeft
        s1_y2L = s1_y1L; s1_y1L = midL
        // Left: Stage 2
        val outL = b0_2 * midL + b1_2 * s2_x1L + b2_2 * s2_x2L - a1_2 * s2_y1L - a2_2 * s2_y2L
        s2_x2L = s2_x1L; s2_x1L = midL
        s2_y2L = s2_y1L; s2_y1L = outL

        // Right: Stage 1
        val midR = b0_1 * inputRight + b1_1 * s1_x1R + b2_1 * s1_x2R - a1_1 * s1_y1R - a2_1 * s1_y2R
        s1_x2R = s1_x1R; s1_x1R = inputRight
        s1_y2R = s1_y1R; s1_y1R = midR
        // Right: Stage 2
        val outR = b0_2 * midR + b1_2 * s2_x1R + b2_2 * s2_x2R - a1_2 * s2_y1R - a2_2 * s2_y2R
        s2_x2R = s2_x1R; s2_x1R = midR
        s2_y2R = s2_y1R; s2_y1R = outR

        return outL to outR
    }

    /**
     * Reset all filter state (clears history for both stages).
     * Call when starting a new audio stream or disabling the filter.
     */
    fun reset() {
        s1_x1L = 0.0; s1_x2L = 0.0; s1_y1L = 0.0; s1_y2L = 0.0
        s1_x1R = 0.0; s1_x2R = 0.0; s1_y1R = 0.0; s1_y2R = 0.0
        s2_x1L = 0.0; s2_x2L = 0.0; s2_y1L = 0.0; s2_y2L = 0.0
        s2_x1R = 0.0; s2_x2R = 0.0; s2_y1R = 0.0; s2_y2R = 0.0
    }

    companion object {
        /** Butterworth Q factor: maximally flat passband, no resonance at cutoff. */
        const val BUTTERWORTH_Q = 0.707f
    }
}
