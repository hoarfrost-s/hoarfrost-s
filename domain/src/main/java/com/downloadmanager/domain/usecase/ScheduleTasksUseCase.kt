package com.downloadmanager.domain.usecase

import com.downloadmanager.common.TaskStatus
import com.downloadmanager.domain.repository.ConfigRepository
import com.downloadmanager.domain.repository.TaskRepository

class ScheduleTasksUseCase(
    private val taskRepository: TaskRepository,
    private val configRepository: ConfigRepository
) {
    suspend fun getNextTasksToStart(count: Int): List<String> {
        return taskRepository.getQueuedTasks()
            .take(count)
            .map { it.id }
    }
}