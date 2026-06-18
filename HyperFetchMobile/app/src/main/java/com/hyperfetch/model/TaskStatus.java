package com.hyperfetch.model;

/**
 * 任务状态枚举
 * 用于标识下载任务的当前状态
 */
public enum TaskStatus {
    /**
     * 排队中 - 任务已创建，等待开始下载
     */
    QUEUED,

    /**
     * 下载中 - 任务正在下载
     */
    DOWNLOADING,

    /**
     * 已暂停 - 任务被用户暂停
     */
    PAUSED,

    /**
     * 已完成 - 任务下载完成
     */
    COMPLETED,

    /**
     * 失败 - 任务下载失败
     */
    FAILED,

    /**
     * 完成但有警告 - 任务完成但存在问题（如合并失败保留原始文件）
     */
    COMPLETED_WITH_WARNING
}