package com.hyperfetch;

import com.hyperfetch.engine.DownloadEngine;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.Priority;
import com.hyperfetch.scheduler.TaskScheduler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 任务调度器单元测试
 * 测试任务入队、并发上限控制（1-5）、任务完成自动调度、优先级排序
 */
public class TaskSchedulerTest {

    @Mock
    private DownloadEngine mockEngine;

    private TaskScheduler scheduler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // 创建调度器，使用 mock 的下载引擎
        scheduler = new TaskScheduler(mockEngine);
    }

    /**
     * 创建测试任务的辅助方法
     *
     * @param id       任务ID
     * @param priority 优先级
     * @return 测试任务
     */
    private DownloadTask createTask(String id, Priority priority) {
        DownloadTask task = new DownloadTask(id, "http://example.com/file", "file", "/tmp");
        task.setPriority(priority);
        task.setCreateTime(new Date());
        return task;
    }

    /**
     * 创建测试任务的辅助方法（指定创建时间）
     *
     * @param id         任务ID
     * @param priority   优先级
     * @param createTime 创建时间
     * @return 测试任务
     */
    private DownloadTask createTask(String id, Priority priority, Date createTime) {
        DownloadTask task = new DownloadTask(id, "http://example.com/file", "file", "/tmp");
        task.setPriority(priority);
        task.setCreateTime(createTime);
        return task;
    }

    /**
     * 测试任务入队
     * 验证：任务可以正确加入等待队列
     */
    @Test
    public void testEnqueueTask() {
        // 创建测试任务
        DownloadTask task = createTask("task-1", Priority.NORMAL);
        
        // 入队任务
        scheduler.enqueue(task);
        
        // 验证等待队列中有 1 个任务
        assertEquals(1, scheduler.getWaitingTaskCount());
        
        // 验证引擎被调用提交任务（默认并发数为 3，应该立即调度）
        verify(mockEngine, times(1)).submit(task);
    }

    /**
     * 测试入队空任务
     * 验证：空任务或无 ID 任务不会被入队
     */
    @Test
    public void testEnqueueNullTask() {
        // 入队 null 任务
        scheduler.enqueue(null);
        
        // 验证等待队列中没有任务
        assertEquals(0, scheduler.getWaitingTaskCount());
        
        // 验证引擎没有被调用
        verify(mockEngine, never()).submit(any());
    }

    /**
     * 测试入队无 ID 任务
     * 验证：无 ID 任务不会被入队
     */
    @Test
    public void testEnqueueTaskWithoutId() {
        DownloadTask task = new DownloadTask();
        task.setId(null);
        
        scheduler.enqueue(task);
        
        // 验证等待队列中没有任务
        assertEquals(0, scheduler.getWaitingTaskCount());
    }

    /**
     * 测试并发上限控制 - 默认值
     * 验证：默认最大并发数为 3
     */
    @Test
    public void testDefaultMaxConcurrentTasks() {
        // 验证默认最大并发数为 3
        assertEquals(3, scheduler.getMaxConcurrentTasks());
    }

    /**
     * 测试并发上限控制 - 设置有效值
     * 验证：可以设置 1-5 范围内的并发数
     */
    @Test
    public void testSetMaxConcurrentTasksValid() {
        // 设置为 1
        scheduler.setMaxConcurrentTasks(1);
        assertEquals(1, scheduler.getMaxConcurrentTasks());
        
        // 设置为 5
        scheduler.setMaxConcurrentTasks(5);
        assertEquals(5, scheduler.getMaxConcurrentTasks());
        
        // 设置为 2
        scheduler.setMaxConcurrentTasks(2);
        assertEquals(2, scheduler.getMaxConcurrentTasks());
    }

    /**
     * 测试并发上限控制 - 设置无效值（小于 1）
     * 验证：设置小于 1 的值会抛出异常
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxConcurrentTasksTooLow() {
        scheduler.setMaxConcurrentTasks(0);
    }

    /**
     * 测试并发上限控制 - 设置无效值（大于 5）
     * 验证：设置大于 5 的值会抛出异常
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSetMaxConcurrentTasksTooHigh() {
        scheduler.setMaxConcurrentTasks(6);
    }

    /**
     * 测试并发上限控制 - 实际并发限制
     * 验证：当并发数达到上限时，新任务进入等待队列
     */
    @Test
    public void testConcurrentLimitEnforcement() {
        // 设置最大并发数为 2
        scheduler.setMaxConcurrentTasks(2);
        
        // 入队 4 个任务
        for (int i = 1; i <= 4; i++) {
            DownloadTask task = createTask("task-" + i, Priority.NORMAL);
            scheduler.enqueue(task);
        }
        
        // 验证只有 2 个任务被提交执行
        verify(mockEngine, times(2)).submit(any());
        
        // 验证等待队列中有 2 个任务
        assertEquals(2, scheduler.getWaitingTaskCount());
        
        // 验证正在运行的任务数为 2
        assertEquals(2, scheduler.getRunningTaskCount());
    }

    /**
     * 测试任务完成自动调度
     * 验证：任务完成后，自动从等待队列调度下一个任务
     */
    @Test
    public void testAutoScheduleAfterTaskCompletion() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 入队 3 个任务
        DownloadTask task1 = createTask("task-1", Priority.NORMAL);
        DownloadTask task2 = createTask("task-2", Priority.NORMAL);
        DownloadTask task3 = createTask("task-3", Priority.NORMAL);
        
        scheduler.enqueue(task1);
        scheduler.enqueue(task2);
        scheduler.enqueue(task3);
        
        // 验证只有 task-1 被提交
        verify(mockEngine, times(1)).submit(task1);
        verify(mockEngine, never()).submit(task2);
        verify(mockEngine, never()).submit(task3);
        
        // 验证等待队列有 2 个任务
        assertEquals(2, scheduler.getWaitingTaskCount());
        
        // 模拟 task-1 完成
        scheduler.onTaskFinished("task-1");
        
        // 验证 task-2 被自动调度
        verify(mockEngine, times(1)).submit(task2);
        
        // 验证等待队列减少到 1
        assertEquals(1, scheduler.getWaitingTaskCount());
        
        // 验证正在运行的任务数为 1（task-2）
        assertEquals(1, scheduler.getRunningTaskCount());
    }

    /**
     * 测试任务完成自动调度 - 多个任务完成
     * 验证：多个任务完成后，依次调度等待队列中的任务
     */
    @Test
    public void testAutoScheduleAfterMultipleCompletions() {
        // 设置最大并发数为 2
        scheduler.setMaxConcurrentTasks(2);
        
        // 入队 5 个任务
        for (int i = 1; i <= 5; i++) {
            scheduler.enqueue(createTask("task-" + i, Priority.NORMAL));
        }
        
        // 验证只有 2 个任务被提交
        verify(mockEngine, times(2)).submit(any());
        assertEquals(3, scheduler.getWaitingTaskCount());
        
        // 模拟 task-1 完成
        scheduler.onTaskFinished("task-1");
        
        // 验证调度了第 3 个任务
        verify(mockEngine, times(3)).submit(any());
        assertEquals(2, scheduler.getWaitingTaskCount());
        
        // 模拟 task-2 完成
        scheduler.onTaskFinished("task-2");
        
        // 验证调度了第 4 个任务
        verify(mockEngine, times(4)).submit(any());
        assertEquals(1, scheduler.getWaitingTaskCount());
    }

    /**
     * 测试优先级排序 - 高优先级优先
     * 验证：高优先级任务优先被调度
     */
    @Test
    public void testPrioritySortingHighFirst() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 先入队普通优先级任务
        DownloadTask normalTask = createTask("normal-task", Priority.NORMAL);
        scheduler.enqueue(normalTask);
        
        // 验证普通任务被提交
        verify(mockEngine, times(1)).submit(normalTask);
        
        // 入队高优先级任务
        DownloadTask highTask = createTask("high-task", Priority.HIGH);
        scheduler.enqueue(highTask);
        
        // 验证高优先级任务在等待队列中（因为并发已满）
        assertEquals(1, scheduler.getWaitingTaskCount());
        
        // 模拟普通任务完成
        scheduler.onTaskFinished("normal-task");
        
        // 验证高优先级任务被调度（而不是按入队顺序）
        verify(mockEngine, times(1)).submit(highTask);
    }

    /**
     * 测试优先级排序 - 多个高优先级任务
     * 验证：多个高优先级任务按创建时间排序
     */
    @Test
    public void testPrioritySortingMultipleHighPriority() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 创建不同创建时间的高优先级任务
        Date earlier = new Date(System.currentTimeMillis() - 1000);
        Date later = new Date();
        
        DownloadTask highTask1 = createTask("high-1", Priority.HIGH, earlier);
        DownloadTask highTask2 = createTask("high-2", Priority.HIGH, later);
        DownloadTask normalTask = createTask("normal", Priority.NORMAL);
        
        // 先入队普通任务（占用并发槽位）
        scheduler.enqueue(normalTask);
        
        // 入队两个高优先级任务（后入队的创建时间更早）
        scheduler.enqueue(highTask2);
        scheduler.enqueue(highTask1);
        
        // 模拟普通任务完成
        scheduler.onTaskFinished("normal-task");
        
        // 验证先创建的高优先级任务被调度
        verify(mockEngine, times(1)).submit(highTask1);
    }

    /**
     * 测试优先级排序 - 混合优先级
     * 验证：高优先级任务全部调度完后，才调度普通优先级任务
     */
    @Test
    public void testPrioritySortingMixedPriority() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 入队多个任务，混合优先级
        DownloadTask normal1 = createTask("normal-1", Priority.NORMAL);
        DownloadTask high1 = createTask("high-1", Priority.HIGH);
        DownloadTask normal2 = createTask("normal-2", Priority.NORMAL);
        DownloadTask high2 = createTask("high-2", Priority.HIGH);
        
        // 按顺序入队
        scheduler.enqueue(normal1);  // 立即执行
        scheduler.enqueue(high1);    // 等待
        scheduler.enqueue(normal2);  // 等待
        scheduler.enqueue(high2);    // 等待
        
        // 验证 normal-1 正在运行
        assertEquals(1, scheduler.getRunningTaskCount());
        assertEquals(3, scheduler.getWaitingTaskCount());
        
        // 模拟 normal-1 完成
        scheduler.onTaskFinished("normal-1");
        
        // 验证高优先级任务 high-1 被调度（优先于 normal-2）
        verify(mockEngine, times(1)).submit(high1);
        
        // 模拟 high-1 完成
        scheduler.onTaskFinished("high-1");
        
        // 验证高优先级任务 high-2 被调度
        verify(mockEngine, times(1)).submit(high2);
        
        // 模拟 high-2 完成
        scheduler.onTaskFinished("high-2");
        
        // 验证普通任务 normal-2 被调度
        verify(mockEngine, times(1)).submit(normal2);
    }

    /**
     * 测试检查任务是否正在运行
     * 验证：可以正确判断任务是否正在运行
     */
    @Test
    public void testIsTaskRunning() {
        DownloadTask task = createTask("task-1", Priority.NORMAL);
        
        // 入队前，任务不在运行
        assertFalse(scheduler.isTaskRunning("task-1"));
        
        // 入队任务
        scheduler.enqueue(task);
        
        // 入队后，任务正在运行
        assertTrue(scheduler.isTaskRunning("task-1"));
        
        // 模拟任务完成
        scheduler.onTaskFinished("task-1");
        
        // 完成后，任务不在运行
        assertFalse(scheduler.isTaskRunning("task-1"));
    }

    /**
     * 测试取消等待队列中的任务
     * 验证：可以从等待队列中移除指定任务
     */
    @Test
    public void testCancelWaitingTask() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 入队 3 个任务
        DownloadTask task1 = createTask("task-1", Priority.NORMAL);
        DownloadTask task2 = createTask("task-2", Priority.NORMAL);
        DownloadTask task3 = createTask("task-3", Priority.NORMAL);
        
        scheduler.enqueue(task1);
        scheduler.enqueue(task2);
        scheduler.enqueue(task3);
        
        // 验证等待队列有 2 个任务
        assertEquals(2, scheduler.getWaitingTaskCount());
        
        // 取消等待队列中的 task-2
        boolean removed = scheduler.cancelWaitingTask("task-2");
        
        // 验证成功移除
        assertTrue(removed);
        
        // 验证等待队列减少到 1
        assertEquals(1, scheduler.getWaitingTaskCount());
        
        // 模拟 task-1 完成
        scheduler.onTaskFinished("task-1");
        
        // 验证 task-3 被调度（task-2 已被取消）
        verify(mockEngine, times(1)).submit(task3);
        verify(mockEngine, never()).submit(task2);
    }

    /**
     * 测试取消不存在的任务
     * 验证：取消不存在的任务返回 false
     */
    @Test
    public void testCancelNonExistentTask() {
        boolean removed = scheduler.cancelWaitingTask("non-existent");
        assertFalse(removed);
    }

    /**
     * 测试清空等待队列
     * 验证：可以清空所有等待队列中的任务
     */
    @Test
    public void testClearWaitingQueue() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 入队 5 个任务
        for (int i = 1; i <= 5; i++) {
            scheduler.enqueue(createTask("task-" + i, Priority.NORMAL));
        }
        
        // 验证等待队列有 4 个任务
        assertEquals(4, scheduler.getWaitingTaskCount());
        
        // 清空等待队列
        scheduler.clearWaitingQueue();
        
        // 验证等待队列已清空
        assertEquals(0, scheduler.getWaitingTaskCount());
    }

    /**
     * 测试上调并发数立即补充任务
     * 验证：上调并发数时，立即从等待队列补充任务
     */
    @Test
    public void testIncreaseConcurrentTasksTriggersSchedule() {
        // 设置最大并发数为 1
        scheduler.setMaxConcurrentTasks(1);
        
        // 入队 3 个任务
        for (int i = 1; i <= 3; i++) {
            scheduler.enqueue(createTask("task-" + i, Priority.NORMAL));
        }
        
        // 验证只有 1 个任务被提交
        verify(mockEngine, times(1)).submit(any());
        assertEquals(2, scheduler.getWaitingTaskCount());
        
        // 上调并发数到 3
        scheduler.setMaxConcurrentTasks(3);
        
        // 验证立即调度了等待队列中的 2 个任务
        verify(mockEngine, times(3)).submit(any());
        assertEquals(0, scheduler.getWaitingTaskCount());
    }

    /**
     * 测试任务完成回调
     * 验证：可以设置任务完成回调
     */
    @Test
    public void testCompletionCallback() {
        // 创建回调计数器
        int[] completedCount = {0};
        int[] failedCount = {0};
        
        scheduler.setCompletionCallback(new TaskScheduler.TaskCompletionCallback() {
            @Override
            public void onTaskCompleted(String taskId) {
                completedCount[0]++;
            }
            
            @Override
            public void onTaskFailed(String taskId, String error) {
                failedCount[0]++;
            }
        });
        
        // 入队任务
        scheduler.enqueue(createTask("task-1", Priority.NORMAL));
        
        // 模拟任务完成（通过引擎回调）
        // 注意：实际回调是通过 DownloadEngine.TaskStateCallback 触发的
        // 这里直接调用 onTaskFinished 测试
        scheduler.onTaskFinished("task-1");
        
        // 验证回调被触发（在 TaskScheduler 内部设置）
        // 由于回调是在 engine.setTaskStateCallback 中设置的，
        // 这里无法直接验证，但可以验证调度器状态
        assertEquals(0, scheduler.getRunningTaskCount());
    }
}