package com.hyperfetch.scheduler

import com.hyperfetch.engine.DownloadEngine
import com.hyperfetch.event.EventBusWrapper
import com.hyperfetch.event.SchedulerConfigChangedEvent
import com.hyperfetch.model.DownloadTask
import com.hyperfetch.model.Priority
import com.hyperfetch.model.QueueStrategy
import com.hyperfetch.model.TaskStatus
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任务调度器
 * 控制同时下载任务数、队列排序、任务调度
 */
class TaskScheduler(
    private val engine: DownloadEngine,
    private val onTaskStatusChanged: (String, TaskStatus, TaskStatus) -> Unit
) {

    private val waitingQueue = PriorityQueue<DownloadTask> { a, b ->
        // 优先级优先，然后按创建时间
        val priorityCompare = b.priority.ordinal - a.priority.ordinal
        if (priorityCompare != 0) priorityCompare
        else a.createTime.compareTo(b.createTime)
    }

    private val runningTasks = mutableSetOf<String>()
    private val taskOrder = mutableMapOf<String, Long>() // 用于创建时间排序

    @Volatile
    private var maxConcurrentTasks = 3

    @Volatile
    private var multiTaskEnabled = true

    @Volatile
    private var queueStrategy = QueueStrategy.PRIORITY_FIRST

    private val runningCount = AtomicInteger(0)

    /**
     * 入队任务
     */
    @Synchronized
    fun enqueue(task: DownloadTask) {
        taskOrder[task.id] = System.currentTimeMillis()
        waitingQueue.add(task)
        tryScheduleNext()
    }

    /**
     * 任务完成回调
     */
    @Synchronized
    fun onTaskFinished(taskId: String) {
        runningTasks.remove(taskId)
        runningCount.decrementAndGet()
        tryScheduleNext()
    }

    /**
     * 任务失败回调
     */
    @Synchronized
    fun onTaskFailed(taskId: String) {
        runningTasks.remove(taskId)
        runningCount.decrementAndGet()
        tryScheduleNext()
    }

    /**
     * 移除任务
     */
    @Synchronized
    fun removeTask(taskId: String) {
        waitingQueue.removeIf { it.id == taskId }
        runningTasks.remove(taskId)
        taskOrder.remove(taskId)
        tryScheduleNext()
    }

    /**
     * 设置最大并发任务数
     */
    @Synchronized
    fun setMaxConcurrentTasks(count: Int) {
        require(count in 1..5) { "Concurrent tasks must be between 1 and 5" }
        maxConcurrentTasks = count
        EventBusWrapper.postMain(
            SchedulerConfigChangedEvent(maxConcurrentTasks, multiTaskEnabled)
        )
        tryScheduleNext()
    }

    /**
     * 设置多任务开关
     */
    @Synchronized
    fun setMultiTaskEnabled(enabled: Boolean) {
        multiTaskEnabled = enabled
        if (!enabled) {
            maxConcurrentTasks = 1
        } else {
            maxConcurrentTasks = 3.coerceAtMost(maxConcurrentTasks)
        }
        EventBusWrapper.postMain(
            SchedulerConfigChangedEvent(maxConcurrentTasks, multiTaskEnabled)
        )
        tryScheduleNext()
    }

    /**
     * 设置排队策略
     */
    @Synchronized
    fun setQueueStrategy(strategy: QueueStrategy) {
        queueStrategy = strategy
        resortQueue()
    }

    /**
     * 获取当前运行的任务数
     */
    fun getRunningCount(): Int = runningCount.get()

    /**
     * 获取等待队列大小
     */
    fun getWaitingCount(): Int = waitingQueue.size

    /**
     * 获取最大并发数
     */
    fun getMaxConcurrentTasks(): Int = maxConcurrentTasks

    /**
     * 是否启用多任务
     */
    fun isMultiTaskEnabled(): Boolean = multiTaskEnabled

    /**
     * 尝试调度下一个任务
     */
    private fun tryScheduleNext() {
        if (!multiTaskEnabled && runningCount.get() >= 1) {
            return
        }

        while (runningCount.get() < maxConcurrentTasks && waitingQueue.isNotEmpty()) {
            val nextTask = when (queueStrategy) {
                QueueStrategy.PRIORITY_FIRST -> {
                    // 优先级队列已按优先级排序
                    waitingQueue.poll()
                }
                QueueStrategy.TIME_FIRST -> {
                    // 按创建时间排序
                    waitingQueue.poll()
                }
            }

            if (nextTask != null) {
                runningTasks.add(nextTask.id)
                runningCount.incrementAndGet()
                engine.submit(nextTask)
                onTaskStatusChanged(nextTask.id, TaskStatus.QUEUED, TaskStatus.DOWNLOADING)
            }
        }
    }

    /**
     * 重新排序队列
     */
    private fun resortQueue() {
        val tasks = mutableListOf<DownloadTask>()
        while (waitingQueue.isNotEmpty()) {
            tasks.add(waitingQueue.poll())
        }

        val comparator = when (queueStrategy) {
            QueueStrategy.PRIORITY_FIRST -> Comparator<DownloadTask> { a, b ->
                val priorityCompare = b.priority.ordinal - a.priority.ordinal
                if (priorityCompare != 0) priorityCompare
                else taskOrder[a.id]!!.compareTo(taskOrder[b.id]!!)
            }
            QueueStrategy.TIME_FIRST -> Comparator<DownloadTask> { a, b ->
                taskOrder[a.id]!!.compareTo(taskOrder[b.id]!!)
            }
        }

        tasks.sortedWith(comparator).forEach { waitingQueue.add(it) }
    }

    /**
     * 清空调度器
     */
    fun clear() {
        waitingQueue.clear()
        runningTasks.clear()
        runningCount.set(0)
    }
}
