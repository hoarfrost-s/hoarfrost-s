package com.hyperfetch.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import com.hyperfetch.R;
import com.hyperfetch.event.CompletedEvent;
import com.hyperfetch.event.ErrorEvent;
import com.hyperfetch.event.EventBus;
import com.hyperfetch.event.ProgressEvent;
import com.hyperfetch.event.StatusChangedEvent;
import com.hyperfetch.event.TaskCreatedEvent;
import com.hyperfetch.model.Category;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.TaskStatus;
import com.hyperfetch.service.DownloadService;
import com.hyperfetch.service.DownloadServiceBinder;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 主界面 Activity
 * 负责管理下载任务列表，处理用户交互和 UI 更新
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Material Toolbar
     */
    private MaterialToolbar toolbar;

    /**
     * 分类 TabLayout
     */
    private TabLayout tabLayout;

    /**
     * 任务列表 RecyclerView
     */
    private RecyclerView recyclerViewTasks;

    /**
     * 底部统计栏
     */
    private android.widget.TextView textStats;

    /**
     * 新建任务 FAB
     */
    private FloatingActionButton fabNewTask;

    /**
     * 任务列表适配器
     */
    private TaskAdapter taskAdapter;

    /**
     * 分类 Tab 适配器
     */
    private CategoryTabAdapter categoryTabAdapter;

    /**
     * DownloadService 绑定器
     */
    private DownloadServiceBinder serviceBinder;

    /**
     * 服务是否已绑定
     */
    private boolean isServiceBound = false;

    /**
     * 服务连接
     */
    private ServiceConnection serviceConnection;

    /**
     * EventBus 进度事件订阅者
     */
    private Consumer<Object> progressEventSubscriber;

    /**
     * EventBus 状态变更事件订阅者
     */
    private Consumer<Object> statusChangedEventSubscriber;

    /**
     * EventBus 任务创建事件订阅者
     */
    private Consumer<Object> taskCreatedEventSubscriber;

    /**
     * EventBus 完成事件订阅者
     */
    private Consumer<Object> completedEventSubscriber;

    /**
     * EventBus 错误事件订阅者
     */
    private Consumer<Object> errorEventSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图组件
        initViews();

        // 设置 Toolbar
        setupToolbar();

        // 初始化适配器
        initAdapters();

        // 设置 RecyclerView
        setupRecyclerView();

        // 设置 TabLayout
        setupTabLayout();

        // 设置 FAB
        setupFab();

        // 注册 EventBus 事件
        registerEventBusEvents();

        // 启动并绑定 DownloadService
        startAndBindService();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        recyclerViewTasks = findViewById(R.id.recycler_view_tasks);
        textStats = findViewById(R.id.text_stats);
        fabNewTask = findViewById(R.id.fab_new_task);
    }

    /**
     * 设置 Toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setTitle("HyperFetch");
    }

    /**
     * 初始化适配器
     */
    private void initAdapters() {
        // 初始化任务列表适配器
        taskAdapter = new TaskAdapter();
        taskAdapter.setActionListener(new TaskAdapter.TaskActionListener() {
            @Override
            public void onPauseTask(String taskId) {
                if (serviceBinder != null) {
                    serviceBinder.pauseTask(taskId);
                }
            }

            @Override
            public void onResumeTask(String taskId) {
                if (serviceBinder != null) {
                    serviceBinder.resumeTask(taskId);
                }
            }

            @Override
            public void onDeleteTask(String taskId) {
                if (serviceBinder != null) {
                    serviceBinder.deleteTask(taskId);
                }
            }

            @Override
            public void onOpenFile(DownloadTask task) {
                openDownloadedFile(task);
            }
        });

        // 初始化分类 Tab 适配器
        categoryTabAdapter = new CategoryTabAdapter(this);
        categoryTabAdapter.setOnTabClickListener((category, position) -> {
            // 切换分类过滤
            taskAdapter.setCategoryFilter(category);
            updateStats();
        });
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerViewTasks.setLayoutManager(layoutManager);
        recyclerViewTasks.setAdapter(taskAdapter);

        // 添加分隔线
        recyclerViewTasks.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                this, RecyclerView.VERTICAL));
    }

    /**
     * 设置 TabLayout
     */
    private void setupTabLayout() {
        // 使用自定义 Tab 适配器
        for (int i = 0; i < categoryTabAdapter.getItemCount(); i++) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(createTabCustomView(i));
            tabLayout.addTab(tab);
        }

        // 设置 Tab 选择监听
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                categoryTabAdapter.setSelectedPosition(tab.getPosition());
                Category category = categoryTabAdapter.getSelectedCategory();
                taskAdapter.setCategoryFilter(category);
                updateStats();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    /**
     * 创建自定义 Tab 视图
     *
     * @param position Tab 位置
     * @return 自定义视图
     */
    private View createTabCustomView(int position) {
        // 创建自定义 Tab 视图
        View customView = getLayoutInflater().inflate(R.layout.item_category_tab, null);
        // 由 CategoryTabAdapter 的 ViewHolder 处理绑定
        return customView;
    }

    /**
     * 设置 FAB
     */
    private void setupFab() {
        fabNewTask.setOnClickListener(v -> {
            showNewTaskDialog();
        });
    }

    /**
     * 显示新建任务对话框
     */
    private void showNewTaskDialog() {
        NewTaskBottomSheet bottomSheet = new NewTaskBottomSheet();
        bottomSheet.setOnTaskCreatedListener(task -> {
            createNewTask(task);
        });

        // 设置默认值
        bottomSheet.setDefaultSavePath(getDefaultDownloadPath());
        bottomSheet.setDefaultThreadCount(3);

        bottomSheet.show(getSupportFragmentManager(), "NewTaskBottomSheet");
    }

    /**
     * 创建新任务
     *
     * @param task 任务对象
     */
    private void createNewTask(DownloadTask task) {
        if (serviceBinder != null) {
            String taskId = serviceBinder.createTask(task);
            if (taskId != null) {
                // 任务创建成功，适配器会通过 EventBus 事件自动更新
            }
        } else {
            // 服务未绑定，显示提示
            Toast.makeText(this, "下载服务未绑定", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取默认下载路径
     *
     * @return 默认下载路径
     */
    private String getDefaultDownloadPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    /**
     * 注册 EventBus 事件
     */
    private void registerEventBusEvents() {
        EventBus eventBus = EventBus.getInstance();

        // 注册进度事件订阅者
        progressEventSubscriber = event -> {
            if (event instanceof ProgressEvent) {
                ProgressEvent progressEvent = (ProgressEvent) event;
                runOnUiThread(() -> {
                    taskAdapter.updateProgress(progressEvent);
                    updateStats();
                });
            }
        };
        eventBus.register(ProgressEvent.class, progressEventSubscriber);

        // 注册状态变更事件订阅者
        statusChangedEventSubscriber = event -> {
            if (event instanceof StatusChangedEvent) {
                StatusChangedEvent statusEvent = (StatusChangedEvent) event;
                runOnUiThread(() -> {
                    TaskStatus newStatus = TaskStatus.valueOf(statusEvent.getNewState());
                    taskAdapter.updateTaskStatus(statusEvent.getTaskId(), newStatus);
                    updateStats();
                    updateCategoryTabs();
                });
            }
        };
        eventBus.register(StatusChangedEvent.class, statusChangedEventSubscriber);

        // 注册任务创建事件订阅者
        taskCreatedEventSubscriber = event -> {
            if (event instanceof TaskCreatedEvent) {
                TaskCreatedEvent createdEvent = (TaskCreatedEvent) event;
                runOnUiThread(() -> {
                    // 刷新任务列表
                    refreshTaskList();
                    updateStats();
                    updateCategoryTabs();
                });
            }
        };
        eventBus.register(TaskCreatedEvent.class, taskCreatedEventSubscriber);

        // 注册完成事件订阅者
        completedEventSubscriber = event -> {
            if (event instanceof CompletedEvent) {
                CompletedEvent completedEvent = (CompletedEvent) event;
                runOnUiThread(() -> {
                    taskAdapter.updateTaskStatus(completedEvent.getTaskId(), TaskStatus.COMPLETED);
                    updateStats();
                    updateCategoryTabs();
                    showCompletionNotification(completedEvent);
                });
            }
        };
        eventBus.register(CompletedEvent.class, completedEventSubscriber);

        // 注册错误事件订阅者
        errorEventSubscriber = event -> {
            if (event instanceof ErrorEvent) {
                ErrorEvent errorEvent = (ErrorEvent) event;
                runOnUiThread(() -> {
                    taskAdapter.updateTaskStatus(errorEvent.getTaskId(), TaskStatus.FAILED);
                    updateStats();
                    updateCategoryTabs();
                    showErrorNotification(errorEvent);
                });
            }
        };
        eventBus.register(ErrorEvent.class, errorEventSubscriber);
    }

    /**
     * 取消注册 EventBus 事件
     */
    private void unregisterEventBusEvents() {
        EventBus eventBus = EventBus.getInstance();

        if (progressEventSubscriber != null) {
            eventBus.unregister(ProgressEvent.class, progressEventSubscriber);
        }

        if (statusChangedEventSubscriber != null) {
            eventBus.unregister(StatusChangedEvent.class, statusChangedEventSubscriber);
        }

        if (taskCreatedEventSubscriber != null) {
            eventBus.unregister(TaskCreatedEvent.class, taskCreatedEventSubscriber);
        }

        if (completedEventSubscriber != null) {
            eventBus.unregister(CompletedEvent.class, completedEventSubscriber);
        }

        if (errorEventSubscriber != null) {
            eventBus.unregister(ErrorEvent.class, errorEventSubscriber);
        }
    }

    /**
     * 启动并绑定 DownloadService
     */
    private void startAndBindService() {
        // 创建服务连接
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service instanceof DownloadServiceBinder) {
                    serviceBinder = (DownloadServiceBinder) service;
                    isServiceBound = true;

                    // 服务绑定成功，刷新任务列表
                    refreshTaskList();
                    updateStats();
                    updateCategoryTabs();

                    // 注册服务监听器
                    registerServiceListeners();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceBinder = null;
                isServiceBound = false;

                // 注销服务监听器
                unregisterServiceListeners();
            }
        };

        // 启动服务
        Intent serviceIntent = new Intent(this, DownloadService.class);
        startForegroundService(serviceIntent);

        // 绑定服务
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 注册服务监听器
     */
    private void registerServiceListeners() {
        if (serviceBinder == null) {
            return;
        }

        // 注册进度监听器
        serviceBinder.registerProgressListener(new DownloadServiceBinder.ProgressListener() {
            @Override
            public void onProgress(ProgressEvent event) {
                runOnUiThread(() -> {
                    taskAdapter.updateProgress(event);
                    updateStats();
                });
            }
        });

        // 注册任务状态监听器
        serviceBinder.registerTaskStateListener(new DownloadServiceBinder.TaskStateListener() {
            @Override
            public void onTaskCompleted(String taskId) {
                runOnUiThread(() -> {
                    taskAdapter.updateTaskStatus(taskId, TaskStatus.COMPLETED);
                    updateStats();
                    updateCategoryTabs();
                });
            }

            @Override
            public void onTaskFailed(String taskId, String error) {
                runOnUiThread(() -> {
                    taskAdapter.updateTaskStatus(taskId, TaskStatus.FAILED);
                    updateStats();
                    updateCategoryTabs();
                });
            }

            @Override
            public void onTaskStateChanged(String taskId, String oldState, String newState) {
                runOnUiThread(() -> {
                    if (!"DELETED".equals(newState)) {
                        TaskStatus newStatus = TaskStatus.valueOf(newState);
                        taskAdapter.updateTaskStatus(taskId, newStatus);
                    }
                    updateStats();
                    updateCategoryTabs();
                });
            }
        });
    }

    /**
     * 注销服务监听器
     */
    private void unregisterServiceListeners() {
        // 监听器已在 Service 中管理，这里不需要额外操作
    }

    /**
     * 刷新任务列表
     */
    private void refreshTaskList() {
        if (serviceBinder != null) {
            List<DownloadTask> tasks = serviceBinder.getAllTasks();
            taskAdapter.setTaskList(tasks);
        }
    }

    /**
     * 更新底部统计栏
     */
    private void updateStats() {
        int downloadingCount = taskAdapter.getDownloadingCount();
        int completedCount = taskAdapter.getCompletedCount();
        int totalCount = taskAdapter.getTotalCount();

        String statsText = UiUtils.formatTaskStats(downloadingCount, completedCount, totalCount);
        textStats.setText(statsText);
    }

    /**
     * 更新分类 Tab 数量
     */
    private void updateCategoryTabs() {
        Map<Category, Integer> categoryCounts = taskAdapter.getCategoryTaskCounts();
        categoryTabAdapter.updateCategoryCounts(categoryCounts);
    }

    /**
     * 显示完成通知
     *
     * @param event 完成事件
     */
    private void showCompletionNotification(CompletedEvent event) {
        Toast.makeText(this,
                "任务完成: " + event.getFileName(),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示错误通知
     *
     * @param event 错误事件
     */
    private void showErrorNotification(ErrorEvent event) {
        Toast.makeText(this,
                "下载失败: " + event.getError(),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 打开已下载的文件
     *
     * @param task 任务对象
     */
    private void openDownloadedFile(DownloadTask task) {
        try {
            File file = new File(task.getSavePath(), task.getFileName());
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", file);
                intent.setDataAndType(fileUri, getMimeType(task.getFileName()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取文件的 MIME 类型
     *
     * @param fileName 文件名
     * @return MIME 类型
     */
    private String getMimeType(String fileName) {
        if (fileName == null) {
            return "*/*";
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "mp4":
            case "mkv":
            case "avi":
            case "mov":
                return "video/*";
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                return "audio/*";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
                return "image/*";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "application/zip";
            case "apk":
                return "application/vnd.android.package-archive";
            case "exe":
                return "application/octet-stream";
            default:
                return "*/*";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            // 打开设置页面
            openSettings();
            return true;
        } else if (itemId == R.id.action_refresh) {
            // 刷新任务列表
            refreshTaskList();
            updateStats();
            updateCategoryTabs();
            return true;
        } else if (itemId == R.id.action_clear_completed) {
            // 清除已完成任务
            clearCompletedTasks();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 打开设置页面
     */
    private void openSettings() {
        // 使用 Fragment 显示设置页面
        SettingsFragment settingsFragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .addToBackStack("settings")
                .commit();
    }

    /**
     * 清除已完成任务
     */
    private void clearCompletedTasks() {
        if (serviceBinder != null) {
            List<DownloadTask> tasks = serviceBinder.getAllTasks();
            for (DownloadTask task : tasks) {
                if (task.getStatus() == TaskStatus.COMPLETED) {
                    serviceBinder.deleteTask(task.getId());
                }
            }
            refreshTaskList();
            updateStats();
            updateCategoryTabs();
            Toast.makeText(this, "已完成任务已清除", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新任务列表
        if (isServiceBound) {
            refreshTaskList();
            updateStats();
            updateCategoryTabs();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 取消注册 EventBus 事件
        unregisterEventBusEvents();

        // 解绑服务
        if (isServiceBound && serviceConnection != null) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}