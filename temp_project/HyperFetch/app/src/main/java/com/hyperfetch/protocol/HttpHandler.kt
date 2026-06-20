package com.hyperfetch.protocol

import com.hyperfetch.model.ResourceMeta
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URL

/**
 * HTTP/HTTPS 协议处理器
 */
class HttpHandler(
    private val client: OkHttpClient
) : ProtocolHandler {

    override fun supports(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    override fun probe(url: String): ResourceMeta {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        val response = client.newCall(request).execute()
        return parseProbeResponse(response, url)
    }

    override fun download(url: String, outputPath: String, callback: ProgressCallback) {
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP error: ${response.code}")
        }

        response.body?.let { body ->
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            java.io.FileOutputStream(outputPath).use { fos ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        callback.onProgress(downloadedBytes, totalBytes)
                    }
                }
            }
        }

        callback.onComplete()
    }

    override fun downloadRange(
        url: String,
        start: Long,
        end: Long,
        outputPath: String,
        callback: ProgressCallback
    ) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful && response.code != 206) {
            throw IOException("HTTP error: ${response.code}")
        }

        response.body?.let { body ->
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            java.io.RandomAccessFile(outputPath, "rw").use { raf ->
                raf.seek(start)
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        callback.onProgress(downloadedBytes, totalBytes)
                    }
                }
            }
        }

        callback.onComplete()
    }

    private fun parseProbeResponse(response: Response, url: String): ResourceMeta {
        val headers = response.headers
        val contentLength = headers["Content-Length"]?.toLongOrNull() ?: 0L
        val contentType = headers["Content-Type"]
        val acceptRanges = headers["Accept-Ranges"]?.equals("bytes", ignoreCase = true) ?: false

        // 尝试从 Content-Disposition 获取文件名
        var fileName: String? = null
        val contentDisposition = headers["Content-Disposition"]
        if (contentDisposition != null) {
            val filenameRegex = Regex("""filename[^;]*=['"]?([^'"]+)['"]?""", RegexOption.IGNORE_CASE)
            filenameRegex.find(contentDisposition)?.let { match ->
                fileName = match.groupValues[1]
            }
        }

        // 如果没有文件名，从 URL 提取
        if (fileName == null) {
            try {
                fileName = URL(url).path.substringAfterLast("/")
            } catch (e: Exception) {
                // ignore
            }
        }

        // 检测是否支持 Range
        val supportRange = acceptRanges || contentLength > 0

        return ResourceMeta(
            totalSize = contentLength,
            supportRange = supportRange,
            contentType = contentType,
            fileName = fileName
        )
    }
}
