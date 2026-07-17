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

    private val _latestReleaseInfo = MutableStateFlow<ReleaseInfo?>(null)
    val latestReleaseInfo: StateFlow<ReleaseInfo?> = _latestReleaseInfo.asStateFlow()

    val currentVersion: String = BuildKonfig.versionName

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    init {
        checkForUpdate()
    }

    fun manualCheckForUpdate() {
        viewModelScope.launch {
            _isChecking.value = true
            // Add slight delay for better UX (let animation show)
            kotlinx.coroutines.delay(1000)
            checkForUpdate()
            // Keep spinner for at least 1.5 seconds for smooth feel
            kotlinx.coroutines.delay(500)
            _isChecking.value = false
        }
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            updateRepository.checkForUpdate().collectLatest { status ->
                if (status is UpdateStatus.Available) {
                    _latestReleaseInfo.value = status.release
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
