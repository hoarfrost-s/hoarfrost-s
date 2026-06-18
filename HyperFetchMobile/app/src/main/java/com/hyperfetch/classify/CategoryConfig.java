package com.hyperfetch.classify;

import com.hyperfetch.model.Category;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 分类配置类
 * 支持通过配置文件扩展后缀映射
 */
public class CategoryConfig {

    /**
     * 默认连接超时时间（毫秒）
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /**
     * 默认读取超时时间（毫秒）
     */
    private static final int DEFAULT_READ_TIMEOUT = 5000;

    /**
     * 连接超时时间
     */
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /**
     * 读取超时时间
     */
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    /**
     * 自定义后缀映射
     */
    private Map<String, Category> customSuffixMappings;

    /**
     * 默认构造函数
     */
    public CategoryConfig() {
        this.customSuffixMappings = new HashMap<>();
    }

    /**
     * 从 JSON 字符串创建配置
     *
     * @param json JSON 配置字符串
     * @return 分类配置对象
     * @throws JSONException JSON 解析异常
     */
    public static CategoryConfig fromJson(String json) throws JSONException {
        CategoryConfig config = new CategoryConfig();
        JSONObject jsonObject = new JSONObject(json);

        // 解析超时配置
        if (jsonObject.has("connectTimeout")) {
            config.connectTimeout = jsonObject.getInt("connectTimeout");
        }
        if (jsonObject.has("readTimeout")) {
            config.readTimeout = jsonObject.getInt("readTimeout");
        }

        // 解析自定义后缀映射
        if (jsonObject.has("customSuffixMappings")) {
            JSONObject mappings = jsonObject.getJSONObject("customSuffixMappings");
            config.customSuffixMappings = parseSuffixMappings(mappings);
        }

        return config;
    }

    /**
     * 解析后缀映射 JSON 对象
     *
     * @param mappingsObj JSON 对象
     * @return 后缀映射表
     * @throws JSONException JSON 解析异常
     */
    private static Map<String, Category> parseSuffixMappings(JSONObject mappingsObj) throws JSONException {
        Map<String, Category> mappings = new HashMap<>();

        // 遍历每个分类
        JSONArray categories = mappingsObj.names();
        if (categories != null) {
            for (int i = 0; i < categories.length(); i++) {
                String categoryName = categories.getString(i);
                Category category;
                try {
                    category = Category.valueOf(categoryName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // 忽略无效的分类名称
                    continue;
                }

                JSONArray suffixes = mappingsObj.getJSONArray(categoryName);
                for (int j = 0; j < suffixes.length(); j++) {
                    String suffix = suffixes.getString(j).toLowerCase();
                    mappings.put(suffix, category);
                }
            }
        }

        return mappings;
    }

    /**
     * 转换为 JSON 字符串
     *
     * @return JSON 配置字符串
     */
    public String toJson() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("connectTimeout", connectTimeout);
            jsonObject.put("readTimeout", readTimeout);

            // 添加自定义后缀映射
            JSONObject mappingsObj = new JSONObject();
            Map<String, JSONArray> categorySuffixes = new HashMap<>();

            for (Map.Entry<String, Category> entry : customSuffixMappings.entrySet()) {
                String categoryName = entry.getValue().name();
                if (!categorySuffixes.containsKey(categoryName)) {
                    categorySuffixes.put(categoryName, new JSONArray());
                }
                categorySuffixes.get(categoryName).put(entry.getKey());
            }

            for (Map.Entry<String, JSONArray> entry : categorySuffixes.entrySet()) {
                mappingsObj.put(entry.getKey(), entry.getValue());
            }

            jsonObject.put("customSuffixMappings", mappingsObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    /**
     * 添加自定义后缀映射
     *
     * @param suffix   文件后缀
     * @param category 分类
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig addSuffixMapping(String suffix, Category category) {
        if (suffix != null && !suffix.isEmpty() && category != null) {
            customSuffixMappings.put(suffix.toLowerCase(), category);
        }
        return this;
    }

    /**
     * 移除自定义后缀映射
     *
     * @param suffix 文件后缀
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig removeSuffixMapping(String suffix) {
        if (suffix != null) {
            customSuffixMappings.remove(suffix.toLowerCase());
        }
        return this;
    }

    /**
     * 清空所有自定义后缀映射
     *
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig clearCustomMappings() {
        customSuffixMappings.clear();
        return this;
    }

    // ==================== Getter/Setter ====================

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间（毫秒）
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置连接超时时间
     *
     * @param connectTimeout 连接超时时间（毫秒）
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 获取读取超时时间
     *
     * @return 读取超时时间（毫秒）
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 设置读取超时时间
     *
     * @param readTimeout 读取超时时间（毫秒）
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * 获取自定义后缀映射
     *
     * @return 自定义后缀映射表
     */
    public Map<String, Category> getCustomSuffixMappings() {
        return new HashMap<>(customSuffixMappings);
    }

    /**
     * 设置自定义后缀映射
     *
     * @param customSuffixMappings 自定义后缀映射表
     * @return 当前配置对象（支持链式调用）
     */
    public CategoryConfig setCustomSuffixMappings(Map<String, Category> customSuffixMappings) {
        this.customSuffixMappings = customSuffixMappings != null
                ? new HashMap<>(customSuffixMappings)
                : new HashMap<>();
        return this;
    }
}