package com.downloadmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.app.category.CategoryInfo
import com.downloadmanager.app.data.entity.DownloadTaskEntity
import com.downloadmanager.app.repository.CategoryRepository
import com.downloadmanager.app.repository.DownloadRepository
import com.downloadmanager.app.repository.SettingsRepository
import com.downloadmanager.app.util.ClipboardHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val categories: List<CategoryInfo> = emptyList(),
    val recentTasks: List<DownloadTaskEntity> = emptyList(),
    val totalDownloadedSize: Long = 0L,
    val todayDownloadCount: Int = 0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val clipboardUrl: String? = null,
    val lastClipboardContent: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clipboardHelper: ClipboardHelper
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    private val _clipboardUrl = MutableStateFlow<String?>(null)
    private val _lastClipboardContent = MutableStateFlow<String?>(null)
    private val _totalDownloadedSize = MutableStateFlow(0L)
    private val _todayDownloadCount = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        categoryRepository.getAllCategories(),
        downloadRepository.observeRecentTasks(10),
        _totalDownloadedSize,
        _todayDownloadCount,
        _showAddDialog,
        _clipboardUrl,
        _lastClipboardContent,
        _isLoading
    ) { categories, recentTasks, totalDownloadedSize, todayDownloadCount, showAddDialog, clipboardUrl, lastClipboardContent, isLoading ->
        HomeUiState(
            categories = categories,
            recentTasks = recentTasks,
            totalDownloadedSize = totalDownloadedSize,
            todayDownloadCount = todayDownloadCount,
            isLoading = isLoading,
            showAddDialog = showAddDialog,
            clipboardUrl = clipboardUrl,
            lastClipboardContent = lastClipboardContent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        loadStats()
    }

    fun showAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun addDownloadFromUi(url: String, fileName: String? = null) {
        viewModelScope.launch {
            addDownload(url, fileName)
        }
    }

    suspend fun addDownload(url: String, fileName: String? = null): String? {
        return try {
            val taskId = downloadRepository.addDownload(url, fileName)
            _showAddDialog.value = false
            taskId
        } catch (e: Exception) {
            null
        }
    }

    fun setClipboardUrl(url: String?) {
        _clipboardUrl.value = url
        url?.let {
            _lastClipboardContent.value = it
        }
    }

    fun clearClipboardUrl() {
        _clipboardUrl.value = null
    }

    fun checkClipboard() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                if (!settings.enableClipboardDetect) {
                    return@launch
                }

                val clipboardUrl = clipboardHelper.getClipboardUrl() ?: return@launch
                val lastContent = _lastClipboardContent.value

                if (clipboardUrl != lastContent) {
                    _clipboardUrl.value = clipboardUrl
                    _lastClipboardContent.value = clipboardUrl
                }
            } catch (e: Exception) {
            }
        }
    }

    fun handleShareUrl(url: String) {
        viewModelScope.launch {
            try {
                if (clipboardHelper.isUrl(url)) {
                    _clipboardUrl.value = url
                    _showAddDialog.value = true
                }
            } catch (e: Exception) {
            }
        }
    }

    fun openFileFromNotification(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.openFile(taskId)
            } catch (e: Exception) {
            }
        }
    }

    fun shareFileFromNotification(taskId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.launchShareFile(taskId)
            } catch (e: Exception) {
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val totalSize = downloadRepository.getTotalDownloadedSize()
                val todayCount = downloadRepository.getTodayDownloadCount()
                _totalDownloadedSize.value = totalSize
                _todayDownloadCount.value = todayCount
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
