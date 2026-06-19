package com.downloadmanager.domain.usecase

import com.downloadmanager.common.TaskStatus
import com.downloadmanager.domain.repository.TaskRepository

class ResumeTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun invoke(taskId: String) {
        val task = taskRepository.getTaskById(taskId) ?: return
        if (task.status.canTransitionTo(TaskStatus.DOWNLOADING)) {
            taskRepository.updateTaskStatus(taskId, TaskStatus.WAITING)
        }
    }
}