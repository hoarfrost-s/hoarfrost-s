package com.downloadmanager.domain.usecase

import com.downloadmanager.common.TaskStatus
import com.downloadmanager.domain.repository.TaskRepository

class CancelTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun invoke(taskId: String) {
        val task = taskRepository.getTaskById(taskId) ?: return
        if (task.status.canTransitionTo(TaskStatus.CANCELLED)) {
            taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED)
        }
    }
}