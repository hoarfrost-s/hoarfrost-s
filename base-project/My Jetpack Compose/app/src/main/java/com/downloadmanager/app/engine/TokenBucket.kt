package com.downloadmanager.app.engine

import kotlinx.coroutines.delay
import kotlin.math.min

class TokenBucket(bytesPerSecond: Long) {

    @Volatile
    private var capacity: Long = if (bytesPerSecond > 0) bytesPerSecond * 2 else 0

    @Volatile
    private var tokens: Long = capacity

    @Volatile
    private var fillRate: Long = bytesPerSecond

    @Volatile
    private var lastRefillTime: Long = System.nanoTime()

    private val lock = Any()

    init {
        require(bytesPerSecond >= 0) { "Rate must be non-negative" }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsedNanos = now - lastRefillTime
        if (elapsedNanos <= 0 || fillRate == 0L) {
            lastRefillTime = now
            return
        }
        val tokensToAdd = (elapsedNanos * fillRate) / 1_000_000_000L
        if (tokensToAdd > 0) {
            tokens = min(capacity, tokens + tokensToAdd)
            lastRefillTime = now
        }
    }

    @Synchronized
    fun tryConsume(bytes: Long): Boolean {
        if (fillRate == 0L) return true
        refill()
        return if (tokens >= bytes) {
            tokens -= bytes
            true
        } else {
            false
        }
    }

    suspend fun consumeBlocking(bytes: Long) {
        if (fillRate == 0L) return
        while (true) {
            if (tryConsume(bytes)) {
                return
            }
            val needed = bytes - tokens
            val waitNanos = if (fillRate > 0) {
                (needed * 1_000_000_000L) / fillRate
            } else {
                1_000_000L
            }
            val waitMs = (waitNanos / 1_000_000L).coerceAtLeast(1L)
            delay(waitMs)
        }
    }

    @Synchronized
    fun setRate(bytesPerSecond: Long) {
        require(bytesPerSecond >= 0) { "Rate must be non-negative" }
        refill()
        fillRate = bytesPerSecond
        capacity = if (bytesPerSecond > 0) bytesPerSecond * 2 else 0
        tokens = tokens.coerceAtMost(capacity)
    }

    @Synchronized
    fun getRate(): Long = fillRate

    @Synchronized
    fun availableTokens(): Long {
        refill()
        return tokens
    }
}
