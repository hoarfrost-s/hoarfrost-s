package com.hyperfetch.classify;

import com.hyperfetch.model.Category;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 分类服务类
 * 实现双阶段识别：先用 URL 后缀快速命中，未命中则发起 HEAD 请求读取 Content-Type
 */
public class CategoryService {

    /**
     * 后缀映射表
     */
    private final Map<String, Category> suffixMap;

    /**
     * 分类配置
     */
    private final CategoryConfig config;

    /**
     * 构造函数 - 使用默认配置
     */
    public CategoryService() {
        this(new CategoryConfig());
    }

    /**
     * 构造函数 - 使用自定义配置
     *
     * @param config 分类配置
     */
    public CategoryService(CategoryConfig config) {
        this.config = config;
        this.suffixMap = SuffixMapping.createSuffixMap(config);
    }

    /**
     * 分类方法 - 对 URL 进行分类识别
     * 双阶段识别：先用 URL 后缀快速命中，未命中则发起 HEAD 请求读取 Content-Type
     *
     * @param url 待分类的 URL
     * @return 分类结果
     */
    public Category classify(String url) {
        if (url == null || url.isEmpty()) {
            return Category.OTHER;
        }

        // 第一阶段：通过后缀识别
        Category category = classifyBySuffix(extractSuffix(url));
        if (category != Category.OTHER) {
            return category;
        }

        // 第二阶段：通过 Content-Type 识别
        return classifyByContentType(fetchContentType(url));
    }

    /**
     * 从 URL 中提取文件后缀
     *
     * @param url URL 地址
     * @return 文件后缀（小写，不含点），如果没有后缀则返回空字符串
     */
    private String extractSuffix(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        // 移除查询参数和锚点
        int queryIndex = url.indexOf('?');
        int anchorIndex = url.indexOf('#');
        int cutIndex = url.length();

        if (queryIndex > 0) {
            cutIndex = Math.min(cutIndex, queryIndex);
        }
        if (anchorIndex > 0) {
            cutIndex = Math.min(cutIndex, anchorIndex);
        }

        String cleanUrl = url.substring(0, cutIndex);

        // 查找最后一个点
        int dotIndex = cleanUrl.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < cleanUrl.length() - 1) {
            return cleanUrl.substring(dotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 通过后缀识别分类
     *
     * @param suffix 文件后缀
     * @return 分类结果
     */
    private Category classifyBySuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return Category.OTHER;
        }
        return suffixMap.getOrDefault(suffix.toLowerCase(), Category.OTHER);
    }

    /**
     * 通过 Content-Type 识别分类
     *
     * @param contentType MIME 类型
     * @return 分类结果
     */
    private Category classifyByContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return Category.OTHER;
        }
        return ContentTypeMapping.mapContentType(contentType);
    }

    /**
     * 发起 HEAD 请求获取 Content-Type
     *
     * @param url URL 地址
     * @return Content-Type 字符串，获取失败返回 null
     */
    private String fetchContentType(String url) {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = connection.getContentType();
                // 移除可能的字符集参数，如 "video/mp4; charset=utf-8"
                if (contentType != null) {
                    int semicolonIndex = contentType.indexOf(';');
                    if (semicolonIndex > 0) {
                        contentType = contentType.substring(0, semicolonIndex).trim();
                    }
                }
                return contentType;
            }
        } catch (IOException e) {
            // 网络请求失败，返回 null
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * 获取后缀映射表
     *
     * @return 后缀映射表的副本
     */
    public Map<String, Category> getSuffixMap() {
        return new java.util.HashMap<>(suffixMap);
    }

    /**
     * 获取当前配置
     *
     * @return 分类配置
     */
    public CategoryConfig getConfig() {
        return config;
    }
}