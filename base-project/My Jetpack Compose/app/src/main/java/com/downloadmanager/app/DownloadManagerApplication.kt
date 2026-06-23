package com.downloadmanager.app

import android.app.Application
import com.downloadmanager.app.engine.DownloadEngine
import com.downloadmanager.app.repository.CategoryRepository
import com.downloadmanager.app.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DownloadManagerApplication : Application() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var downloadEngine: DownloadEngine

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initialize()
    }

    private fun initialize() {
        applicationScope.launch {
            try {
                categoryRepository.initDefaultCategories()

                val settings = settingsRepository.settingsFlow.first()

                downloadEngine.setMaxConcurrentTasks(settings.maxConcurrent)
                downloadEngine.setGlobalSpeedLimit(settings.globalSpeedLimit)
            } catch (e: Exception) {
            }
        }
    }
}
