package com.hyperfetch.ui;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;
import com.hyperfetch.R;
import com.hyperfetch.model.DownloadTask;
import com.hyperfetch.model.Priority;
import com.hyperfetch.model.TaskStatus;

import java.util.UUID;

/**
 * 新建任务对话框
 * BottomSheetDialogFragment 实现，用于创建新的下载任务
 */
public class NewTaskBottomSheet extends BottomSheetDialogFragment {

    /**
     * 下载链接输入框
     */
    private EditText editUrl;

    /**
     * 文件名输入框
     */
    private EditText editFileName;

    /**
     * 保存路径输入框
     */
    private EditText editSavePath;

    /**
     * 线程数滑块
     */
    private Slider sliderThreadCount;

    /**
     * 线程数显示文本
     */
    private TextView textThreadCount;

    /**
     * 速度限制开关
     */
    private Switch switchSpeedLimit;

    /**
     * 速度限制输入框
     */
    private EditText editSpeedLimit;

    /**
     * 取消按钮
     */
    private Button btnCancel;

    /**
     * 确认按钮
     */
    private Button btnConfirm;

    /**
     * 任务创建监听器
     */
    private OnTaskCreatedListener taskCreatedListener;

    /**
     * 默认保存路径
     */
    private String defaultSavePath;

    /**
     * 默认线程数
     */
    private int defaultThreadCount = 3;

    /**
     * 默认速度限制（0 表示无限制）
     */
    private long defaultSpeedLimit = 0;

    /**
     * 构造函数
     */
    public NewTaskBottomSheet() {
    }

    /**
     * 设置任务创建监听器
     *
     * @param listener 任务创建监听器
     */
    public void setOnTaskCreatedListener(OnTaskCreatedListener listener) {
        this.taskCreatedListener = listener;
    }

    /**
     * 设置默认保存路径
     *
     * @param path 默认保存路径
     */
    public void setDefaultSavePath(String path) {
        this.defaultSavePath = path;
    }

    /**
     * 设置默认线程数
     *
     * @param threadCount 默认线程数
     */
    public void setDefaultThreadCount(int threadCount) {
        this.defaultThreadCount = threadCount;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_sheet_new_task, container, false);

        // 初始化视图组件
        initViews(rootView);

        // 设置默认值
        setupDefaults();

        // 设置监听器
        setupListeners();

        // 自动填充剪贴板内容
        autoFillFromClipboard();

        return rootView;
    }

    /**
     * 初始化视图组件
     *
     * @param rootView 根视图
     */
    private void initViews(View rootView) {
        editUrl = rootView.findViewById(R.id.edit_url);
        editFileName = rootView.findViewById(R.id.edit_file_name);
        editSavePath = rootView.findViewById(R.id.edit_save_path);
        sliderThreadCount = rootView.findViewById(R.id.slider_thread_count);
        textThreadCount = rootView.findViewById(R.id.text_thread_count);
        switchSpeedLimit = rootView.findViewById(R.id.switch_speed_limit);
        editSpeedLimit = rootView.findViewById(R.id.edit_speed_limit);
        btnCancel = rootView.findViewById(R.id.btn_cancel);
        btnConfirm = rootView.findViewById(R.id.btn_confirm);
    }

    /**
     * 设置默认值
     */
    private void setupDefaults() {
        // 设置默认保存路径
        if (defaultSavePath != null && !defaultSavePath.isEmpty()) {
            editSavePath.setText(defaultSavePath);
        } else {
            // 使用默认下载目录
            editSavePath.setText(getDefaultDownloadPath());
        }

        // 设置默认线程数
        sliderThreadCount.setValue(defaultThreadCount);
        textThreadCount.setText(UiUtils.formatThreadCount(defaultThreadCount));

        // 设置速度限制默认状态
        switchSpeedLimit.setChecked(false);
        editSpeedLimit.setEnabled(false);
        editSpeedLimit.setVisibility(View.GONE);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 线程数滑块监听
        sliderThreadCount.addOnChangeListener((slider, value, fromUser) -> {
            int threadCount = (int) value;
            textThreadCount.setText(UiUtils.formatThreadCount(threadCount));
        });

        // 速度限制开关监听
        switchSpeedLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editSpeedLimit.setEnabled(isChecked);
            editSpeedLimit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && editSpeedLimit.getText().toString().isEmpty()) {
                editSpeedLimit.setText("1024"); // 默认 1 MB/s
            }
        });

        // 取消按钮点击
        btnCancel.setOnClickListener(v -> dismiss());

        // 确认按钮点击
        btnConfirm.setOnClickListener(v -> createTask());
    }

    /**
     * 自动填充剪贴板内容
     * 检测剪贴板是否包含 URL，自动填充到下载链接输入框
     */
    private void autoFillFromClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
            CharSequence clipText = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            if (clipText != null) {
                String text = clipText.toString().trim();
                // 检查是否是有效的 URL
                if (isValidUrl(text) && editUrl.getText().toString().isEmpty()) {
                    editUrl.setText(text);
                    // 自动提取文件名
                    autoExtractFileName(text);
                }
            }
        }
    }

    /**
     * 自动从 URL 提取文件名
     *
     * @param url 下载链接
     */
    private void autoExtractFileName(String url) {
        if (TextUtils.isEmpty(url) || !TextUtils.isEmpty(editFileName.getText().toString())) {
            return;
        }

        try {
            // 从 URL 中提取文件名
            String fileName = extractFileNameFromUrl(url);
            if (!TextUtils.isEmpty(fileName)) {
                editFileName.setText(fileName);
            }
        } catch (Exception e) {
            // 提取失败，忽略
        }
    }

    /**
     * 从 URL 提取文件名
     *
     * @param url 下载链接
     * @return 文件名
     */
    private String extractFileNameFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }

        // 获取最后一个路径段
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
            String lastSegment = url.substring(lastSlashIndex + 1);

            // 检查是否包含文件扩展名
            if (lastSegment.contains(".")) {
                return lastSegment;
            }
        }

        // 无法提取，返回默认文件名
        return "download_" + System.currentTimeMillis();
    }

    /**
     * 验证 URL 是否有效
     *
     * @param url 待验证的 URL
     * @return 是否有效
     */
    private boolean isValidUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        // 简单检查 URL 格式
        return url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("ftp://") || url.startsWith("magnet:");
    }

    /**
     * 创建下载任务
     */
    private void createTask() {
        // 获取输入值
        String url = editUrl.getText().toString().trim();
        String fileName = editFileName.getText().toString().trim();
        String savePath = editSavePath.getText().toString().trim();
        int threadCount = (int) sliderThreadCount.getValue();
        long speedLimit = 0;

        // 验证输入
        if (!validateInput(url, fileName, savePath)) {
            return;
        }

        // 获取速度限制
        if (switchSpeedLimit.isChecked()) {
            String speedLimitStr = editSpeedLimit.getText().toString().trim();
            if (!TextUtils.isEmpty(speedLimitStr)) {
                try {
                    // 输入值单位为 KB/s，转换为字节/秒
                    speedLimit = Long.parseLong(speedLimitStr) * 1024;
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "速度限制格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // 创建任务对象
        DownloadTask task = new DownloadTask();
        task.setId(generateTaskId());
        task.setUrl(url);
        task.setFileName(fileName);
        task.setSavePath(savePath);
        task.setThreadCount(threadCount);
        task.setSpeedLimit(speedLimit);
        task.setStatus(TaskStatus.QUEUED);
        task.setPriority(Priority.NORMAL);

        // 回调任务创建监听器
        if (taskCreatedListener != null) {
            taskCreatedListener.onTaskCreated(task);
        }

        // 关闭对话框
        dismiss();

        // 显示成功提示
        Toast.makeText(requireContext(), "任务已创建", Toast.LENGTH_SHORT).show();
    }

    /**
     * 验证输入值
     *
     * @param url      下载链接
     * @param fileName 文件名
     * @param savePath 保存路径
     * @return 是否验证通过
     */
    private boolean validateInput(String url, String fileName, String savePath) {
        // 验证 URL
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(requireContext(), "请输入下载链接", Toast.LENGTH_SHORT).show();
            editUrl.requestFocus();
            return false;
        }

        if (!isValidUrl(url)) {
            Toast.makeText(requireContext(), "下载链接格式不正确", Toast.LENGTH_SHORT).show();
            editUrl.requestFocus();
            return false;
        }

        // 验证文件名
        if (TextUtils.isEmpty(fileName)) {
            // 自动提取文件名
            fileName = extractFileNameFromUrl(url);
            if (TextUtils.isEmpty(fileName)) {
                Toast.makeText(requireContext(), "请输入文件名", Toast.LENGTH_SHORT).show();
                editFileName.requestFocus();
                return false;
            }
            editFileName.setText(fileName);
        }

        // 验证保存路径
        if (TextUtils.isEmpty(savePath)) {
            Toast.makeText(requireContext(), "请输入保存路径", Toast.LENGTH_SHORT).show();
            editSavePath.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * 生成任务 ID
     *
     * @return 任务 ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取默认下载路径
     *
     * @return 默认下载路径
     */
    private String getDefaultDownloadPath() {
        // 使用外部存储的 Downloads 目录
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }
        // 使用应用私有目录
        return requireContext().getExternalFilesDir(
                android.os.Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    /**
     * 任务创建监听器接口
     */
    public interface OnTaskCreatedListener {
        /**
         * 任务创建回调
         *
         * @param task 创建的任务对象
         */
        void onTaskCreated(DownloadTask task);
    }
}