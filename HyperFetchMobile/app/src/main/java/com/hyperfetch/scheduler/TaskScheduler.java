package com.hyperfetch.scheduler;

import com.hyperfetch.engine.DownloadEngine;
import com.hyperfetch.model.DownloadTask;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 多任务调度器
 * 负责管理下载任务的排队和并发调度
 *
 * 功能特点：
 * 1. 使用优先级队列管理等待中的任务
 * 2. 支持动态调整最大并发数（1-5）
 * 3. 支持任务入队和完成回调
 * 4. 线程安全设计
 */
public class TaskScheduler {

    /**
     * 等待队列 - 优先级队列
     * 按照任务优先级和创建时间排序
     */
    private final PriorityQueue<DownloadTask> waitingQueue;

    /**
     * 正在运行的任务ID集合
     * 用于跟踪当前正在执行的任务
     */
    private final Set<String> runningTasks;

    /**
     * 最大并发任务数
     * 使用volatile保证可见性
     */
    private volatile int maxConcurrentTasks = 3;

    /**
     * 下载引擎引用
     * 用于提交任务执行
     */
    private final DownloadEngine engine;

    /**
     * 任务完成回调接口
     */
    private TaskCompletionCallback completionCallback;

    /**
     * 任务完成回调接口定义
     */
    public interface TaskCompletionCallback {
        /**
         * 任务完成回调
         *
         * @param taskId 任务ID
         */
        void onTaskCompleted(String taskId);

        /**
         * 任务失败回调
         *
         * @param taskId 任务ID
         * @param error  错误信息
         */
        void onTaskFailed(String taskId, String error);
    }

    /**
     * 构造函数
     *
     * @param engine 下载引擎实例
     */
    public TaskScheduler(DownloadEngine engine) {
        this.engine = engine;
        this.waitingQueue = new PriorityQueue<>(new TaskComparator());
        this.runningTasks = new HashSet<>();

        // 设置下载引擎的任务状态回调
        engine.setTaskStateCallback(new DownloadEngine.TaskStateCallback() {
            @Override
            public void onTaskCompleted(String taskId) {
                onTaskFinished(taskId);
                if (completionCallback != null) {
                    completionCallback.onTaskCompleted(taskId);
                }
            }

            @Override
            public void onTaskFailed(String taskId, String error) {
                onTaskFinished(taskId);
                if (completionCallback != null) {
                    completionCallback.onTaskFailed(taskId, error);
                }
            }
        });
    }

    /**
     * 将任务加入等待队列
     * 线程安全方法
     *
     * @param task 下载任务
     */
    public synchronized void enqueue(DownloadTask task) {
        if (task == null || task.getId() == null) {
            return;
        }
        waitingQueue.add(task);
        tryScheduleNext();
    }

    /**
     * 任务完成时的回调处理
     * 从运行集合中移除任务，并尝试调度下一个任务
     * 线程安全方法
     *
     * @param taskId 完成的任务ID
     */
    public synchronized void onTaskFinished(String taskId) {
        runningTasks.remove(taskId);
        tryScheduleNext();
    }

    /**
     * 尝试调度下一个任务
     * 当有空闲槽位且等待队列不为空时，从队列中取出任务执行
     */
    private void tryScheduleNext() {
        while (runningTasks.size() < maxConcurrentTasks && !waitingQueue.isEmpty()) {
            DownloadTask next = waitingQueue.poll();
            if (next != null) {
                runningTasks.add(next.getId());
                engine.submit(next);
            }
        }
    }

    /**
     * 设置最大并发任务数
     * 取值范围：1-5
     * 当上调并发数时，会立即尝试补充新任务
     *
     * @param value 最大并发数
     * @throws IllegalArgumentException 当值不在有效范围内时抛出
     */
    public void setMaxConcurrentTasks(int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("最大并发数必须在1到5之间");
        }
        synchronized (this) {
            this.maxConcurrentTasks = value;
            tryScheduleNext();  // 上调时立即补充
        }
    }

    /**
     * 获取最大并发任务数
     *
     * @return 最大并发数
     */
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    /**
     * 获取当前正在运行的任务数
     *
     * @return 正在运行的任务数
     */
    public synchronized int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 获取等待队列中的任务数
     *
     * @return 等待队列长度
     */
    public synchronized int getWaitingTaskCount() {
        return waitingQueue.size();
    }

    /**
     * 检查指定任务是否正在运行
     *
     * @param taskId 任务ID
     * @return 是否正在运行
     */
    public synchronized boolean isTaskRunning(String taskId) {
        return runningTasks.contains(taskId);
    }

    /**
     * 从等待队列中移除指定任务
     *
     * @param taskId 任务ID
     * @return 是否成功移除
     */
    public synchronized boolean cancelWaitingTask(String taskId) {
        return waitingQueue.removeIf(task -> task.getId().equals(taskId));
    }

    /**
     * 清空等待队列
     */
    public synchronized void clearWaitingQueue() {
        waitingQueue.clear();
    }

    /**
     * 设置任务完成回调
     *
     * @param callback 回调接口
     */
    public void setCompletionCallback(TaskCompletionCallback callback) {
        this.completionCallback = callback;
    }
}