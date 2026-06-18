package com.hyperfetch.service;

import android.os.Binder;

import com.hyperfetch.event.ProgressEvent;
import com.hyperfetch.model.DownloadTask;

import java.util.List;

/**
 * 服务绑定类
 * 提供 TaskService 接口给 UI 层调用
 */
public class DownloadServiceBinder extends Binder {

    /**
     * 关联的 DownloadService 实例
     */
    private final DownloadService service;

    /**
     * 构造函数
     *
     * @param service DownloadService 实例
     */
    public DownloadServiceBinder(DownloadService service) {
        this.service = service;
    }

    /**
     * 获取服务实例
     *
     * @return DownloadService 实例
     */
    public DownloadService getService() {
        return service;
    }

    // ==================== 任务管理接口 ====================

    /**
     * 创建下载任务
     *
     * @param url      下载地址
     * @param fileName 文件名
     * @param savePath 保存路径
     * @return 任务ID
     */
    public String createTask(String url, String fileName, String savePath) {
        return service.createTask(url, fileName, savePath);
    }

    /**
     * 创建下载任务（带选项）
     *
     * @param task 下载任务对象
     * @return 任务ID
     */
    public String createTask(DownloadTask task) {
        return service.createTask(task);
    }

    /**
     * 暂停下载任务
     *
     * @param taskId 任务ID
     */
    public void pauseTask(String taskId) {
        service.pauseTask(taskId);
    }

    /**
     * 恢复下载任务
     *
     * @param taskId 任务ID
     */
    public void resumeTask(String taskId) {
        service.resumeTask(taskId);
    }

    /**
     * 删除下载任务
     *
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        service.deleteTask(taskId);
    }

    /**
     * 重试下载任务
     *
     * @param taskId 任务ID
     */
    public void retryTask(String taskId) {
        service.retryTask(taskId);
    }

    /**
     * 取消下载任务
     *
     * @param taskId 任务ID
     */
    public void cancelTask(String taskId) {
        service.cancelTask(taskId);
    }

    /**
     * 获取所有下载任务
     *
     * @return 任务列表
     */
    public List<DownloadTask> getAllTasks() {
        return service.getAllTasks();
    }

    /**
     * 根据ID获取下载任务
     *
     * @param taskId 任务ID
     * @return 下载任务，如果不存在返回null
     */
    public DownloadTask getTask(String taskId) {
        return service.getTask(taskId);
    }

    // ==================== 进度监听接口 ====================

    /**
     * 注册进度监听器
     *
     * @param listener 进度监听器
     */
    public void registerProgressListener(ProgressListener listener) {
        service.registerProgressListener(listener);
    }

    /**
     * 注销进度监听器
     *
     * @param listener 进度监听器
     */
    public void unregisterProgressListener(ProgressListener listener) {
        service.unregisterProgressListener(listener);
    }

    /**
     * 注册任务状态监听器
     *
     * @param listener 任务状态监听器
     */
    public void registerTaskStateListener(TaskStateListener listener) {
        service.registerTaskStateListener(listener);
    }

    /**
     * 注销任务状态监听器
     *
     * @param listener 任务状态监听器
     */
    public void unregisterTaskStateListener(TaskStateListener listener) {
        service.unregisterTaskStateListener(listener);
    }

    // ==================== 配置接口 ====================

    /**
     * 设置任务线程数
     *
     * @param taskId      任务ID
     * @param threadCount 线程数
     */
    public void setThreadCount(String taskId, int threadCount) {
        service.setThreadCount(taskId, threadCount);
    }

    /**
     * 设置任务速度限制
     *
     * @param taskId     任务ID
     * @param speedLimit 速度限制（字节/秒）
     */
    public void setSpeedLimit(String taskId, long speedLimit) {
        service.setSpeedLimit(taskId, speedLimit);
    }

    /**
     * 检查服务是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isServiceRunning() {
        return service.isRunning();
    }

    // ==================== 监听器接口定义 ====================

    /**
     * 进度监听器接口
     */
    public interface ProgressListener {
        /**
         * 进度更新回调
         *
         * @param event 进度事件
         */
        void onProgress(ProgressEvent event);
    }

    /**
     * 任务状态监听器接口
     */
    public interface TaskStateListener {
        /**
         * 任务完成回调
         *
         * @param taskId 任务ID
         */
        void onTaskCompleted(String taskId);

        /**
         * 任务失败回调
         *
         * @param taskId 任务ID
         * @param error  错误信息
         */
        void onTaskFailed(String taskId, String error);

        /**
         * 任务状态变化回调
         *
         * @param taskId   任务ID
         * @param oldState 旧状态
         * @param newState 新状态
         */
        void onTaskStateChanged(String taskId, String oldState, String newState);
    }
}