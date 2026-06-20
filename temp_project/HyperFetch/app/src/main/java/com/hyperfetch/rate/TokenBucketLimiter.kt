package com.hyperfetch.rate

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

/**
 * 令牌桶限速器
 * 使用令牌桶算法实现精确的速度限制
 */
class TokenBucketLimiter {

    companion object {
        private const val MIN_RATE = 16 * 1024L // 最小速率 16 KB/s
    }

    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    @Volatile
    private var rate: Long = Long.MAX_VALUE // 字节/秒，Long.MAX_VALUE 表示无限制

    private var tokens: Long = 0L
    private var lastRefillNanos: Long = System.nanoTime()

    /**
     * 获取指定字节数的令牌
     * 如果速率无限制则立即返回
     * @param bytes 需要获取的字节数
     * @throws InterruptedException 如果等待时被中断
     */
    @Throws(InterruptedException::class)
    fun acquire(bytes: Int) {
        if (rate == Long.MAX_VALUE) return

        lock.lock()
        try {
            while (tokens < bytes) {
                refill()
                if (tokens < bytes) {
                    val waitMs = max((bytes - tokens) * 1000 / rate, 1)
                    condition.await(waitMs.coerceAtMost(1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            tokens -= bytes
        } finally {
            lock.unlock()
        }
    }

    /**
     * 尝试获取令牌，不阻塞
     * @param bytes 需要获取的字节数
     * @return true 如果获取成功，false 如果令牌不足
     */
    fun tryAcquire(bytes: Int): Boolean {
        if (rate == Long.MAX_VALUE) return true

        lock.lock()
        try {
            refill()
            if (tokens >= bytes) {
                tokens -= bytes
                return true
            }
            return false
        } finally {
            lock.unlock()
        }
    }

    /**
     * 设置限速值
     * @param bytesPerSecond 限速值（字节/秒），Long.MAX_VALUE 表示无限制
     */
    fun setRate(bytesPerSecond: Long) {
        lock.lock()
        try {
            this.rate = if (bytesPerSecond != Long.MAX_VALUE && bytesPerSecond < MIN_RATE) {
                MIN_RATE
            } else {
                bytesPerSecond
            }
            condition.signalAll()
        } finally {
            lock.unlock()
        }
    }

    /**
     * 获取当前限速值
     */
    fun getRate(): Long = rate

    /**
     * 是否启用限速
     */
    fun isLimited(): Boolean = rate != Long.MAX_VALUE

    /**
     * 补充令牌
     */
    private fun refill() {
        val now = System.nanoTime()
        val elapsed = now - lastRefillNanos
        if (elapsed > 0) {
            val add = elapsed * rate / 1_000_000_000L
            if (add > 0) {
                tokens = minOf(rate, tokens + add)
                lastRefillNanos = now
            }
        }
    }

    /**
     * 重置限速器
     */
    fun reset() {
        lock.lock()
        try {
            rate = Long.MAX_VALUE
            tokens = 0L
            lastRefillNanos = System.nanoTime()
        } finally {
            lock.unlock()
        }
    }
}
