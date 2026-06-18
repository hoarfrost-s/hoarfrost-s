package com.hyperfetch.event;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 事件总线类
 * 用于组件间通信，支持主线程和子线程事件派发
 */
public class EventBus {

    /**
     * 订阅者映射表
     * 键：事件类型
     * 值：订阅该事件类型的消费者列表
     */
    private final Map<Class<?>, List<Consumer<Object>>> subscribers;

    /**
     * 主线程Handler
     * 用于在主线程派发UI相关事件
     */
    private final Handler mainHandler;

    /**
     * IO线程执行器
     * 用于在子线程派发通知更新等事件
     */
    private final Executor ioExecutor;

    /**
     * 单例实例
     */
    private static volatile EventBus instance;

    /**
     * 私有构造函数
     */
    private EventBus() {
        this.subscribers = new ConcurrentHashMap<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.ioExecutor = Executors.newCachedThreadPool();
    }

    /**
     * 获取单例实例
     *
     * @return EventBus实例
     */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    /**
     * 注册事件订阅者
     *
     * @param eventType  事件类型
     * @param subscriber 订阅者（消费者）
     * @param <T>        事件类型泛型
     */
    public <T> void register(Class<T> eventType, Consumer<Object> subscriber) {
        if (eventType == null || subscriber == null) {
            return;
        }
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    /**
     * 取消注册事件订阅者
     *
     * @param eventType  事件类型
     * @param subscriber 订阅者（消费者）
     * @param <T>        事件类型泛型
     */
    public <T> void unregister(Class<T> eventType, Consumer<Object> subscriber) {
        if (eventType == null || subscriber == null) {
            return;
        }
        List<Consumer<Object>> subscriberList = subscribers.get(eventType);
        if (subscriberList != null) {
            subscriberList.remove(subscriber);
            // 如果列表为空，移除该事件类型
            if (subscriberList.isEmpty()) {
                subscribers.remove(eventType);
            }
        }
    }

    /**
     * 发布事件（在当前线程执行）
     *
     * @param event 事件对象
     */
    public void post(Object event) {
        if (event == null) {
            return;
        }
        dispatchEvent(event.getClass(), event);
    }

    /**
     * 发布事件到主线程
     * 用于UI更新相关事件
     *
     * @param event 事件对象
     */
    public void postToMain(Object event) {
        if (event == null) {
            return;
        }
        mainHandler.post(() -> dispatchEvent(event.getClass(), event));
    }

    /**
     * 发布事件到IO线程
     * 用于通知更新等耗时操作
     *
     * @param event 事件对象
     */
    public void postToIO(Object event) {
        if (event == null) {
            return;
        }
        ioExecutor.execute(() -> dispatchEvent(event.getClass(), event));
    }

    /**
     * 派发事件给订阅者
     *
     * @param eventType 事件类型
     * @param event     事件对象
     */
    private void dispatchEvent(Class<?> eventType, Object event) {
        List<Consumer<Object>> subscriberList = subscribers.get(eventType);
        if (subscriberList == null || subscriberList.isEmpty()) {
            return;
        }
        for (Consumer<Object> subscriber : subscriberList) {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                // 捕获异常，避免影响其他订阅者
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除所有订阅者
     */
    public void clear() {
        subscribers.clear();
    }

    /**
     * 检查是否有指定事件类型的订阅者
     *
     * @param eventType 事件类型
     * @return 是否有订阅者
     */
    public boolean hasSubscribers(Class<?> eventType) {
        List<Consumer<Object>> subscriberList = subscribers.get(eventType);
        return subscriberList != null && !subscriberList.isEmpty();
    }

    /**
     * 获取指定事件类型的订阅者数量
     *
     * @param eventType 事件类型
     * @return 订阅者数量
     */
    public int getSubscriberCount(Class<?> eventType) {
        List<Consumer<Object>> subscriberList = subscribers.get(eventType);
        return subscriberList != null ? subscriberList.size() : 0;
    }
}