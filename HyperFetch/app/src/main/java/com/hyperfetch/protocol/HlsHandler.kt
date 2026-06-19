package com.hyperfetch.protocol

import com.hyperfetch.model.ResourceMeta
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * HLS (m3u8) 流媒体协议处理器
 */
class HlsHandler(
    private val client: OkHttpClient
) : ProtocolHandler {

    private val m3u8Pattern = Pattern.compile("#EXTINF:([\\d.]+),")

    override fun supports(url: String): Boolean {
        return url.endsWith(".m3u8", ignoreCase = true) ||
                url.contains(".m3u8?", ignoreCase = true)
    }

    override fun probe(url: String): ResourceMeta {
        // HLS 无法直接获取总大小，只能估算
        val segments = parseM3u8(url)
        val segmentCount = segments.size
        // 假设平均每个分片 2MB
        val estimatedSize = segmentCount * 2L * 1024 * 1024

        return ResourceMeta(
            totalSize = estimatedSize,
            supportRange = false, // HLS 不支持 Range
            contentType = "application/vnd.apple.mpegurl",
            fileName = extractFileName(url)
        )
    }

    override fun download(url: String, outputPath: String, callback: ProgressCallback) {
        val segments = parseM3u8(url)
        if (segments.isEmpty()) {
            callback.onError(IOException("No segments found in m3u8"))
            return
        }

        val baseUrl = url.substringBeforeLast("/") + "/"
        var totalDownloaded = 0L
        val tempDir = outputPath + ".segments"

        java.io.File(tempDir).mkdirs()

        try {
            segments.forEachIndexed { index, segment ->
                val segmentUrl = if (segment.startsWith("http")) segment else baseUrl + segment
                val segmentFile = java.io.File(tempDir, "segment_$index.ts")

                downloadSegment(segmentUrl, segmentFile)

                totalDownloaded++
                callback.onProgress(totalDownloaded, segments.size.toLong())
            }

            // 合并分片
            mergeSegments(tempDir, outputPath, segments.size)

            // 清理临时文件
            java.io.File(tempDir).deleteRecursively()

            callback.onComplete()
        } catch (e: Exception) {
            callback.onError(IOException("Failed to download HLS: ${e.message}", e))
        }
    }

    override fun downloadRange(
        url: String,
        start: Long,
        end: Long,
        outputPath: String,
        callback: ProgressCallback
    ) {
        // HLS 不支持 Range，直接下载整个文件
        download(url, outputPath, callback)
    }

    private fun parseM3u8(url: String): List<String> {
        val segments = mutableListOf<String>()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return segments
        }

        response.body?.let { body ->
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line = line?.trim()
                    if (line?.isNotEmpty() == true && !line!!.startsWith("#")) {
                        segments.add(line!!)
                    }
                }
            }
        }

        return segments
    }

    private fun downloadSegment(segmentUrl: String, outputFile: java.io.File) {
        val request = Request.Builder()
            .url(segmentUrl)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to download segment: ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun mergeSegments(tempDir: String, outputPath: String, segmentCount: Int) {
        java.io.FileOutputStream(outputPath).use { fos ->
            for (i in 0 until segmentCount) {
                val segmentFile = java.io.File(tempDir, "segment_$i.ts")
                if (segmentFile.exists()) {
                    segmentFile.inputStream().use { input ->
                        input.copyTo(fos)
                    }
                }
            }
        }
    }

    private fun extractFileName(url: String): String {
        return try {
            val path = url.substringBefore("?").substringAfterLast("/")
            if (path.contains(".")) {
                path.substringBeforeLast(".") + ".mp4"
            } else {
                "video_${System.currentTimeMillis()}.mp4"
            }
        } catch (e: Exception) {
            "video_${System.currentTimeMillis()}.mp4"
        }
    }
}
