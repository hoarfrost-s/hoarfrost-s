package com.hyperfetch.model;

/**
 * 资源元数据类
 * 用于存储下载资源的元信息
 */
public class ResourceMeta {

    /**
     * 资源总大小（字节）
     */
    private long totalSize;

    /**
     * 是否支持断点续传（Range请求）
     */
    private boolean supportRange;

    /**
     * 内容类型（MIME类型）
     */
    private String contentType;

    /**
     * 默认构造函数
     */
    public ResourceMeta() {
    }

    /**
     * 带参数的构造函数
     *
     * @param totalSize    资源总大小
     * @param supportRange 是否支持断点续传
     * @param contentType  内容类型
     */
    public ResourceMeta(long totalSize, boolean supportRange, String contentType) {
        this.totalSize = totalSize;
        this.supportRange = supportRange;
        this.contentType = contentType;
    }

    // Getter 和 Setter 方法

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public boolean isSupportRange() {
        return supportRange;
    }

    public void setSupportRange(boolean supportRange) {
        this.supportRange = supportRange;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取格式化的文件大小字符串
     *
     * @return 格式化的大小字符串（如：1.5 GB）
     */
    public String getFormattedSize() {
        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.2f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", totalSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
        }
    }
}