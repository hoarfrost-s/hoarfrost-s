package com.hyperfetch.ui;

import android.content.Context;

import com.hyperfetch.model.TaskStatus;

import java.text.DecimalFormat;

/**
 * UI 工具类
 * 提供格式化文件大小、下载速度、剩余时间等常用方法
 */
public class UiUtils {

    /**
     * 文件大小格式化器
     */
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");

    /**
     * 速度格式化器
     */
    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("#.##");

    /**
     * 时间格式化器
     */
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("#");

    /**
     * KB 单位
     */
    private static final long KB = 1024;

    /**
     * MB 单位
     */
    private static final long MB = KB * 1024;

    /**
     * GB 单位
     */
    private static final long GB = MB * 1024;

    /**
     * 格式化文件大小
     * 根据大小自动选择合适的单位（KB/MB/GB）
     *
     * @param bytes 字节数
     * @return 格式化后的字符串，如 "1.5 MB"
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }

        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            return SIZE_FORMAT.format(bytes / KB) + " KB";
        } else if (bytes < GB) {
            return SIZE_FORMAT.format(bytes / MB) + " MB";
        } else {
            return SIZE_FORMAT.format(bytes / GB) + " GB";
        }
    }

    /**
     * 格式化下载速度
     * 根据速度自动选择合适的单位（KB/s、MB/s、GB/s）
     *
     * @param bytesPerSecond 每秒下载字节数
     * @return 格式化后的字符串，如 "2.5 MB/s"
     */
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 0) {
            return "0 B/s";
        }

        if (bytesPerSecond < KB) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < MB) {
            return SPEED_FORMAT.format(bytesPerSecond / KB) + " KB/s";
        } else if (bytesPerSecond < GB) {
            return SPEED_FORMAT.format(bytesPerSecond / MB) + " MB/s";
        } else {
            return SPEED_FORMAT.format(bytesPerSecond / GB) + " GB/s";
        }
    }

    /**
     * 格式化剩余时间
     * 根据剩余秒数自动选择合适的单位（秒、分钟、小时）
     *
     * @param remainingSeconds 剩余秒数
     * @return 格式化后的字符串，如 "5 分钟" 或 "2 小时"
     */
    public static String formatRemainingTime(long remainingSeconds) {
        if (remainingSeconds < 0 || remainingSeconds == Long.MAX_VALUE) {
            return "计算中...";
        }

        if (remainingSeconds < 60) {
            return remainingSeconds + " 秒";
        } else if (remainingSeconds < 3600) {
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            if (seconds > 0) {
                return minutes + " 分 " + seconds + " 秒";
            }
            return minutes + " 分钟";
        } else {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            if (minutes > 0) {
                return hours + " 小时 " + minutes + " 分";
            }
            return hours + " 小时";
        }
    }

    /**
     * 计算剩余时间
     * 根据已下载大小、总大小和当前速度计算剩余时间
     *
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     * @param speed      当前下载速度（字节/秒）
     * @return 剩余秒数，如果无法计算返回 Long.MAX_VALUE
     */
    public static long calculateRemainingTime(long downloaded, long totalSize, long speed) {
        if (totalSize <= 0 || speed <= 0) {
            return Long.MAX_VALUE;
        }

        long remainingBytes = totalSize - downloaded;
        if (remainingBytes <= 0) {
            return 0;
        }

        return remainingBytes / speed;
    }

    /**
     * 获取状态徽章颜色资源 ID
     * 根据任务状态返回对应的颜色
     *
     * @param status 任务状态
     * @param context 上下文
     * @return 颜色值
     */
    public static int getStatusBadgeColor(TaskStatus status, Context context) {
        int colorResId;
        switch (status) {
            case QUEUED:
                colorResId = com.hyperfetch.R.color.statusPending;
                break;
            case DOWNLOADING:
                colorResId = com.hyperfetch.R.color.statusDownloading;
                break;
            case PAUSED:
                colorResId = com.hyperfetch.R.color.statusPaused;
                break;
            case COMPLETED:
                colorResId = com.hyperfetch.R.color.statusCompleted;
                break;
            case FAILED:
                colorResId = com.hyperfetch.R.color.statusFailed;
                break;
            case COMPLETED_WITH_WARNING:
                colorResId = com.hyperfetch.R.color.warning;
                break;
            default:
                colorResId = com.hyperfetch.R.color.statusCancelled;
                break;
        }
        return context.getResources().getColor(colorResId, null);
    }

    /**
     * 获取状态显示文本
     * 根据任务状态返回对应的中文文本
     *
     * @param status 任务状态
     * @param context 上下文
     * @return 状态文本
     */
    public static String getStatusText(TaskStatus status, Context context) {
        switch (status) {
            case QUEUED:
                return context.getString(com.hyperfetch.R.string.status_pending);
            case DOWNLOADING:
                return context.getString(com.hyperfetch.R.string.status_downloading);
            case PAUSED:
                return context.getString(com.hyperfetch.R.string.status_paused);
            case COMPLETED:
                return context.getString(com.hyperfetch.R.string.status_completed);
            case FAILED:
                return context.getString(com.hyperfetch.R.string.status_failed);
            case COMPLETED_WITH_WARNING:
                return "完成（有警告）";
            default:
                return context.getString(com.hyperfetch.R.string.status_cancelled);
        }
    }

    /**
     * 获取分类显示名称
     * 根据文件分类返回对应的中文名称
     *
     * @param category 分类枚举
     * @return 分类名称
     */
    public static String getCategoryDisplayName(com.hyperfetch.model.Category category) {
        switch (category) {
            case VIDEO:
                return "视频";
            case AUDIO:
                return "音频";
            case ARCHIVE:
                return "压缩包";
            case DOCUMENT:
                return "文档";
            case PROGRAM:
                return "程序";
            case OTHER:
                return "其他";
            default:
                return "未知";
        }
    }

    /**
     * 获取分类图标资源 ID
     * 根据文件分类返回对应的图标
     *
     * @param category 分类枚举
     * @return 图标资源 ID（暂时返回通用图标）
     */
    public static int getCategoryIconResId(com.hyperfetch.model.Category category) {
        // 暂时使用 Android 内置图标，后续可替换为自定义图标
        switch (category) {
            case VIDEO:
                return android.R.drawable.ic_media_play;
            case AUDIO:
                return android.R.drawable.ic_media_play;
            case ARCHIVE:
                return android.R.drawable.ic_menu_save;
            case DOCUMENT:
                return android.R.drawable.ic_menu_edit;
            case PROGRAM:
                return android.R.drawable.ic_menu_manage;
            default:
                return android.R.drawable.ic_menu_help;
        }
    }

    /**
     * 格式化进度百分比
     *
     * @param progress 进度值（0-100）
     * @return 格式化后的百分比字符串，如 "50%"
     */
    public static String formatProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        return progress + "%";
    }

    /**
     * 格式化进度百分比（带小数）
     *
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     * @return 格式化后的百分比字符串，如 "50.5%"
     */
    public static String formatProgressWithDecimal(long downloaded, long totalSize) {
        if (totalSize <= 0) {
            return "0%";
        }

        double progress = (double) downloaded * 100 / totalSize;
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }

        return SPEED_FORMAT.format(progress) + "%";
    }

    /**
     * 格式化任务统计信息
     *
     * @param downloadingCount 正在下载的任务数
     * @param completedCount   已完成的任务数
     * @param totalCount       总任务数
     * @return 格式化后的统计字符串
     */
    public static String formatTaskStats(int downloadingCount, int completedCount, int totalCount) {
        return String.format("下载中: %d | 已完成: %d | 总计: %d",
                downloadingCount, completedCount, totalCount);
    }

    /**
     * 格式化线程数显示
     *
     * @param threadCount 线程数
     * @return 格式化后的字符串
     */
    public static String formatThreadCount(int threadCount) {
        return threadCount + " 线程";
    }

    /**
     * 格式化速度限制显示
     *
     * @param speedLimit 速度限制（字节/秒），0 表示无限制
     * @return 格式化后的字符串
     */
    public static String formatSpeedLimit(long speedLimit) {
        if (speedLimit <= 0) {
            return "无限制";
        }
        return "限速 " + formatSpeed(speedLimit);
    }
}