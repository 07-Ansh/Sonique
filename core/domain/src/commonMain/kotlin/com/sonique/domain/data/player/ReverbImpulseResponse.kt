package com.sonique.domain.data.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A generated stereo impulse response.
 */
class StereoImpulse(
    val left: FloatArray,
    val right: FloatArray,
    val sampleRate: Int,
)

/**
 * Synthesises the impulse response each [ReverbPreset] is convolved against.
 */
object ReverbImpulseResponse {
    const val SAMPLE_RATE: Int = 48_000
    const val VERSION: Int = 1

    private const val MAX_LENGTH_MS = 4_000
    private const val DECAY_PER_RT60 = 6.907
    private const val DAMPING_DECAY_PER_RT60 = 1.3862943611198906
    private const val EARLY_REFLECTION_WINDOW_MS = 50
    private const val EARLY_REFLECTION_COUNT = 5
    private const val EARLY_REFLECTION_PEAK = 0.8
    private const val EARLY_REFLECTION_DECAY = 0.35
    private const val RIGHT_CHANNEL_SEED = 0x6A09E667F3BCC909L
    private const val EARLY_REFLECTION_SEED = 0x3C6EF372FE94F82BL

    fun generate(
        preset: ReverbPreset,
        sampleRate: Int = SAMPLE_RATE,
    ): StereoImpulse {
        val rate = sampleRate.coerceAtLeast(1)
        val lengthMs = minOf(preset.preDelayMs + preset.rt60Ms, MAX_LENGTH_MS)
        val totalFrames = maxOf(1, lengthMs * rate / 1_000)
        val preDelayFrames = (preset.preDelayMs * rate / 1_000).coerceIn(0, totalFrames - 1)

        val left = FloatArray(totalFrames)
        val right = FloatArray(totalFrames)
        renderTail(left, preDelayFrames, preset, rate, Xorshift64Star(preset.seed))
        renderTail(right, preDelayFrames, preset, rate, Xorshift64Star(preset.seed xor RIGHT_CHANNEL_SEED))
        addEarlyReflections(left, right, preDelayFrames, preset, rate)
        normaliseToUnitEnergy(left)
        normaliseToUnitEnergy(right)
        return StereoImpulse(left = left, right = right, sampleRate = rate)
    }

    private fun renderTail(
        frames: FloatArray,
        preDelayFrames: Int,
        preset: ReverbPreset,
        rate: Int,
        random: Xorshift64Star,
    ) {
        val rt60Seconds = maxOf(preset.rt60Ms, 1) / 1_000.0
        val envelopeRate = -DECAY_PER_RT60 / rt60Seconds
        val dampingRate = -DAMPING_DECAY_PER_RT60 / rt60Seconds
        val dampingHz = preset.dampingHz.toDouble()
        val twoPiOverRate = 2.0 * PI / rate

        var lowPassed = 0.0
        for (index in preDelayFrames until frames.size) {
            val seconds = (index - preDelayFrames) / rate.toDouble()
            val cutoffHz = dampingHz * exp(dampingRate * seconds)
            val coefficient = 1.0 - exp(-twoPiOverRate * cutoffHz)
            lowPassed += coefficient * (random.nextGaussian() - lowPassed)
            frames[index] = (lowPassed * exp(envelopeRate * seconds)).toFloat()
        }
    }

    private fun addEarlyReflections(
        left: FloatArray,
        right: FloatArray,
        preDelayFrames: Int,
        preset: ReverbPreset,
        rate: Int,
    ) {
        val windowFrames = EARLY_REFLECTION_WINDOW_MS * rate / 1_000
        val sliceFrames = windowFrames / EARLY_REFLECTION_COUNT
        if (sliceFrames <= 0) return

        val random = Xorshift64Star(preset.seed xor EARLY_REFLECTION_SEED)
        val offsets = IntArray(EARLY_REFLECTION_COUNT)
        val gains = FloatArray(EARLY_REFLECTION_COUNT)
        for (index in 0 until EARLY_REFLECTION_COUNT) {
            offsets[index] = index * sliceFrames + (random.nextDouble() * sliceFrames).toInt()
            val magnitude = EARLY_REFLECTION_PEAK * exp(-EARLY_REFLECTION_DECAY * index)
            gains[index] = (magnitude * (2.0 * random.nextDouble() - 1.0)).toFloat()
        }

        for (index in 0 until EARLY_REFLECTION_COUNT) {
            val position = preDelayFrames + offsets[index]
            if (position >= left.size) break
            left[position] += gains[index]
            right[position] += gains[EARLY_REFLECTION_COUNT - 1 - index]
        }
    }

    private fun normaliseToUnitEnergy(frames: FloatArray) {
        var energy = 0.0
        for (sample in frames) {
            energy += sample.toDouble() * sample.toDouble()
        }
        if (energy <= 0.0) return
        val scale = (1.0 / sqrt(energy)).toFloat()
        for (index in frames.indices) {
            frames[index] = frames[index] * scale
        }
    }
}

private class Xorshift64Star(
    seed: Long,
) {
    private var state: Long = if (seed == 0L) FALLBACK_SEED else seed
    private var spareGaussian = 0.0
    private var hasSpareGaussian = false

    private fun nextLong(): Long {
        var x = state
        x = x xor (x ushr 12)
        x = x xor (x shl 25)
        x = x xor (x ushr 27)
        state = x
        return x * MULTIPLIER
    }

    fun nextDouble(): Double = (nextLong() ushr 11) * DOUBLE_UNIT

    fun nextGaussian(): Double {
        if (hasSpareGaussian) {
            hasSpareGaussian = false
            return spareGaussian
        }
        val radius = sqrt(-2.0 * ln(1.0 - nextDouble()))
        val angle = 2.0 * PI * nextDouble()
        spareGaussian = radius * sin(angle)
        hasSpareGaussian = true
        return radius * cos(angle)
    }

    private companion object {
        const val FALLBACK_SEED = 0x106689D45497FDB5L
        const val MULTIPLIER = 0x2545F4914F6CDD1DL
        const val DOUBLE_UNIT = 1.0 / 9_007_199_254_740_992.0
    }
}
