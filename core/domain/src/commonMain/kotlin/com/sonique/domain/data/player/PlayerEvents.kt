package com.sonique.domain.data.player

 
data class PlayerEvents(
    val eventFlags: Int,
) {
    fun containsAny(vararg events: Int): Boolean = events.any { eventFlags and it != 0 }

    fun contains(event: Int): Boolean = eventFlags and event != 0
}

