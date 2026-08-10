package com.sonique.media3.audio

import com.sonique.domain.data.player.AudioEffects
import com.sonique.domain.data.player.DelayEffect

private const val ECHO_PCM16_MIN = -32_768.0
private const val ECHO_PCM16_MAX = 32_767.0

/**
 * Feed-forward delay/echo arithmetic operating on 16-bit PCM samples.
 */
class EchoKernel {
    private var sampleRate = 0
    private var channelCount = 0

    private var appliedEffects: AudioEffects? = null

    private var lines: Array<ShortArray> = emptyArray()
    private var ringLength = 0
    private var writeIndex = 0
    private var channelCursor = 0

    private var tapOffsets = IntArray(0)
    private var tapDecays = FloatArray(0)
    private var inGain = 1.0
    private var outGain = 1.0

    var isBypassed = true
        private set

    fun configure(
        sampleRate: Int,
        channelCount: Int,
    ) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        ringLength = ((DelayEffect.MAX_TIME_MS.toLong() * DelayEffect.MAX_TAPS * sampleRate) / 1_000L).toInt()
        if (lines.size != channelCount || lines.firstOrNull()?.size != ringLength) {
            lines = emptyArray()
            writeIndex = 0
            channelCursor = 0
        } else {
            flush()
        }
        appliedEffects = null
    }

    private fun ensureLines(): Boolean {
        if (lines.size == channelCount && lines.firstOrNull()?.size == ringLength) return true
        if (channelCount <= 0 || ringLength <= 0) return false
        lines = Array(channelCount) { ShortArray(ringLength) }
        writeIndex = 0
        channelCursor = 0
        return true
    }

    fun flush() {
        lines.forEach { it.fill(0) }
        writeIndex = 0
        channelCursor = 0
    }

    fun reset() {
        lines = emptyArray()
        tapOffsets = IntArray(0)
        tapDecays = FloatArray(0)
        appliedEffects = null
        sampleRate = 0
        channelCount = 0
        ringLength = 0
        writeIndex = 0
        channelCursor = 0
        isBypassed = true
    }

    fun sync(effects: AudioEffects) {
        if (effects === appliedEffects) return
        appliedEffects = effects
        rebuild(effects.delay)
    }

    private fun rebuild(delay: DelayEffect?) {
        if (delay == null || !ensureLines()) {
            if (!isBypassed) flush()
            isBypassed = true
            return
        }

        val taps = delay.taps()
        if (tapOffsets.size != taps.delaysMs.size) {
            tapOffsets = IntArray(taps.delaysMs.size)
            tapDecays = FloatArray(taps.delaysMs.size)
        }
        taps.delaysMs.forEachIndexed { index, delayMs ->
            tapOffsets[index] = (delayMs.toDouble() * sampleRate / 1_000.0).toInt().coerceIn(1, ringLength)
        }
        taps.decays.forEachIndexed { index, decay -> tapDecays[index] = decay }
        inGain = taps.inGain.toDouble()
        outGain = taps.outGain.toDouble()
        isBypassed = false
    }

    fun beginBuffer() {
        channelCursor = 0
    }

    fun processInterleaved(input: Int): Int {
        val line = lines[channelCursor]
        var out = input * inGain
        var tap = 0
        while (tap < tapOffsets.size) {
            var readIndex = writeIndex + ringLength - tapOffsets[tap]
            if (readIndex >= ringLength) readIndex -= ringLength
            val product: Float = line[readIndex] * tapDecays[tap]
            out += product.toDouble()
            tap++
        }
        out *= outGain
        line[writeIndex] = input.toShort()

        channelCursor++
        if (channelCursor == channelCount) {
            channelCursor = 0
            writeIndex++
            if (writeIndex == ringLength) writeIndex = 0
        }
        return out.coerceIn(ECHO_PCM16_MIN, ECHO_PCM16_MAX).toInt()
    }
}
