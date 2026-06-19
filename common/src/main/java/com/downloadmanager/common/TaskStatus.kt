package com.downloadmanager.common

enum class TaskStatus(val displayName: String) {
    WAITING("等待中"),
    QUEUED("排队中"),
    DOWNLOADING("下载中"),
    MERGING("合并中"),
    COMPLETED("已完成"),
    PAUSED("已暂停"),
    FAILED("失败"),
    CANCELLED("已取消");

    fun canTransitionTo(target: TaskStatus): Boolean {
        return when (this) {
            WAITING -> target in setOf(QUEUED, DOWNLOADING, CANCELLED)
            QUEUED -> target in setOf(DOWNLOADING, PAUSED, CANCELLED)
            DOWNLOADING -> target in setOf(MERGING, PAUSED, FAILED, CANCELLED)
            MERGING -> target in setOf(COMPLETED, FAILED)
            PAUSED -> target in setOf(DOWNLOADING, CANCELLED)
            FAILED -> target in setOf(WAITING, CANCELLED)
            COMPLETED -> false
            CANCELLED -> false
        }
    }

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
    val isActive: Boolean get() = this == DOWNLOADING || this == MERGING
    val isWaiting: Boolean get() = this == WAITING || this == QUEUED
}