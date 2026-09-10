package com.sonique.app.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonique.data.listentogether.ListenTogetherPlaybackBridge
import com.sonique.data.listentogether.ListenTogetherPrefs
import com.sonique.domain.data.model.listentogether.ListenTogetherRoom
import com.sonique.domain.manager.DataStoreManager
import com.sonique.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    init {
        bridge.start()

        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_JOINS).collect {
                repository.autoApproveJoins = it == ListenTogetherPrefs.TRUE
            }
        }
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS).collect {
                repository.autoApproveSuggestions = it == ListenTogetherPrefs.TRUE
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListenTogetherRoom())

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _roomCodeInput = MutableStateFlow("")
    val roomCodeInput: StateFlow<String> = _roomCodeInput.asStateFlow()

    fun onDisplayNameChange(value: String) {
        _displayName.value = value.take(MAX_USERNAME_LENGTH)
    }

    fun onRoomCodeChange(value: String) {
        _roomCodeInput.value =
            value.uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_LENGTH)
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()

    fun createRoom() {
        repository.createRoom(_displayName.value)
    }

    fun joinRoom() {
        repository.joinRoom(_roomCodeInput.value, _displayName.value)
    }

    fun leaveRoom() {
        repository.leaveRoom()
        _roomCodeInput.value = ""
    }

    fun approveJoin(userId: String) {
        repository.approveJoin(userId)
    }

    fun rejectJoin(userId: String) {
        repository.rejectJoin(userId)
    }

    fun approveSuggestion(id: String) {
        repository.approveSuggestion(id)
    }

    fun rejectSuggestion(id: String) {
        repository.rejectSuggestion(id)
    }

    fun kickUser(userId: String) {
        repository.kickUser(userId)
    }

    fun blockAndKick(
        userId: String,
        username: String,
    ) {
        viewModelScope.launch {
            val current =
                dataStore
                    .getString(ListenTogetherPrefs.BLOCKLIST)
                    .first()
                    .orEmpty()
                    .split(ListenTogetherPrefs.BLOCKLIST_SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (current.none { it.equals(username, ignoreCase = true) }) {
                dataStore.putString(
                    ListenTogetherPrefs.BLOCKLIST,
                    (current + username).joinToString(ListenTogetherPrefs.BLOCKLIST_SEPARATOR),
                )
            }
            repository.kickUser(userId)
        }
    }

    fun transferHost(userId: String) {
        repository.transferHost(userId)
    }

    fun cancelJoin() = repository.cancelJoin()

    fun clearError() = repository.clearError()

    companion object {
        const val ROOM_CODE_LENGTH = 8
        private const val MAX_USERNAME_LENGTH = 50
    }
}
