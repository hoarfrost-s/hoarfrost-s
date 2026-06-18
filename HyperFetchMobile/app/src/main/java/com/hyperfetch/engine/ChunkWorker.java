package com.hyperfetch.engine;

import com.hyperfetch.model.ChunkStatus;
import com.hyperfetch.model.TaskChunk;
import com.hyperfetch.rate.RateLimiter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分块下载工作类
 * 负责单个分块的下载逻辑，包括重试机制和限速控制
 */
public class ChunkWorker implements Runnable {

    private static final String TAG = "ChunkWorker";

    /**
     * 分块信息
     */
    private final TaskChunk chunk;

    /**
     * 下载URL
     */
    private final String url;

    /**
     * 目标文件路径
     */
    private final String filePath;

    /**
     * 限速器（可选）
     */
    private final RateLimiter rateLimiter;

    /**
     * 最大重试次数
     */
    private final int maxRetries;

    /**
     * 连接超时时间（毫秒）
     */
    private final int connectTimeout;

    /**
     * 读取超时时间（毫秒）
     */
    private final int readTimeout;

    /**
     * 缓冲区大小
     */
    private final int bufferSize;

    /**
     * 下载回调接口
     */
    private final ChunkCallback callback;

    /**
     * 取消标志
     */
    private final AtomicBoolean cancelled;

    /**
     * 暂停标志
     */
    private final AtomicBoolean paused;

    /**
     * 分块回调接口
     */
    public interface ChunkCallback {
        /**
         * 进度更新回调
         *
         * @param chunkIndex 分块索引
         * @param downloaded 本次下载的字节数
         */
        void onProgress(int chunkIndex, long downloaded);

        /**
         * 分块下载完成回调
         *
         * @param chunkIndex 分块索引
         */
        void onComplete(int chunkIndex);

        /**
         * 分块下载失败回调
         *
         * @param chunkIndex 分块索引
         * @param error      错误信息
         */
        void onError(int chunkIndex, String error);
    }

    /**
     * 构造函数
     *
     * @param chunk      分块信息
     * @param url        下载URL
     * @param filePath   目标文件路径
     * @param config     下载配置
     * @param rateLimiter 限速器（可为null）
     * @param callback   回调接口
     */
    public ChunkWorker(TaskChunk chunk, String url, String filePath,
                       DownloadConfig config, RateLimiter rateLimiter,
                       ChunkCallback callback) {
        this.chunk = chunk;
        this.url = url;
        this.filePath = filePath;
        this.rateLimiter = rateLimiter;
        this.maxRetries = config.getRetryCount();
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
        this.bufferSize = DownloadConfig.BUFFER_SIZE;
        this.callback = callback;
        this.cancelled = new AtomicBoolean(false);
        this.paused = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        chunk.setStatus(ChunkStatus.DOWNLOADING);
        int retryCount = 0;
        boolean success = false;

        while (retryCount <= maxRetries && !cancelled.get() && !paused.get()) {
            try {
                downloadChunk();
                success = true;
                break;
            } catch (IOException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    // 指数退避重试
                    long delay = DownloadConfig.getRetryDelay(retryCount - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (cancelled.get()) {
            chunk.setStatus(ChunkStatus.PENDING);
        } else if (paused.get()) {
            chunk.setStatus(ChunkStatus.PENDING);
        } else if (success) {
            chunk.setStatus(ChunkStatus.COMPLETED);
            if (callback != null) {
                callback.onComplete(chunk.getIndex());
            }
        } else {
            chunk.setStatus(ChunkStatus.FAILED);
            if (callback != null) {
                callback.onError(chunk.getIndex(), "下载失败，已达到最大重试次数");
            }
        }
    }

    /**
     * 执行分块下载
     *
     * @throws IOException          当IO错误发生时抛出
     * @throws InterruptedException 当线程被中断时抛出
     */
    private void downloadChunk() throws IOException, InterruptedException {
        HttpURLConnection connection = null;
        BufferedInputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;

        try {
            // 创建HTTP连接
            URL downloadUrl = new URL(url);
            connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            // 设置Range请求头，指定分块范围
            long startByte = chunk.getStart() + chunk.getDownloaded();
            long endByte = chunk.getEnd();
            connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL
                    && responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("服务器返回错误响应码: " + responseCode);
            }

            // 获取输入流
            inputStream = new BufferedInputStream(connection.getInputStream(), bufferSize);

            // 打开文件，使用RandomAccessFile进行随机写入
            File file = new File(filePath);
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileChannel = randomAccessFile.getChannel();

            // 定位到分块起始位置
            fileChannel.position(startByte);

            // 读取数据并写入文件
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1
                    && !cancelled.get() && !paused.get()) {

                // 限速控制
                if (rateLimiter != null) {
                    rateLimiter.acquire(bytesRead);
                }

                // 写入文件
                fileChannel.write(java.nio.ByteBuffer.wrap(buffer, 0, bytesRead));

                // 更新进度
                totalRead += bytesRead;
                chunk.setDownloaded(chunk.getDownloaded() + bytesRead);

                // 回调进度更新
                if (callback != null) {
                    callback.onProgress(chunk.getIndex(), bytesRead);
                }
            }

        } finally {
            // 关闭资源
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException ignored) {
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 取消下载
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 暂停下载
     */
    public void pause() {
        paused.set(true);
    }

    /**
     * 恢复下载
     */
    public void resume() {
        paused.set(false);
    }

    /**
     * 检查是否已取消
     *
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 检查是否已暂停
     *
     * @return 是否已暂停
     */
    public boolean isPaused() {
        return paused.get();
    }

    /**
     * 获取分块信息
     *
     * @return 分块信息
     */
    public TaskChunk getChunk() {
        return chunk;
    }
}