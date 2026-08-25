package com.sonique.domain.repository

import com.sonique.domain.data.model.listentogether.ListenTogetherRoom
import com.sonique.domain.data.model.listentogether.RoomTrack
import kotlinx.coroutines.flow.StateFlow

interface ListenTogetherRepository {
    val room: StateFlow<ListenTogetherRoom>

    var autoApproveJoins: Boolean
    var autoApproveSuggestions: Boolean

    fun connect()

    fun disconnect()

    fun createRoom(username: String)

    fun joinRoom(
        roomCode: String,
        username: String,
    )

    fun cancelJoin()

    fun leaveRoom()

    fun approveJoin(userId: String)

    fun rejectJoin(userId: String)

    fun approveSuggestion(suggestionId: String)

    fun rejectSuggestion(suggestionId: String)

    fun kickUser(userId: String)

    fun transferHost(userId: String)

    fun suggestTrack(track: RoomTrack)

    fun requestSync()

    fun clearError()
}
