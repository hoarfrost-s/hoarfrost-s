package com.hyperfetch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hyperfetch.event.EventBusWrapper
import com.hyperfetch.event.ProgressEvent
import com.hyperfetch.model.*
import com.hyperfetch.service.TaskService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 */
class MainViewModel(
    private val taskService: TaskService
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val _totalSpeed = MutableStateFlow(0L)
    val totalSpeed: StateFlow<Long> = _totalSpeed.asStateFlow()

    private val _downloadingCount = MutableStateFlow(0)
    val downloadingCount: StateFlow<Int> = _downloadingCount.asStateFlow()

    private val _showNewTaskSheet = MutableStateFlow(false)
    val showNewTaskSheet: StateFlow<Boolean> = _showNewTaskSheet.asStateFlow()

    private val _schedulerConfig = MutableStateFlow(SchedulerConfig())
    val schedulerConfig: StateFlow<SchedulerConfig> = _schedulerConfig.asStateFlow()

    init {
        // 收集任务列表
        viewModelScope.launch {
            taskService.tasks.collect { taskList ->
                _tasks.value = taskList
                _downloadingCount.value = taskList.count { it.isDownloading }
                _totalSpeed.value = taskList.filter { it.isDownloading }
                    .sumOf { it.speed }
            }
        }

        // 收集调度配置
        viewModelScope.launch {
            taskService.schedulerConfig.collect {
                _schedulerConfig.value = it
            }
        }
    }

    val filteredTasks: StateFlow<List<DownloadTask>> = combine(
        _tasks,
        _selectedCategory
    ) { tasks, category ->
        if (category == null) {
            tasks
        } else {
            tasks.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryStats: StateFlow<Map<Category, Int>> = _tasks.map { taskList ->
        taskList.groupBy { it.category }
            .mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun showNewTaskSheet() {
        _showNewTaskSheet.value = true
    }

    fun hideNewTaskSheet() {
        _showNewTaskSheet.value = false
    }

    fun createTask(url: String, options: DownloadOptions) {
        viewModelScope.launch {
            taskService.createTask(url, options)
            hideNewTaskSheet()
        }
    }

    fun startTask(taskId: String) {
        viewModelScope.launch {
            taskService.start(taskId)
        }
    }

    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            taskService.pause(taskId)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            taskService.resume(taskId)
        }
    }

    fun deleteTask(taskId: String, deleteFile: Boolean = false) {
        viewModelScope.launch {
            taskService.delete(taskId, deleteFile)
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            taskService.retry(taskId)
        }
    }

    fun setMaxConcurrentTasks(count: Int) {
        viewModelScope.launch {
            taskService.setMaxConcurrentTasks(count)
        }
    }

    fun setMultiTaskEnabled(enabled: Boolean) {
        viewModelScope.launch {
            taskService.setMultiTaskEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    class Factory(private val taskService: TaskService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(taskService) as T
        }
    }
}
