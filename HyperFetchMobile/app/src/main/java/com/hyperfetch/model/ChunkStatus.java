package com.hyperfetch.model;

/**
 * 分块状态枚举
 * 用于标识下载分块的当前状态
 */
public enum ChunkStatus {
    /**
     * 等待中 - 分块等待开始下载
     */
    PENDING,

    /**
     * 下载中 - 分块正在下载
     */
    DOWNLOADING,

    /**
     * 已完成 - 分块下载完成
     */
    COMPLETED,

    /**
     * 失败 - 分块下载失败
     */
    FAILED
}