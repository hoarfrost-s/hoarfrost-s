package com.hyperfetch.classify

import com.hyperfetch.model.Category

/**
 * 分类服务
 * 根据 URL 后缀自动识别文件类型
 */
object CategoryService {

    private val categoryMap = mapOf(
        // 视频
        Category.VIDEO to listOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v",
            "mpeg", "mpg", "3gp", "3g2", "mts", "m2ts", "vob", "ogv",
            "ts", "m2t", "mxf", "prproj", "dvr-ms", "smi", "sami"
        ),
        // 音频
        Category.AUDIO to listOf(
            "mp3", "flac", "aac", "wav", "ogg", "wma", "m4a", "aiff",
            "ape", "opus", "ac3", "dts", "alac", "amr", "awb", "imy"
        ),
        // 压缩包
        Category.ARCHIVE to listOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tar.gz",
            "tar.bz2", "tar.xz", "tgz", "tbz2", "txz", "iso", "cab",
            "arj", "lzh", "jar", "war", "ear", "zipx", "s7z"
        ),
        // 文档
        Category.DOCUMENT to listOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "rtf", "odt", "ods", "odp", "csv", "tsv", "md", "markdown",
            "xml", "json", "yaml", "yml", "ini", "cfg", "conf"
        ),
        // 程序
        Category.PROGRAM to listOf(
            "apk", "ipa", "exe", "msi", "dmg", "pkg", "deb", "rpm",
            "appimage", "snap", "flatpak", "xapk", "aab", "appx"
        )
    )

    private val mimeTypeMap = mapOf(
        "video/" to Category.VIDEO,
        "audio/" to Category.AUDIO,
        "application/zip" to Category.ARCHIVE,
        "application/x-rar" to Category.ARCHIVE,
        "application/x-7z" to Category.ARCHIVE,
        "application/pdf" to Category.DOCUMENT,
        "application/vnd.ms-" to Category.DOCUMENT,
        "application/vnd.openxmlformats-" to Category.DOCUMENT,
        "application/vnd.android.package-archive" to Category.PROGRAM,
        "application/java-archive" to Category.PROGRAM
    )

    /**
     * 根据 URL 自动识别分类
     * @param url 下载链接
     * @param contentType 可选的 Content-Type
     * @return 识别的分类
     */
    fun classify(url: String, contentType: String? = null): Category {
        // 首先尝试从 URL 后缀识别
        val extension = extractExtension(url)
        if (extension != null) {
            val lowerExt = extension.lowercase()
            for ((category, extensions) in categoryMap) {
                if (extensions.contains(lowerExt)) {
                    return category
                }
            }
        }

        // 如果后缀未命中，尝试 Content-Type
        if (contentType != null) {
            for ((mimePrefix, category) in mimeTypeMap) {
                if (contentType.startsWith(mimePrefix)) {
                    return category
                }
            }
        }

        return Category.OTHER
    }

    /**
     * 从 URL 中提取文件扩展名
     */
    fun extractExtension(url: String): String? {
        return try {
            val path = url.substringBefore("?").substringAfterLast("/")
            if (path.contains(".") && path.lastIndexOf(".") < path.length - 1) {
                path.substringAfterLast(".")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从 URL 中提取文件名
     */
    fun extractFileName(url: String): String {
        return try {
            val path = url.substringBefore("?").substringAfterLast("/")
            if (path.isNotEmpty() && !path.endsWith("/")) {
                java.net.URLDecoder.decode(path, "UTF-8")
            } else {
                "download_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    /**
     * 获取分类对应的颜色
     */
    fun getCategoryColor(category: Category): Long {
        return when (category) {
            Category.VIDEO -> 0xFFFFB547
            Category.AUDIO -> 0xFF44D4A4
            Category.ARCHIVE -> 0xFF5FA8FF
            Category.DOCUMENT -> 0xFFA78BFF
            Category.PROGRAM -> 0xFFFF8FA3
            Category.OTHER -> 0xFF8A93A6
        }
    }
}
