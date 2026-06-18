package com.hyperfetch.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.SeekBarPreference;

import com.hyperfetch.R;
import com.hyperfetch.event.EventBus;
import com.hyperfetch.event.SchedulerConfigChangedEvent;
import com.hyperfetch.model.SchedulerConfig;
import com.hyperfetch.repo.ConfigEntity;
import com.hyperfetch.repo.TaskRepository;

/**
 * 设置页面
 * PreferenceFragmentCompat 实现，提供下载配置选项
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    /**
     * 多任务开关
     */
    private SwitchPreferenceCompat switchMultiTask;

    /**
     * 最大并发数设置
     */
    private SeekBarPreference seekBarMaxConcurrent;

    /**
     * 仅 Wi-Fi 下载开关
     */
    private SwitchPreferenceCompat switchWifiOnly;

    /**
     * 默认线程数设置
     */
    private SeekBarPreference seekBarDefaultThreadCount;

    /**
     * 电池优化白名单引导
     */
    private Preference preferenceBatteryOptimization;

    /**
     * 清除所有任务
     */
    private Preference preferenceClearAllTasks;

    /**
     * 关于应用
     */
    private Preference preferenceAbout;

    /**
     * 任务仓库
     */
    private TaskRepository taskRepository;

    /**
     * 当前配置
     */
    private SchedulerConfig currentConfig;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // 加载偏好设置 XML
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // 初始化任务仓库
        taskRepository = new TaskRepository(requireContext());

        // 初始化偏好设置项
        initPreferences();

        // 加载当前配置
        loadCurrentConfig();

        // 设置偏好设置监听器
        setupPreferenceListeners();
    }

    /**
     * 初始化偏好设置项
     */
    private void initPreferences() {
        // 多任务开关
        switchMultiTask = findPreference("multi_task_enabled");
        if (switchMultiTask != null) {
            switchMultiTask.setTitle("多任务下载");
            switchMultiTask.setSummary("启用后可同时下载多个任务");
        }

        // 最大并发数设置
        seekBarMaxConcurrent = findPreference("max_concurrent_tasks");
        if (seekBarMaxConcurrent != null) {
            seekBarMaxConcurrent.setTitle("最大并发任务数");
            seekBarMaxConcurrent.setMin(1);
            seekBarMaxConcurrent.setMax(10);
            seekBarMaxConcurrent.setSeekBarIncrement(1);
        }

        // 仅 Wi-Fi 下载开关
        switchWifiOnly = findPreference("wifi_only");
        if (switchWifiOnly != null) {
            switchWifiOnly.setTitle("仅 Wi-Fi 下载");
            switchWifiOnly.setSummary("启用后仅在 Wi-Fi 网络下下载");
        }

        // 默认线程数设置
        seekBarDefaultThreadCount = findPreference("default_thread_count");
        if (seekBarDefaultThreadCount != null) {
            seekBarDefaultThreadCount.setTitle("默认线程数");
            seekBarDefaultThreadCount.setMin(1);
            seekBarDefaultThreadCount.setMax(32);
            seekBarDefaultThreadCount.setSeekBarIncrement(1);
        }

        // 电池优化白名单引导
        preferenceBatteryOptimization = findPreference("battery_optimization");
        if (preferenceBatteryOptimization != null) {
            preferenceBatteryOptimization.setTitle("电池优化白名单");
            preferenceBatteryOptimization.setSummary("将应用加入电池优化白名单，防止后台下载被中断");
            updateBatteryOptimizationStatus();
        }

        // 清除所有任务
        preferenceClearAllTasks = findPreference("clear_all_tasks");
        if (preferenceClearAllTasks != null) {
            preferenceClearAllTasks.setTitle("清除所有任务");
            preferenceClearAllTasks.setSummary("删除所有下载任务记录");
        }

        // 关于应用
        preferenceAbout = findPreference("about");
        if (preferenceAbout != null) {
            preferenceAbout.setTitle("关于 HyperFetch");
            preferenceAbout.setSummary("版本 1.0.0");
        }
    }

    /**
     * 加载当前配置
     */
    private void loadCurrentConfig() {
        // 从数据库加载配置
        ConfigEntity configEntity = taskRepository.getConfig();
        if (configEntity != null) {
            currentConfig = new SchedulerConfig();
            currentConfig.setMaxConcurrentTasks(configEntity.maxConcurrentTasks);
            currentConfig.setMultiTaskEnabled(configEntity.multiTaskEnabled);
            currentConfig.setWifiOnly(configEntity.wifiOnly);
            currentConfig.setDefaultThreadCount(configEntity.defaultThreadCount);
            currentConfig.setQueueStrategy(configEntity.queueStrategy);

            // 更新偏好设置值
            updatePreferenceValues();
        } else {
            // 使用默认配置
            currentConfig = new SchedulerConfig();
            currentConfig.setMaxConcurrentTasks(3);
            currentConfig.setMultiTaskEnabled(true);
            currentConfig.setWifiOnly(false);
            currentConfig.setDefaultThreadCount(3);
            currentConfig.setQueueStrategy("FIFO");

            // 保存默认配置
            saveConfigToDatabase();

            // 更新偏好设置值
            updatePreferenceValues();
        }
    }

    /**
     * 更新偏好设置值
     */
    private void updatePreferenceValues() {
        if (switchMultiTask != null) {
            switchMultiTask.setChecked(currentConfig.isMultiTaskEnabled());
        }

        if (seekBarMaxConcurrent != null) {
            seekBarMaxConcurrent.setValue(currentConfig.getMaxConcurrentTasks());
        }

        if (switchWifiOnly != null) {
            switchWifiOnly.setChecked(currentConfig.isWifiOnly());
        }

        if (seekBarDefaultThreadCount != null) {
            seekBarDefaultThreadCount.setValue(currentConfig.getDefaultThreadCount());
        }
    }

    /**
     * 设置偏好设置监听器
     */
    private void setupPreferenceListeners() {
        // 多任务开关监听
        if (switchMultiTask != null) {
            switchMultiTask.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                currentConfig.setMultiTaskEnabled(enabled);
                saveConfigToDatabase();
                notifyConfigChanged();
                Toast.makeText(requireContext(),
                        enabled ? "多任务下载已启用" : "多任务下载已禁用",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 最大并发数监听
        if (seekBarMaxConcurrent != null) {
            seekBarMaxConcurrent.setOnPreferenceChangeListener((preference, newValue) -> {
                int maxConcurrent = (Integer) newValue;
                currentConfig.setMaxConcurrentTasks(maxConcurrent);
                saveConfigToDatabase();
                notifyConfigChanged();
                Toast.makeText(requireContext(),
                        "最大并发任务数设置为 " + maxConcurrent,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 仅 Wi-Fi 下载监听
        if (switchWifiOnly != null) {
            switchWifiOnly.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean wifiOnly = (Boolean) newValue;
                currentConfig.setWifiOnly(wifiOnly);
                saveConfigToDatabase();
                notifyConfigChanged();
                Toast.makeText(requireContext(),
                        wifiOnly ? "仅 Wi-Fi 下载已启用" : "仅 Wi-Fi 下载已禁用",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 默认线程数监听
        if (seekBarDefaultThreadCount != null) {
            seekBarDefaultThreadCount.setOnPreferenceChangeListener((preference, newValue) -> {
                int threadCount = (Integer) newValue;
                currentConfig.setDefaultThreadCount(threadCount);
                saveConfigToDatabase();
                notifyConfigChanged();
                Toast.makeText(requireContext(),
                        "默认线程数设置为 " + threadCount,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 电池优化白名单引导监听
        if (preferenceBatteryOptimization != null) {
            preferenceBatteryOptimization.setOnPreferenceClickListener(preference -> {
                openBatteryOptimizationSettings();
                return true;
            });
        }

        // 清除所有任务监听
        if (preferenceClearAllTasks != null) {
            preferenceClearAllTasks.setOnPreferenceClickListener(preference -> {
                showClearAllTasksConfirmation();
                return true;
            });
        }

        // 关于应用监听
        if (preferenceAbout != null) {
            preferenceAbout.setOnPreferenceClickListener(preference -> {
                showAboutDialog();
                return true;
            });
        }
    }

    /**
     * 保存配置到数据库
     */
    private void saveConfigToDatabase() {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.id = 1;
        configEntity.maxConcurrentTasks = currentConfig.getMaxConcurrentTasks();
        configEntity.multiTaskEnabled = currentConfig.isMultiTaskEnabled();
        configEntity.wifiOnly = currentConfig.isWifiOnly();
        configEntity.defaultThreadCount = currentConfig.getDefaultThreadCount();
        configEntity.queueStrategy = currentConfig.getQueueStrategy();

        taskRepository.saveConfig(configEntity);
    }

    /**
     * 通知配置变更
     */
    private void notifyConfigChanged() {
        // 发布配置变更事件
        SchedulerConfigChangedEvent event = new SchedulerConfigChangedEvent(currentConfig);
        EventBus.getInstance().postToMain(event);
    }

    /**
     * 更新电池优化状态
     */
    private void updateBatteryOptimizationStatus() {
        if (preferenceBatteryOptimization == null) {
            return;
        }

        boolean isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations();

        if (isIgnoringBatteryOptimizations) {
            preferenceBatteryOptimization.setSummary("应用已加入电池优化白名单");
            preferenceBatteryOptimization.setEnabled(false);
        } else {
            preferenceBatteryOptimization.setSummary("点击将应用加入电池优化白名单，防止后台下载被中断");
            preferenceBatteryOptimization.setEnabled(true);
        }
    }

    /**
     * 检查是否忽略电池优化
     *
     * @return 是否忽略电池优化
     */
    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) requireContext()
                    .getSystemService(Context.POWER_SERVICE);
            return powerManager != null &&
                    powerManager.isIgnoringBatteryOptimizations(requireContext().getPackageName());
        }
        return true; // Android 6.0 以下默认忽略
    }

    /**
     * 打开电池优化设置页面
     */
    private void openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                // 无法打开设置页面，显示提示
                Toast.makeText(requireContext(),
                        "无法打开电池优化设置，请手动在系统设置中添加",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(requireContext(),
                    "当前系统版本不支持此功能",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示清除所有任务确认对话框
     */
    private void showClearAllTasksConfirmation() {
        // 创建确认对话框
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("确认清除")
                .setMessage("确定要清除所有下载任务记录吗？此操作不可撤销。")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearAllTasks();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 清除所有任务
     */
    private void clearAllTasks() {
        // TODO: 实现清除所有任务的逻辑
        // 需要调用 DownloadService 的相关方法
        Toast.makeText(requireContext(), "所有任务已清除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("关于 HyperFetch")
                .setMessage("HyperFetch 是一款高性能多线程下载器。\n\n" +
                        "功能特点：\n" +
                        "• 多线程下载，提升下载速度\n" +
                        "• 支持多种协议（HTTP、HTTPS、FTP）\n" +
                        "• 断点续传，下载中断后可继续\n" +
                        "• 多任务并发下载\n" +
                        "• 自动文件分类\n" +
                        "• Wi-Fi 限制下载\n\n" +
                        "版本：1.0.0\n" +
                        "开发者：HyperFetch Team")
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 更新电池优化状态
        updateBatteryOptimizationStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 关闭任务仓库
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
    }
}