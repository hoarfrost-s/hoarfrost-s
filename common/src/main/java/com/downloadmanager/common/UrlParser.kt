package com.downloadmanager.common

object UrlParser {
    fun extractFileName(url: String): String {
        val path = try {
            java.net.URI(url).path
        } catch (e: Exception) {
            url.substringAfterLast('/')
        }
        val fileName = path.substringAfterLast('/')
        return if (fileName.isNotBlank()) {
            java.net.URLDecoder.decode(fileName, "UTF-8")
        } else {
            "unknown_file"
        }
    }

    fun extractExtension(url: String): String? {
        val fileName = extractFileName(url)
        return fileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.lowercase()
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url.trim())
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun isM3u8(url: String): Boolean {
        return extractExtension(url) == "m3u8"
    }
}