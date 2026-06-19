package com.downloadmanager.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class SemaphoreSlotManager(maxSlots: Int = 3) : SlotManager {
    private val semaphore = Semaphore(maxSlots)
    @Volatile private var maxSlots: Int = maxSlots

    override suspend fun tryAcquire(taskId: String): Boolean {
        return withContext(Dispatchers.IO) {
            semaphore.tryAcquire()
        }
    }

    override fun release(taskId: String) {
        semaphore.release()
    }

    override fun availableSlots(): Int = semaphore.availablePermits

    override fun resize(newMaxSlots: Int) {
        val diff = newMaxSlots - maxSlots
        if (diff > 0) {
            repeat(diff) { semaphore.release() }
        } else if (diff < 0) {
            repeat(-diff) {
                kotlinx.coroutines.runBlocking { semaphore.acquire() }
            }
        }
        maxSlots = newMaxSlots
    }
}