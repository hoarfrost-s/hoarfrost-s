package com.hyperfetch.model

/**
 * 文件分类枚举
 */
enum class Category(val displayName: String) {
    VIDEO("视频"),
    AUDIO("音频"),
    ARCHIVE("压缩包"),
    DOCUMENT("文档"),
    PROGRAM("程序"),
    OTHER("其他")
}

/**
 * 任务状态枚举
 */
enum class TaskStatus {
    QUEUED,       // 排队中
    DOWNLOADING,  // 下载中
    PAUSED,       // 已暂停
    COMPLETED,    // 已完成
    FAILED        // 失败
}

/**
 * 优先级枚举
 */
enum class Priority {
    NORMAL,
    HIGH
}

/**
 * 分块状态枚举
 */
enum class ChunkStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

/**
 * 排队策略枚举
 */
enum class QueueStrategy {
    PRIORITY_FIRST,  // 优先级优先
    TIME_FIRST       // 创建时间优先
}
