package com.downloadmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.app.data.entity.DownloadStatus
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadUiState(
    val tasks: List<DownloadTaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val filterStatus: DownloadStatus? = null,
    val selectedTaskIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<DownloadStatus?>(null)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DownloadUiState> = combine(
        _filterStatus,
        _selectedTaskIds,
        _isMultiSelectMode,
        _errorMessage,
        downloadRepository.observeAllTasks()
    ) { filterStatus, selectedTaskIds, isMultiSelectMode, errorMessage, allTasks ->
        val filteredTasks = if (filterStatus != null) {
            allTasks.filter { it.status == filterStatus }
        } else {
            allTasks
        }
        DownloadUiState(
            tasks = filteredTasks,
            isLoading = false,
            errorMessage = errorMessage,
            filterStatus = filterStatus,
            selectedTaskIds = selectedTaskIds,
            isMultiSelectMode = isMultiSelectMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadUiState()
    )

    fun setFilterStatus(status: DownloadStatus?) {
        _filterStatus.value = status
    }

    suspend fun addDownload(url: String, fileName: String? = null): String? {
        return try {
            val taskId = downloadRepository.addDownload(url, fileName)
            _errorMessage.value = null
            taskId
        } catch (e: Exception) {
            _errorMessage.value = e.message
            null
        }
    }

    fun pauseDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.pauseDownload(taskId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun resumeDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.resumeDownload(taskId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun cancelDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.cancelDownload(taskId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun retryDownload(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.retryDownload(taskId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            try {
                downloadRepository.pauseAll()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            try {
                downloadRepository.resumeAll()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun cancelAll() {
        viewModelScope.launch {
            try {
                downloadRepository.cancelAll()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun toggleTaskSelection(taskId: String) {
        val currentSelected = _selectedTaskIds.value.toMutableSet()
        if (currentSelected.contains(taskId)) {
            currentSelected.remove(taskId)
        } else {
            currentSelected.add(taskId)
        }
        _selectedTaskIds.value = currentSelected
        if (currentSelected.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun enterMultiSelectMode(taskId: String) {
        _isMultiSelectMode.value = true
        _selectedTaskIds.value = setOf(taskId)
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedTaskIds.value = emptySet()
    }

    fun deleteSelectedTasks() {
        val taskIds = _selectedTaskIds.value
        viewModelScope.launch {
            try {
                taskIds.forEach { taskId ->
                    downloadRepository.deleteTask(taskId)
                }
                _errorMessage.value = null
                clearSelection()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
}
