package com.hyperfetch.protocol

import okhttp3.OkHttpClient

/**
 * 协议工厂
 * 根据 URL 自动选择合适的协议处理器
 */
object ProtocolFactory {

    private var client: OkHttpClient? = null

    private var httpHandler: HttpHandler? = null
    private var hlsHandler: HlsHandler? = null
    private var ftpHandler: FtpHandler? = null

    /**
     * 初始化工厂
     */
    fun initialize(client: OkHttpClient) {
        this.client = client
        httpHandler = HttpHandler(client)
        hlsHandler = HlsHandler(client)
        ftpHandler = FtpHandler(client)
    }

    /**
     * 获取合适的协议处理器
     */
    fun getHandler(url: String): ProtocolHandler {
        return when {
            hlsHandler?.supports(url) == true -> hlsHandler!!
            ftpHandler?.supports(url) == true -> ftpHandler!!
            httpHandler?.supports(url) == true -> httpHandler!!
            else -> httpHandler!! // 默认使用 HTTP 处理器
        }
    }

    /**
     * 检查 URL 是否支持
     */
    fun isSupported(url: String): Boolean {
        return httpHandler?.supports(url) == true ||
                hlsHandler?.supports(url) == true ||
                ftpHandler?.supports(url) == true
    }

    /**
     * 重置工厂
     */
    fun reset() {
        client = null
        httpHandler = null
        hlsHandler = null
        ftpHandler = null
    }
}
