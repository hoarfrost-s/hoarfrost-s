package com.hyperfetch.protocol;

import com.hyperfetch.model.ResourceMeta;
import com.hyperfetch.rate.RateLimiter;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * HTTP协议处理器实现
 * 支持HTTP/HTTPS协议的资源探测和下载
 */
public class HttpHandler implements ProtocolHandler {

    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";

    /**
     * HTTP客户端实例
     */
    private final OkHttpClient client;

    /**
     * 默认缓冲区大小（8KB）
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 构造函数
     *
     * @param client OkHttp客户端实例
     */
    public HttpHandler(OkHttpClient client) {
        this.client = client;
    }

    /**
     * 默认构造函数
     * 使用默认配置的OkHttp客户端
     */
    public HttpHandler() {
        this(new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build());
    }

    @Override
    public boolean supports(String url) {
        if (url == null) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith(HTTP_PREFIX) || lowerUrl.startsWith(HTTPS_PREFIX);
    }

    @Override
    public ResourceMeta probe(String url) throws IOException {
        // 构建HEAD请求探测资源信息
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("探测失败，HTTP状态码: " + response.code());
            }

            ResourceMeta meta = new ResourceMeta();

            // 获取Content-Length
            long contentLength = 0;
            String contentLengthHeader = response.header("Content-Length");
            if (contentLengthHeader != null) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader);
                } catch (NumberFormatException e) {
                    contentLength = 0;
                }
            }
            meta.setTotalSize(contentLength);

            // 检测是否支持断点续传（Accept-Ranges: bytes）
            String acceptRanges = response.header("Accept-Ranges");
            meta.setSupportRange("bytes".equalsIgnoreCase(acceptRanges));

            // 获取Content-Type
            String contentType = response.header("Content-Type");
            meta.setContentType(contentType);

            return meta;
        }
    }

    @Override
    public void download(String url, long start, long end, RateLimiter limiter, ProgressCallback cb) throws IOException {
        // 构建Range请求
        String rangeHeader = "bytes=" + start + "-" + end;
        Request request = new Request.Builder()
                .url(url)
                .header("Range", rangeHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 检查响应状态码（206 Partial Content 或 200 OK）
            if (!response.isSuccessful()) {
                throw new IOException("下载失败，HTTP状态码: " + response.code());
            }

            // 对于Range请求，期望206状态码，但有些服务器可能返回200
            if (response.code() != 206 && response.code() != 200) {
                throw new IOException("不支持断点续传，HTTP状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            long total = end - start + 1;
            long downloaded = 0;

            try (InputStream inputStream = body.byteStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // 应用限速控制
                    if (limiter != null) {
                        try {
                            limiter.acquire(bytesRead);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("下载被中断", e);
                        }
                    }

                    // TODO: 将数据写入文件，这里需要外部提供文件路径或输出流
                    // 目前仅计算进度，实际文件写入应由调用方处理
                    downloaded += bytesRead;

                    // 回调进度
                    if (cb != null) {
                        cb.onProgress(downloaded, total);
                    }
                }

                // 下载完成回调
                if (cb != null) {
                    cb.onComplete();
                }
            }
        } catch (Exception e) {
            // 错误回调
            if (cb != null) {
                cb.onError(e instanceof IOException ? (IOException) e : new IOException(e));
            }
            throw e;
        }
    }

    /**
     * 下载指定范围的数据到文件
     * 支持断点续传的文件写入方式
     *
     * @param url      资源URL地址
     * @param start    起始字节位置（包含）
     * @param end      结束字节位置（包含）
     * @param filePath 目标文件路径
     * @param limiter  限速器
     * @param cb       进度回调接口
     * @throws IOException 当下载失败时抛出
     */
    public void downloadToFile(String url, long start, long end, String filePath,
                               RateLimiter limiter, ProgressCallback cb) throws IOException {
        // 构建Range请求
        String rangeHeader = "bytes=" + start + "-" + end;
        Request request = new Request.Builder()
                .url(url)
                .header("Range", rangeHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 检查响应状态码
            if (!response.isSuccessful()) {
                throw new IOException("下载失败，HTTP状态码: " + response.code());
            }

            if (response.code() != 206 && response.code() != 200) {
                throw new IOException("不支持断点续传，HTTP状态码: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            long total = end - start + 1;
            long downloaded = 0;

            // 使用RandomAccessFile支持断点续传写入
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
                 InputStream inputStream = body.byteStream()) {

                // 定位到起始位置
                raf.seek(start);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // 应用限速控制
                    if (limiter != null) {
                        try {
                            limiter.acquire(bytesRead);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("下载被中断", e);
                        }
                    }

                    // 写入文件
                    raf.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;

                    // 回调进度
                    if (cb != null) {
                        cb.onProgress(downloaded, total);
                    }
                }

                // 下载完成回调
                if (cb != null) {
                    cb.onComplete();
                }
            }
        } catch (Exception e) {
            // 错误回调
            if (cb != null) {
                cb.onError(e instanceof IOException ? (IOException) e : new IOException(e));
            }
            throw e;
        }
    }
}