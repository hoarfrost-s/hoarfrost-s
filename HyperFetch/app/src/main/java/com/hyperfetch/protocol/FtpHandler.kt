package com.hyperfetch.protocol

import com.hyperfetch.model.ResourceMeta
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

/**
 * FTP/SFTP 协议处理器
 * 注意：FTP 需要特殊处理，这里提供简化实现
 */
class FtpHandler(
    private val client: OkHttpClient
) : ProtocolHandler {

    override fun supports(url: String): Boolean {
        return url.startsWith("ftp://") || url.startsWith("sftp://")
    }

    override fun probe(url: String): ResourceMeta {
        // FTP 不支持 HEAD 请求，尝试通过列表命令获取信息
        // 这里返回估算值，实际大小需要在下载过程中获取
        return ResourceMeta(
            totalSize = 0L, // 未知
            supportRange = true,
            contentType = "application/octet-stream",
            fileName = extractFileName(url)
        )
    }

    override fun download(url: String, outputPath: String, callback: ProgressCallback) {
        // FTP 下载需要特殊处理
        // 使用 URLConnection 进行 FTP 下载
        try {
            val urlConnection = URL(url).openConnection() as java.net.URLConnection
            urlConnection.connect()

            val contentLength = urlConnection.contentLength.toLong()
            var downloadedBytes = 0L

            urlConnection.getInputStream().use { input ->
                java.io.FileOutputStream(outputPath).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        callback.onProgress(downloadedBytes, contentLength)
                    }
                }
            }

            callback.onComplete()
        } catch (e: Exception) {
            callback.onError(IOException("FTP download failed: ${e.message}", e))
        }
    }

    override fun downloadRange(
        url: String,
        start: Long,
        end: Long,
        outputPath: String,
        callback: ProgressCallback
    ) {
        try {
            val urlConnection = URL(url).openConnection() as java.net.URLConnection
            urlConnection.setRequestProperty("Range", "bytes=$start-$end")
            urlConnection.connect()

            val contentLength = urlConnection.contentLength.toLong()
            var downloadedBytes = 0L

            urlConnection.getInputStream().use { input ->
                java.io.RandomAccessFile(outputPath, "rw").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        callback.onProgress(downloadedBytes, contentLength)
                    }
                }
            }

            callback.onComplete()
        } catch (e: Exception) {
            callback.onError(IOException("FTP range download failed: ${e.message}", e))
        }
    }

    private fun extractFileName(url: String): String {
        return try {
            val path = url.substringAfter("://")
                .substringAfter("/")
                .substringBeforeLast("/")
            if (path.isNotEmpty()) {
                path
            } else {
                "file_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            "file_${System.currentTimeMillis()}"
        }
    }
}
