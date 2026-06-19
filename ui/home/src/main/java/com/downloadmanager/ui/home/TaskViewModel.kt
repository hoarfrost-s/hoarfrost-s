package com.downloadmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.downloadmanager.common.DownloadTask
import com.downloadmanager.common.TaskStatus
import com.downloadmanager.domain.repository.CategoryRepository
import com.downloadmanager.domain.repository.TaskRepository
import com.downloadmanager.domain.usecase.AddTaskUseCase
import com.downloadmanager.domain.usecase.CancelTaskUseCase
import com.downloadmanager.domain.usecase.PauseTaskUseCase
import com.downloadmanager.domain.usecase.ResumeTaskUseCase
import com.downloadmanager.engine.TaskScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val addTaskUseCase: AddTaskUseCase,
    private val pauseTaskUseCase: PauseTaskUseCase,
    private val resumeTaskUseCase: ResumeTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase,
    private val taskScheduler: TaskScheduler
) : ViewModel() {

    init {
        viewModelScope.launch {
            val pendingTasks = taskRepository.getQueuedTasks()
            taskScheduler.restoreTasks(pendingTasks)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<TaskStatus?>(null)
    private val _isMultiSelectMode = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val selectedStatus: StateFlow<TaskStatus?> = _selectedStatus.asStateFlow()
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()
    val selectedTaskIds: StateFlow<Set<String>> = _selectedTaskIds.asStateFlow()

    val allTasks: StateFlow<List<DownloadTask>> = taskRepository.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTasks: StateFlow<List<DownloadTask>> = combine(
        allTasks, _searchQuery, _selectedStatus
    ) { tasks, query, status ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() || task.fileName.contains(query, ignoreCase = true)
            val matchesStatus = status == null || task.status == status
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChanged(status: TaskStatus?) {
        _selectedStatus.value = status
    }

    fun addTask(url: String) {
        viewModelScope.launch {
            val task = addTaskUseCase.invoke(url)
            taskScheduler.submit(task)
        }
    }

    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            pauseTaskUseCase.invoke(taskId)
            taskScheduler.pauseTask(taskId)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            resumeTaskUseCase.invoke(taskId)
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                taskScheduler.submit(task)
            }
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            cancelTaskUseCase.invoke(taskId)
            taskScheduler.cancelTask(taskId)
        }
    }

    fun toggleTaskSelection(taskId: String) {
        _selectedTaskIds.value = _selectedTaskIds.value.let { current ->
            if (taskId in current) current - taskId else current + taskId
        }
    }

    fun enterMultiSelectMode(taskId: String) {
        _isMultiSelectMode.value = true
        _selectedTaskIds.value = setOf(taskId)
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedTaskIds.value = emptySet()
    }

    fun batchPause() {
        viewModelScope.launch {
            _selectedTaskIds.value.forEach { pauseTask(it) }
            exitMultiSelectMode()
        }
    }

    fun batchCancel() {
        viewModelScope.launch {
            _selectedTaskIds.value.forEach { cancelTask(it) }
            exitMultiSelectMode()
        }
    }
}