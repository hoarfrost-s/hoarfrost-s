package com.hyperfetch.rate;

/**
 * 令牌桶限速器实现
 * 使用令牌桶算法控制下载速率
 */
public class TokenBucketLimiter implements RateLimiter {
    
    /** 最小速率：16 KB/s = 16384 字节/秒 */
    private static final long MIN_RATE = 16 * 1024;
    
    /** 当前速率（字节/秒） */
    private volatile long rate;
    
    /** 当前桶内令牌数 */
    private volatile long tokens;
    
    /** 上次填充令牌的时间（纳秒） */
    private long lastRefillNanos;
    
    /**
     * 构造函数
     * 默认无限制速率
     */
    public TokenBucketLimiter() {
        this.rate = Long.MAX_VALUE;
        this.tokens = Long.MAX_VALUE;
        this.lastRefillNanos = System.nanoTime();
    }
    
    /**
     * 构造函数
     * 
     * @param bytesPerSecond 初始速率（字节/秒）
     */
    public TokenBucketLimiter(long bytesPerSecond) {
        setRate(bytesPerSecond);
        this.tokens = this.rate;
        this.lastRefillNanos = System.nanoTime();
    }
    
    /**
     * 申领令牌
     * 阻塞直到获取足够的令牌
     * 
     * @param bytes 需要申领的字节数
     * @throws InterruptedException 当线程被中断时抛出
     */
    @Override
    public synchronized void acquire(int bytes) throws InterruptedException {
        // 无限制速率时直接返回
        if (rate == Long.MAX_VALUE) {
            return;
        }
        
        // 循环等待直到有足够的令牌
        while (tokens < bytes) {
            refill();
            if (tokens < bytes) {
                // 计算需要等待的时间（毫秒）
                long waitMs = (bytes - tokens) * 1000 / rate;
                wait(Math.max(waitMs, 1));
            }
        }
        
        // 消耗令牌
        tokens -= bytes;
    }
    
    /**
     * 设置速率
     * 
     * @param bytesPerSecond 每秒字节数
     */
    @Override
    public synchronized void setRate(long bytesPerSecond) {
        // 如果不是无限制速率且小于最小速率，则设置为最小速率
        if (bytesPerSecond != Long.MAX_VALUE && bytesPerSecond < MIN_RATE) {
            bytesPerSecond = MIN_RATE;
        }
        
        this.rate = bytesPerSecond;
        
        // 唤醒等待的线程
        notifyAll();
    }
    
    /**
     * 获取当前速率
     * 
     * @return 当前速率（字节/秒）
     */
    @Override
    public long getRate() {
        return rate;
    }
    
    /**
     * 填充令牌
     * 根据时间差计算并添加新令牌
     */
    private void refill() {
        long now = System.nanoTime();
        // 计算应该添加的令牌数：时间差 * 速率 / 10^9
        long add = (now - lastRefillNanos) * rate / 1_000_000_000L;
        
        if (add > 0) {
            // 桶容量 = rate，令牌数不能超过桶容量
            tokens = Math.min(rate, tokens + add);
            lastRefillNanos = now;
        }
    }
}