package com.downloadmanager.engine

interface SlotManager {
    suspend fun tryAcquire(taskId: String): Boolean
    fun release(taskId: String)
    fun availableSlots(): Int
    fun resize(newMaxSlots: Int)
}