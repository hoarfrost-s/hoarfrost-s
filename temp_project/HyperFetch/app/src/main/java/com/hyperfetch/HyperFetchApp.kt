package com.hyperfetch

import android.app.Application
import com.hyperfetch.notification.NotificationHelper
import com.hyperfetch.protocol.ProtocolFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 应用入口
 */
class HyperFetchApp : Application() {

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()

        // 初始化 OkHttp
        initOkHttp()

        // 初始化通知助手
        notificationHelper = NotificationHelper(this)
    }

    private fun initOkHttp() {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        ProtocolFactory.initialize(client)
    }
}
