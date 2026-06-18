package com.hyperfetch.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TS 分片合并器
 * 用于将多个 .ts 文件合并为单个 .mp4 文件
 * 支持使用 FFmpeg 进行转换，失败时保留原始 .ts 文件
 */
public class TsMerger {

    /**
     * 合并结果
     */
    public static class MergeResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 输出文件路径
         */
        private String outputPath;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 是否保留了原始 TS 文件
         */
        private boolean tsFilesRetained;

        public MergeResult() {
        }

        public MergeResult(boolean success, String outputPath, String errorMessage, boolean tsFilesRetained) {
            this.success = success;
            this.outputPath = outputPath;
            this.errorMessage = errorMessage;
            this.tsFilesRetained = tsFilesRetained;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isTsFilesRetained() {
            return tsFilesRetained;
        }

        public void setTsFilesRetained(boolean tsFilesRetained) {
            this.tsFilesRetained = tsFilesRetained;
        }
    }

    /**
     * 合并进度回调接口
     */
    public interface MergeCallback {
        /**
         * 进度更新
         *
         * @param progress 进度百分比 (0-100)
         */
        void onProgress(int progress);

        /**
         * 合并完成
         *
         * @param result 合并结果
         */
        void onComplete(MergeResult result);
    }

    /**
     * 合并监听器
     */
    private MergeCallback callback;

    /**
     * 是否取消合并
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * FFmpeg 可执行文件路径（可选，用于外部 FFmpeg）
     */
    private String ffmpegPath;

    /**
     * 是否使用 FFmpeg 进行转换
     */
    private boolean useFFmpeg = true;

    /**
     * 默认构造函数
     */
    public TsMerger() {
    }

    /**
     * 带回调的构造函数
     *
     * @param callback 合并回调
     */
    public TsMerger(MergeCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置合并回调
     *
     * @param callback 合并回调
     */
    public void setCallback(MergeCallback callback) {
        this.callback = callback;
    }

    /**
     * 设置 FFmpeg 可执行文件路径
     *
     * @param ffmpegPath FFmpeg 路径
     */
    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    /**
     * 设置是否使用 FFmpeg
     *
     * @param useFFmpeg 是否使用 FFmpeg
     */
    public void setUseFFmpeg(boolean useFFmpeg) {
        this.useFFmpeg = useFFmpeg;
    }

    /**
     * 取消合并
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 合并 TS 文件列表为 MP4 文件
     *
     * @param tsFiles    TS 文件列表（按顺序）
     * @param outputPath 输出 MP4 文件路径
     * @return 合并结果
     */
    public MergeResult mergeToMp4(List<File> tsFiles, String outputPath) {
        if (tsFiles == null || tsFiles.isEmpty()) {
            return new MergeResult(false, null, "TS 文件列表为空", false);
        }

        // 检查所有 TS 文件是否存在
        for (File tsFile : tsFiles) {
            if (!tsFile.exists()) {
                return new MergeResult(false, null, "TS 文件不存在: " + tsFile.getAbsolutePath(), false);
            }
        }

        // 对文件进行排序（按文件名或序号）
        List<File> sortedFiles = new ArrayList<>(tsFiles);
        Collections.sort(sortedFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                // 尝试按数字序号排序
                try {
                    int n1 = extractNumber(f1.getName());
                    int n2 = extractNumber(f2.getName());
                    return Integer.compare(n1, n2);
                } catch (Exception e) {
                    // 按文件名排序
                    return f1.getName().compareTo(f2.getName());
                }
            }
        });

        // 尝试使用 FFmpeg 合并
        if (useFFmpeg && isFFmpegAvailable()) {
            MergeResult result = mergeWithFFmpeg(sortedFiles, outputPath);
            if (result.isSuccess()) {
                return result;
            }
            // FFmpeg 失败，尝试简单合并
        }

        // 简单合并（直接拼接）
        MergeResult result = mergeSimple(sortedFiles, outputPath);

        // 如果简单合并成功，尝试重命名为 mp4
        if (result.isSuccess()) {
            File outputFile = new File(result.getOutputPath());
            File mp4File = new File(outputPath);
            if (outputFile.renameTo(mp4File)) {
                result.setOutputPath(mp4File.getAbsolutePath());
            }
            // 注意：简单合并的 TS 文件可能不是标准的 MP4 格式
            // 但某些播放器可以播放
        }

        return result;
    }

    /**
     * 使用 FFmpeg 合并 TS 文件为 MP4
     *
     * @param tsFiles    TS 文件列表
     * @param outputPath 输出文件路径
     * @return 合并结果
     */
    private MergeResult mergeWithFFmpeg(List<File> tsFiles, String outputPath) {
        try {
            // 创建临时文件列表
            File listFile = createFileList(tsFiles);
            if (listFile == null) {
                return new MergeResult(false, null, "无法创建文件列表", false);
            }

            // 构建 FFmpeg 命令
            String ffmpeg = ffmpegPath != null ? ffmpegPath : "ffmpeg";
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpeg,
                    "-f", "concat",
                    "-safe", "0",
                    "-i", listFile.getAbsolutePath(),
                    "-c", "copy",
                    "-bsf:a", "aac_adtstoasc",
                    "-y",  // 覆盖输出文件
                    outputPath
            );

            pb.redirectErrorStream(true);

            // 启动进程
            Process process = pb.start();

            // 读取输出（避免缓冲区满导致阻塞）
            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = process.getInputStream().read(buffer)) != -1) {
                if (cancelled.get()) {
                    process.destroy();
                    listFile.delete();
                    return new MergeResult(false, null, "合并已取消", false);
                }
                output.append(new String(buffer, 0, len));
            }

            // 等待进程结束
            int exitCode = process.waitFor();

            // 删除临时文件列表
            listFile.delete();

            if (exitCode == 0) {
                File outputFile = new File(outputPath);
                if (outputFile.exists() && outputFile.length() > 0) {
                    // 合并成功，删除原始 TS 文件
                    deleteTsFiles(tsFiles);
                    return new MergeResult(true, outputPath, null, false);
                } else {
                    return new MergeResult(false, null, "输出文件无效", false);
                }
            } else {
                return new MergeResult(false, null, "FFmpeg 执行失败: " + output.toString(), false);
            }
        } catch (IOException | InterruptedException e) {
            return new MergeResult(false, null, "FFmpeg 合并异常: " + e.getMessage(), false);
        }
    }

    /**
     * 简单合并 TS 文件（直接拼接）
     *
     * @param tsFiles    TS 文件列表
     * @param outputPath 输出文件路径
     * @return 合并结果
     */
    private MergeResult mergeSimple(List<File> tsFiles, String outputPath) {
        // 输出为 .ts 文件（简单拼接）
        String tsOutputPath = outputPath.replace(".mp4", ".ts");
        if (tsOutputPath.equals(outputPath)) {
            tsOutputPath = outputPath + ".ts";
        }

        try (FileOutputStream fos = new FileOutputStream(tsOutputPath)) {
            long totalSize = 0;
            for (File f : tsFiles) {
                totalSize += f.length();
            }

            long copied = 0;
            byte[] buffer = new byte[64 * 1024]; // 64KB 缓冲区

            for (int i = 0; i < tsFiles.size(); i++) {
                if (cancelled.get()) {
                    new File(tsOutputPath).delete();
                    return new MergeResult(false, null, "合并已取消", false);
                }

                File tsFile = tsFiles.get(i);
                try (FileInputStream fis = new FileInputStream(tsFile)) {
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        copied += len;

                        // 更新进度
                        if (callback != null) {
                            int progress = (int) (copied * 100 / totalSize);
                            callback.onProgress(progress);
                        }
                    }
                }
            }

            fos.flush();

            // 合并成功，保留原始 TS 文件（因为简单合并可能不兼容）
            // 用户可以选择手动删除
            return new MergeResult(true, tsOutputPath, null, true);

        } catch (IOException e) {
            return new MergeResult(false, null, "简单合并失败: " + e.getMessage(), false);
        }
    }

    /**
     * 创建 FFmpeg 文件列表
     *
     * @param tsFiles TS 文件列表
     * @return 文件列表文件
     */
    private File createFileList(List<File> tsFiles) {
        try {
            File listFile = File.createTempFile("ts_merge_list_", ".txt");
            try (FileOutputStream fos = new FileOutputStream(listFile)) {
                for (File tsFile : tsFiles) {
                    // FFmpeg concat 格式: file 'path'
                    String line = "file '" + tsFile.getAbsolutePath() + "'\n";
                    fos.write(line.getBytes("UTF-8"));
                }
            }
            return listFile;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 检查 FFmpeg 是否可用
     *
     * @return 是否可用
     */
    private boolean isFFmpegAvailable() {
        // 如果指定了 FFmpeg 路径，检查是否存在
        if (ffmpegPath != null && !ffmpegPath.isEmpty()) {
            File ffmpegFile = new File(ffmpegPath);
            return ffmpegFile.exists() && ffmpegFile.canExecute();
        }

        // 检查系统 PATH 中是否有 ffmpeg
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从文件名中提取数字序号
     *
     * @param filename 文件名
     * @return 数字序号
     */
    private int extractNumber(String filename) {
        // 移除扩展名
        String name = filename.replaceAll("\\.[^.]+$", "");

        // 提取数字
        StringBuilder numStr = new StringBuilder();
        for (int i = name.length() - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (Character.isDigit(c)) {
                numStr.insert(0, c);
            } else if (numStr.length() > 0) {
                break;
            }
        }

        if (numStr.length() > 0) {
            return Integer.parseInt(numStr.toString());
        }
        return 0;
    }

    /**
     * 删除 TS 文件
     *
     * @param tsFiles TS 文件列表
     */
    private void deleteTsFiles(List<File> tsFiles) {
        for (File tsFile : tsFiles) {
            try {
                if (tsFile.exists()) {
                    tsFile.delete();
                }
            } catch (Exception e) {
                // 忽略删除失败
            }
        }
    }

    /**
     * 合并 TS 文件（异步）
     *
     * @param tsFiles    TS 文件列表
     * @param outputPath 输出文件路径
     * @param callback   回调
     */
    public void mergeToMp4Async(final List<File> tsFiles, final String outputPath, final MergeCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MergeResult result = mergeToMp4(tsFiles, outputPath);
                if (callback != null) {
                    callback.onComplete(result);
                }
            }
        }).start();
    }

    /**
     * 仅合并 TS 文件为单个 TS 文件（不转换格式）
     *
     * @param tsFiles    TS 文件列表
     * @param outputPath 输出 TS 文件路径
     * @return 合并结果
     */
    public MergeResult mergeToTs(List<File> tsFiles, String outputPath) {
        // 临时禁用 FFmpeg
        boolean originalUseFFmpeg = this.useFFmpeg;
        this.useFFmpeg = false;

        MergeResult result = mergeSimple(tsFiles, outputPath);

        this.useFFmpeg = originalUseFFmpeg;
        return result;
    }
}