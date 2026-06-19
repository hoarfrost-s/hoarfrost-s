package com.downloadmanager.common

enum class FileCategory(val displayName: String, val subDir: String) {
    VIDEO("视频", "Videos"),
    AUDIO("音频", "Audio"),
    ARCHIVE("压缩包", "Archives"),
    DOCUMENT("文档", "Documents"),
    PICTURE("图片", "Pictures"),
    APPLICATION("可执行", "Applications"),
    OTHER("其他", "Others");

    companion object {
        private val suffixMap: Map<String, FileCategory> = mapOf(
            // 视频
            "mp4" to VIDEO, "mkv" to VIDEO, "avi" to VIDEO, "mov" to VIDEO,
            "flv" to VIDEO, "wmv" to VIDEO, "webm" to VIDEO, "ts" to VIDEO,
            "m3u8" to VIDEO, "rmvb" to VIDEO, "3gp" to VIDEO,
            // 音频
            "mp3" to AUDIO, "flac" to AUDIO, "aac" to AUDIO, "wav" to AUDIO,
            "ogg" to AUDIO, "wma" to AUDIO, "m4a" to AUDIO, "ape" to AUDIO, "alac" to AUDIO,
            // 压缩包
            "zip" to ARCHIVE, "rar" to ARCHIVE, "7z" to ARCHIVE, "tar" to ARCHIVE,
            "gz" to ARCHIVE, "bz2" to ARCHIVE, "xz" to ARCHIVE, "zst" to ARCHIVE, "lz4" to ARCHIVE,
            // 文档
            "pdf" to DOCUMENT, "doc" to DOCUMENT, "docx" to DOCUMENT, "xls" to DOCUMENT,
            "xlsx" to DOCUMENT, "ppt" to DOCUMENT, "pptx" to DOCUMENT, "txt" to DOCUMENT,
            "md" to DOCUMENT, "csv" to DOCUMENT, "json" to DOCUMENT, "xml" to DOCUMENT,
            // 图片
            "jpg" to PICTURE, "jpeg" to PICTURE, "png" to PICTURE, "gif" to PICTURE,
            "bmp" to PICTURE, "svg" to PICTURE, "webp" to PICTURE, "ico" to PICTURE,
            "tiff" to PICTURE, "raw" to PICTURE,
            // 可执行
            "exe" to APPLICATION, "msi" to APPLICATION, "dmg" to APPLICATION,
            "apk" to APPLICATION, "appimage" to APPLICATION, "deb" to APPLICATION,
            "rpm" to APPLICATION, "sh" to APPLICATION, "bat" to APPLICATION,
            // 其他
            "iso" to OTHER, "torrent" to OTHER
        )

        fun fromExtension(extension: String?): FileCategory {
            if (extension.isNullOrBlank()) return OTHER
            return suffixMap[extension.lowercase()] ?: OTHER
        }
    }
}