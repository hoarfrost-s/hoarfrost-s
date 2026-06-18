package com.hyperfetch.service;

import com.hyperfetch.model.TaskStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 状态流转规则类
 * 定义任务状态之间的合法转换规则
 *
 * 状态流转规则：
 * - QUEUED -> DOWNLOADING: 调度开始
 * - DOWNLOADING -> PAUSED: 用户暂停 / Wi-Fi断开
 * - DOWNLOADING -> COMPLETED: 下载完成
 * - DOWNLOADING -> FAILED: 错误 / 重试次数用尽
 * - PAUSED -> DOWNLOADING: 用户恢复 / Wi-Fi恢复
 * - FAILED -> DOWNLOADING: 用户重试
 */
public class StatusTransition {

    /**
     * 状态转换原因枚举
     */
    public enum TransitionReason {
        /**
         * 调度开始
         */
        SCHEDULE,

        /**
         * 用户暂停
         */
        USER_PAUSE,

        /**
         * Wi-Fi断开
         */
        WIFI_DISCONNECTED,

        /**
         * 用户恢复
         */
        USER_RESUME,

        /**
         * Wi-Fi恢复
         */
        WIFI_RECONNECTED,

        /**
         * 下载完成
         */
        DOWNLOAD_COMPLETE,

        /**
         * 下载错误
         */
        DOWNLOAD_ERROR,

        /**
         * 重试次数用尽
         */
        RETRIES_EXHAUSTED,

        /**
         * 用户重试
         */
        USER_RETRY
    }

    /**
     * 状态转换规则映射
     * Key: 当前状态
     * Value: 允许转换到的目标状态集合
     */
    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITION_RULES = new HashMap<>();

    static {
        // QUEUED -> DOWNLOADING: 调度开始
        Set<TaskStatus> queuedTargets = new HashSet<>();
        queuedTargets.add(TaskStatus.DOWNLOADING);
        TRANSITION_RULES.put(TaskStatus.QUEUED, queuedTargets);

        // DOWNLOADING -> PAUSED / COMPLETED / FAILED
        Set<TaskStatus> downloadingTargets = new HashSet<>();
        downloadingTargets.add(TaskStatus.PAUSED);
        downloadingTargets.add(TaskStatus.COMPLETED);
        downloadingTargets.add(TaskStatus.FAILED);
        TRANSITION_RULES.put(TaskStatus.DOWNLOADING, downloadingTargets);

        // PAUSED -> DOWNLOADING
        Set<TaskStatus> pausedTargets = new HashSet<>();
        pausedTargets.add(TaskStatus.DOWNLOADING);
        TRANSITION_RULES.put(TaskStatus.PAUSED, pausedTargets);

        // FAILED -> DOWNLOADING
        Set<TaskStatus> failedTargets = new HashSet<>();
        failedTargets.add(TaskStatus.DOWNLOADING);
        TRANSITION_RULES.put(TaskStatus.FAILED, failedTargets);

        // COMPLETED 和 COMPLETED_WITH_WARNING 为终态，不允许转换
        TRANSITION_RULES.put(TaskStatus.COMPLETED, new HashSet<>());
        TRANSITION_RULES.put(TaskStatus.COMPLETED_WITH_WARNING, new HashSet<>());
    }

    /**
     * 状态转换原因映射
     * Key: "fromStatus:toStatus" 格式的字符串
     * Value: 允许的转换原因
     */
    private static final Map<String, Set<TransitionReason>> TRANSITION_REASONS = new HashMap<>();

    static {
        // QUEUED -> DOWNLOADING: 调度开始
        String queuedToDownloading = TaskStatus.QUEUED + ":" + TaskStatus.DOWNLOADING;
        Set<TransitionReason> scheduleReasons = new HashSet<>();
        scheduleReasons.add(TransitionReason.SCHEDULE);
        TRANSITION_REASONS.put(queuedToDownloading, scheduleReasons);

        // DOWNLOADING -> PAUSED: 用户暂停 / Wi-Fi断开
        String downloadingToPaused = TaskStatus.DOWNLOADING + ":" + TaskStatus.PAUSED;
        Set<TransitionReason> pauseReasons = new HashSet<>();
        pauseReasons.add(TransitionReason.USER_PAUSE);
        pauseReasons.add(TransitionReason.WIFI_DISCONNECTED);
        TRANSITION_REASONS.put(downloadingToPaused, pauseReasons);

        // DOWNLOADING -> COMPLETED: 下载完成
        String downloadingToCompleted = TaskStatus.DOWNLOADING + ":" + TaskStatus.COMPLETED;
        Set<TransitionReason> completeReasons = new HashSet<>();
        completeReasons.add(TransitionReason.DOWNLOAD_COMPLETE);
        TRANSITION_REASONS.put(downloadingToCompleted, completeReasons);

        // DOWNLOADING -> FAILED: 错误 / 重试次数用尽
        String downloadingToFailed = TaskStatus.DOWNLOADING + ":" + TaskStatus.FAILED;
        Set<TransitionReason> failedReasons = new HashSet<>();
        failedReasons.add(TransitionReason.DOWNLOAD_ERROR);
        failedReasons.add(TransitionReason.RETRIES_EXHAUSTED);
        TRANSITION_REASONS.put(downloadingToFailed, failedReasons);

        // PAUSED -> DOWNLOADING: 用户恢复 / Wi-Fi恢复
        String pausedToDownloading = TaskStatus.PAUSED + ":" + TaskStatus.DOWNLOADING;
        Set<TransitionReason> resumeReasons = new HashSet<>();
        resumeReasons.add(TransitionReason.USER_RESUME);
        resumeReasons.add(TransitionReason.WIFI_RECONNECTED);
        TRANSITION_REASONS.put(pausedToDownloading, resumeReasons);

        // FAILED -> DOWNLOADING: 用户重试
        String failedToDownloading = TaskStatus.FAILED + ":" + TaskStatus.DOWNLOADING;
        Set<TransitionReason> retryReasons = new HashSet<>();
        retryReasons.add(TransitionReason.USER_RETRY);
        TRANSITION_REASONS.put(failedToDownloading, retryReasons);
    }

    /**
     * 检查状态转换是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 是否允许转换
     */
    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<TaskStatus> allowedTargets = TRANSITION_RULES.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    /**
     * 检查状态转换是否合法（带原因）
     *
     * @param from   当前状态
     * @param to     目标状态
     * @param reason 转换原因
     * @return 是否允许转换
     */
    public static boolean canTransition(TaskStatus from, TaskStatus to, TransitionReason reason) {
        if (!canTransition(from, to)) {
            return false;
        }
        String key = from + ":" + to;
        Set<TransitionReason> allowedReasons = TRANSITION_REASONS.get(key);
        return allowedReasons != null && allowedReasons.contains(reason);
    }

    /**
     * 获取指定状态允许转换到的所有目标状态
     *
     * @param from 当前状态
     * @return 允许转换到的目标状态集合
     */
    public static Set<TaskStatus> getAllowedTargets(TaskStatus from) {
        Set<TaskStatus> targets = TRANSITION_RULES.get(from);
        return targets != null ? new HashSet<>(targets) : new HashSet<>();
    }

    /**
     * 获取状态转换的允许原因
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 允许的转换原因集合
     */
    public static Set<TransitionReason> getAllowedReasons(TaskStatus from, TaskStatus to) {
        String key = from + ":" + to;
        Set<TransitionReason> reasons = TRANSITION_REASONS.get(key);
        return reasons != null ? new HashSet<>(reasons) : new HashSet<>();
    }

    /**
     * 判断状态是否为终态
     *
     * @param status 任务状态
     * @return 是否为终态
     */
    public static boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED || status == TaskStatus.COMPLETED_WITH_WARNING;
    }

    /**
     * 判断状态是否为活跃状态（正在下载）
     *
     * @param status 任务状态
     * @return 是否为活跃状态
     */
    public static boolean isActiveStatus(TaskStatus status) {
        return status == TaskStatus.DOWNLOADING;
    }

    /**
     * 判断状态是否为可恢复状态
     *
     * @param status 任务状态
     * @return 是否可恢复
     */
    public static boolean isResumableStatus(TaskStatus status) {
        return status == TaskStatus.PAUSED || status == TaskStatus.FAILED;
    }
}