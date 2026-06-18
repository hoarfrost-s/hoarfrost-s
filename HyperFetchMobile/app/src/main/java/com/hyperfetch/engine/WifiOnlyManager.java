package com.hyperfetch.engine;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 仅 Wi-Fi 下载管理器
 * 负责管理仅 Wi-Fi 下载模式，在移动网络下自动暂停所有任务，Wi-Fi 恢复后自动恢复任务
 */
public class WifiOnlyManager {

    private static final String TAG = "WifiOnlyManager";

    /**
     * SharedPreferences 文件名
     */
    private static final String PREFS_NAME = "wifi_only_settings";

    /**
     * 仅 Wi-Fi 模式开关键
     */
    private static final String KEY_WIFI_ONLY_MODE = "wifi_only_mode";

    /**
     * 暂停的任务 ID 集合键
     */
    private static final String KEY_PAUSED_TASK_IDS = "paused_task_ids";

    /**
     * Wi-Fi 模式回调接口
     */
    public interface WifiOnlyCallback {
        /**
         * 仅 Wi-Fi 模式已启用回调
         */
        void onWifiOnlyModeEnabled();

        /**
         * 仅 Wi-Fi 模式已禁用回调
         */
        void onWifiOnlyModeDisabled();

        /**
         * 任务因移动网络被暂停回调
         *
         * @param taskId 任务 ID
         */
        void onTaskPausedByMobileNetwork(String taskId);

        /**
         * 任务因 Wi-Fi 恢复被恢复回调
         *
         * @param taskId 任务 ID
         */
        void onTaskResumedByWifi(String taskId);

        /**
         * 所有任务因移动网络被暂停回调
         */
        void onAllTasksPausedByMobileNetwork();

        /**
         * 所有任务因 Wi-Fi 恢复被恢复回调
         */
        void onAllTasksResumedByWifi();
    }

    /**
     * 应用上下文
     */
    private final Context context;

    /**
     * SharedPreferences
     */
    private final SharedPreferences sharedPreferences;

    /**
     * 网络状态监控器
     */
    private final NetworkStateMonitor networkStateMonitor;

    /**
     * 回调列表
     */
    private final CopyOnWriteArrayList<WifiOnlyCallback> callbacks;

    /**
     * 因移动网络暂停的任务 ID 集合
     */
    private final Set<String> pausedTaskIds;

    /**
     * 仅 Wi-Fi 模式是否启用
     */
    private volatile boolean wifiOnlyModeEnabled;

    /**
     * 下载引擎引用
     */
    private DownloadEngine downloadEngine;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public WifiOnlyManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.networkStateMonitor = new NetworkStateMonitor(context);
        this.callbacks = new CopyOnWriteArrayList<>();
        this.pausedTaskIds = new HashSet<>();

        // 从 SharedPreferences 加载设置
        loadSettings();

        // 注册网络状态监听
        registerNetworkListener();
    }

    /**
     * 设置下载引擎引用
     *
     * @param engine 下载引擎
     */
    public void setDownloadEngine(DownloadEngine engine) {
        this.downloadEngine = engine;
    }

    /**
     * 从 SharedPreferences 加载设置
     */
    private void loadSettings() {
        wifiOnlyModeEnabled = sharedPreferences.getBoolean(KEY_WIFI_ONLY_MODE, false);

        // 加载暂停的任务 ID 集合
        Set<String> savedTaskIds = sharedPreferences.getStringSet(KEY_PAUSED_TASK_IDS, null);
        if (savedTaskIds != null) {
            pausedTaskIds.addAll(savedTaskIds);
        }
    }

    /**
     * 保存设置到 SharedPreferences
     */
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_WIFI_ONLY_MODE, wifiOnlyModeEnabled);
        editor.putStringSet(KEY_PAUSED_TASK_IDS, pausedTaskIds);
        editor.apply();
    }

    /**
     * 注册网络状态监听
     */
    private void registerNetworkListener() {
        networkStateMonitor.addCallback(new NetworkStateMonitor.NetworkStateCallback() {
            @Override
            public void onNetworkConnected(NetworkStateMonitor.NetworkType networkType) {
                // 网络连接时的处理
            }

            @Override
            public void onNetworkDisconnected() {
                // 网络断开时的处理
                if (wifiOnlyModeEnabled) {
                    pauseAllTasks();
                }
            }

            @Override
            public void onWifiConnected() {
                // Wi-Fi 连接时恢复任务
                if (wifiOnlyModeEnabled) {
                    resumeAllTasks();
                }
            }

            @Override
            public void onWifiDisconnected() {
                // Wi-Fi 断开时暂停任务
                if (wifiOnlyModeEnabled) {
                    pauseAllTasks();
                }
            }

            @Override
            public void onMobileNetworkConnected() {
                // 切换到移动网络时暂停任务
                if (wifiOnlyModeEnabled) {
                    pauseAllTasks();
                }
            }
        });

        networkStateMonitor.register();

        // 检查当前网络状态
        if (wifiOnlyModeEnabled && !networkStateMonitor.isWifiConnected()) {
            pauseAllTasks();
        }
    }

    /**
     * 启用仅 Wi-Fi 下载模式
     */
    public void enableWifiOnlyMode() {
        if (wifiOnlyModeEnabled) {
            return;
        }

        wifiOnlyModeEnabled = true;
        saveSettings();

        // 通知回调
        notifyWifiOnlyModeEnabled();

        // 如果当前不是 Wi-Fi 网络，暂停所有任务
        if (!networkStateMonitor.isWifiConnected()) {
            pauseAllTasks();
        }
    }

    /**
     * 禁用仅 Wi-Fi 下载模式
     */
    public void disableWifiOnlyMode() {
        if (!wifiOnlyModeEnabled) {
            return;
        }

        wifiOnlyModeEnabled = false;
        saveSettings();

        // 通知回调
        notifyWifiOnlyModeDisabled();

        // 恢复所有因移动网络暂停的任务
        resumeAllTasks();
    }

    /**
     * 设置仅 Wi-Fi 下载模式
     *
     * @param enabled 是否启用
     */
    public void setWifiOnlyMode(boolean enabled) {
        if (enabled) {
            enableWifiOnlyMode();
        } else {
            disableWifiOnlyMode();
        }
    }

    /**
     * 检查仅 Wi-Fi 模式是否启用
     *
     * @return 是否启用
     */
    public boolean isWifiOnlyModeEnabled() {
        return wifiOnlyModeEnabled;
    }

    /**
     * 检查当前是否允许下载
     * 在仅 Wi-Fi 模式下，只有 Wi-Fi 连接时才允许下载
     *
     * @return 是否允许下载
     */
    public boolean isDownloadAllowed() {
        if (!wifiOnlyModeEnabled) {
            return true;
        }
        return networkStateMonitor.isWifiConnected();
    }

    /**
     * 暂停所有任务
     * 在移动网络下自动暂停所有任务
     */
    public void pauseAllTasks() {
        if (downloadEngine != null) {
            // 这里需要通过 DownloadEngine 获取所有正在下载的任务
            // 实际实现需要根据 DownloadEngine 的接口进行调整
            notifyAllTasksPausedByMobileNetwork();
        }
    }

    /**
     * 恢复所有任务
     * Wi-Fi 恢复后自动恢复任务
     */
    public void resumeAllTasks() {
        if (downloadEngine != null) {
            // 恢复所有因移动网络暂停的任务
            notifyAllTasksResumedByWifi();
        }
        // 清空暂停的任务 ID 集合
        pausedTaskIds.clear();
        saveSettings();
    }

    /**
     * 暂停指定任务
     *
     * @param taskId 任务 ID
     */
    public void pauseTask(String taskId) {
        if (downloadEngine != null) {
            downloadEngine.pause(taskId);
        }
        pausedTaskIds.add(taskId);
        saveSettings();

        // 通知回调
        notifyTaskPausedByMobileNetwork(taskId);
    }

    /**
     * 恢复指定任务
     *
     * @param taskId 任务 ID
     */
    public void resumeTask(String taskId) {
        if (downloadEngine != null) {
            downloadEngine.resume(taskId);
        }
        pausedTaskIds.remove(taskId);
        saveSettings();

        // 通知回调
        notifyTaskResumedByWifi(taskId);
    }

    /**
     * 添加因移动网络暂停的任务
     *
     * @param taskId 任务 ID
     */
    public void addPausedTask(String taskId) {
        pausedTaskIds.add(taskId);
        saveSettings();
    }

    /**
     * 移除因移动网络暂停的任务
     *
     * @param taskId 任务 ID
     */
    public void removePausedTask(String taskId) {
        pausedTaskIds.remove(taskId);
        saveSettings();
    }

    /**
     * 获取因移动网络暂停的任务 ID 集合
     *
     * @return 任务 ID 集合
     */
    public Set<String> getPausedTaskIds() {
        return new HashSet<>(pausedTaskIds);
    }

    /**
     * 检查任务是否因移动网络暂停
     *
     * @param taskId 任务 ID
     * @return 是否因移动网络暂停
     */
    public boolean isTaskPausedByMobileNetwork(String taskId) {
        return pausedTaskIds.contains(taskId);
    }

    /**
     * 添加回调
     *
     * @param callback 回调接口
     */
    public void addCallback(WifiOnlyCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    /**
     * 移除回调
     *
     * @param callback 回调接口
     */
    public void removeCallback(WifiOnlyCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * 清除所有回调
     */
    public void clearCallbacks() {
        callbacks.clear();
    }

    /**
     * 通知仅 Wi-Fi 模式已启用
     */
    private void notifyWifiOnlyModeEnabled() {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onWifiOnlyModeEnabled();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知仅 Wi-Fi 模式已禁用
     */
    private void notifyWifiOnlyModeDisabled() {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onWifiOnlyModeDisabled();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知任务因移动网络被暂停
     *
     * @param taskId 任务 ID
     */
    private void notifyTaskPausedByMobileNetwork(String taskId) {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onTaskPausedByMobileNetwork(taskId);
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知任务因 Wi-Fi 恢复被恢复
     *
     * @param taskId 任务 ID
     */
    private void notifyTaskResumedByWifi(String taskId) {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onTaskResumedByWifi(taskId);
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知所有任务因移动网络被暂停
     */
    private void notifyAllTasksPausedByMobileNetwork() {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onAllTasksPausedByMobileNetwork();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 通知所有任务因 Wi-Fi 恢复被恢复
     */
    private void notifyAllTasksResumedByWifi() {
        for (WifiOnlyCallback callback : callbacks) {
            try {
                callback.onAllTasksResumedByWifi();
            } catch (Exception e) {
                // 忽略回调异常
            }
        }
    }

    /**
     * 获取网络状态监控器
     *
     * @return 网络状态监控器
     */
    public NetworkStateMonitor getNetworkStateMonitor() {
        return networkStateMonitor;
    }

    /**
     * 检查是否为 Wi-Fi 网络
     *
     * @return 是否为 Wi-Fi 网络
     */
    public boolean isWifiConnected() {
        return networkStateMonitor.isWifiConnected();
    }

    /**
     * 检查是否为移动网络
     *
     * @return 是否为移动网络
     */
    public boolean isMobileNetwork() {
        return networkStateMonitor.isMobileNetwork();
    }

    /**
     * 销毁管理器
     */
    public void destroy() {
        networkStateMonitor.unregister();
        networkStateMonitor.clearCallbacks();
        callbacks.clear();
        pausedTaskIds.clear();
    }
}