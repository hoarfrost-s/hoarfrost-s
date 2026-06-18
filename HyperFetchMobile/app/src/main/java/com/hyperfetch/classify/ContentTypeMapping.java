package com.hyperfetch.classify;

import com.hyperfetch.model.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * MIME 类型映射
 * 定义 Content-Type 与分类的映射关系
 */
public class ContentTypeMapping {

    /**
     * 视频 MIME 类型前缀
     */
    private static final String VIDEO_PREFIX = "video/";

    /**
     * 音频 MIME 类型前缀
     */
    private static final String AUDIO_PREFIX = "audio/";

    /**
     * MIME 类型映射表
     */
    private static final Map<String, Category> MIME_CATEGORY_MAP = new HashMap<>();

    static {
        // 压缩包 MIME 类型
        MIME_CATEGORY_MAP.put("application/zip", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-zip-compressed", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-rar-compressed", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-rar", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-7z-compressed", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-tar", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/gzip", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-gzip", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-bzip2", Category.ARCHIVE);
        MIME_CATEGORY_MAP.put("application/x-xz", Category.ARCHIVE);

        // 文档 MIME 类型
        MIME_CATEGORY_MAP.put("application/pdf", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/msword", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/vnd.ms-excel", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/vnd.ms-powerpoint", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/rtf", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("text/plain", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("text/html", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("text/csv", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/epub+zip", Category.DOCUMENT);
        MIME_CATEGORY_MAP.put("application/x-mobipocket-ebook", Category.DOCUMENT);

        // 程序 MIME 类型
        MIME_CATEGORY_MAP.put("application/octet-stream", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-msdownload", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-msdos-program", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/vnd.android.package-archive", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-apple-diskimage", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-debian-package", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-redhat-package-manager", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/x-rpm", Category.PROGRAM);
        MIME_CATEGORY_MAP.put("application/java-archive", Category.PROGRAM);

        // 特殊视频类型
        MIME_CATEGORY_MAP.put("application/x-mpegURL", Category.VIDEO);
        MIME_CATEGORY_MAP.put("application/vnd.apple.mpegurl", Category.VIDEO);
        MIME_CATEGORY_MAP.put("application/mp4", Category.VIDEO);

        // 特殊音频类型
        MIME_CATEGORY_MAP.put("application/ogg", Category.AUDIO);
        MIME_CATEGORY_MAP.put("application/x-flac", Category.AUDIO);
    }

    /**
     * 根据 Content-Type 映射分类
     *
     * @param contentType MIME 类型字符串
     * @return 分类结果
     */
    public static Category mapContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return Category.OTHER;
        }

        // 转换为小写并去除空格
        String normalizedContentType = contentType.toLowerCase().trim();

        // 移除可能的参数部分（如 charset）
        int semicolonIndex = normalizedContentType.indexOf(';');
        if (semicolonIndex > 0) {
            normalizedContentType = normalizedContentType.substring(0, semicolonIndex).trim();
        }

        // 1. 检查视频前缀
        if (normalizedContentType.startsWith(VIDEO_PREFIX)) {
            return Category.VIDEO;
        }

        // 2. 检查音频前缀
        if (normalizedContentType.startsWith(AUDIO_PREFIX)) {
            return Category.AUDIO;
        }

        // 3. 检查精确映射
        Category category = MIME_CATEGORY_MAP.get(normalizedContentType);
        if (category != null) {
            return category;
        }

        // 4. application/octet-stream 需要进一步判断
        // 这里返回 OTHER，实际使用时可能需要结合后缀判断
        return Category.OTHER;
    }

    /**
     * 判断是否为视频 MIME 类型
     *
     * @param contentType MIME 类型
     * @return 是否为视频类型
     */
    public static boolean isVideo(String contentType) {
        return mapContentType(contentType) == Category.VIDEO;
    }

    /**
     * 判断是否为音频 MIME 类型
     *
     * @param contentType MIME 类型
     * @return 是否为音频类型
     */
    public static boolean isAudio(String contentType) {
        return mapContentType(contentType) == Category.AUDIO;
    }

    /**
     * 判断是否为压缩包 MIME 类型
     *
     * @param contentType MIME 类型
     * @return 是否为压缩包类型
     */
    public static boolean isArchive(String contentType) {
        return mapContentType(contentType) == Category.ARCHIVE;
    }

    /**
     * 判断是否为文档 MIME 类型
     *
     * @param contentType MIME 类型
     * @return 是否为文档类型
     */
    public static boolean isDocument(String contentType) {
        return mapContentType(contentType) == Category.DOCUMENT;
    }

    /**
     * 判断是否为程序 MIME 类型
     *
     * @param contentType MIME 类型
     * @return 是否为程序类型
     */
    public static boolean isProgram(String contentType) {
        return mapContentType(contentType) == Category.PROGRAM;
    }

    /**
     * 获取 MIME 类型映射表
     *
     * @return 映射表副本
     */
    public static Map<String, Category> getMimeCategoryMap() {
        return new HashMap<>(MIME_CATEGORY_MAP);
    }
}