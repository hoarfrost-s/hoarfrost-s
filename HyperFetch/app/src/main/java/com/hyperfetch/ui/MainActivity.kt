package com.hyperfetch.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hyperfetch.event.EventBusWrapper
import com.hyperfetch.event.ProgressEvent
import com.hyperfetch.model.DownloadOptions
import com.hyperfetch.notification.NotificationHelper
import com.hyperfetch.service.DownloadService
import com.hyperfetch.service.TaskService
import com.hyperfetch.ui.screens.MainScreen
import com.hyperfetch.ui.theme.HyperFetchTheme
import com.hyperfetch.ui.viewmodel.MainViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 主 Activity
 */
class MainActivity : ComponentActivity() {

    private var downloadService: DownloadService? = null
    private var serviceBound = false

    private lateinit var viewModel: MainViewModel
    private lateinit var notificationHelper: NotificationHelper

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DownloadService.LocalBinder
            downloadService = binder.getService()
            serviceBound = true

            // 初始化 ViewModel
            viewModel = ViewModelProvider(
                this@MainActivity,
                MainViewModel.Factory(binder.getService().taskService)
            )[MainViewModel::class.java]
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService = null
            serviceBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限请求结果处理
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startDownloadService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationHelper = NotificationHelper(this)

        // 检查权限
        checkAndRequestPermissions()

        // 注册事件订阅
        EventBusWrapper.register(this)

        setContent {
            HyperFetchTheme {
                if (::viewModel.isInitialized) {
                    MainScreen(
                        tasks = viewModel.tasks.collectAsState().value,
                        filteredTasks = viewModel.filteredTasks.collectAsState().value,
                        categoryStats = viewModel.categoryStats.collectAsState().value,
                        selectedCategory = viewModel.selectedCategory.collectAsState().value,
                        totalSpeed = viewModel.totalSpeed.collectAsState().value,
                        downloadingCount = viewModel.downloadingCount.collectAsState().value,
                        onSelectCategory = viewModel::selectCategory,
                        onCreateTask = viewModel::createTask,
                        onPauseTask = viewModel::pauseTask,
                        onResumeTask = viewModel::resumeTask,
                        onDeleteTask = viewModel::deleteTask,
                        onRetryTask = viewModel::retryTask,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 处理分享意图
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBusWrapper.unregister(this)
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用媒体权限
            permissions.addAll(
                listOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 需要 MANAGE_EXTERNAL_STORAGE
            if (!android.os.Environment.isExternalStorageManager()) {
                permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        } else {
            // Android 10 及以下
            permissions.addAll(
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startDownloadService()
        }
    }

    private fun startDownloadService() {
        val intent = Intent(this, DownloadService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    // 提取 URL
                    val url = extractUrl(sharedText)
                    if (url != null) {
                        // 自动打开新建任务页面并填充 URL
                        if (::viewModel.isInitialized) {
                            viewModel.createTask(url, DownloadOptions())
                        }
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex(
            """(https?://[^\s<>"{}|\\^`\[\]]+)"""
        )
        return urlPattern.find(text)?.value
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProgressEvent(event: ProgressEvent) {
        // 更新通知栏进度
        val task = viewModel.tasks.value.find { it.id == event.taskId }
        if (task != null) {
            notificationHelper.showProgressNotification(
                taskId = event.taskId,
                fileName = task.fileName,
                progress = event.progressPercent,
                speed = formatSpeed(event.speed),
                eta = formatEta(event.eta)
            )
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "0 B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var size = bytesPerSecond.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return "%.1f %s".format(size, units[unitIndex])
    }

    private fun formatEta(seconds: Long): String {
        if (seconds <= 0) return ""
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "剩余 ${hours}h ${minutes}m"
            minutes > 0 -> "剩余 ${minutes}m"
            else -> "剩余 ${seconds}s"
        }
    }
}
