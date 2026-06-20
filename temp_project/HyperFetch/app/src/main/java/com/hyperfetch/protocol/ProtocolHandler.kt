package com.hyperfetch.protocol

import com.hyperfetch.model.ResourceMeta
import java.io.IOException

/**
 * 进度回调接口
 */
interface ProgressCallback {
    /**
     * 进度回调
     * @param downloaded 本次下载的字节数
     * @param total 本次下载的总字节数
     */
    fun onProgress(downloaded: Long, total: Long)

    /**
     * 下载完成
     */
    fun onComplete()

    /**
     * 下载失败
     * @param e 异常
     */
    fun onError(e: IOException)
}

/**
 * 协议处理器接口
 * 支持 HTTP/HTTPS, HLS, FTP 等协议
 */
interface ProtocolHandler {

    /**
     * 检查是否支持该 URL
     */
    fun supports(url: String): Boolean

    /**
     * 探测资源元数据
     * @return 资源元数据，包含文件大小、是否支持断点续传等
     */
    @Throws(IOException::class)
    fun probe(url: String): ResourceMeta

    /**
     * 下载文件
     * @param url 下载链接
     * @param outputPath 输出文件路径
     * @param callback 进度回调
     */
    @Throws(IOException::class)
    fun download(url: String, outputPath: String, callback: ProgressCallback)

    /**
     * 分块下载（用于多线程下载）
     * @param url 下载链接
     * @param start 起始字节
     * @param end 结束字节
     * @param outputPath 输出文件路径
     * @param callback 进度回调
     */
    @Throws(IOException::class)
    fun downloadRange(
        url: String,
        start: Long,
        end: Long,
        outputPath: String,
        callback: ProgressCallback
    )
}
