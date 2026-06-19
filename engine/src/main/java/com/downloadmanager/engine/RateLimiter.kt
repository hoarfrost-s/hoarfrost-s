package com.downloadmanager.engine

interface RateLimiter {
    suspend fun acquire(bytes: Long)
    fun setRate(bytesPerSecond: Long)
}