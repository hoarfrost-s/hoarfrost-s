package com.hyperfetch.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * 恢复管理类
 * 负责注册和管理 WorkManager 周期性任务
 * 检查并恢复未完成的下载任务
 */
public class RecoveryManager {

    private static final String TAG = "RecoveryManager";

    /**
     * 周期性恢复任务的唯一名称
     */
    private static final String RECOVERY_WORK_NAME = "download_recovery_work";

    /**
     * 周期性检查间隔（小时）
     */
    private static final long RECOVERY_INTERVAL_HOURS = 6;

    /**
     * 单例实例
     */
    private static volatile RecoveryManager instance;

    /**
     * 应用上下文
     */
    private final Context context;

    /**
     * WorkManager 实例
     */
    private final WorkManager workManager;

    /**
     * 私有构造函数
     *
     * @param context 应用上下文
     */
    private RecoveryManager(Context context) {
        this.context = context.getApplicationContext();
        this.workManager = WorkManager.getInstance(this.context);
    }

    /**
     * 获取单例实例
     *
     * @param context 应用上下文
     * @return RecoveryManager 实例
     */
    public static RecoveryManager getInstance(Context context) {
        if (instance == null) {
            synchronized (RecoveryManager.class) {
                if (instance == null) {
                    instance = new RecoveryManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 注册周期性恢复任务
     * 定期检查未完成的下载任务并尝试恢复
     */
    public void registerPeriodicRecovery() {
        Log.d(TAG, "注册周期性恢复任务，间隔: " + RECOVERY_INTERVAL_HOURS + " 小时");

        // 构建约束条件
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(false)           // 不需要设备空闲
                .setRequiresCharging(false)             // 不需要充电
                .setRequiresBatteryNotLow(true)         // 需要电量不低
                .build();

        // 创建周期性工作请求
        PeriodicWorkRequest recoveryWork = new PeriodicWorkRequest.Builder(
                DownloadRecoveryWorker.class,
                RECOVERY_INTERVAL_HOURS,
                TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)   // 延迟1分钟执行首次检查
                .addTag("recovery")
                .build();

        // 注册周期性任务（如果已存在则保留现有任务）
        workManager.enqueueUniquePeriodicWork(
                RECOVERY_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                recoveryWork
        );

        Log.d(TAG, "周期性恢复任务已注册");
    }

    /**
     * 取消周期性恢复任务
     */
    public void cancelPeriodicRecovery() {
        Log.d(TAG, "取消周期性恢复任务");
        workManager.cancelUniqueWork(RECOVERY_WORK_NAME);
    }

    /**
     * 立即执行一次性恢复检查
     * 用于应用启动时检查未完成的任务
     */
    public void performImmediateRecovery() {
        Log.d(TAG, "执行立即恢复检查");

        // 使用 OneTimeWorkRequest 立即执行
        androidx.work.OneTimeWorkRequest immediateRecoveryWork =
                new androidx.work.OneTimeWorkRequest.Builder(DownloadRecoveryWorker.class)
                        .addTag("immediate_recovery")
                        .build();

        workManager.enqueue(immediateRecoveryWork);
    }

    /**
     * 检查是否有未完成的下载任务
     * 此方法用于 UI 层显示恢复提示
     *
     * @param callback 检查结果回调
     */
    public void checkUnfinishedTasks(UnfinishedTasksCallback callback) {
        new Thread(() -> {
            try {
                com.hyperfetch.repo.AppDatabase database =
                        com.hyperfetch.repo.AppDatabase.getInstance(context);
                com.hyperfetch.repo.DownloadTaskDao taskDao = database.getDownloadTaskDao();

                java.util.List<com.hyperfetch.repo.DownloadTaskEntity> unfinishedTasks =
                        taskDao.getUnfinishedTasks();

                if (callback != null) {
                    callback.onResult(unfinishedTasks != null && !unfinishedTasks.isEmpty(),
                            unfinishedTasks != null ? unfinishedTasks.size() : 0);
                }
            } catch (Exception e) {
                Log.e(TAG, "检查未完成任务失败", e);
                if (callback != null) {
                    callback.onResult(false, 0);
                }
            }
        }).start();
    }

    /**
     * 未完成任务检查回调接口
     */
    public interface UnfinishedTasksCallback {
        /**
         * 检查结果回调
         *
         * @param hasUnfinished 是否有未完成任务
         * @param count         未完成任务数量
         */
        void onResult(boolean hasUnfinished, int count);
    }
}