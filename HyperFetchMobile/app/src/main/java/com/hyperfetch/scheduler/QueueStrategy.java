package com.hyperfetch.scheduler;

/**
 * 排队策略枚举
 * 定义任务调度器中任务的排队顺序策略
 */
public enum QueueStrategy {
    /**
     * 优先级优先策略
     * 按照任务优先级排序，高优先级任务优先执行
     * 优先级相同时，按照创建时间排序
     */
    PRIORITY_FIRST,

    /**
     * 创建时间优先策略
     * 按照任务创建时间排序，先创建的任务优先执行
     * 遵循先进先出（FIFO）原则
     */
    CREATE_TIME_FIRST
}