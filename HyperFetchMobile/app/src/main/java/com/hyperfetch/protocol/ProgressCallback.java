package com.hyperfetch.protocol;

/**
 * 进度回调接口
 * 用于通知下载进度、错误和完成事件
 */
public interface ProgressCallback {

    /**
     * 进度更新回调
     *
     * @param downloaded 已下载的字节数
     * @param total      总字节数
     */
    void onProgress(long downloaded, long total);

    /**
     * 错误回调
     *
     * @param e 发生的异常
     */
    void onError(Exception e);

    /**
     * 下载完成回调
     */
    void onComplete();
}