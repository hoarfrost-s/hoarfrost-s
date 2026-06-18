package com.hyperfetch.protocol;

import com.hyperfetch.model.ResourceMeta;
import com.hyperfetch.rate.RateLimiter;

import java.io.IOException;

/**
 * 协议处理器接口
 * 定义不同协议（HTTP、FTP等）的统一处理规范
 */
public interface ProtocolHandler {

    /**
     * 判断是否支持该URL
     *
     * @param url 待检测的URL地址
     * @return 如果支持该协议返回true，否则返回false
     */
    boolean supports(String url);

    /**
     * 探测资源元数据
     * 发送请求获取资源的基本信息（大小、是否支持断点续传等）
     *
     * @param url 资源URL地址
     * @return 资源元数据对象
     * @throws IOException 当网络请求失败时抛出
     */
    ResourceMeta probe(String url) throws IOException;

    /**
     * 下载指定范围的数据
     * 支持断点续传和限速控制
     *
     * @param url    资源URL地址
     * @param start  起始字节位置（包含）
     * @param end    结束字节位置（包含）
     * @param limiter 限速器，用于控制下载速率，可为null表示不限速
     * @param cb     进度回调接口
     * @throws IOException 当下载失败时抛出
     */
    void download(String url, long start, long end, RateLimiter limiter, ProgressCallback cb) throws IOException;
}