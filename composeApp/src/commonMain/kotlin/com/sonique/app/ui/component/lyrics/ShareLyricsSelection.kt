package com.sonique.app.ui.component.lyrics

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val MAX_SHARE_LYRIC_LINES = 3

@Stable
class ShareLyricsSelection(
    initialIndex: Int? = null,
) {
    var range by mutableStateOf(initialIndex?.let { it..it })
        private set

    val count: Int get() = range?.count() ?: 0

    val isEmpty: Boolean get() = range == null

    fun isSelected(index: Int): Boolean = range?.contains(index) == true

    fun toggle(
        index: Int,
        onLimitReached: () -> Unit = {},
    ) {
        val current = range
        if (current == null) {
            range = index..index
            return
        }

        when {
            index == current.first && index == current.last -> range = null
            index == current.first -> range = (current.first + 1)..current.last
            index == current.last -> range = current.first..(current.last - 1)
            current.contains(index) -> range = index..index
            index == current.first - 1 || index == current.last + 1 -> {
                if (current.count() >= MAX_SHARE_LYRIC_LINES) {
                    onLimitReached()
                } else {
                    range = if (index < current.first) index..current.last else current.first..index
                }
            }
            else -> range = index..index
        }
    }
}
