package com.hyperfetch.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.repo.DownloadTaskDao;
import com.hyperfetch.repo.DownloadTaskEntity;
import com.hyperfetch.repo.AppDatabase;

import java.util.List;

/**
 * 下载恢复 Worker
 * 用于检查并恢复未完成的下载任务
 * 在应用启动或设备重启后自动执行
 */
public class DownloadRecoveryWorker extends Worker {

    private static final String TAG = "DownloadRecoveryWorker";

    /**
     * 构造函数
     *
     * @param context      应用上下文
     * @param workerParams Worker 参数
     */
    public DownloadRecoveryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * 执行后台任务
     * 检查是否有未完成的下载任务，并启动 DownloadService 恢复下载
     *
     * @return 任务执行结果
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "开始执行下载恢复检查...");

        try {
            // 获取数据库实例
            AppDatabase database = AppDatabase.getApplicationContext(getApplicationContext());
            DownloadTaskDao taskDao = database.getDownloadTaskDao();

            // 查询所有未完成的任务（DOWNLOADING 和 PAUSED 状态）
            List<DownloadTaskEntity> unfinishedTasks = getUnfinishedTasks(taskDao);

            if (unfinishedTasks.isEmpty()) {
                Log.d(TAG, "没有未完成的下载任务");
                return Result.success();
            }

            Log.d(TAG, "发现 " + unfinishedTasks.size() + " 个未完成的下载任务");

            // 将 DOWNLOADING 状态的任务重置为 PAUSED（防止进程被杀后状态不一致）
            resetDownloadingTasks(taskDao, unfinishedTasks);

            // 启动 DownloadService 恢复下载
            startDownloadService(unfinishedTasks);

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "下载恢复检查失败", e);
            return Result.retry();
        }
    }

    /**
     * 获取所有未完成的下载任务
     *
     * @param taskDao 任务数据访问对象
     * @return 未完成任务列表
     */
    private List<DownloadTaskEntity> getUnfinishedTasks(DownloadTaskDao taskDao) {
        // 查询 DOWNLOADING 和 PAUSED 状态的任务
        return taskDao.getUnfinishedTasks();
    }

    /**
     * 重置 DOWNLOADING 状态的任务为 PAUSED
     * 防止进程被杀后状态不一致
     *
     * @param taskDao 任务数据访问对象
     * @param tasks   任务列表
     */
    private void resetDownloadingTasks(DownloadTaskDao taskDao, List<DownloadTaskEntity> tasks) {
        for (DownloadTaskEntity task : tasks) {
            if (TaskStatus.DOWNLOADING.name().equals(task.status)) {
                Log.d(TAG, "重置任务状态: " + task.id + " -> PAUSED");
                task.status = TaskStatus.PAUSED.name();
                taskDao.update(task);
            }
        }
    }

    /**
     * 启动 DownloadService 恢复下载
     *
     * @param unfinishedTasks 未完成任务列表
     */
    private void startDownloadService(List<DownloadTaskEntity> unfinishedTasks) {
        Context context = getApplicationContext();

        // 创建启动 DownloadService 的 Intent
        Intent serviceIntent = new Intent(context, DownloadService.class);
        serviceIntent.setAction(ServiceActions.ACTION_RECOVER);

        // 传递需要恢复的任务数量
        serviceIntent.putExtra(ServiceActions.KEY_RECOVERY_COUNT, unfinishedTasks.size());

        // 启动前台服务
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        Log.d(TAG, "已启动 DownloadService 恢复 " + unfinishedTasks.size() + " 个任务");
    }
}