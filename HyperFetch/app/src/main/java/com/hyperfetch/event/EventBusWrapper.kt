package com.hyperfetch.event

import android.os.Handler
import android.os.Looper
import org.greenrobot.eventbus.EventBus

/**
 * EventBus 封装类
 * 提供线程安全的 EventBus 操作
 */
object EventBusWrapper {

    private val eventBus: EventBus by lazy {
        EventBus.builder()
            .logNoSubscriberMessages(false)
            .sendNoSubscriberEvent(false)
            .build()
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 注册订阅者
     */
    fun register(subscriber: Any) {
        if (!eventBus.isRegistered(subscriber)) {
            eventBus.register(subscriber)
        }
    }

    /**
     * 取消注册订阅者
     */
    fun unregister(subscriber: Any) {
        if (eventBus.isRegistered(subscriber)) {
            eventBus.unregister(subscriber)
        }
    }

    /**
     * 在主线程发布事件
     */
    fun postMain(event: Any) {
        mainHandler.post {
            eventBus.post(event)
        }
    }

    /**
     * 在后台线程发布事件
     */
    fun post(event: Any) {
        eventBus.post(event)
    }

    /**
     * 发布粘性事件
     */
    fun postSticky(event: Any) {
        eventBus.postSticky(event)
    }

    /**
     * 获取粘性事件
     */
    fun <T> getStickyEvent(eventClass: Class<T>): T? {
        return eventBus.getStickyEvent(eventClass)
    }

    /**
     * 移除粘性事件
     */
    fun <T> removeStickyEvent(eventClass: Class<T>): T? {
        return eventBus.removeStickyEvent(eventClass)
    }

    /**
     * 取消事件传递
     */
    fun cancelEventDelivery(event: Any) {
        eventBus.cancelEventDelivery(event)
    }
}
