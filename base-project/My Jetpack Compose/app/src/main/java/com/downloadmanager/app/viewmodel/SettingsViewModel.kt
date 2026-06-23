package com.downloadmanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.app.engine.DownloadEngine
import com.downloadmanager.app.repository.SettingsData
import com.downloadmanager.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: SettingsData = SettingsData(),
    val isLoading: Boolean = true,
    val showSpeedLimitDialog: Boolean = false,
    val showPathDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    private val _showSpeedLimitDialog = MutableStateFlow(false)
    private val _showPathDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        _showSpeedLimitDialog,
        _showPathDialog
    ) { settings, showSpeedLimitDialog, showPathDialog ->
        SettingsUiState(
            settings = settings,
            isLoading = false,
            showSpeedLimitDialog = showSpeedLimitDialog,
            showPathDialog = showPathDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThreadCount(count: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setThreadCount(count)
            } catch (e: Exception) {
            }
        }
    }

    fun setMaxConcurrent(count: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setMaxConcurrent(count)
                downloadEngine.setMaxConcurrentTasks(count)
            } catch (e: Exception) {
            }
        }
    }

    fun setGlobalSpeedLimit(bytesPerSecond: Long) {
        viewModelScope.launch {
            try {
                settingsRepository.setGlobalSpeedLimit(bytesPerSecond)
                downloadEngine.setGlobalSpeedLimit(bytesPerSecond)
            } catch (e: Exception) {
            }
        }
    }

    fun setDownloadRootPath(path: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setDownloadRootPath(path)
            } catch (e: Exception) {
            }
        }
    }

    fun setEnableEncryption(enable: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setEnableEncryption(enable)
            } catch (e: Exception) {
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(mode)
            } catch (e: Exception) {
            }
        }
    }

    fun setEnableClipboardDetect(enable: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setEnableClipboardDetect(enable)
            } catch (e: Exception) {
            }
        }
    }

    fun showSpeedLimitDialog(show: Boolean) {
        _showSpeedLimitDialog.value = show
    }

    fun showPathDialog(show: Boolean) {
        _showPathDialog.value = show
    }
}
