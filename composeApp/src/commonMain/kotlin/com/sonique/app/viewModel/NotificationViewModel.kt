package com.sonique.app.viewModel

import androidx.lifecycle.viewModelScope
import com.sonique.domain.data.entities.NotificationEntity
import com.sonique.domain.repository.CommonRepository
import com.sonique.app.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val commonRepository: CommonRepository,
) : BaseViewModel() {
    private var _listNotification: MutableStateFlow<List<NotificationEntity>?> =
        MutableStateFlow(null)
    val listNotification: StateFlow<List<NotificationEntity>?> = _listNotification

    init {
        viewModelScope.launch {
            commonRepository.getAllNotifications().collect { notificationEntities ->
                _listNotification.value =
                    notificationEntities?.sortedByDescending {
                        it.time
                    }
            }
        }
    }
    
    fun clearAllNotifications() {
        viewModelScope.launch {
            _listNotification.value?.forEach { notification ->
                notification.id?.let { id ->
                    commonRepository.deleteNotification(id)
                }
            }
            // Clear local state immediately for instant UI update
            _listNotification.value = emptyList()
        }
    }
}

