package com.hyperfetch.scheduler;

import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.Priority;

import java.util.Comparator;

/**
 * 任务优先级比较器
 * 用于下载任务的优先级排序
 *
 * 排序规则：
 * 1. 先按 Priority 比较（HIGH > NORMAL）
 * 2. 再按 createTime 比较（早创建的优先）
 */
public class TaskComparator implements Comparator<DownloadTask> {

    /**
     * 比较两个下载任务的优先级
     *
     * @param task1 第一个任务
     * @param task2 第二个任务
     * @return 负数表示task1优先，正数表示task2优先，0表示相等
     */
    @Override
    public int compare(DownloadTask task1, DownloadTask task2) {
        // 1. 先按优先级比较（HIGH > NORMAL）
        int priorityCompare = comparePriority(task1.getPriority(), task2.getPriority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // 2. 再按创建时间比较（早创建的优先）
        return compareCreateTime(task1, task2);
    }

    /**
     * 比较两个优先级
     * HIGH 优先于 NORMAL
     *
     * @param p1 第一个优先级
     * @param p2 第二个优先级
     * @return 负数表示p1优先，正数表示p2优先，0表示相等
     */
    private int comparePriority(Priority p1, Priority p2) {
        if (p1 == p2) {
            return 0;
        }
        // HIGH 优先级更高，返回负数表示排在前面
        if (p1 == Priority.HIGH) {
            return -1;
        }
        return 1;
    }

    /**
     * 比较两个任务的创建时间
     * 创建时间早的优先
     *
     * @param task1 第一个任务
     * @param task2 第二个任务
     * @return 负数表示task1创建时间更早，正数表示task2创建时间更早
     */
    private int compareCreateTime(DownloadTask task1, DownloadTask task2) {
        if (task1.getCreateTime() == null && task2.getCreateTime() == null) {
            return 0;
        }
        if (task1.getCreateTime() == null) {
            return 1; // null 排在后面
        }
        if (task2.getCreateTime() == null) {
            return -1; // null 排在后面
        }
        return task1.getCreateTime().compareTo(task2.getCreateTime());
    }
}