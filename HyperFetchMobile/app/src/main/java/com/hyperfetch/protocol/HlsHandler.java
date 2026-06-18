package com.hyperfetch.protocol;

import android.util.Log;

import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.ResourceMeta;
import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.rate.RateLimiter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HLS 流媒体处理器
 * 用于处理 .m3u8 格式的 HLS 流媒体下载
 * 支持主播放列表和媒体播放列表，多线程下载 TS 分片并合并
 * 实现 ProtocolHandler 接口以与现有架构兼容
 */
public class HlsHandler implements ProtocolHandler {

    private static final String TAG = "HlsHandler";

    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        /**
         * 进度更新
         *
         * @param downloaded 已下载字节数
         * @param total      总字节数
         */
        void onProgress(long downloaded, long total);

        /**
         * 下载完成
         *
         * @param success 是否成功
         * @param message 消息
         */
        void onComplete(boolean success, String message);

        /**
         * 状态变化
         *
         * @param status 新状态
         */
        void onStatusChanged(TaskStatus status);
    }

    /**
     * 分片下载结果
     */
    private static class SegmentDownloadResult {
        File file;
        boolean success;
        String error;
        long size;

        SegmentDownloadResult(File file, boolean success, String error, long size) {
            this.file = file;
            this.success = success;
            this.error = error;
            this.size = size;
        }
    }

    // M3U8 解析器
    private final M3u8Parser m3u8Parser;

    // TS 合并器
    private final TsMerger tsMerger;

    // 是否取消下载
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // 是否暂停下载
    private final AtomicBoolean paused = new AtomicBoolean(false);

    // 已下载字节数
    private final AtomicLong downloadedBytes = new AtomicLong(0);

    // 总字节数
    private final AtomicLong totalBytes = new AtomicLong(0);

    // 已完成分片数
    private final AtomicInteger completedSegments = new AtomicInteger(0);

    // 总分片数
    private final AtomicInteger totalSegments = new AtomicInteger(0);

    // 连接超时（毫秒）
    private int connectTimeout = 15000;

    // 读取超时（毫秒）
    private int readTimeout = 30000;

    // 缓冲区大小
    private int bufferSize = 8192;

    // 重试次数
    private int retryCount = 3;

    // 重试间隔（毫秒）
    private int retryInterval = 1000;

    /**
     * 默认构造函数
     */
    public HlsHandler() {
        this.m3u8Parser = new M3u8Parser();
        this.tsMerger = new TsMerger();
    }

    /**
     * 判断 URL 是否支持 HLS 协议
     *
     * @param url 下载地址
     * @return 是否支持（URL 以 .m3u8 结尾）
     */
    @Override
    public boolean supports(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".m3u8") || lowerUrl.contains(".m3u8?");
    }

    /**
     * 探测 HLS 资源信息
     * 下载并解析 m3u8 索引文件，提取分片信息
     *
     * @param url 下载地址
     * @return 资源元数据
     * @throws IOException 当探测失败时抛出
     */
    @Override
    public ResourceMeta probe(String url) throws IOException {
        ResourceMeta meta = new ResourceMeta();

        // 下载 m3u8 文件内容
        String m3u8Content = downloadM3u8Content(url);
        if (m3u8Content == null || m3u8Content.isEmpty()) {
            throw new IOException("无法下载 m3u8 文件内容");
        }

        // 解析 m3u8 文件
        M3u8Parser.ParseResult parseResult = m3u8Parser.parse(m3u8Content, url);

        if (parseResult.isMasterPlaylist()) {
            // 主播放列表，选择最佳子流
            M3u8Parser.VariantStream bestStream = m3u8Parser.selectBestVariantStream(
                    parseResult.getVariantStreams());
            if (bestStream != null) {
                // 递归探测子流
                return probe(bestStream.getUrl());
            } else {
                throw new IOException("无法找到有效的子流");
            }
        } else {
            // 媒体播放列表
            List<M3u8Parser.Segment> segments = parseResult.getSegments();
            if (!segments.isEmpty()) {
                // 估算总大小（假设每个分片平均 2MB）
                long estimatedSegmentSize = 2 * 1024 * 1024;
                meta.setTotalSize(segments.size() * estimatedSegmentSize);
                meta.setSupportRange(false); // HLS 不支持 Range 请求
                meta.setContentType("application/x-mpegURL");
            }
        }

        return meta;
    }

    /**
     * 下载 HLS 流媒体
     *
     * @param task      下载任务
     * @param callback  下载回调
     * @return 输出文件路径
     */
    public String download(DownloadTask task, DownloadCallback callback) {
        cancelled.set(false);
        paused.set(false);
        downloadedBytes.set(0);
        completedSegments.set(0);

        String url = task.getUrl();
        String savePath = task.getSavePath();
        String fileName = task.getFileName();
        int threadCount = task.getThreadCount();

        // 确保保存目录存在
        File saveDir = new File(savePath);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        // 创建临时目录存放 TS 分片
        File tempDir = new File(savePath, ".hls_temp_" + task.getId());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        try {
            // 1. 下载并解析 m3u8 文件
            if (callback != null) {
                callback.onStatusChanged(TaskStatus.DOWNLOADING);
            }

            String m3u8Content = downloadM3u8Content(url);
            if (m3u8Content == null || m3u8Content.isEmpty()) {
                throw new IOException("无法下载 m3u8 文件");
            }

            M3u8Parser.ParseResult parseResult = m3u8Parser.parse(m3u8Content, url);

            // 处理主播放列表
            List<M3u8Parser.Segment> segments;
            if (parseResult.isMasterPlaylist()) {
                // 选择最佳子流
                M3u8Parser.VariantStream bestStream = m3u8Parser.selectBestVariantStream(
                        parseResult.getVariantStreams());
                if (bestStream == null) {
                    throw new IOException("无法找到有效的子流");
                }

                // 下载子流 m3u8
                String subM3u8Content = downloadM3u8Content(bestStream.getUrl());
                M3u8Parser.ParseResult subResult = m3u8Parser.parse(subM3u8Content, bestStream.getUrl());
                segments = subResult.getSegments();
            } else {
                segments = parseResult.getSegments();
            }

            if (segments == null || segments.isEmpty()) {
                throw new IOException("未找到有效的分片");
            }

            // 检查加密
            if (parseResult.getEncryptionInfo() != null && parseResult.getEncryptionInfo().isEncrypted()) {
                Log.w(TAG, "检测到加密流，可能无法正常下载");
                // TODO: 实现解密逻辑
            }

            totalSegments.set(segments.size());

            // 2. 多线程下载分片
            List<File> tsFiles = downloadSegmentsParallel(segments, tempDir, threadCount, callback);

            if (cancelled.get()) {
                throw new InterruptedException("下载已取消");
            }

            if (tsFiles.isEmpty()) {
                throw new IOException("没有成功下载任何分片");
            }

            // 3. 合并分片为 MP4
            String outputFileName = fileName;
            if (!outputFileName.toLowerCase().endsWith(".mp4")) {
                outputFileName = outputFileName.replaceAll("\\.[^.]+$", "") + ".mp4";
            }
            String outputPath = new File(savePath, outputFileName).getAbsolutePath();

            TsMerger.MergeResult mergeResult = tsMerger.mergeToMp4(tsFiles, outputPath);

            if (mergeResult.isSuccess()) {
                // 清理临时目录
                deleteDirectory(tempDir);

                // 根据合并结果设置状态
                if (mergeResult.isTsFilesRetained()) {
                    if (callback != null) {
                        callback.onStatusChanged(TaskStatus.COMPLETED_WITH_WARNING);
                    }
                    if (callback != null) {
                        callback.onComplete(true, "下载完成（已保留 TS 文件）");
                    }
                } else {
                    if (callback != null) {
                        callback.onStatusChanged(TaskStatus.COMPLETED);
                    }
                    if (callback != null) {
                        callback.onComplete(true, "下载完成");
                    }
                }

                return mergeResult.getOutputPath();
            } else {
                // 合并失败，保留 TS 文件
                Log.w(TAG, "合并失败: " + mergeResult.getErrorMessage() + "，保留 TS 文件");

                if (callback != null) {
                    callback.onStatusChanged(TaskStatus.COMPLETED_WITH_WARNING);
                    callback.onComplete(true, "下载完成（合并失败，已保留 TS 文件）");
                }

                // 返回 TS 文件目录
                return tempDir.getAbsolutePath();
            }

        } catch (InterruptedException e) {
            if (callback != null) {
                callback.onStatusChanged(TaskStatus.PAUSED);
                callback.onComplete(false, "下载已取消");
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "下载失败: " + e.getMessage());
            if (callback != null) {
                callback.onStatusChanged(TaskStatus.FAILED);
                callback.onComplete(false, "下载失败: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 并行下载分片
     *
     * @param segments    分片列表
     * @param tempDir     临时目录
     * @param threadCount 线程数
     * @param callback    回调
     * @return 下载的文件列表
     */
    private List<File> downloadSegmentsParallel(List<M3u8Parser.Segment> segments,
                                                 File tempDir,
                                                 int threadCount,
                                                 DownloadCallback callback) throws InterruptedException {
        // 确保线程数不超过分片数
        int actualThreadCount = Math.min(threadCount, segments.size());
        actualThreadCount = Math.max(1, actualThreadCount);

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(actualThreadCount);

        // 分配分片给各线程
        int segmentCount = segments.size();
        int segmentsPerThread = (segmentCount + actualThreadCount - 1) / actualThreadCount;

        // 结果列表（线程安全）
        List<File> tsFiles = new ArrayList<>();
        List<SegmentDownloadResult> results = new ArrayList<>();

        // 使用 CountDownLatch 等待所有线程完成
        CountDownLatch latch = new CountDownLatch(actualThreadCount);

        // 创建下载任务
        for (int i = 0; i < actualThreadCount; i++) {
            final int threadIndex = i;
            final int startIndex = i * segmentsPerThread;
            final int endIndex = Math.min(startIndex + segmentsPerThread, segmentCount);

            if (startIndex >= segmentCount) {
                latch.countDown();
                continue;
            }

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 每个线程串行下载分配的分片
                        for (int j = startIndex; j < endIndex && !cancelled.get() && !paused.get(); j++) {
                            M3u8Parser.Segment segment = segments.get(j);

                            // 生成文件名
                            String tsFileName = String.format("segment_%05d.ts", segment.getSequence());
                            File tsFile = new File(tempDir, tsFileName);

                            // 下载分片
                            SegmentDownloadResult result = downloadSegment(segment, tsFile);

                            synchronized (results) {
                                results.add(result);
                                if (result.success) {
                                    tsFiles.add(result.file);
                                    downloadedBytes.addAndGet(result.size);
                                }
                            }

                            completedSegments.incrementAndGet();

                            // 更新进度
                            if (callback != null) {
                                callback.onProgress(downloadedBytes.get(), totalBytes.get());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "线程 " + threadIndex + " 下载失败: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executor.shutdown();

        // 按序号排序文件
        tsFiles.sort((f1, f2) -> {
            int n1 = extractSequenceFromFileName(f1.getName());
            int n2 = extractSequenceFromFileName(f2.getName());
            return Integer.compare(n1, n2);
        });

        return tsFiles;
    }

    /**
     * 下载单个分片
     *
     * @param segment 分片信息
     * @param tsFile  目标文件
     * @return 下载结果
     */
    private SegmentDownloadResult downloadSegment(M3u8Parser.Segment segment, File tsFile) {
        for (int retry = 0; retry < retryCount; retry++) {
            if (cancelled.get()) {
                return new SegmentDownloadResult(tsFile, false, "已取消", 0);
            }

            HttpURLConnection connection = null;
            InputStream is = null;
            FileOutputStream fos = null;

            try {
                URL url = new URL(segment.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "HyperFetch/1.0");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP 错误: " + responseCode);
                }

                is = connection.getInputStream();
                fos = new FileOutputStream(tsFile);

                byte[] buffer = new byte[bufferSize];
                int len;
                long totalSize = 0;

                while ((len = is.read(buffer)) != -1) {
                    if (cancelled.get()) {
                        tsFile.delete();
                        return new SegmentDownloadResult(tsFile, false, "已取消", 0);
                    }

                    fos.write(buffer, 0, len);
                    totalSize += len;
                }

                fos.flush();
                return new SegmentDownloadResult(tsFile, true, null, totalSize);

            } catch (Exception e) {
                Log.w(TAG, "分片下载失败 (重试 " + (retry + 1) + "/" + retryCount + "): " + segment.getUrl());

                if (retry < retryCount - 1) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                try {
                    if (fos != null) fos.close();
                    if (is != null) is.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }

        return new SegmentDownloadResult(tsFile, false, "下载失败", 0);
    }

    /**
     * 下载 m3u8 文件内容
     *
     * @param url m3u8 文件 URL
     * @return 文件内容
     */
    private String downloadM3u8Content(String url) {
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            URL m3u8Url = new URL(url);
            connection = (HttpURLConnection) m3u8Url.openConnection();
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "HyperFetch/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP 错误: " + responseCode);
            }

            is = connection.getInputStream();
            byte[] buffer = new byte[8192];
            StringBuilder content = new StringBuilder();

            int len;
            while ((len = is.read(buffer)) != -1) {
                content.append(new String(buffer, 0, len, "UTF-8"));
            }

            return content.toString();

        } catch (Exception e) {
            Log.e(TAG, "下载 m3u8 文件失败: " + e.getMessage());
            return null;
        } finally {
            try {
                if (is != null) is.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 从文件名中提取序号
     *
     * @param fileName 文件名
     * @return 序号
     */
    private int extractSequenceFromFileName(String fileName) {
        try {
            // 格式: segment_00001.ts
            String numStr = fileName.replaceAll("[^0-9]", "");
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 删除目录及其内容
     *
     * @param dir 目录
     */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
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
     * 获取已下载字节数
     *
     * @return 已下载字节数
     */
    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    /**
     * 获取总字节数
     *
     * @return 总字节数
     */
    public long getTotalBytes() {
        return totalBytes.get();
    }

    /**
     * 获取已完成分片数
     *
     * @return 已完成分片数
     */
    public int getCompletedSegments() {
        return completedSegments.get();
    }

    /**
     * 获取总分片数
     *
     * @return 总分片数
     */
    public int getTotalSegments() {
        return totalSegments.get();
    }

    // Getter 和 Setter 方法

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    /**
     * 实现 ProtocolHandler 接口的下载方法
     * 注意：HLS 协议不支持 Range 请求，此方法将下载整个流
     * start 和 end 参数对于 HLS 协议无效
     *
     * @param url     资源 URL 地址
     * @param start   起始字节位置（对于 HLS 无效）
     * @param end     结束字节位置（对于 HLS 无效）
     * @param limiter 限速器
     * @param cb      进度回调接口
     * @throws IOException 当下载失败时抛出
     */
    @Override
    public void download(String url, long start, long end, RateLimiter limiter, ProgressCallback cb) throws IOException {
        // HLS 不支持 Range 请求，忽略 start 和 end 参数
        // 使用默认配置下载
        cancelled.set(false);
        paused.set(false);
        downloadedBytes.set(0);
        completedSegments.set(0);

        try {
            // 下载并解析 m3u8 文件
            String m3u8Content = downloadM3u8Content(url);
            if (m3u8Content == null || m3u8Content.isEmpty()) {
                throw new IOException("无法下载 m3u8 文件");
            }

            M3u8Parser.ParseResult parseResult = m3u8Parser.parse(m3u8Content, url);

            // 处理主播放列表
            List<M3u8Parser.Segment> segments;
            if (parseResult.isMasterPlaylist()) {
                M3u8Parser.VariantStream bestStream = m3u8Parser.selectBestVariantStream(
                        parseResult.getVariantStreams());
                if (bestStream == null) {
                    throw new IOException("无法找到有效的子流");
                }
                String subM3u8Content = downloadM3u8Content(bestStream.getUrl());
                M3u8Parser.ParseResult subResult = m3u8Parser.parse(subM3u8Content, bestStream.getUrl());
                segments = subResult.getSegments();
            } else {
                segments = parseResult.getSegments();
            }

            if (segments == null || segments.isEmpty()) {
                throw new IOException("未找到有效的分片");
            }

            totalSegments.set(segments.size());

            // 创建临时目录
            File tempDir = File.createTempFile("hls_download_", null);
            tempDir.delete();
            tempDir.mkdirs();

            // 下载分片
            List<File> tsFiles = new ArrayList<>();
            for (M3u8Parser.Segment segment : segments) {
                if (cancelled.get()) {
                    throw new IOException("下载已取消");
                }

                String tsFileName = String.format("segment_%05d.ts", segment.getSequence());
                File tsFile = new File(tempDir, tsFileName);

                SegmentDownloadResult result = downloadSegment(segment, tsFile);
                if (result.success) {
                    tsFiles.add(result.file);
                    downloadedBytes.addAndGet(result.size);
                    completedSegments.incrementAndGet();

                    if (cb != null) {
                        cb.onProgress(downloadedBytes.get(), totalBytes.get());
                    }
                } else {
                    throw new IOException("分片下载失败: " + segment.getUrl());
                }
            }

            // 下载完成回调
            if (cb != null) {
                cb.onComplete();
            }

            // 注意：此方法不进行合并，调用方需要自行处理合并逻辑
            // 或者使用 download(DownloadTask, DownloadCallback) 方法进行完整下载

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载被中断", e);
        } catch (Exception e) {
            if (cb != null) {
                cb.onError(e instanceof IOException ? (IOException) e : new IOException(e));
            }
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
    }
}