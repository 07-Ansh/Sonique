package com.sonique.domain.data.model.listentogether

sealed interface RoomConnection {
    data object Disconnected : RoomConnection

    data object Connecting : RoomConnection

    data class Connected(val serverVersion: String) : RoomConnection

    data class Failed(val reason: String) : RoomConnection
}

data class RoomMember(
    val userId: String,
    val username: String,
    val isHost: Boolean,
    val isConnected: Boolean,
    val isBuffering: Boolean = false,
)

data class RoomJoinRequest(
    val userId: String,
    val username: String,
)

data class RoomTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val thumbnail: String = "",
)

data class RoomSuggestion(
    val suggestionId: String,
    val fromUsername: String,
    val track: RoomTrack,
)

data class ListenTogetherRoom(
    val connection: RoomConnection = RoomConnection.Disconnected,
    val roomCode: String? = null,
    val selfUserId: String = "",
    val isHost: Boolean = false,
    val members: List<RoomMember> = emptyList(),
    val joinRequests: List<RoomJoinRequest> = emptyList(),
    val suggestions: List<RoomSuggestion> = emptyList(),
    val currentTrack: RoomTrack? = null,
    val queue: List<RoomTrack> = emptyList(),
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val waitingFor: List<String> = emptyList(),
    val pendingJoinCode: String? = null,
    val error: String? = null,
) {
    val inRoom: Boolean get() = roomCode != null
    val isConnected: Boolean get() = connection is RoomConnection.Connected

    val waitingForNames: List<String>
        get() = waitingFor.mapNotNull { id -> members.firstOrNull { it.userId == id }?.username }
}
