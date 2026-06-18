package com.hyperfetch.engine;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 重试处理器类
 * 负责判断异常是否可重试，以及计算重试延迟时间
 * 使用指数退避策略进行重试
 */
public class RetryHandler {

    private static final String TAG = "RetryHandler";

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRIES = 5;

    /**
     * 重试延迟时间数组（指数退避）
     * 单位：毫秒
     * 序列：1s, 2s, 4s, 8s, 16s
     */
    private static final long[] RETRY_DELAYS = {1000, 2000, 4000, 8000, 16000};

    /**
     * 异常分类器
     */
    private final ExceptionClassifier exceptionClassifier;

    /**
     * 默认构造函数
     */
    public RetryHandler() {
        this.exceptionClassifier = new ExceptionClassifier();
    }

    /**
     * 构造函数
     *
     * @param exceptionClassifier 异常分类器
     */
    public RetryHandler(ExceptionClassifier exceptionClassifier) {
        this.exceptionClassifier = exceptionClassifier;
    }

    /**
     * 判断是否应该重试
     *
     * @param e          发生的异常
     * @param retryCount 当前已重试次数
     * @return 是否应该重试
     */
    public boolean shouldRetry(Exception e, int retryCount) {
        // 已达到最大重试次数
        if (retryCount >= MAX_RETRIES) {
            return false;
        }

        // 判断异常是否可重试
        return isRetryable(e);
    }

    /**
     * 获取重试延迟时间
     * 使用指数退避策略
     *
     * @param retryCount 当前重试次数（从0开始）
     * @return 重试延迟时间（毫秒）
     */
    public long getRetryDelay(int retryCount) {
        if (retryCount < 0) {
            return RETRY_DELAYS[0];
        }
        if (retryCount >= RETRY_DELAYS.length) {
            return RETRY_DELAYS[RETRY_DELAYS.length - 1];
        }
        return RETRY_DELAYS[retryCount];
    }

    /**
     * 判断异常是否可重试
     *
     * @param e 异常对象
     * @return 是否可重试
     */
    public boolean isRetryable(Exception e) {
        return exceptionClassifier.isRetryable(e);
    }

    /**
     * 判断是否为网络瞬断异常
     * 网络瞬断包括：SocketTimeoutException, ConnectException, UnknownHostException
     *
     * @param e 异常对象
     * @return 是否为网络瞬断
     */
    public boolean isTransientNetworkError(Exception e) {
        return exceptionClassifier.isTransientNetworkError(e);
    }

    /**
     * 判断是否为IO异常
     *
     * @param e 异常对象
     * @return 是否为IO异常
     */
    public boolean isIOException(Exception e) {
        return exceptionClassifier.isIOException(e);
    }

    /**
     * 获取异常分类
     *
     * @param e 异常对象
     * @return 异常分类
     */
    public ExceptionClassifier.ExceptionCategory classify(Exception e) {
        return exceptionClassifier.classify(e);
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return MAX_RETRIES;
    }

    /**
     * 获取所有重试延迟时间
     *
     * @return 重试延迟时间数组
     */
    public long[] getRetryDelays() {
        return RETRY_DELAYS.clone();
    }

    /**
     * 计算自定义重试延迟时间
     * 基于指数退避算法
     *
     * @param baseDelay 基础延迟时间（毫秒）
     * @param retryCount 重试次数
     * @return 计算后的延迟时间
     */
    public static long calculateExponentialDelay(long baseDelay, int retryCount) {
        if (retryCount < 0) {
            return baseDelay;
        }
        // 指数退避：baseDelay * 2^retryCount
        // 最大不超过 60 秒
        long delay = baseDelay * (long) Math.pow(2, retryCount);
        return Math.min(delay, 60000);
    }
}