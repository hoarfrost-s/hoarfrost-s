package com.hyperfetch.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M3U8 文件解析器
 * 用于解析 m3u8 播放列表文件，提取分片 URL、时长和加密信息
 * 支持主播放列表（Master Playlist）和媒体播放列表（Media Playlist）
 */
public class M3u8Parser {

    /**
     * M3U8 分片信息
     */
    public static class Segment {
        /**
         * 分片 URL
         */
        private String url;

        /**
         * 分片时长（秒）
         */
        private double duration;

        /**
         * 分片序号
         */
        private int sequence;

        public Segment() {
        }

        public Segment(String url, double duration, int sequence) {
            this.url = url;
            this.duration = duration;
            this.sequence = sequence;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public double getDuration() {
            return duration;
        }

        public void setDuration(double duration) {
            this.duration = duration;
        }

        public int getSequence() {
            return sequence;
        }

        public void setSequence(int sequence) {
            this.sequence = sequence;
        }
    }

    /**
     * 子流信息（用于主播放列表）
     */
    public static class VariantStream {
        /**
         * 子流 URL
         */
        private String url;

        /**
         * 带宽（bps）
         */
        private int bandwidth;

        /**
         * 分辨率
         */
        private String resolution;

        /**
         * 编码格式
         */
        private String codecs;

        public VariantStream() {
        }

        public VariantStream(String url, int bandwidth, String resolution, String codecs) {
            this.url = url;
            this.bandwidth = bandwidth;
            this.resolution = resolution;
            this.codecs = codecs;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(int bandwidth) {
            this.bandwidth = bandwidth;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getCodecs() {
            return codecs;
        }

        public void setCodecs(String codecs) {
            this.codecs = codecs;
        }
    }

    /**
     * 加密信息
     */
    public static class EncryptionInfo {
        /**
         * 加密方法（如 AES-128）
         */
        private String method;

        /**
         * 密钥 URI
         */
        private String keyUri;

        /**
         * IV 向量
         */
        private String iv;

        public EncryptionInfo() {
        }

        public EncryptionInfo(String method, String keyUri, String iv) {
            this.method = method;
            this.keyUri = keyUri;
            this.iv = iv;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getKeyUri() {
            return keyUri;
        }

        public void setKeyUri(String keyUri) {
            this.keyUri = keyUri;
        }

        public String getIv() {
            return iv;
        }

        public void setIv(String iv) {
            this.iv = iv;
        }

        /**
         * 是否加密
         */
        public boolean isEncrypted() {
            return method != null && !method.equals("NONE");
        }
    }

    /**
     * M3U8 解析结果
     */
    public static class ParseResult {
        /**
         * 是否为主播放列表
         */
        private boolean isMasterPlaylist;

        /**
         * 分片列表（媒体播放列表时使用）
         */
        private List<Segment> segments;

        /**
         * 子流列表（主播放列表时使用）
         */
        private List<VariantStream> variantStreams;

        /**
         * 加密信息
         */
        private EncryptionInfo encryptionInfo;

        /**
         * 目标时长（秒）
         */
        private double targetDuration;

        /**
         * 总时长（秒）
         */
        private double totalDuration;

        /**
         * 媒体序列号起始值
         */
        private int mediaSequence;

        /**
         * 是否为直播流
         */
        private boolean isLive;

        public ParseResult() {
            this.segments = new ArrayList<>();
            this.variantStreams = new ArrayList<>();
            this.isLive = false;
        }

        public boolean isMasterPlaylist() {
            return isMasterPlaylist;
        }

        public void setMasterPlaylist(boolean masterPlaylist) {
            isMasterPlaylist = masterPlaylist;
        }

        public List<Segment> getSegments() {
            return segments;
        }

        public void setSegments(List<Segment> segments) {
            this.segments = segments;
        }

        public List<VariantStream> getVariantStreams() {
            return variantStreams;
        }

        public void setVariantStreams(List<VariantStream> variantStreams) {
            this.variantStreams = variantStreams;
        }

        public EncryptionInfo getEncryptionInfo() {
            return encryptionInfo;
        }

        public void setEncryptionInfo(EncryptionInfo encryptionInfo) {
            this.encryptionInfo = encryptionInfo;
        }

        public double getTargetDuration() {
            return targetDuration;
        }

        public void setTargetDuration(double targetDuration) {
            this.targetDuration = targetDuration;
        }

        public double getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(double totalDuration) {
            this.totalDuration = totalDuration;
        }

        public int getMediaSequence() {
            return mediaSequence;
        }

        public void setMediaSequence(int mediaSequence) {
            this.mediaSequence = mediaSequence;
        }

        public boolean isLive() {
            return isLive;
        }

        public void setLive(boolean live) {
            isLive = live;
        }

        /**
         * 计算总时长（基于分片时长累加）
         */
        public void calculateTotalDuration() {
            totalDuration = 0;
            for (Segment segment : segments) {
                totalDuration += segment.getDuration();
            }
        }
    }

    // 正则表达式模式
    private static final Pattern EXT_X_TARGET_DURATION = Pattern.compile("#EXT-X-TARGETDURATION:(\\d+)");
    private static final Pattern EXT_X_MEDIA_SEQUENCE = Pattern.compile("#EXT-X-MEDIA-SEQUENCE:(\\d+)");
    private static final Pattern EXTINF = Pattern.compile("#EXTINF:([\\d.]+)");
    private static final Pattern EXT_X_STREAM_INF = Pattern.compile("#EXT-X-STREAM-INF:.*BANDWIDTH=(\\d+)");
    private static final Pattern EXT_X_KEY = Pattern.compile("#EXT-X-KEY:METHOD=([^,]+),URI=\"([^\"]+)\"(?:,IV=([^,]+))?");
    private static final Pattern RESOLUTION = Pattern.compile("RESOLUTION=(\\d+x\\d+)");
    private static final Pattern CODECS = Pattern.compile("CODECS=\"([^\"]+)\"");

    /**
     * 解析 M3U8 文件内容
     *
     * @param content  M3U8 文件内容
     * @param baseUrl  基础 URL（用于解析相对路径）
     * @return 解析结果
     */
    public ParseResult parse(String content, String baseUrl) {
        ParseResult result = new ParseResult();

        if (content == null || content.isEmpty()) {
            return result;
        }

        // 检查是否为有效的 M3U8 文件
        if (!content.startsWith("#EXTM3U")) {
            return result;
        }

        // 检查是否为主播放列表
        result.setMasterPlaylist(content.contains("#EXT-X-STREAM-INF"));

        String[] lines = content.split("\n");
        int sequence = 0;

        if (result.isMasterPlaylist()) {
            // 解析主播放列表
            parseMasterPlaylist(lines, baseUrl, result);
        } else {
            // 解析媒体播放列表
            parseMediaPlaylist(lines, baseUrl, result, sequence);
        }

        // 计算总时长
        result.calculateTotalDuration();

        return result;
    }

    /**
     * 解析主播放列表（Master Playlist）
     *
     * @param lines   文件行数组
     * @param baseUrl 基础 URL
     * @param result  解析结果
     */
    private void parseMasterPlaylist(String[] lines, String baseUrl, ParseResult result) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 解析子流信息
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                VariantStream variant = parseVariantStream(line, lines, i, baseUrl);
                if (variant != null) {
                    result.getVariantStreams().add(variant);
                }
            }
        }
    }

    /**
     * 解析子流信息
     *
     * @param tagLine 标签行
     * @param lines   所有行
     * @param index   当前行索引
     * @param baseUrl 基础 URL
     * @return 子流信息
     */
    private VariantStream parseVariantStream(String tagLine, String[] lines, int index, String baseUrl) {
        try {
            // 解析带宽
            Matcher bandwidthMatcher = EXT_X_STREAM_INF.matcher(tagLine);
            int bandwidth = 0;
            if (bandwidthMatcher.find()) {
                bandwidth = Integer.parseInt(bandwidthMatcher.group(1));
            }

            // 解析分辨率
            String resolution = null;
            Matcher resolutionMatcher = RESOLUTION.matcher(tagLine);
            if (resolutionMatcher.find()) {
                resolution = resolutionMatcher.group(1);
            }

            // 解析编码格式
            String codecs = null;
            Matcher codecsMatcher = CODECS.matcher(tagLine);
            if (codecsMatcher.find()) {
                codecs = codecsMatcher.group(1);
            }

            // 获取下一行的 URL
            String url = null;
            if (index + 1 < lines.length) {
                String nextLine = lines[index + 1].trim();
                if (!nextLine.startsWith("#") && !nextLine.isEmpty()) {
                    url = resolveUrl(nextLine, baseUrl);
                }
            }

            if (url != null) {
                return new VariantStream(url, bandwidth, resolution, codecs);
            }
        } catch (Exception e) {
            // 解析失败，返回 null
        }
        return null;
    }

    /**
     * 解析媒体播放列表（Media Playlist）
     *
     * @param lines    文件行数组
     * @param baseUrl  基础 URL
     * @param result   解析结果
     * @param sequence 分片序号
     */
    private void parseMediaPlaylist(String[] lines, String baseUrl, ParseResult result, int sequence) {
        double currentDuration = 0;
        EncryptionInfo currentEncryption = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 解析目标时长
            Matcher targetDurationMatcher = EXT_X_TARGET_DURATION.matcher(line);
            if (targetDurationMatcher.find()) {
                result.setTargetDuration(Double.parseDouble(targetDurationMatcher.group(1)));
                continue;
            }

            // 解析媒体序列号
            Matcher mediaSequenceMatcher = EXT_X_MEDIA_SEQUENCE.matcher(line);
            if (mediaSequenceMatcher.find()) {
                result.setMediaSequence(Integer.parseInt(mediaSequenceMatcher.group(1)));
                sequence = result.getMediaSequence();
                continue;
            }

            // 解析加密信息
            if (line.startsWith("#EXT-X-KEY:")) {
                currentEncryption = parseEncryptionInfo(line);
                result.setEncryptionInfo(currentEncryption);
                continue;
            }

            // 解析分片时长
            if (line.startsWith("#EXTINF:")) {
                Matcher extinfMatcher = EXTINF.matcher(line);
                if (extinfMatcher.find()) {
                    currentDuration = Double.parseDouble(extinfMatcher.group(1));
                }
                continue;
            }

            // 检查是否为直播流
            if (line.equals("#EXT-X-PLAYLIST-TYPE:EVENT") || !contentContainsEndList(lines)) {
                result.setLive(true);
            }

            // 检查是否为结束标记
            if (line.equals("#EXT-X-ENDLIST")) {
                result.setLive(false);
                continue;
            }

            // 解析分片 URL
            if (!line.startsWith("#") && !line.isEmpty()) {
                String segmentUrl = resolveUrl(line, baseUrl);
                Segment segment = new Segment(segmentUrl, currentDuration, sequence++);
                result.getSegments().add(segment);
                currentDuration = 0; // 重置时长
            }
        }
    }

    /**
     * 解析加密信息
     *
     * @param line 加密标签行
     * @return 加密信息
     */
    private EncryptionInfo parseEncryptionInfo(String line) {
        try {
            Matcher matcher = EXT_X_KEY.matcher(line);
            if (matcher.find()) {
                String method = matcher.group(1);
                String keyUri = matcher.group(2);
                String iv = matcher.group(3);
                return new EncryptionInfo(method, keyUri, iv);
            }
        } catch (Exception e) {
            // 解析失败，返回 null
        }
        return null;
    }

    /**
     * 检查内容是否包含结束标记
     *
     * @param lines 文件行数组
     * @return 是否包含结束标记
     */
    private boolean contentContainsEndList(String[] lines) {
        for (String line : lines) {
            if (line.trim().equals("#EXT-X-ENDLIST")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析相对 URL 为绝对 URL
     *
     * @param url     相对或绝对 URL
     * @param baseUrl 基础 URL
     * @return 绝对 URL
     */
    public String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 已经是绝对 URL
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // 处理相对 URL
        if (baseUrl == null || baseUrl.isEmpty()) {
            return url;
        }

        try {
            // 获取基础 URL 的目录部分
            int lastSlashIndex = baseUrl.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                String baseDir = baseUrl.substring(0, lastSlashIndex + 1);

                // 处理以 ./ 开头的相对路径
                if (url.startsWith("./")) {
                    return baseDir + url.substring(2);
                }

                // 处理以 ../ 开头的相对路径
                if (url.startsWith("../")) {
                    // 简单处理：去掉上一级目录
                    int secondLastSlash = baseDir.lastIndexOf('/', baseDir.length() - 2);
                    if (secondLastSlash > 0) {
                        return baseDir.substring(0, secondLastSlash + 1) + url.substring(3);
                    }
                }

                // 普通相对路径
                return baseDir + url;
            }
        } catch (Exception e) {
            // 解析失败，返回原始 URL
        }

        return url;
    }

    /**
     * 选择最佳子流（选择带宽最高的）
     *
     * @param variantStreams 子流列表
     * @return 最佳子流
     */
    public VariantStream selectBestVariantStream(List<VariantStream> variantStreams) {
        if (variantStreams == null || variantStreams.isEmpty()) {
            return null;
        }

        VariantStream best = variantStreams.get(0);
        for (VariantStream variant : variantStreams) {
            if (variant.getBandwidth() > best.getBandwidth()) {
                best = variant;
            }
        }
        return best;
    }

    /**
     * 选择指定带宽范围的子流
     *
     * @param variantStreams 子流列表
     * @param minBandwidth   最小带宽
     * @param maxBandwidth   最大带宽
     * @return 符合条件的最佳子流
     */
    public VariantStream selectVariantStreamByBandwidth(List<VariantStream> variantStreams,
                                                         int minBandwidth, int maxBandwidth) {
        if (variantStreams == null || variantStreams.isEmpty()) {
            return null;
        }

        VariantStream best = null;
        for (VariantStream variant : variantStreams) {
            int bandwidth = variant.getBandwidth();
            if (bandwidth >= minBandwidth && bandwidth <= maxBandwidth) {
                if (best == null || bandwidth > best.getBandwidth()) {
                    best = variant;
                }
            }
        }

        // 如果没有找到符合条件的，返回带宽最接近的
        if (best == null) {
            best = variantStreams.get(0);
            int minDiff = Math.abs(best.getBandwidth() - maxBandwidth);
            for (VariantStream variant : variantStreams) {
                int diff = Math.abs(variant.getBandwidth() - maxBandwidth);
                if (diff < minDiff) {
                    minDiff = diff;
                    best = variant;
                }
            }
        }

        return best;
    }
}