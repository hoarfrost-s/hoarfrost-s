package com.downloadmanager.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI

class M3u8Parser(private val client: OkHttpClient) {
    data class M3u8Info(
        val tsUrls: List<String>,
        val estimatedSize: Long,
        val resolution: String
    )

    suspend fun parse(url: String): M3u8Info = withContext(Dispatchers.IO) {
        val content = fetchContent(url)
        if (content.contains("#EXT-X-STREAM-INF")) {
            parseMasterAndSelectBest(content, url)
        } else {
            parseMediaPlaylist(content, url)
        }
    }

    private fun fetchContent(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw RuntimeException("Empty m3u8 content")
    }

    private fun parseMasterAndSelectBest(content: String, baseUrl: String): M3u8Info {
        val lines = content.lines()
        var bestBandwidth = 0L
        var bestUrl = ""
        var resolution = ""

        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val bandwidth = line.split("BANDWIDTH=")
                    .getOrNull(1)
                    ?.split(",")
                    ?.firstOrNull()
                    ?.toLongOrNull() ?: 0L
                val res = line.split("RESOLUTION=")
                    .getOrNull(1)
                    ?.split(",")
                    ?.firstOrNull() ?: ""

                if (bandwidth > bestBandwidth && i + 1 < lines.size) {
                    bestBandwidth = bandwidth
                    bestUrl = lines[i + 1].trim()
                    resolution = res
                }
            }
        }

        val resolvedUrl = resolveUrl(baseUrl, bestUrl)
        return parseMediaPlaylist(fetchContent(resolvedUrl), resolvedUrl)
    }

    private fun parseMediaPlaylist(content: String, baseUrl: String): M3u8Info {
        val tsUrls = mutableListOf<String>()
        var estimatedSize = 0L

        content.lines().forEach { line ->
            if (line.startsWith("#EXTINF:")) {
                // Duration in seconds, estimate size
                estimatedSize += 2_000_000 // rough estimate ~2MB per segment
            } else if (line.isNotBlank() && !line.startsWith("#")) {
                val tsUrl = resolveUrl(baseUrl, line.trim())
                tsUrls.add(tsUrl)
            }
        }

        return M3u8Info(
            tsUrls = tsUrls,
            estimatedSize = estimatedSize,
            resolution = ""
        )
    }

    private fun resolveUrl(baseUrl: String, path: String): String {
        return try {
            val base = URI(baseUrl)
            URI(base.scheme, base.authority, path, null, null).toString()
        } catch (e: Exception) {
            path
        }
    }
}