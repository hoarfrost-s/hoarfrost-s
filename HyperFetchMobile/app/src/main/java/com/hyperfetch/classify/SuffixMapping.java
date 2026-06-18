package com.hyperfetch.classify;

import com.hyperfetch.model.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * 后缀映射表
 * 定义文件后缀与分类的映射关系
 */
public class SuffixMapping {

    /**
     * 视频文件后缀
     */
    private static final String[] VIDEO_SUFFIXES = {
            "mp4", "m3u8", "mkv", "avi", "mov", "wmv", "flv",
            "webm", "m4v", "mpeg", "mpg", "3gp", "ts", "f4v"
    };

    /**
     * 音频文件后缀
     */
    private static final String[] AUDIO_SUFFIXES = {
            "mp3", "flac", "aac", "wav", "ogg", "wma", "m4a",
            "ape", "alac", "aiff", "mid", "midi", "ra", "ram"
    };

    /**
     * 压缩包文件后缀
     */
    private static final String[] ARCHIVE_SUFFIXES = {
            "zip", "rar", "7z", "tar", "gz", "bz2",
            "xz", "z", "tgz", "tbz2", "iso", "cab", "arj"
    };

    /**
     * 文档文件后缀
     */
    private static final String[] DOCUMENT_SUFFIXES = {
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "rtf", "odt", "ods", "odp", "epub", "mobi", "wps", "csv"
    };

    /**
     * 程序文件后缀
     */
    private static final String[] PROGRAM_SUFFIXES = {
            "exe", "apk", "dmg", "deb", "rpm", "iso",
            "msi", "jar", "war", "apk", "ipa", "app", "run", "bin"
    };

    /**
     * 创建默认的后缀映射表
     *
     * @return 后缀映射表
     */
    public static Map<String, Category> createSuffixMap() {
        return createSuffixMap(null);
    }

    /**
     * 创建后缀映射表（支持配置扩展）
     *
     * @param config 分类配置，可为 null
     * @return 后缀映射表
     */
    public static Map<String, Category> createSuffixMap(CategoryConfig config) {
        Map<String, Category> map = new HashMap<>();

        // 初始化视频后缀映射
        for (String suffix : VIDEO_SUFFIXES) {
            map.put(suffix, Category.VIDEO);
        }

        // 初始化音频后缀映射
        for (String suffix : AUDIO_SUFFIXES) {
            map.put(suffix, Category.AUDIO);
        }

        // 初始化压缩包后缀映射
        for (String suffix : ARCHIVE_SUFFIXES) {
            map.put(suffix, Category.ARCHIVE);
        }

        // 初始化文档后缀映射
        for (String suffix : DOCUMENT_SUFFIXES) {
            map.put(suffix, Category.DOCUMENT);
        }

        // 初始化程序后缀映射
        for (String suffix : PROGRAM_SUFFIXES) {
            map.put(suffix, Category.PROGRAM);
        }

        // 如果有配置，合并自定义映射
        if (config != null && config.getCustomSuffixMappings() != null) {
            map.putAll(config.getCustomSuffixMappings());
        }

        return map;
    }

    /**
     * 获取视频文件后缀列表
     *
     * @return 视频后缀数组
     */
    public static String[] getVideoSuffixes() {
        return VIDEO_SUFFIXES.clone();
    }

    /**
     * 获取音频文件后缀列表
     *
     * @return 音频后缀数组
     */
    public static String[] getAudioSuffixes() {
        return AUDIO_SUFFIXES.clone();
    }

    /**
     * 获取压缩包文件后缀列表
     *
     * @return 压缩包后缀数组
     */
    public static String[] getArchiveSuffixes() {
        return ARCHIVE_SUFFIXES.clone();
    }

    /**
     * 获取文档文件后缀列表
     *
     * @return 文档后缀数组
     */
    public static String[] getDocumentSuffixes() {
        return DOCUMENT_SUFFIXES.clone();
    }

    /**
     * 获取程序文件后缀列表
     *
     * @return 程序后缀数组
     */
    public static String[] getProgramSuffixes() {
        return PROGRAM_SUFFIXES.clone();
    }
}