package com.downloadmanager.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    val maxConcurrency: Flow<Int>
    val threadCount: Flow<Int>
    val speedLimit: Flow<Long>
    val defaultSavePath: Flow<String>
    val cellularAutoLimit: Flow<Boolean>
    val maxRetries: Flow<Int>
    val themeMode: Flow<Int>  // 0=System, 1=Light, 2=Dark

    suspend fun setMaxConcurrency(value: Int)
    suspend fun setThreadCount(value: Int)
    suspend fun setSpeedLimit(bytesPerSecond: Long)
    suspend fun setDefaultSavePath(path: String)
    suspend fun setCellularAutoLimit(enabled: Boolean)
    suspend fun setMaxRetries(value: Int)
    suspend fun setThemeMode(mode: Int)
}