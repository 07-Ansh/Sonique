package com.sonique.media3.audio

/**
 * One channel of uniform partitioned convolution, streaming.
 *
 * The impulse response comes from `ReverbImpulseResponse`, and this class computes streaming
 * uniform partitioned overlap-save convolution.
 *
 * **Uniform Partitioning:** A direct time-domain convolution against a long impulse response
 * is too computationally expensive for streaming mobile playback. A single FFT over the whole
 * impulse cannot stream as it requires the entire input first. Uniform partitioning cuts the impulse
 * into [PARTITION_SIZE]-sample pieces whose spectra are precomputed once at construction, the input is
 * transformed one block at a time, and the output is the sum of each input block's spectrum against
 * the corresponding impulse partition.
 *
 * **Latency:** Overlap-save emits output once a whole block arrives ([PARTITION_SIZE] frames,
 * ~85 ms at 48 kHz).
 *
 * **Alignment:** Output sample `n` is the convolution at input sample `n` — sample-aligned to avoid
 * comb filtering when blended with dry audio.
 *
 * **Memory layout:** Spectra are stored as `Float` arrays for bins `0..PARTITION_SIZE` (Hermitian half-spectrum),
 * while FFT transformations and accumulations run in `Double` precision.
 *
 * @param impulse the impulse response for this channel.
 */
class PartitionedConvolver(
    impulse: FloatArray,
) {
    private val fft = Fft(FFT_SIZE)

    private val partitions = maxOf(1, (impulse.size + PARTITION_SIZE - 1) / PARTITION_SIZE)

    private val impulseReal = FloatArray(partitions * SPECTRUM_BINS)
    private val impulseImaginary = FloatArray(partitions * SPECTRUM_BINS)

    private val historyReal = FloatArray(partitions * SPECTRUM_BINS)
    private val historyImaginary = FloatArray(partitions * SPECTRUM_BINS)

    private var historyIndex = 0

    private val previousBlock = DoubleArray(PARTITION_SIZE)

    private val incoming = DoubleArray(PARTITION_SIZE)
    private var incomingCount = 0

    private val windowReal = DoubleArray(FFT_SIZE)
    private val windowImaginary = DoubleArray(FFT_SIZE)

    private val productReal = DoubleArray(FFT_SIZE)
    private val productImaginary = DoubleArray(FFT_SIZE)

    private var pending = DoubleArray(FFT_SIZE)
    private var pendingHead = 0
    private var pendingTail = 0

    init {
        for (partition in 0 until partitions) {
            val start = partition * PARTITION_SIZE
            val length = minOf(PARTITION_SIZE, impulse.size - start)
            windowReal.fill(0.0)
            windowImaginary.fill(0.0)
            for (index in 0 until length) {
                windowReal[index] = impulse[start + index].toDouble()
            }
            fft.forward(windowReal, windowImaginary)
            storeHalfSpectrum(impulseReal, impulseImaginary, partition)
        }
        windowReal.fill(0.0)
        windowImaginary.fill(0.0)
    }

    val availableOutput: Int
        get() = pendingTail - pendingHead

    fun write(
        input: FloatArray,
        offset: Int,
        count: Int,
    ) {
        var index = 0
        while (index < count) {
            val chunk = minOf(count - index, PARTITION_SIZE - incomingCount)
            for (step in 0 until chunk) {
                incoming[incomingCount + step] = input[offset + index + step].toDouble()
            }
            incomingCount += chunk
            index += chunk
            if (incomingCount == PARTITION_SIZE) {
                convolveBlock()
            }
        }
    }

    fun read(
        output: FloatArray,
        offset: Int,
        count: Int,
    ): Int {
        val moved = minOf(count, availableOutput)
        for (index in 0 until moved) {
            output[offset + index] = pending[pendingHead + index].toFloat()
        }
        pendingHead += moved
        if (pendingHead == pendingTail) {
            pendingHead = 0
            pendingTail = 0
        }
        return moved
    }

    fun reset() {
        previousBlock.fill(0.0)
        incoming.fill(0.0)
        incomingCount = 0
        historyReal.fill(0f)
        historyImaginary.fill(0f)
        historyIndex = 0
        pendingHead = 0
        pendingTail = 0
    }

    fun flush() {
        reset()
    }

    private fun convolveBlock() {
        previousBlock.copyInto(windowReal, destinationOffset = 0)
        incoming.copyInto(windowReal, destinationOffset = PARTITION_SIZE)
        windowImaginary.fill(0.0)
        fft.forward(windowReal, windowImaginary)

        historyIndex = if (historyIndex + 1 == partitions) 0 else historyIndex + 1
        storeHalfSpectrum(historyReal, historyImaginary, historyIndex)

        productReal.fill(0.0)
        productImaginary.fill(0.0)
        for (partition in 0 until partitions) {
            var slot = historyIndex - partition
            if (slot < 0) slot += partitions
            val window = slot * SPECTRUM_BINS
            val response = partition * SPECTRUM_BINS
            for (bin in 0 until SPECTRUM_BINS) {
                val windowRe = historyReal[window + bin].toDouble()
                val windowIm = historyImaginary[window + bin].toDouble()
                val responseRe = impulseReal[response + bin].toDouble()
                val responseIm = impulseImaginary[response + bin].toDouble()
                productReal[bin] += windowRe * responseRe - windowIm * responseIm
                productImaginary[bin] += windowRe * responseIm + windowIm * responseRe
            }
        }
        mirrorConjugateHalf()
        fft.inverse(productReal, productImaginary)

        appendPending(productReal, PARTITION_SIZE, PARTITION_SIZE)
        incoming.copyInto(previousBlock)
        incomingCount = 0
    }

    private fun storeHalfSpectrum(
        real: FloatArray,
        imaginary: FloatArray,
        index: Int,
    ) {
        val offset = index * SPECTRUM_BINS
        for (bin in 0 until SPECTRUM_BINS) {
            real[offset + bin] = windowReal[bin].toFloat()
            imaginary[offset + bin] = windowImaginary[bin].toFloat()
        }
    }

    private fun mirrorConjugateHalf() {
        for (bin in 1 until PARTITION_SIZE) {
            productReal[FFT_SIZE - bin] = productReal[bin]
            productImaginary[FFT_SIZE - bin] = -productImaginary[bin]
        }
    }

    private fun appendPending(
        source: DoubleArray,
        from: Int,
        count: Int,
    ) {
        if (pendingTail + count > pending.size) {
            val kept = availableOutput
            if (kept + count > pending.size) {
                var capacity = pending.size
                while (capacity < kept + count) {
                    capacity *= 2
                }
                val grown = DoubleArray(capacity)
                pending.copyInto(grown, destinationOffset = 0, startIndex = pendingHead, endIndex = pendingTail)
                pending = grown
            } else {
                pending.copyInto(pending, destinationOffset = 0, startIndex = pendingHead, endIndex = pendingTail)
            }
            pendingHead = 0
            pendingTail = kept
        }
        source.copyInto(pending, destinationOffset = pendingTail, startIndex = from, endIndex = from + count)
        pendingTail += count
    }

    companion object {
        const val PARTITION_SIZE: Int = 4096
        const val FFT_SIZE: Int = 2 * PARTITION_SIZE
        private const val SPECTRUM_BINS: Int = FFT_SIZE / 2 + 1
    }
}
