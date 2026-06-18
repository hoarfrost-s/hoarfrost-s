package com.hyperfetch.service;

/**
 * 服务动作常量类
 * 定义 DownloadService 支持的所有动作和 Intent 键
 */
public class ServiceActions {

    /**
     * 创建下载任务动作
     */
    public static final String ACTION_CREATE = "com.hyperfetch.action.CREATE";

    /**
     * 暂停下载任务动作
     */
    public static final String ACTION_PAUSE = "com.hyperfetch.action.PAUSE";

    /**
     * 恢复下载任务动作
     */
    public static final String ACTION_RESUME = "com.hyperfetch.action.RESUME";

    /**
     * 删除下载任务动作
     */
    public static final String ACTION_DELETE = "com.hyperfetch.action.DELETE";

    /**
     * 重试下载任务动作
     */
    public static final String ACTION_RETRY = "com.hyperfetch.action.RETRY";

    /**
     * 取消下载任务动作
     */
    public static final String ACTION_CANCEL = "com.hyperfetch.action.CANCEL";

    /**
     * 恢复下载任务动作（用于进程被杀后的自动恢复）
     */
    public static final String ACTION_RECOVER = "com.hyperfetch.action.RECOVER";

    // ==================== Intent 键常量 ====================

    /**
     * 下载地址键
     */
    public static final String KEY_URL = "url";

    /**
     * 任务ID键
     */
    public static final String KEY_TASK_ID = "task_id";

    /**
     * 文件名键
     */
    public static final String KEY_FILE_NAME = "file_name";

    /**
     * 保存路径键
     */
    public static final String KEY_SAVE_PATH = "save_path";

    /**
     * 线程数键
     */
    public static final String KEY_THREAD_COUNT = "thread_count";

    /**
     * 速度限制键
     */
    public static final String KEY_SPEED_LIMIT = "speed_limit";

    /**
     * 分类键
     */
    public static final String KEY_CATEGORY = "category";

    /**
     * 优先级键
     */
    public static final String KEY_PRIORITY = "priority";

    /**
     * 下载选项键（用于传递额外的下载配置）
     */
    public static final String KEY_OPTIONS = "options";

    /**
     * 恢复任务数量键（用于恢复下载时传递需要恢复的任务数量）
     */
    public static final String KEY_RECOVERY_COUNT = "recovery_count";

    /**
     * 私有构造函数，防止实例化
     */
    private ServiceActions() {
        throw new AssertionError("不允许实例化常量类");
    }
}