package com.hyperfetch.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperfetch.R;
import com.hyperfetch.event.ProgressEvent;
import com.hyperfetch.model.Category;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务列表适配器
 * 用于显示下载任务列表，支持进度实时更新和操作按钮交互
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskViewHolder> {

    /**
     * 任务列表数据
     */
    private List<DownloadTask> taskList;

    /**
     * 过滤后的任务列表（用于分类过滤）
     */
    private List<DownloadTask> filteredTaskList;

    /**
     * 当前选中的分类（null 表示全部）
     */
    private Category selectedCategory;

    /**
     * 任务 ID 到 ViewHolder 位置的映射
     */
    private Map<String, Integer> taskIdToPositionMap;

    /**
     * 任务操作监听器
     */
    private TaskActionListener actionListener;

    /**
     * 构造函数
     */
    public TaskAdapter() {
        this.taskList = new ArrayList<>();
        this.filteredTaskList = new ArrayList<>();
        this.selectedCategory = null;
        this.taskIdToPositionMap = new HashMap<>();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建任务卡片视图
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        TaskViewHolder holder = new TaskViewHolder(itemView);

        // 设置任务操作监听器
        holder.setActionListener(new TaskViewHolder.TaskActionListener() {
            @Override
            public void onPauseClick(DownloadTask task) {
                if (actionListener != null) {
                    actionListener.onPauseTask(task.getId());
                }
            }

            @Override
            public void onResumeClick(DownloadTask task) {
                if (actionListener != null) {
                    actionListener.onResumeTask(task.getId());
                }
            }

            @Override
            public void onDeleteClick(DownloadTask task) {
                if (actionListener != null) {
                    actionListener.onDeleteTask(task.getId());
                }
            }

            @Override
            public void onOpenClick(DownloadTask task) {
                if (actionListener != null) {
                    actionListener.onOpenFile(task);
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        DownloadTask task = filteredTaskList.get(position);
        holder.bindTask(task);
    }

    @Override
    public int getItemCount() {
        return filteredTaskList.size();
    }

    @Override
    public void onViewRecycled(@NonNull TaskViewHolder holder) {
        super.onViewRecycled(holder);
        // 清理 ViewHolder 资源
        holder.cleanup();
    }

    // ==================== 数据操作方法 ====================

    /**
     * 设置任务列表数据
     *
     * @param tasks 任务列表
     */
    public void setTaskList(List<DownloadTask> tasks) {
        this.taskList = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        applyCategoryFilter();
        rebuildPositionMap();
        notifyDataSetChanged();
    }

    /**
     * 添加单个任务
     *
     * @param task 任务对象
     */
    public void addTask(DownloadTask task) {
        if (task == null) {
            return;
        }

        // 添加到原始列表
        taskList.add(task);

        // 如果符合当前分类过滤条件，添加到过滤列表
        if (matchesCategoryFilter(task)) {
            filteredTaskList.add(task);
            rebuildPositionMap();
            notifyItemInserted(filteredTaskList.size() - 1);
        }
    }

    /**
     * 更新单个任务
     *
     * @param task 任务对象
     */
    public void updateTask(DownloadTask task) {
        if (task == null) {
            return;
        }

        // 更新原始列表中的任务
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(task.getId())) {
                taskList.set(i, task);
                break;
            }
        }

        // 更新过滤列表中的任务
        Integer position = taskIdToPositionMap.get(task.getId());
        if (position != null && position < filteredTaskList.size()) {
            DownloadTask existingTask = filteredTaskList.get(position);
            if (existingTask.getId().equals(task.getId())) {
                filteredTaskList.set(position, task);
                notifyItemChanged(position);
            }
        }
    }

    /**
     * 移除单个任务
     *
     * @param taskId 任务 ID
     */
    public void removeTask(String taskId) {
        if (taskId == null) {
            return;
        }

        // 从原始列表移除
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(taskId)) {
                taskList.remove(i);
                break;
            }
        }

        // 从过滤列表移除
        Integer position = taskIdToPositionMap.get(taskId);
        if (position != null && position < filteredTaskList.size()) {
            filteredTaskList.remove(position);
            rebuildPositionMap();
            notifyItemRemoved(position);
        }
    }

    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        taskList.clear();
        filteredTaskList.clear();
        taskIdToPositionMap.clear();
        notifyDataSetChanged();
    }

    // ==================== 分类过滤方法 ====================

    /**
     * 设置分类过滤
     *
     * @param category 分类（null 表示全部）
     */
    public void setCategoryFilter(Category category) {
        this.selectedCategory = category;
        applyCategoryFilter();
        rebuildPositionMap();
        notifyDataSetChanged();
    }

    /**
     * 应用分类过滤
     */
    private void applyCategoryFilter() {
        filteredTaskList.clear();
        for (DownloadTask task : taskList) {
            if (matchesCategoryFilter(task)) {
                filteredTaskList.add(task);
            }
        }
    }

    /**
     * 检查任务是否符合当前分类过滤条件
     *
     * @param task 任务对象
     * @return 是否符合过滤条件
     */
    private boolean matchesCategoryFilter(DownloadTask task) {
        if (selectedCategory == null) {
            return true;
        }
        return task.getCategory() == selectedCategory;
    }

    /**
     * 重建任务 ID 到位置的映射
     */
    private void rebuildPositionMap() {
        taskIdToPositionMap.clear();
        for (int i = 0; i < filteredTaskList.size(); i++) {
            taskIdToPositionMap.put(filteredTaskList.get(i).getId(), i);
        }
    }

    // ==================== 进度更新方法 ====================

    /**
     * 更新任务进度
     * 根据进度事件更新对应任务的 UI
     *
     * @param event 进度事件
     */
    public void updateProgress(ProgressEvent event) {
        if (event == null || event.getTaskId() == null) {
            return;
        }

        Integer position = taskIdToPositionMap.get(event.getTaskId());
        if (position != null && position < filteredTaskList.size()) {
            DownloadTask task = filteredTaskList.get(position);
            if (task != null) {
                // 更新任务数据
                task.setDownloaded(event.getDownloaded());
                task.setTotalSize(event.getTotalSize());

                // 更新 ViewHolder（如果可见）
                RecyclerView.ViewHolder holder = getViewHolderAtPosition(position);
                if (holder instanceof TaskViewHolder) {
                    TaskViewHolder taskHolder = (TaskViewHolder) holder;
                    taskHolder.updateProgress(event.getProgress(), event.getDownloaded(), event.getTotalSize());
                    taskHolder.updateSpeed(event.getSpeed());

                    // 计算并更新剩余时间
                    long remainingTime = UiUtils.calculateRemainingTime(
                            event.getDownloaded(), event.getTotalSize(), event.getSpeed());
                    taskHolder.updateRemainingTime(remainingTime);
                }
            }
        }
    }

    /**
     * 更新任务状态
     *
     * @param taskId   任务 ID
     * @param newStatus 新状态
     */
    public void updateTaskStatus(String taskId, TaskStatus newStatus) {
        if (taskId == null) {
            return;
        }

        Integer position = taskIdToPositionMap.get(taskId);
        if (position != null && position < filteredTaskList.size()) {
            DownloadTask task = filteredTaskList.get(position);
            if (task != null) {
                task.setStatus(newStatus);

                // 更新 ViewHolder
                RecyclerView.ViewHolder holder = getViewHolderAtPosition(position);
                if (holder instanceof TaskViewHolder) {
                    TaskViewHolder taskHolder = (TaskViewHolder) holder;
                    taskHolder.updateProgress(task.getProgress(), task.getDownloaded(), task.getTotalSize());
                }
                notifyItemChanged(position);
            }
        }
    }

    /**
     * 获取指定位置的 ViewHolder
     *
     * @param position 位置
     * @return ViewHolder（可能为 null）
     */
    private RecyclerView.ViewHolder getViewHolderAtPosition(int position) {
        // 注意：此方法需要 RecyclerView 实例才能实现
        // 在实际使用中，可以通过观察者模式或直接调用 notifyItemChanged 来更新
        return null;
    }

    // ==================== 统计方法 ====================

    /**
     * 获取正在下载的任务数量
     *
     * @return 正在下载的任务数量
     */
    public int getDownloadingCount() {
        int count = 0;
        for (DownloadTask task : taskList) {
            if (task.getStatus() == TaskStatus.DOWNLOADING) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取已完成任务数量
     *
     * @return 已完成的任务数量
     */
    public int getCompletedCount() {
        int count = 0;
        for (DownloadTask task : taskList) {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取总任务数量
     *
     * @return 总任务数量
     */
    public int getTotalCount() {
        return taskList.size();
    }

    /**
     * 获取指定分类的任务数量
     *
     * @param category 分类
     * @return 任务数量
     */
    public int getTaskCountByCategory(Category category) {
        int count = 0;
        for (DownloadTask task : taskList) {
            if (task.getCategory() == category) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取所有分类的任务数量统计
     *
     * @return 分类任务数量映射
     */
    public Map<Category, Integer> getCategoryTaskCounts() {
        Map<Category, Integer> counts = new HashMap<>();
        for (Category category : Category.values()) {
            counts.put(category, getTaskCountByCategory(category));
        }
        return counts;
    }

    // ==================== 监听器设置 ====================

    /**
     * 设置任务操作监听器
     *
     * @param listener 任务操作监听器
     */
    public void setActionListener(TaskActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * 获取指定位置的任务
     *
     * @param position 位置
     * @return 任务对象（可能为 null）
     */
    public DownloadTask getTaskAtPosition(int position) {
        if (position >= 0 && position < filteredTaskList.size()) {
            return filteredTaskList.get(position);
        }
        return null;
    }

    /**
     * 任务操作监听器接口
     */
    public interface TaskActionListener {
        /**
         * 暂停任务
         *
         * @param taskId 任务 ID
         */
        void onPauseTask(String taskId);

        /**
         * 恢复任务
         *
         * @param taskId 任务 ID
         */
        void onResumeTask(String taskId);

        /**
         * 删除任务
         *
         * @param taskId 任务 ID
         */
        void onDeleteTask(String taskId);

        /**
         * 打开文件
         *
         * @param task 任务对象
         */
        void onOpenFile(DownloadTask task);
    }
}