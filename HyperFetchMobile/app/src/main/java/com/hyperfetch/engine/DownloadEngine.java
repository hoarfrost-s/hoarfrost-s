package com.hyperfetch.engine;

import com.hyperfetch.event.ProgressEvent;
import com.hyperfetch.model.ChunkStatus;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskChunk;
import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.rate.RateLimiter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多线程下载引擎
 * 负责管理下载任务的调度、分块下载和进度更新
 */
public class DownloadEngine {

    private static final String TAG = "DownloadEngine";

    /**
     * 下载配置
     */
    private final DownloadConfig config;

    /**
     * 任务调度线程池
     * 用于管理并发任务
     */
    private final ExecutorService taskScheduler;

    /**
     * 分块工作线程池映射
     * 每个任务独占一个线程池
     */
    private final Map<String, ThreadPoolExecutor> chunkWorkerPools;

    /**
     * 任务分块映射
     */
    private final Map<String, List<TaskChunk>> taskChunks;

    /**
     * ChunkWorker映射
     */
    private final Map<String, List<ChunkWorker>> chunkWorkers;

    /**
     * 限速器映射
     */
    private final Map<String, RateLimiter> rateLimiters;

    /**
     * 进度更新调度器
     */
    private final ScheduledExecutorService progressScheduler;

    /**
     * 进度回调接口
     */
    private ProgressCallback progressCallback;

    /**
     * 任务状态回调接口
     */
    private TaskStateCallback taskStateCallback;

    /**
     * 引擎是否已关闭
     */
    private final AtomicBoolean isShutdown;

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        /**
         * 进度更新回调
         *
         * @param event 进度事件
         */
        void onProgress(ProgressEvent event);
    }

    /**
     * 任务状态回调接口
     */
    public interface TaskStateCallback {
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
     * 默认构造函数
     * 使用默认配置创建下载引擎
     */
    public DownloadEngine() {
        this(new DownloadConfig());
    }

    /**
     * 构造函数
     *
     * @param config 下载配置
     */
    public DownloadEngine(DownloadConfig config) {
        this.config = config;
        this.taskScheduler = Executors.newFixedThreadPool(config.getMaxConcurrentTasks());
        this.chunkWorkerPools = new ConcurrentHashMap<>();
        this.taskChunks = new ConcurrentHashMap<>();
        this.chunkWorkers = new ConcurrentHashMap<>();
        this.rateLimiters = new ConcurrentHashMap<>();
        this.progressScheduler = Executors.newSingleThreadScheduledExecutor();
        this.isShutdown = new AtomicBoolean(false);
    }

    /**
     * 提交下载任务
     *
     * @param task 下载任务
     */
    public void submit(DownloadTask task) {
        if (isShutdown.get()) {
            return;
        }

        taskScheduler.submit(() -> {
            try {
                executeTask(task);
            } catch (Exception e) {
                task.setStatus(TaskStatus.FAILED);
                if (taskStateCallback != null) {
                    taskStateCallback.onTaskFailed(task.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * 执行下载任务
     *
     * @param task 下载任务
     */
    private void executeTask(DownloadTask task) {
        try {
            // 1. 获取文件大小
            long totalSize = getFileSize(task.getUrl());
            if (totalSize <= 0) {
                throw new IOException("无法获取文件大小");
            }
            task.setTotalSize(totalSize);

            // 2. 创建目标文件
            File targetFile = new File(task.getSavePath(), task.getFileName());
            if (!targetFile.exists()) {
                targetFile.getParentFile().mkdirs();
                // 预分配文件空间
                try (RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {
                    raf.setLength(totalSize);
                }
            }

            // 3. 分块计算
            int threadCount = task.getThreadCount();
            List<TaskChunk> chunks = splitChunks(task.getId(), totalSize, threadCount);
            taskChunks.put(task.getId(), chunks);

            // 4. 创建分块工作线程池
            ThreadPoolExecutor chunkPool = createChunkWorkerPool(threadCount);
            chunkWorkerPools.put(task.getId(), chunkPool);

            // 5. 创建限速器（如果需要）
            if (task.getSpeedLimit() > 0) {
                RateLimiter limiter = createRateLimiter(task.getSpeedLimit());
                rateLimiters.put(task.getId(), limiter);
            }

            // 6. 更新任务状态
            task.setStatus(TaskStatus.DOWNLOADING);

            // 7. 启动进度更新
            startProgressTracking(task);

            // 8. 创建并提交ChunkWorker
            List<ChunkWorker> workers = new ArrayList<>();
            AtomicLong lastDownloaded = new AtomicLong(0);

            for (TaskChunk chunk : chunks) {
                ChunkWorker worker = new ChunkWorker(
                        chunk,
                        task.getUrl(),
                        targetFile.getAbsolutePath(),
                        config,
                        rateLimiters.get(task.getId()),
                        new ChunkWorker.ChunkCallback() {
                            @Override
                            public void onProgress(int chunkIndex, long downloaded) {
                                task.addDownloaded(downloaded);
                            }

                            @Override
                            public void onComplete(int chunkIndex) {
                                checkTaskCompletion(task);
                            }

                            @Override
                            public void onError(int chunkIndex, String error) {
                                // 单个分块失败，检查整体任务状态
                                checkTaskCompletion(task);
                            }
                        }
                );
                workers.add(worker);
                chunkPool.submit(worker);
            }
            chunkWorkers.put(task.getId(), workers);

        } catch (IOException e) {
            task.setStatus(TaskStatus.FAILED);
            if (taskStateCallback != null) {
                taskStateCallback.onTaskFailed(task.getId(), e.getMessage());
            }
        }
    }

    /**
     * 将文件按线程数等分计算分块区间
     *
     * @param taskId     任务ID
     * @param totalSize  文件总大小
     * @param threadCount 线程数
     * @return 分块列表
     */
    private List<TaskChunk> splitChunks(String taskId, long totalSize, int threadCount) {
        List<TaskChunk> chunks = new ArrayList<>();
        long chunkSize = totalSize / threadCount;

        for (int i = 0; i < threadCount; i++) {
            long start = i * chunkSize;
            long end = (i == threadCount - 1) ? totalSize - 1 : start + chunkSize - 1;
            chunks.add(new TaskChunk(taskId, i, start, end));
        }

        return chunks;
    }

    /**
     * 创建分块工作线程池
     *
     * @param coreSize 核心线程数
     * @return 线程池
     */
    private ThreadPoolExecutor createChunkWorkerPool(int coreSize) {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(coreSize);
    }

    /**
     * 创建限速器
     *
     * @param bytesPerSecond 每秒字节数
     * @return 限速器
     */
    private RateLimiter createRateLimiter(long bytesPerSecond) {
        // 返回一个简单的令牌桶限速器实现
        return new RateLimiter() {
            private final Object lock = new Object();
            private long rate = bytesPerSecond;
            private long availableTokens = bytesPerSecond;
            private long lastRefillTime = System.currentTimeMillis();

            @Override
            public void acquire(int bytes) throws InterruptedException {
                synchronized (lock) {
                    while (true) {
                        refillTokens();
                        if (availableTokens >= bytes) {
                            availableTokens -= bytes;
                            return;
                        }
                        // 计算需要等待的时间
                        long waitTime = (long) (bytes * 1000.0 / rate);
                        lock.wait(waitTime);
                    }
                }
            }

            private void refillTokens() {
                long now = System.currentTimeMillis();
                long elapsed = now - lastRefillTime;
                if (elapsed > 0) {
                    long tokensToAdd = (elapsed * rate) / 1000;
                    availableTokens = Math.min(availableTokens + tokensToAdd, rate);
                    lastRefillTime = now;
                }
            }

            @Override
            public void setRate(long bytesPerSecond) {
                synchronized (lock) {
                    this.rate = bytesPerSecond;
                    this.availableTokens = bytesPerSecond;
                    lock.notifyAll();
                }
            }

            @Override
            public long getRate() {
                return rate;
            }
        };
    }

    /**
     * 获取远程文件大小
     *
     * @param url 文件URL
     * @return 文件大小（字节）
     * @throws IOException 当IO错误发生时抛出
     */
    private long getFileSize(String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL downloadUrl = new URL(url);
            connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("服务器返回错误响应码: " + responseCode);
            }

            String contentLength = connection.getHeaderField("Content-Length");
            if (contentLength == null || contentLength.isEmpty()) {
                throw new IOException("无法获取文件大小");
            }

            return Long.parseLong(contentLength);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 启动进度跟踪
     *
     * @param task 下载任务
     */
    private void startProgressTracking(DownloadTask task) {
        final AtomicLong lastDownloaded = new AtomicLong(0);
        final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());

        progressScheduler.scheduleAtFixedRate(() -> {
            if (task.getStatus() == TaskStatus.DOWNLOADING) {
                long currentDownloaded = task.getDownloaded();
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastTime.get();

                if (elapsed > 0) {
                    long speed = (currentDownloaded - lastDownloaded.get()) * 1000 / elapsed;

                    ProgressEvent event = new ProgressEvent(
                            task.getId(),
                            currentDownloaded,
                            task.getTotalSize(),
                            speed
                    );

                    if (progressCallback != null) {
                        progressCallback.onProgress(event);
                    }

                    lastDownloaded.set(currentDownloaded);
                    lastTime.set(currentTime);
                }
            }
        }, 0, DownloadConfig.PROGRESS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查任务是否完成
     *
     * @param task 下载任务
     */
    private void checkTaskCompletion(DownloadTask task) {
        List<TaskChunk> chunks = taskChunks.get(task.getId());
        if (chunks == null) {
            return;
        }

        boolean allCompleted = true;
        boolean anyFailed = false;

        for (TaskChunk chunk : chunks) {
            if (chunk.getStatus() == ChunkStatus.FAILED) {
                anyFailed = true;
            } else if (chunk.getStatus() != ChunkStatus.COMPLETED) {
                allCompleted = false;
            }
        }

        if (allCompleted) {
            task.setStatus(TaskStatus.COMPLETED);
            cleanupTask(task.getId());
            if (taskStateCallback != null) {
                taskStateCallback.onTaskCompleted(task.getId());
            }
        } else if (anyFailed) {
            // 检查是否所有分块都已完成或失败
            boolean allDone = true;
            for (TaskChunk chunk : chunks) {
                if (chunk.getStatus() != ChunkStatus.COMPLETED
                        && chunk.getStatus() != ChunkStatus.FAILED) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                task.setStatus(TaskStatus.FAILED);
                cleanupTask(task.getId());
                if (taskStateCallback != null) {
                    taskStateCallback.onTaskFailed(task.getId(), "部分分块下载失败");
                }
            }
        }
    }

    /**
     * 热调整线程数
     * 使用 ThreadPoolExecutor.setCorePoolSize() 动态调整
     *
     * @param taskId 任务ID
     * @param n      新的线程数
     */
    public void setThreadCount(String taskId, int n) {
        ThreadPoolExecutor pool = chunkWorkerPools.get(taskId);
        if (pool != null) {
            int newThreadCount = Math.max(DownloadConfig.MIN_THREAD_COUNT,
                    Math.min(DownloadConfig.MAX_THREAD_COUNT, n));
            pool.setCorePoolSize(newThreadCount);
            pool.setMaximumPoolSize(newThreadCount);
        }
    }

    /**
     * 暂停任务
     *
     * @param taskId 任务ID
     */
    public void pause(String taskId) {
        List<ChunkWorker> workers = chunkWorkers.get(taskId);
        if (workers != null) {
            for (ChunkWorker worker : workers) {
                worker.pause();
            }
        }
    }

    /**
     * 恢复任务
     *
     * @param taskId 任务ID
     */
    public void resume(String taskId) {
        List<ChunkWorker> workers = chunkWorkers.get(taskId);
        if (workers != null) {
            for (ChunkWorker worker : workers) {
                worker.resume();
            }
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     */
    public void cancel(String taskId) {
        List<ChunkWorker> workers = chunkWorkers.get(taskId);
        if (workers != null) {
            for (ChunkWorker worker : workers) {
                worker.cancel();
            }
        }
        cleanupTask(taskId);
    }

    /**
     * 清理任务资源
     *
     * @param taskId 任务ID
     */
    private void cleanupTask(String taskId) {
        ThreadPoolExecutor pool = chunkWorkerPools.remove(taskId);
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        taskChunks.remove(taskId);
        chunkWorkers.remove(taskId);
        rateLimiters.remove(taskId);
    }

    /**
     * 关闭下载引擎
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            // 关闭任务调度器
            taskScheduler.shutdown();
            try {
                if (!taskScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    taskScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // 关闭所有分块工作线程池
            for (ThreadPoolExecutor pool : chunkWorkerPools.values()) {
                pool.shutdownNow();
            }
            chunkWorkerPools.clear();

            // 关闭进度调度器
            progressScheduler.shutdown();
            try {
                if (!progressScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    progressScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                progressScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // 清理资源
            taskChunks.clear();
            chunkWorkers.clear();
            rateLimiters.clear();
        }
    }

    /**
     * 设置进度回调
     *
     * @param callback 进度回调
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * 设置任务状态回调
     *
     * @param callback 任务状态回调
     */
    public void setTaskStateCallback(TaskStateCallback callback) {
        this.taskStateCallback = callback;
    }

    /**
     * 获取配置
     *
     * @return 下载配置
     */
    public DownloadConfig getConfig() {
        return config;
    }

    /**
     * 检查引擎是否已关闭
     *
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return isShutdown.get();
    }
}