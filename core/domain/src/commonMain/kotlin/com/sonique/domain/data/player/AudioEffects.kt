package com.sonique.domain.data.player

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

data class DelayEffect(
    val timeMs: Int,
    val feedback: Float,
    val mix: Float,
) {
    fun taps(): DelayTaps {
        val time = timeMs.coerceIn(MIN_TIME_MS, MAX_TIME_MS)
        val feedbackAmount = clamp(feedback, 0f, MAX_FEEDBACK)
        val wet = clamp(mix, 0f, 1f)

        val count = tapCount(feedbackAmount)
        val delaysMs = IntArray(count) { time * (it + 1) }
        val decays = FloatArray(count) {
            (wet * feedbackAmount.pow(it + 1)).coerceAtLeast(DECAY_FLOOR)
        }

        val inGain = 1f - wet
        val outGain = 1f / maxOf(1f, inGain + decays.sum())
        return DelayTaps(inGain = inGain, outGain = outGain, delaysMs = delaysMs, decays = decays)
    }

    companion object {
        const val MIN_TIME_MS: Int = 20
        const val MAX_TIME_MS: Int = 2_000
        const val MAX_FEEDBACK: Float = 0.9f
        const val MAX_TAPS: Int = 12
        const val TAIL_FLOOR: Float = 0.05f
        private const val DECAY_FLOOR: Float = 0.001f

        private fun clamp(
            value: Float,
            min: Float,
            max: Float,
        ): Float = if (value.isNaN()) min else value.coerceIn(min, max)

        private fun tapCount(feedback: Float): Int {
            if (feedback <= DECAY_FLOOR) return 1
            val exact = ln(TAIL_FLOOR.toDouble()) / ln(feedback.toDouble())
            return ceil(exact).toInt().coerceIn(1, MAX_TAPS)
        }
    }
}

data class DelayTaps(
    val inGain: Float,
    val outGain: Float,
    val delaysMs: IntArray,
    val decays: FloatArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DelayTaps) return false
        return inGain == other.inGain &&
            outGain == other.outGain &&
            delaysMs.contentEquals(other.delaysMs) &&
            decays.contentEquals(other.decays)
    }

    override fun hashCode(): Int {
        var result = inGain.hashCode()
        result = 31 * result + outGain.hashCode()
        result = 31 * result + delaysMs.contentHashCode()
        result = 31 * result + decays.contentHashCode()
        return result
    }
}

enum class ReverbPreset(
    val rt60Ms: Int,
    val preDelayMs: Int,
    val dampingHz: Int,
    val seed: Long,
) {
    ROOM(rt60Ms = 600, preDelayMs = 5, dampingHz = 6_000, seed = 0x2F6E5B1D4C3A9187L),
    HALL(rt60Ms = 2_200, preDelayMs = 20, dampingHz = 4_500, seed = 0x51A7C3E9B2D40F63L),
    PLATE(rt60Ms = 1_500, preDelayMs = 0, dampingHz = 9_000, seed = 0x7B39D85F6C1E2A4DL),
    CATHEDRAL(rt60Ms = 4_000, preDelayMs = 30, dampingHz = 3_000, seed = 0x4C8E1F72A560D3B9L),
}

data class ReverbEffect(
    val preset: ReverbPreset,
    val mix: Float,
)

data class AudioEffects(
    val delay: DelayEffect?,
    val reverb: ReverbEffect?,
) {
    companion object {
        val NONE = AudioEffects(delay = null, reverb = null)
    }
}
