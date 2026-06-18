package com.hyperfetch.model;

/**
 * 文件分类枚举
 * 用于标识下载文件的类型
 */
public enum Category {
    /**
     * 视频文件
     */
    VIDEO,

    /**
     * 音频文件
     */
    AUDIO,

    /**
     * 压缩包文件
     */
    ARCHIVE,

    /**
     * 文档文件
     */
    DOCUMENT,

    /**
     * 程序文件
     */
    PROGRAM,

    /**
     * 其他类型文件
     */
    OTHER
}