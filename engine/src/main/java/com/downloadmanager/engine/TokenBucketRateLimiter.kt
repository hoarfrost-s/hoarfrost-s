package com.downloadmanager.engine

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class TokenBucketRateLimiter(rate: Long = 0) : RateLimiter {
    private val tokens = AtomicLong(if (rate > 0) rate * 2 else Long.MAX_VALUE)
    @Volatile private var rate: Long = rate
    @Volatile private var maxTokens: Long = if (rate > 0) rate * 2 else Long.MAX_VALUE
    private var lastRefillTime = System.nanoTime()
    private val lock = Any()

    override suspend fun acquire(bytes: Long) {
        if (rate == 0L) return // No limit
        while (true) {
            val waitMs = synchronized(lock) {
                refill()
                if (tokens.get() >= bytes) {
                    tokens.addAndGet(-bytes)
                    return
                }
                (bytes - tokens.get()) * 1000 / rate + 1
            }
            delay(minOf(waitMs, 200))
        }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsed = (now - lastRefillTime) / 1_000_000
        val newTokens = elapsed * rate / 1000
        if (newTokens > 0) {
            tokens.set(minOf(tokens.get() + newTokens, maxTokens))
            lastRefillTime = now
        }
    }

    override fun setRate(bytesPerSecond: Long) {
        rate = bytesPerSecond
        maxTokens = if (bytesPerSecond > 0) bytesPerSecond * 2 else Long.MAX_VALUE
        if (bytesPerSecond == 0L) {
            tokens.set(Long.MAX_VALUE)
        }
    }
}