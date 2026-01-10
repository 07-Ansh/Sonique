package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import com.sonique.app.BuildKonfig
import com.sonique.app.viewModel.base.BaseViewModel
import com.sonique.domain.repository.ReleaseInfo
import com.sonique.domain.repository.UpdateRepository
import com.sonique.domain.repository.UpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
) : BaseViewModel() {

    private val _updateAvailable = MutableStateFlow<ReleaseInfo?>(null)
    val updateAvailable: StateFlow<ReleaseInfo?> = _updateAvailable.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            updateRepository.checkForUpdate().collectLatest { status ->
                if (status is UpdateStatus.Available) {
                    val remoteVersion = status.release.version.removePrefix("v")
                    val localVersion = BuildKonfig.versionName
                    
                    if (isUpdateAvailable(localVersion, remoteVersion)) {
                        _updateAvailable.value = status.release
                    }
                }
            }
        }
    }

    private fun isUpdateAvailable(local: String, remote: String): Boolean {
         
        val localClean = local.removePrefix("v")
        val remoteClean = remote.removePrefix("v")

        val localParts = localClean.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = remoteClean.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(localParts.size, remoteParts.size)

        for (i in 0 until length) {
            val localPart = localParts.getOrElse(i) { 0 }
            val remotePart = remoteParts.getOrElse(i) { 0 }

            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }

        return false
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }
}
