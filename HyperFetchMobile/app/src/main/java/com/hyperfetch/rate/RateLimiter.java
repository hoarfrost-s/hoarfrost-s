package com.hyperfetch.rate;

/**
 * 限速器接口
 * 定义下载速率控制的基本操作
 */
public interface RateLimiter {
    
    /**
     * 申领令牌
     * 阻塞直到获取足够的令牌，用于控制下载速率
     * 
     * @param bytes 需要申领的字节数
     * @throws InterruptedException 当线程被中断时抛出
     */
    void acquire(int bytes) throws InterruptedException;
    
    /**
     * 设置速率
     * 
     * @param bytesPerSecond 每秒字节数
     */
    void setRate(long bytesPerSecond);
    
    /**
     * 获取当前速率
     * 
     * @return 当前速率（字节/秒）
     */
    long getRate();
}