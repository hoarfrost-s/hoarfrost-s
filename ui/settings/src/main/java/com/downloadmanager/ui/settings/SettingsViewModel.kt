package com.downloadmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val threadCount: StateFlow<Int> = configRepository.threadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val maxConcurrency: StateFlow<Int> = configRepository.maxConcurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val speedLimit: StateFlow<Long> = configRepository.speedLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val defaultSavePath: StateFlow<String> = configRepository.defaultSavePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Download/DM")

    val cellularAutoLimit: StateFlow<Boolean> = configRepository.cellularAutoLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val maxRetries: StateFlow<Int> = configRepository.maxRetries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val themeMode: StateFlow<Int> = configRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setThreadCount(value: Int) {
        viewModelScope.launch { configRepository.setThreadCount(value.coerceIn(1, 9)) }
    }

    fun setMaxConcurrency(value: Int) {
        viewModelScope.launch { configRepository.setMaxConcurrency(value.coerceIn(1, 5)) }
    }

    fun setSpeedLimit(bytesPerSecond: Long) {
        viewModelScope.launch { configRepository.setSpeedLimit(bytesPerSecond) }
    }

    fun setDefaultSavePath(path: String) {
        viewModelScope.launch { configRepository.setDefaultSavePath(path) }
    }

    fun setCellularAutoLimit(enabled: Boolean) {
        viewModelScope.launch { configRepository.setCellularAutoLimit(enabled) }
    }

    fun setMaxRetries(value: Int) {
        viewModelScope.launch { configRepository.setMaxRetries(value.coerceIn(0, 10)) }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { configRepository.setThemeMode(mode) }
    }
}