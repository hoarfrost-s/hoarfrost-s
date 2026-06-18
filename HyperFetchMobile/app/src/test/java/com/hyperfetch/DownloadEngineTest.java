package com.hyperfetch;

import com.hyperfetch.engine.DownloadConfig;
import com.hyperfetch.engine.DownloadEngine;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskChunk;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 下载引擎单元测试
 * 测试分块计算（splitChunks）、线程数范围约束（1-9）、线程数热调整
 */
public class DownloadEngineTest {

    private DownloadEngine engine;
    private DownloadConfig config;

    @Before
    public void setUp() {
        // 创建默认配置的下载引擎
        config = new DownloadConfig();
        engine = new DownloadEngine(config);
    }

    // ==================== 分块计算测试 ====================

    /**
     * 调用 splitChunks 私有方法的辅助方法
     * 使用反射访问私有方法
     *
     * @param taskId      任务ID
     * @param totalSize   文件总大小
     * @param threadCount 线程数
     * @return 分块列表
     */
    private List<TaskChunk> invokeSplitChunks(String taskId, long totalSize, int threadCount) {
        try {
            Method splitChunksMethod = DownloadEngine.class.getDeclaredMethod(
                    "splitChunks", String.class, long.class, int.class);
            splitChunksMethod.setAccessible(true);
            return (List<TaskChunk>) splitChunksMethod.invoke(engine, taskId, totalSize, threadCount);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("无法调用 splitChunks 方法", e);
        }
    }

    /**
     * 测试分块计算 - 基本场景
     * 验证：文件能正确按线程数等分
     */
    @Test
    public void testSplitChunksBasic() {
        String taskId = "test-task";
        long totalSize = 1000; // 1000 字节
        int threadCount = 4;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块数量等于线程数
        assertEquals(threadCount, chunks.size());
        
        // 验证每个分块的基本属性
        for (int i = 0; i < chunks.size(); i++) {
            TaskChunk chunk = chunks.get(i);
            assertEquals(taskId, chunk.getTaskId());
            assertEquals(i, chunk.getIndex());
        }
        
        // 验证分块覆盖整个文件
        long coveredSize = 0;
        for (TaskChunk chunk : chunks) {
            coveredSize += chunk.getChunkSize();
        }
        assertEquals(totalSize, coveredSize);
    }

    /**
     * 测试分块计算 - 单线程
     * 验证：单线程时分块覆盖整个文件
     */
    @Test
    public void testSplitChunksSingleThread() {
        String taskId = "test-task";
        long totalSize = 5000;
        int threadCount = 1;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证只有 1 个分块
        assertEquals(1, chunks.size());
        
        // 验证分块覆盖整个文件
        TaskChunk chunk = chunks.get(0);
        assertEquals(0, chunk.getStart());
        assertEquals(totalSize - 1, chunk.getEnd());
        assertEquals(totalSize, chunk.getChunkSize());
    }

    /**
     * 测试分块计算 - 多线程
     * 验证：多线程时分块正确划分
     */
    @Test
    public void testSplitChunksMultipleThreads() {
        String taskId = "test-task";
        long totalSize = 10000;
        int threadCount = 5;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块数量
        assertEquals(5, chunks.size());
        
        // 验证每个分块大小约为 2000 字节
        long expectedChunkSize = totalSize / threadCount;
        
        for (int i = 0; i < threadCount - 1; i++) {
            TaskChunk chunk = chunks.get(i);
            assertEquals(expectedChunkSize, chunk.getChunkSize());
            assertEquals(i * expectedChunkSize, chunk.getStart());
            assertEquals(i * expectedChunkSize + expectedChunkSize - 1, chunk.getEnd());
        }
        
        // 最后一个分块可能包含剩余字节
        TaskChunk lastChunk = chunks.get(threadCount - 1);
        assertEquals(threadCount - 1, lastChunk.getIndex());
        assertEquals((threadCount - 1) * expectedChunkSize, lastChunk.getStart());
        assertEquals(totalSize - 1, lastChunk.getEnd());
    }

    /**
     * 测试分块计算 - 不均匀分割
     * 验证：文件大小不能被线程数整除时，最后一个分块包含剩余字节
     */
    @Test
    public void testSplitChunksUnevenDivision() {
        String taskId = "test-task";
        long totalSize = 1003; // 不能被 4 整除
        int threadCount = 4;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块数量
        assertEquals(4, chunks.size());
        
        // 验证前 3 个分块大小为 250 字节
        long baseChunkSize = totalSize / threadCount;
        for (int i = 0; i < 3; i++) {
            assertEquals(baseChunkSize, chunks.get(i).getChunkSize());
        }
        
        // 最后一个分块包含剩余字节
        TaskChunk lastChunk = chunks.get(3);
        long expectedLastChunkSize = baseChunkSize + (totalSize % threadCount);
        assertEquals(expectedLastChunkSize, lastChunk.getChunkSize());
        
        // 验证总覆盖大小
        long coveredSize = 0;
        for (TaskChunk chunk : chunks) {
            coveredSize += chunk.getChunkSize();
        }
        assertEquals(totalSize, coveredSize);
    }

    /**
     * 测试分块计算 - 最大线程数（9）
     * 验证：9 线程时分块正确划分
     */
    @Test
    public void testSplitChunksMaxThreads() {
        String taskId = "test-task";
        long totalSize = 9000;
        int threadCount = 9;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块数量
        assertEquals(9, chunks.size());
        
        // 验证每个分块大小为 1000 字节
        for (TaskChunk chunk : chunks) {
            assertEquals(1000, chunk.getChunkSize());
        }
    }

    /**
     * 测试分块计算 - 分块边界连续性
     * 验证：相邻分块的边界连续，无重叠无间隙
     */
    @Test
    public void testSplitChunksBoundaryContinuity() {
        String taskId = "test-task";
        long totalSize = 1000;
        int threadCount = 3;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块边界连续
        for (int i = 0; i < chunks.size() - 1; i++) {
            TaskChunk current = chunks.get(i);
            TaskChunk next = chunks.get(i + 1);
            
            // 当前分块的 end + 1 应等于下一个分块的 start
            assertEquals(current.getEnd() + 1, next.getStart());
        }
        
        // 验证第一个分块从 0 开始
        assertEquals(0, chunks.get(0).getStart());
        
        // 验证最后一个分块到 totalSize - 1 结束
        assertEquals(totalSize - 1, chunks.get(chunks.size() - 1).getEnd());
    }

    /**
     * 测试分块计算 - 大文件
     * 验证：大文件也能正确分块
     */
    @Test
    public void testSplitChunksLargeFile() {
        String taskId = "test-task";
        long totalSize = 1024 * 1024 * 1024; // 1 GB
        int threadCount = 4;
        
        List<TaskChunk> chunks = invokeSplitChunks(taskId, totalSize, threadCount);
        
        // 验证分块数量
        assertEquals(4, chunks.size());
        
        // 验证总覆盖大小
        long coveredSize = 0;
        for (TaskChunk chunk : chunks) {
            coveredSize += chunk.getChunkSize();
        }
        assertEquals(totalSize, coveredSize);
    }

    // ==================== 线程数范围约束测试 ====================

    /**
     * 测试线程数范围约束 - 默认值
     * 验证：默认线程数为 4
     */
    @Test
    public void testDefaultThreadCount() {
        assertEquals(DownloadConfig.DEFAULT_THREAD_COUNT, config.getThreadCount());
        assertEquals(4, config.getThreadCount());
    }

    /**
     * 测试线程数范围约束 - 有效值（1-9）
     * 验证：可以设置 1-9 范围内的线程数
     */
    @Test
    public void testThreadCountValidRange() {
        // 测试最小值 1
        DownloadConfig config1 = new DownloadConfig(1);
        assertEquals(1, config1.getThreadCount());
        
        // 测试最大值 9
        DownloadConfig config9 = new DownloadConfig(9);
        assertEquals(9, config9.getThreadCount());
        
        // 测试中间值
        DownloadConfig config5 = new DownloadConfig(5);
        assertEquals(5, config5.getThreadCount());
        
        // 测试动态设置
        config.setThreadCount(3);
        assertEquals(3, config.getThreadCount());
        
        config.setThreadCount(7);
        assertEquals(7, config.getThreadCount());
    }

    /**
     * 测试线程数范围约束 - 小于最小值
     * 验证：设置小于 1 的值时，自动调整为最小值 1
     */
    @Test
    public void testThreadCountBelowMinimum() {
        // 设置为 0
        config.setThreadCount(0);
        assertEquals(DownloadConfig.MIN_THREAD_COUNT, config.getThreadCount());
        assertEquals(1, config.getThreadCount());
        
        // 设置为负数
        config.setThreadCount(-5);
        assertEquals(1, config.getThreadCount());
    }

    /**
     * 测试线程数范围约束 - 大于最大值
     * 验证：设置大于 9 的值时，自动调整为最大值 9
     */
    @Test
    public void testThreadCountAboveMaximum() {
        // 设置为 10
        config.setThreadCount(10);
        assertEquals(DownloadConfig.MAX_THREAD_COUNT, config.getThreadCount());
        assertEquals(9, config.getThreadCount());
        
        // 设置为更大值
        config.setThreadCount(100);
        assertEquals(9, config.getThreadCount());
    }

    /**
     * 测试线程数范围常量
     * 验证：线程数范围常量正确
     */
    @Test
    public void testThreadCountConstants() {
        assertEquals(1, DownloadConfig.MIN_THREAD_COUNT);
        assertEquals(9, DownloadConfig.MAX_THREAD_COUNT);
        assertEquals(4, DownloadConfig.DEFAULT_THREAD_COUNT);
    }

    // ==================== 线程数热调整测试 ====================

    /**
     * 测试线程数热调整 - 基本场景
     * 验证：可以动态调整线程数
     */
    @Test
    public void testSetThreadCountBasic() {
        // 创建下载引擎
        DownloadEngine testEngine = new DownloadEngine();
        
        // 验证可以设置线程数（在没有任务时）
        // 注意：setThreadCount 方法需要 taskId，这里测试配置层面的调整
        DownloadConfig testConfig = testEngine.getConfig();
        
        testConfig.setThreadCount(5);
        assertEquals(5, testConfig.getThreadCount());
        
        testConfig.setThreadCount(2);
        assertEquals(2, testConfig.getThreadCount());
    }

    /**
     * 测试线程数热调整 - 范围约束
     * 验证：热调整时也受范围约束
     */
    @Test
    public void testSetThreadCountRangeConstraint() {
        // 设置超出范围的值
        config.setThreadCount(0);
        assertEquals(1, config.getThreadCount());
        
        config.setThreadCount(15);
        assertEquals(9, config.getThreadCount());
        
        // 设置有效值
        config.setThreadCount(6);
        assertEquals(6, config.getThreadCount());
    }

    /**
     * 测试线程数热调整 - 从高到低
     * 验证：可以从高线程数调整到低线程数
     */
    @Test
    public void testSetThreadCountDecrease() {
        // 先设置高线程数
        config.setThreadCount(9);
        assertEquals(9, config.getThreadCount());
        
        // 降低线程数
        config.setThreadCount(3);
        assertEquals(3, config.getThreadCount());
        
        // 继续降低
        config.setThreadCount(1);
        assertEquals(1, config.getThreadCount());
    }

    /**
     * 测试线程数热调整 - 从低到高
     * 验证：可以从低线程数调整到高线程数
     */
    @Test
    public void testSetThreadCountIncrease() {
        // 先设置低线程数
        config.setThreadCount(1);
        assertEquals(1, config.getThreadCount());
        
        // 提高线程数
        config.setThreadCount(5);
        assertEquals(5, config.getThreadCount());
        
        // 继续提高
        config.setThreadCount(9);
        assertEquals(9, config.getThreadCount());
    }

    /**
     * 测试下载引擎配置获取
     * 验证：可以获取引擎的配置
     */
    @Test
    public void testGetConfig() {
        DownloadConfig engineConfig = engine.getConfig();
        assertNotNull(engineConfig);
        assertEquals(config, engineConfig);
    }

    /**
     * 测试下载引擎初始状态
     * 验证：引擎初始未关闭
     */
    @Test
    public void testEngineInitialState() {
        assertFalse(engine.isShutdown());
    }

    /**
     * 测试下载引擎关闭
     * 验证：引擎可以正常关闭
     */
    @Test
    public void testEngineShutdown() {
        engine.shutdown();
        assertTrue(engine.isShutdown());
    }

    /**
     * 测试并发任务数配置 - 默认值
     * 验证：默认最大并发任务数为 3
     */
    @Test
    public void testDefaultMaxConcurrentTasks() {
        assertEquals(DownloadConfig.DEFAULT_CONCURRENT_TASKS, config.getMaxConcurrentTasks());
        assertEquals(3, config.getMaxConcurrentTasks());
    }

    /**
     * 测试并发任务数配置 - 有效范围
     * 验证：并发任务数范围 1-5
     */
    @Test
    public void testMaxConcurrentTasksValidRange() {
        config.setMaxConcurrentTasks(1);
        assertEquals(1, config.getMaxConcurrentTasks());
        
        config.setMaxConcurrentTasks(5);
        assertEquals(5, config.getMaxConcurrentTasks());
        
        config.setMaxConcurrentTasks(3);
        assertEquals(3, config.getMaxConcurrentTasks());
    }

    /**
     * 测试并发任务数配置 - 超出范围
     * 验证：超出范围时自动调整
     */
    @Test
    public void testMaxConcurrentTasksOutOfRange() {
        config.setMaxConcurrentTasks(0);
        assertEquals(1, config.getMaxConcurrentTasks());
        
        config.setMaxConcurrentTasks(10);
        assertEquals(5, config.getMaxConcurrentTasks());
    }

    /**
     * 测试并发任务数常量
     * 验证：并发任务数常量正确
     */
    @Test
    public void testConcurrentTasksConstants() {
        assertEquals(1, DownloadConfig.MIN_CONCURRENT_TASKS);
        assertEquals(5, DownloadConfig.MAX_CONCURRENT_TASKS);
        assertEquals(3, DownloadConfig.DEFAULT_CONCURRENT_TASKS);
    }

    /**
     * 测试重试次数配置
     * 验证：重试次数默认为 5
     */
    @Test
    public void testRetryCount() {
        assertEquals(DownloadConfig.DEFAULT_RETRY_COUNT, config.getRetryCount());
        assertEquals(5, config.getRetryCount());
        
        // 设置重试次数
        config.setRetryCount(10);
        assertEquals(10, config.getRetryCount());
        
        // 设置负数重试次数
        config.setRetryCount(-1);
        assertEquals(0, config.getRetryCount());
    }

    /**
     * 测试重试间隔计算（指数退避）
     * 验证：重试间隔按指数增长：1s, 2s, 4s, 8s, 16s
     */
    @Test
    public void testRetryDelay() {
        // 第一次重试：1s
        assertEquals(1000, DownloadConfig.getRetryDelay(0));
        
        // 第二次重试：2s
        assertEquals(2000, DownloadConfig.getRetryDelay(1));
        
        // 第三次重试：4s
        assertEquals(4000, DownloadConfig.getRetryDelay(2));
        
        // 第四次重试：8s
        assertEquals(8000, DownloadConfig.getRetryDelay(3));
        
        // 第五次重试：16s
        assertEquals(16000, DownloadConfig.getRetryDelay(4));
    }

    /**
     * 测试超时配置
     * 验证：连接和读取超时配置正确
     */
    @Test
    public void testTimeoutConfig() {
        // 默认超时时间
        assertEquals(30000, config.getConnectTimeout());
        assertEquals(30000, config.getReadTimeout());
        
        // 设置超时时间
        config.setConnectTimeout(60000);
        assertEquals(60000, config.getConnectTimeout());
        
        config.setReadTimeout(60000);
        assertEquals(60000, config.getReadTimeout());
        
        // 设置无效超时时间（使用默认值）
        config.setConnectTimeout(0);
        assertEquals(DownloadConfig.CONNECT_TIMEOUT, config.getConnectTimeout());
        
        config.setReadTimeout(-1);
        assertEquals(DownloadConfig.READ_TIMEOUT, config.getReadTimeout());
    }

    /**
     * 测试缓冲区大小
     * 验证：缓冲区大小为 8KB
     */
    @Test
    public void testBufferSize() {
        assertEquals(8192, DownloadConfig.BUFFER_SIZE);
    }

    /**
     * 测试进度更新间隔
     * 验证：进度更新间隔为 1 秒
     */
    @Test
    public void testProgressUpdateInterval() {
        assertEquals(1000, DownloadConfig.PROGRESS_UPDATE_INTERVAL);
    }

    /**
     * 测试限速配置
     * 验证：默认无限制，可以设置限速
     */
    @Test
    public void testSpeedLimitConfig() {
        // 默认无限制
        assertEquals(0, config.getSpeedLimit());
        
        // 设置限速
        config.setSpeedLimit(1024 * 1024); // 1 MB/s
        assertEquals(1024 * 1024, config.getSpeedLimit());
        
        // 设置负数限速（调整为 0）
        config.setSpeedLimit(-1000);
        assertEquals(0, config.getSpeedLimit());
    }

    /**
     * 测试下载任务线程数设置
     * 验证：下载任务可以设置线程数
     */
    @Test
    public void testDownloadTaskThreadCount() {
        DownloadTask task = new DownloadTask("test-id", "http://example.com/file", "file", "/tmp");
        
        // 默认线程数为 3
        assertEquals(3, task.getThreadCount());
        
        // 设置线程数
        task.setThreadCount(5);
        assertEquals(5, task.getThreadCount());
        
        // 注意：任务层面的线程数设置不受范围约束，
        // 但在引擎执行时会使用配置的范围约束
    }

    /**
     * 测试配置 toString
     * 验证：配置可以正确转换为字符串
     */
    @Test
    public void testConfigToString() {
        String configStr = config.toString();
        assertNotNull(configStr);
        assertTrue(configStr.contains("threadCount"));
        assertTrue(configStr.contains("speedLimit"));
        assertTrue(configStr.contains("retryCount"));
        assertTrue(configStr.contains("maxConcurrentTasks"));
    }
}