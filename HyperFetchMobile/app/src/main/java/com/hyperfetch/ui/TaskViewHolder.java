package com.hyperfetch.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperfetch.R;
import com.hyperfetch.model.Category;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskStatus;

/**
 * 任务卡片 ViewHolder
 * 用于绑定下载任务数据到视图组件
 */
public class TaskViewHolder extends RecyclerView.ViewHolder {

    /**
     * 类型徽章 TextView
     */
    private final TextView badgeCategory;

    /**
     * 文件名 TextView
     */
    private final TextView textFileName;

    /**
     * 状态徽章 TextView
     */
    private final TextView badgeStatus;

    /**
     * 进度条 ProgressBar
     */
    private final ProgressBar progressBar;

    /**
     * 进度百分比 TextView
     */
    private final TextView textProgress;

    /**
     * 下载速度 TextView
     */
    private final TextView textSpeed;

    /**
     * 已下载/总大小 TextView
     */
    private final TextView textSize;

    /**
     * 剩余时间 TextView
     */
    private final TextView textRemainingTime;

    /**
     * 暂停/继续按钮
     */
    private final ImageButton btnPauseResume;

    /**
     * 删除按钮
     */
    private final ImageButton btnDelete;

    /**
     * 打开文件按钮
     */
    private final ImageButton btnOpen;

    /**
     * 上下文
     */
    private final Context context;

    /**
     * 当前绑定的任务
     */
    private DownloadTask currentTask;

    /**
     * 进度条流光动画
     */
    private ValueAnimator progressAnimator;

    /**
     * 任务操作监听器
     */
    private TaskActionListener actionListener;

    /**
     * 构造函数
     *
     * @param itemView 列表项视图
     */
    public TaskViewHolder(@NonNull View itemView) {
        super(itemView);
        this.context = itemView.getContext();

        // 初始化视图组件
        badgeCategory = itemView.findViewById(R.id.badge_category);
        textFileName = itemView.findViewById(R.id.text_file_name);
        badgeStatus = itemView.findViewById(R.id.badge_status);
        progressBar = itemView.findViewById(R.id.progress_bar);
        textProgress = itemView.findViewById(R.id.text_progress);
        textSpeed = itemView.findViewById(R.id.text_speed);
        textSize = itemView.findViewById(R.id.text_size);
        textRemainingTime = itemView.findViewById(R.id.text_remaining_time);
        btnPauseResume = itemView.findViewById(R.id.btn_pause_resume);
        btnDelete = itemView.findViewById(R.id.btn_delete);
        btnOpen = itemView.findViewById(R.id.btn_open);

        // 设置按钮点击监听
        setupButtonListeners();
    }

    /**
     * 设置按钮点击监听器
     */
    private void setupButtonListeners() {
        btnPauseResume.setOnClickListener(v -> {
            if (actionListener != null && currentTask != null) {
                TaskStatus status = currentTask.getStatus();
                if (status == TaskStatus.DOWNLOADING) {
                    actionListener.onPauseClick(currentTask);
                } else if (status == TaskStatus.PAUSED || status == TaskStatus.FAILED) {
                    actionListener.onResumeClick(currentTask);
                }
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (actionListener != null && currentTask != null) {
                actionListener.onDeleteClick(currentTask);
            }
        });

        btnOpen.setOnClickListener(v -> {
            if (actionListener != null && currentTask != null) {
                actionListener.onOpenClick(currentTask);
            }
        });
    }

    /**
     * 绑定任务数据到视图
     *
     * @param task 下载任务对象
     */
    public void bindTask(DownloadTask task) {
        this.currentTask = task;

        // 更新文件名
        textFileName.setText(task.getFileName());

        // 更新类型徽章
        updateCategoryBadge(task.getCategory());

        // 更新状态徽章
        updateStatusBadge(task.getStatus());

        // 更新进度
        updateProgress(task.getProgress(), task.getDownloaded(), task.getTotalSize());

        // 更新速度（初始为 0）
        updateSpeed(0);

        // 更新大小显示
        updateSize(task.getDownloaded(), task.getTotalSize());

        // 更新剩余时间
        updateRemainingTime(Long.MAX_VALUE);

        // 更新按钮状态
        updateButtonStates(task.getStatus());
    }

    /**
     * 更新类型徽章
     *
     * @param category 文件分类
     */
    private void updateCategoryBadge(Category category) {
        String displayName = UiUtils.getCategoryDisplayName(category);
        badgeCategory.setText(displayName);

        // 设置徽章背景颜色
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(8f);

        int bgColor;
        switch (category) {
            case VIDEO:
                bgColor = context.getResources().getColor(R.color.colorPrimary, null);
                break;
            case AUDIO:
                bgColor = context.getResources().getColor(R.color.colorAccent, null);
                break;
            case ARCHIVE:
                bgColor = context.getResources().getColor(R.color.warning, null);
                break;
            case DOCUMENT:
                bgColor = context.getResources().getColor(R.color.info, null);
                break;
            case PROGRAM:
                bgColor = context.getResources().getColor(R.color.success, null);
                break;
            default:
                bgColor = context.getResources().getColor(R.color.textSecondary, null);
                break;
        }

        drawable.setColor(bgColor);
        badgeCategory.setBackground(drawable);
    }

    /**
     * 更新状态徽章
     *
     * @param status 任务状态
     */
    private void updateStatusBadge(TaskStatus status) {
        String statusText = UiUtils.getStatusText(status, context);
        badgeStatus.setText(statusText);

        // 设置徽章背景颜色
        int badgeColor = UiUtils.getStatusBadgeColor(status, context);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(8f);
        drawable.setColor(badgeColor);
        badgeStatus.setBackground(drawable);
    }

    /**
     * 更新进度条和进度文本
     *
     * @param progress   进度百分比（0-100）
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     */
    public void updateProgress(int progress, long downloaded, long totalSize) {
        // 更新进度条
        progressBar.setProgress(progress);

        // 更新进度文本
        textProgress.setText(UiUtils.formatProgress(progress));

        // 更新大小显示
        updateSize(downloaded, totalSize);

        // 如果正在下载，启动流光动画
        if (currentTask != null && currentTask.getStatus() == TaskStatus.DOWNLOADING) {
            startProgressAnimation();
        } else {
            stopProgressAnimation();
        }
    }

    /**
     * 更新下载速度显示
     *
     * @param speedBytesPerSecond 每秒下载字节数
     */
    public void updateSpeed(long speedBytesPerSecond) {
        textSpeed.setText(UiUtils.formatSpeed(speedBytesPerSecond));
    }

    /**
     * 更新文件大小显示
     *
     * @param downloaded 已下载字节数
     * @param totalSize  总字节数
     */
    private void updateSize(long downloaded, long totalSize) {
        String downloadedStr = UiUtils.formatFileSize(downloaded);
        String totalStr = UiUtils.formatFileSize(totalSize);
        textSize.setText(downloadedStr + " / " + totalStr);
    }

    /**
     * 更新剩余时间显示
     *
     * @param remainingSeconds 剩余秒数
     */
    public void updateRemainingTime(long remainingSeconds) {
        textRemainingTime.setText(UiUtils.formatRemainingTime(remainingSeconds));
    }

    /**
     * 更新按钮状态
     *
     * @param status 任务状态
     */
    private void updateButtonStates(TaskStatus status) {
        switch (status) {
            case DOWNLOADING:
                // 正在下载：显示暂停按钮
                btnPauseResume.setImageResource(android.R.drawable.ic_media_pause);
                btnPauseResume.setVisibility(View.VISIBLE);
                btnPauseResume.setEnabled(true);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(false);
                btnOpen.setVisibility(View.GONE);
                break;

            case PAUSED:
                // 已暂停：显示继续按钮
                btnPauseResume.setImageResource(android.R.drawable.ic_media_play);
                btnPauseResume.setVisibility(View.VISIBLE);
                btnPauseResume.setEnabled(true);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(false);
                btnOpen.setVisibility(View.GONE);
                break;

            case FAILED:
                // 失败：显示重试按钮（使用继续按钮图标）
                btnPauseResume.setImageResource(android.R.drawable.ic_menu_rotate);
                btnPauseResume.setVisibility(View.VISIBLE);
                btnPauseResume.setEnabled(true);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(false);
                btnOpen.setVisibility(View.GONE);
                break;

            case COMPLETED:
                // 已完成：隐藏暂停/继续按钮，显示打开按钮
                btnPauseResume.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(true);
                btnOpen.setVisibility(View.VISIBLE);
                break;

            case QUEUED:
                // 排队中：显示暂停按钮
                btnPauseResume.setImageResource(android.R.drawable.ic_media_pause);
                btnPauseResume.setVisibility(View.VISIBLE);
                btnPauseResume.setEnabled(true);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(false);
                btnOpen.setVisibility(View.GONE);
                break;

            case COMPLETED_WITH_WARNING:
                // 完成但有警告：显示打开按钮
                btnPauseResume.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                btnOpen.setEnabled(true);
                btnOpen.setVisibility(View.VISIBLE);
                break;

            default:
                // 其他状态：禁用所有按钮
                btnPauseResume.setVisibility(View.GONE);
                btnDelete.setEnabled(false);
                btnOpen.setEnabled(false);
                btnOpen.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 启动进度条流光动画
     * 创建渐变流光效果，增强视觉体验
     */
    private void startProgressAnimation() {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            return;
        }

        // 创建进度条渐变动画
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(2000);
        progressAnimator.setRepeatCount(ValueAnimator.INFINITE);
        progressAnimator.setRepeatMode(ValueAnimator.RESTART);

        progressAnimator.addUpdateListener(animation -> {
            // 这里可以添加更复杂的流光效果
            // 暂时使用简单的进度条动画
        });

        progressAnimator.start();
    }

    /**
     * 停止进度条流光动画
     */
    private void stopProgressAnimation() {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
    }

    /**
     * 设置任务操作监听器
     *
     * @param listener 任务操作监听器
     */
    public void setActionListener(TaskActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * 获取当前绑定的任务
     *
     * @return 当前任务对象
     */
    public DownloadTask getCurrentTask() {
        return currentTask;
    }

    /**
     * 清理资源
     * 在 ViewHolder 被回收时调用
     */
    public void cleanup() {
        stopProgressAnimation();
        currentTask = null;
    }

    /**
     * 任务操作监听器接口
     */
    public interface TaskActionListener {
        /**
         * 暂停按钮点击回调
         *
         * @param task 任务对象
         */
        void onPauseClick(DownloadTask task);

        /**
         * 继续按钮点击回调
         *
         * @param task 任务对象
         */
        void onResumeClick(DownloadTask task);

        /**
         * 删除按钮点击回调
         *
         * @param task 任务对象
         */
        void onDeleteClick(DownloadTask task);

        /**
         * 打开文件按钮点击回调
         *
         * @param task 任务对象
         */
        void onOpenClick(DownloadTask task);
    }
}