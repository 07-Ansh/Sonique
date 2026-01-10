 

package com.sonique.media3.utils

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder

 
@OptIn(UnstableApi::class)
class BetterShuffleOrder(
    private val shuffled: IntArray,
) : ShuffleOrder {
    private val indexInShuffled: IntArray = IntArray(shuffled.size)

    constructor(length: Int, startIndex: Int) : this(createShuffledList(length, startIndex))

    init {
        for (i in shuffled.indices) {
            indexInShuffled[shuffled[i]] = i
        }
    }

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int {
        var shuffledIndex = indexInShuffled[index]
        return if (++shuffledIndex < shuffled.size) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        var shuffledIndex = indexInShuffled[index]
        return if (--shuffledIndex >= 0) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int = if (shuffled.isNotEmpty()) shuffled[shuffled.size - 1] else C.INDEX_UNSET

    override fun getFirstIndex(): Int = if (shuffled.isNotEmpty()) shuffled[0] else C.INDEX_UNSET

    @Suppress("KotlinConstantConditions")  
    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        if (shuffled.isEmpty()) {
            return BetterShuffleOrder(insertionCount, -1)
        }

         
         

        val newShuffled = IntArray(shuffled.size + insertionCount)
        val pivot: Int =
            if (insertionIndex < shuffled.size) {
                indexInShuffled[insertionIndex]
            } else {
                indexInShuffled.size
            }
        for (i in shuffled.indices) {
            var currentIndex = shuffled[i]
            if (currentIndex > insertionIndex) {
                currentIndex += insertionCount
            }

            if (i <= pivot) {
                newShuffled[i] = currentIndex
            } else if (i > pivot) {
                newShuffled[i + insertionCount] = currentIndex
            }
        }
        if (insertionIndex < shuffled.size) {
            for (i in 0 until insertionCount) {
                newShuffled[pivot + i + 1] = insertionIndex + i + 1
            }
        } else {
            for (i in 0 until insertionCount) {
                newShuffled[pivot + i] = insertionIndex + i
            }
        }
        return BetterShuffleOrder(newShuffled)
    }

    override fun cloneAndRemove(
        indexFrom: Int,
        indexToExclusive: Int,
    ): ShuffleOrder {
        val numberOfElementsToRemove = indexToExclusive - indexFrom
        val newShuffled = IntArray(shuffled.size - numberOfElementsToRemove)
        var foundElementsCount = 0
        for (i in shuffled.indices) {
            if (shuffled[i] in indexFrom until indexToExclusive) {
                foundElementsCount++
            } else {
                newShuffled[i - foundElementsCount] =
                    if (shuffled[i] >= indexFrom) {
                        shuffled[i] - numberOfElementsToRemove
                    } else {
                        shuffled[i]
                    }
            }
        }
        return BetterShuffleOrder(newShuffled)
    }

    override fun cloneAndClear(): ShuffleOrder = BetterShuffleOrder(0, -1)

    companion object {
        private fun createShuffledList(
            length: Int,
            startIndex: Int,
        ): IntArray {
            val shuffled = IntArray(length)
            for (i in 0 until length) {
                val swapIndex = (0..i).random()
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
            if (startIndex != -1) {
                val startIndexInShuffled = shuffled.indexOf(startIndex)
                val temp = shuffled[0]
                shuffled[0] = shuffled[startIndexInShuffled]
                shuffled[startIndexInShuffled] = temp
            }
            return shuffled
        }
    }
}

