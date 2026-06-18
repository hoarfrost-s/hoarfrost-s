package com.hyperfetch.rate;

/**
 * 全局限速器
 * 单例模式实现，所有下载任务共享同一个限速器
 */
public class GlobalRateLimiter {
    
    /** 单例实例 */
    private static volatile GlobalRateLimiter instance;
    
    /** 底层限速器实现 */
    private final TokenBucketLimiter limiter;
    
    /**
     * 私有构造函数
     */
    private GlobalRateLimiter() {
        this.limiter = new TokenBucketLimiter();
    }
    
    /**
     * 获取单例实例
     * 使用双重检查锁定保证线程安全
     * 
     * @return 全局限速器实例
     */
    public static GlobalRateLimiter getInstance() {
        if (instance == null) {
            synchronized (GlobalRateLimiter.class) {
                if (instance == null) {
                    instance = new GlobalRateLimiter();
                }
            }
        }
        return instance;
    }
    
    /**
     * 申领令牌
     * 阻塞直到获取足够的令牌
     * 
     * @param bytes 需要申领的字节数
     * @throws InterruptedException 当线程被中断时抛出
     */
    public void acquire(int bytes) throws InterruptedException {
        limiter.acquire(bytes);
    }
    
    /**
     * 设置全局限速
     * 
     * @param bytesPerSecond 每秒字节数，Long.MAX_VALUE 表示无限制
     */
    public void setRate(long bytesPerSecond) {
        limiter.setRate(bytesPerSecond);
    }
    
    /**
     * 获取当前全局限速
     * 
     * @return 当前速率（字节/秒）
     */
    public long getRate() {
        return limiter.getRate();
    }
    
    /**
     * 重置全局限速器
     * 用于测试或重置状态
     */
    public static synchronized void reset() {
        instance = null;
    }
}