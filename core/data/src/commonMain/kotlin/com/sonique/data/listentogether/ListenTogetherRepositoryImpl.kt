package com.sonique.data.listentogether

import com.sonique.domain.data.model.listentogether.ListenTogetherRoom
import com.sonique.domain.data.model.listentogether.RoomConnection
import com.sonique.domain.data.model.listentogether.RoomJoinRequest
import com.sonique.domain.data.model.listentogether.RoomMember
import com.sonique.domain.data.model.listentogether.RoomSuggestion
import com.sonique.domain.data.model.listentogether.RoomTrack
import com.sonique.domain.repository.ListenTogetherRepository
import com.sonique.listentogether.ConnectionState
import com.sonique.listentogether.ListenTogetherSession
import com.sonique.listentogether.ListenTogetherState
import com.sonique.listentogether.PendingJoin
import com.sonique.listentogether.PendingSuggestion
import com.sonique.listentogether.RoomMember as ProtocolMember
import com.sonique.listentogether.TrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListenTogetherRepositoryImpl(
    private val session: ListenTogetherSession,
    scope: CoroutineScope,
) : ListenTogetherRepository {
    private val _room = MutableStateFlow(ListenTogetherRoom())
    override val room: StateFlow<ListenTogetherRoom> = _room.asStateFlow()

    override var autoApproveJoins: Boolean
        get() = session.autoApproveJoins
        set(value) {
            session.autoApproveJoins = value
        }

    override var autoApproveSuggestions: Boolean
        get() = session.autoApproveSuggestions
        set(value) {
            session.autoApproveSuggestions = value
        }

    init {
        scope.launch { session.state.collect { _room.value = it.toDomain() } }
    }

    override fun connect() = session.connect()

    override fun disconnect() = session.disconnect()

    override fun createRoom(username: String) {
        session.createRoom(username)
    }

    override fun joinRoom(
        roomCode: String,
        username: String,
    ) {
        session.joinRoom(roomCode, username)
    }

    override fun cancelJoin() = session.cancelJoin()

    override fun leaveRoom() {
        session.leaveRoom()
    }

    override fun approveJoin(userId: String) {
        session.approveJoin(userId)
    }

    override fun rejectJoin(userId: String) {
        session.rejectJoin(userId)
    }

    override fun approveSuggestion(suggestionId: String) {
        session.approveSuggestion(suggestionId)
    }

    override fun rejectSuggestion(suggestionId: String) {
        session.rejectSuggestion(suggestionId)
    }

    override fun kickUser(userId: String) {
        session.kickUser(userId)
    }

    override fun transferHost(userId: String) {
        session.transferHost(userId)
    }

    override fun suggestTrack(track: RoomTrack) {
        session.suggestTrack(track.toProtocol())
    }

    override fun requestSync() {
        session.requestSync()
    }

    override fun clearError() = session.clearError()
}

private fun ListenTogetherState.toDomain() =
    ListenTogetherRoom(
        connection = connection.toDomain(),
        roomCode = roomCode,
        selfUserId = selfUserId,
        isHost = isHost,
        members = members.map { it.toDomain() },
        joinRequests = joinRequests.map { it.toDomain() },
        suggestions = suggestions.map { it.toDomain() },
        currentTrack = currentTrack?.toDomain(),
        queue = queue.map { it.toDomain() },
        isPlaying = isPlaying,
        position = position,
        waitingFor = waitingFor,
        pendingJoinCode = pendingJoinCode,
        error = error,
    )

private fun ConnectionState.toDomain(): RoomConnection =
    when (this) {
        ConnectionState.Disconnected -> RoomConnection.Disconnected
        ConnectionState.Connecting -> RoomConnection.Connecting
        is ConnectionState.Connected -> RoomConnection.Connected(serverVersion)
        is ConnectionState.Failed -> RoomConnection.Failed(reason)
    }

private fun ProtocolMember.toDomain() =
    RoomMember(
        userId = userId,
        username = username,
        isHost = isHost,
        isConnected = isConnected,
        isBuffering = isBuffering,
    )

private fun PendingJoin.toDomain() = RoomJoinRequest(userId = userId, username = username)

private fun PendingSuggestion.toDomain() =
    RoomSuggestion(
        suggestionId = suggestionId,
        fromUsername = fromUsername,
        track = track.toDomain(),
    )

private fun TrackInfo.toDomain() =
    RoomTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = duration,
        thumbnail = thumbnail,
    )

private fun RoomTrack.toProtocol() =
    TrackInfo(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = durationMs,
        thumbnail = thumbnail,
    )
