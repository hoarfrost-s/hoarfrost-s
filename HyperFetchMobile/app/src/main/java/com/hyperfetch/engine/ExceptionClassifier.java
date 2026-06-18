package com.hyperfetch.engine;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/**
 * 异常分类器
 * 负责对异常进行分类，判断异常是否可重试以及重试策略
 */
public class ExceptionClassifier {

    private static final String TAG = "ExceptionClassifier";

    /**
     * 异常分类枚举
     */
    public enum ExceptionCategory {
        /**
         * 网络瞬断 - 可立即重试
         * 包括：SocketTimeoutException, ConnectException, UnknownHostException
         */
        TRANSIENT_NETWORK_ERROR,

        /**
         * HTTP 4xx 错误 - 不可重试
         * 客户端错误，需要修改请求
         */
        HTTP_CLIENT_ERROR,

        /**
         * HTTP 5xx 错误 - 可重试
         * 服务器错误，重置分块偏移后重试
         */
        HTTP_SERVER_ERROR,

        /**
         * HTTP 416 Range Not Satisfiable - 可重试
         * 需要重置分块偏移后重试
         */
        HTTP_RANGE_ERROR,

        /**
         * IO 异常 - 可重试
         * 通用 IO 错误
         */
        IO_ERROR,

        /**
         * SSL 异常 - 可重试
         * SSL 握手或证书相关错误
         */
        SSL_ERROR,

        /**
         * 未知错误 - 默认不可重试
         */
        UNKNOWN_ERROR
    }

    /**
     * HTTP 状态码：Range Not Satisfiable
     */
    public static final int HTTP_RANGE_NOT_SATISFIABLE = 416;

    /**
     * HTTP 4xx 范围起始
     */
    public static final int HTTP_4XX_START = 400;

    /**
     * HTTP 4xx 范围结束
     */
    public static final int HTTP_4XX_END = 499;

    /**
     * HTTP 5xx 范围起始
     */
    public static final int HTTP_5XX_START = 500;

    /**
     * HTTP 5xx 范围结束
     */
    public static final int HTTP_5XX_END = 599;

    /**
     * 对异常进行分类
     *
     * @param e 异常对象
     * @return 异常分类
     */
    public ExceptionCategory classify(Exception e) {
        if (e == null) {
            return ExceptionCategory.UNKNOWN_ERROR;
        }

        // 1. 网络瞬断异常
        if (isTransientNetworkError(e)) {
            return ExceptionCategory.TRANSIENT_NETWORK_ERROR;
        }

        // 2. SSL 异常
        if (isSSLError(e)) {
            return ExceptionCategory.SSL_ERROR;
        }

        // 3. HTTP 错误
        if (e instanceof HttpRetryException) {
            HttpRetryException httpException = (HttpRetryException) e;
            return classifyHttpError(httpException.responseCode());
        }

        // 4. HTTP 响应码异常（自定义异常类型）
        if (e instanceof HttpResponseException) {
            HttpResponseException httpException = (HttpResponseException) e;
            return classifyHttpError(httpException.getResponseCode());
        }

        // 5. IO 异常
        if (e instanceof IOException) {
            return ExceptionCategory.IO_ERROR;
        }

        // 6. 未知错误
        return ExceptionCategory.UNKNOWN_ERROR;
    }

    /**
     * 分类 HTTP 错误
     *
     * @param responseCode HTTP 响应码
     * @return 异常分类
     */
    private ExceptionCategory classifyHttpError(int responseCode) {
        // HTTP 416 Range Not Satisfiable
        if (responseCode == HTTP_RANGE_NOT_SATISFIABLE) {
            return ExceptionCategory.HTTP_RANGE_ERROR;
        }

        // HTTP 5xx 服务器错误
        if (responseCode >= HTTP_5XX_START && responseCode <= HTTP_5XX_END) {
            return ExceptionCategory.HTTP_SERVER_ERROR;
        }

        // HTTP 4xx 客户端错误
        if (responseCode >= HTTP_4XX_START && responseCode <= HTTP_4XX_END) {
            return ExceptionCategory.HTTP_CLIENT_ERROR;
        }

        return ExceptionCategory.UNKNOWN_ERROR;
    }

    /**
     * 判断异常是否可重试
     *
     * @param e 异常对象
     * @return 是否可重试
     */
    public boolean isRetryable(Exception e) {
        ExceptionCategory category = classify(e);
        switch (category) {
            case TRANSIENT_NETWORK_ERROR:
                // 网络瞬断：立即重试
                return true;
            case HTTP_CLIENT_ERROR:
                // HTTP 4xx：不可重试
                return false;
            case HTTP_SERVER_ERROR:
                // HTTP 5xx：可重试，重置分块偏移后重试
                return true;
            case HTTP_RANGE_ERROR:
                // HTTP 416：可重试，重置分块偏移后重试
                return true;
            case IO_ERROR:
                // IO 异常：可重试
                return true;
            case SSL_ERROR:
                // SSL 异常：可重试
                return true;
            case UNKNOWN_ERROR:
            default:
                return false;
        }
    }

    /**
     * 判断是否为网络瞬断异常
     * 网络瞬断包括：SocketTimeoutException, ConnectException, UnknownHostException
     *
     * @param e 异常对象
     * @return 是否为网络瞬断
     */
    public boolean isTransientNetworkError(Exception e) {
        if (e == null) {
            return false;
        }

        // Socket 超时异常
        if (e instanceof SocketTimeoutException) {
            return true;
        }

        // 连接异常
        if (e instanceof ConnectException) {
            return true;
        }

        // 未知主机异常（DNS 解析失败）
        if (e instanceof UnknownHostException) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否为 IO 异常
     *
     * @param e 异常对象
     * @return 是否为 IO 异常
     */
    public boolean isIOException(Exception e) {
        return e instanceof IOException;
    }

    /**
     * 判断是否为 SSL 异常
     *
     * @param e 异常对象
     * @return 是否为 SSL 异常
     */
    public boolean isSSLError(Exception e) {
        return e instanceof SSLException;
    }

    /**
     * 判断是否需要重置分块偏移
     *
     * @param e 异常对象
     * @return 是否需要重置分块偏移
     */
    public boolean shouldResetChunkOffset(Exception e) {
        ExceptionCategory category = classify(e);
        return category == ExceptionCategory.HTTP_SERVER_ERROR
                || category == ExceptionCategory.HTTP_RANGE_ERROR;
    }

    /**
     * 获取重试建议
     *
     * @param e 异常对象
     * @return 重试建议描述
     */
    public String getRetrySuggestion(Exception e) {
        ExceptionCategory category = classify(e);
        switch (category) {
            case TRANSIENT_NETWORK_ERROR:
                return "网络瞬断，建议立即重试";
            case HTTP_CLIENT_ERROR:
                return "客户端请求错误，不可重试，请检查请求参数";
            case HTTP_SERVER_ERROR:
                return "服务器错误，建议重置分块偏移后重试";
            case HTTP_RANGE_ERROR:
                return "分块范围无效，建议重置分块偏移后重试";
            case IO_ERROR:
                return "IO 错误，建议稍后重试";
            case SSL_ERROR:
                return "SSL 错误，建议稍后重试";
            case UNKNOWN_ERROR:
            default:
                return "未知错误，不建议重试";
        }
    }

    /**
     * HTTP 响应异常类
     * 用于封装 HTTP 响应码和错误信息
     */
    public static class HttpResponseException extends IOException {
        private final int responseCode;
        private final String responseMessage;

        /**
         * 构造函数
         *
         * @param responseCode    HTTP 响应码
         * @param responseMessage 响应消息
         */
        public HttpResponseException(int responseCode, String responseMessage) {
            super("HTTP " + responseCode + ": " + responseMessage);
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
        }

        /**
         * 获取 HTTP 响应码
         *
         * @return HTTP 响应码
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * 获取响应消息
         *
         * @return 响应消息
         */
        public String getResponseMessage() {
            return responseMessage;
        }
    }
}