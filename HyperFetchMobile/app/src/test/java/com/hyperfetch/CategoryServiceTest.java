package com.hyperfetch;

import com.hyperfetch.classify.CategoryConfig;
import com.hyperfetch.classify.CategoryService;
import com.hyperfetch.classify.ContentTypeMapping;
import com.hyperfetch.classify.SuffixMapping;
import com.hyperfetch.model.Category;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 分类服务单元测试
 * 测试后缀识别分类、MIME 类型分类、六大分类映射
 */
public class CategoryServiceTest {

    private CategoryService categoryService;

    @Before
    public void setUp() {
        // 创建分类服务实例
        categoryService = new CategoryService();
    }

    // ==================== 后缀识别分类测试 ====================

    /**
     * 测试视频文件后缀识别
     * 验证：常见视频后缀能正确识别为 VIDEO 分类
     */
    @Test
    public void testVideoSuffixRecognition() {
        // 测试常见视频后缀
        String[] videoUrls = {
            "http://example.com/video.mp4",
            "http://example.com/video.mkv",
            "http://example.com/video.avi",
            "http://example.com/video.mov",
            "http://example.com/video.wmv",
            "http://example.com/video.flv",
            "http://example.com/video.webm",
            "http://example.com/video.m4v",
            "http://example.com/video.mpeg",
            "http://example.com/video.mpg",
            "http://example.com/video.3gp",
            "http://example.com/video.ts",
            "http://example.com/stream.m3u8",
            "http://example.com/video.f4v"
        };
        
        for (String url : videoUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 VIDEO", Category.VIDEO, category);
        }
    }

    /**
     * 测试音频文件后缀识别
     * 验证：常见音频后缀能正确识别为 AUDIO 分类
     */
    @Test
    public void testAudioSuffixRecognition() {
        // 测试常见音频后缀
        String[] audioUrls = {
            "http://example.com/audio.mp3",
            "http://example.com/audio.flac",
            "http://example.com/audio.aac",
            "http://example.com/audio.wav",
            "http://example.com/audio.ogg",
            "http://example.com/audio.wma",
            "http://example.com/audio.m4a",
            "http://example.com/audio.ape",
            "http://example.com/audio.alac",
            "http://example.com/audio.aiff",
            "http://example.com/audio.mid",
            "http://example.com/audio.midi"
        };
        
        for (String url : audioUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 AUDIO", Category.AUDIO, category);
        }
    }

    /**
     * 测试压缩包文件后缀识别
     * 验证：常见压缩包后缀能正确识别为 ARCHIVE 分类
     */
    @Test
    public void testArchiveSuffixRecognition() {
        // 测试常见压缩包后缀
        String[] archiveUrls = {
            "http://example.com/file.zip",
            "http://example.com/file.rar",
            "http://example.com/file.7z",
            "http://example.com/file.tar",
            "http://example.com/file.gz",
            "http://example.com/file.bz2",
            "http://example.com/file.xz",
            "http://example.com/file.tgz",
            "http://example.com/file.tbz2",
            "http://example.com/file.iso",
            "http://example.com/file.cab",
            "http://example.com/file.arj"
        };
        
        for (String url : archiveUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 ARCHIVE", Category.ARCHIVE, category);
        }
    }

    /**
     * 测试文档文件后缀识别
     * 验证：常见文档后缀能正确识别为 DOCUMENT 分类
     */
    @Test
    public void testDocumentSuffixRecognition() {
        // 测试常见文档后缀
        String[] documentUrls = {
            "http://example.com/doc.pdf",
            "http://example.com/doc.doc",
            "http://example.com/doc.docx",
            "http://example.com/doc.xls",
            "http://example.com/doc.xlsx",
            "http://example.com/doc.ppt",
            "http://example.com/doc.pptx",
            "http://example.com/doc.txt",
            "http://example.com/doc.rtf",
            "http://example.com/doc.odt",
            "http://example.com/doc.ods",
            "http://example.com/doc.odp",
            "http://example.com/doc.epub",
            "http://example.com/doc.mobi",
            "http://example.com/doc.csv"
        };
        
        for (String url : documentUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 DOCUMENT", Category.DOCUMENT, category);
        }
    }

    /**
     * 测试程序文件后缀识别
     * 验证：常见程序后缀能正确识别为 PROGRAM 分类
     */
    @Test
    public void testProgramSuffixRecognition() {
        // 测试常见程序后缀
        String[] programUrls = {
            "http://example.com/app.exe",
            "http://example.com/app.apk",
            "http://example.com/app.dmg",
            "http://example.com/app.deb",
            "http://example.com/app.rpm",
            "http://example.com/app.msi",
            "http://example.com/app.jar",
            "http://example.com/app.war",
            "http://example.com/app.ipa",
            "http://example.com/app.app",
            "http://example.com/app.run",
            "http://example.com/app.bin"
        };
        
        for (String url : programUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 PROGRAM", Category.PROGRAM, category);
        }
    }

    /**
     * 测试未知后缀识别
     * 验证：未知后缀识别为 OTHER 分类
     */
    @Test
    public void testUnknownSuffixRecognition() {
        // 测试未知后缀
        String[] unknownUrls = {
            "http://example.com/file.xyz",
            "http://example.com/file.abc",
            "http://example.com/file.unknown"
        };
        
        for (String url : unknownUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 OTHER", Category.OTHER, category);
        }
    }

    /**
     * 测试无后缀 URL
     * 验证：无后缀的 URL 识别为 OTHER 分类
     */
    @Test
    public void testNoSuffixRecognition() {
        // 测试无后缀 URL
        String[] noSuffixUrls = {
            "http://example.com/file",
            "http://example.com/",
            "http://example.com/path/to/resource"
        };
        
        for (String url : noSuffixUrls) {
            Category category = categoryService.classify(url);
            assertEquals("URL: " + url + " 应识别为 OTHER", Category.OTHER, category);
        }
    }

    /**
     * 测试带查询参数的 URL
     * 验证：带查询参数的 URL 能正确提取后缀
     */
    @Test
    public void testUrlWithQueryParameters() {
        // 测试带查询参数的 URL
        String urlWithQuery = "http://example.com/video.mp4?token=abc123&expires=123456";
        Category category = categoryService.classify(urlWithQuery);
        assertEquals("带查询参数的 URL 应正确识别", Category.VIDEO, category);
        
        // 测试带锚点的 URL
        String urlWithAnchor = "http://example.com/audio.mp3#section";
        category = categoryService.classify(urlWithAnchor);
        assertEquals("带锚点的 URL 应正确识别", Category.AUDIO, category);
        
        // 测试带查询和锚点的 URL
        String urlWithBoth = "http://example.com/doc.pdf?download=true#page=1";
        category = categoryService.classify(urlWithBoth);
        assertEquals("带查询和锚点的 URL 应正确识别", Category.DOCUMENT, category);
    }

    /**
     * 测试大小写不敏感
     * 验证：后缀大小写不敏感
     */
    @Test
    public void testCaseInsensitiveSuffix() {
        // 测试大写后缀
        assertEquals(Category.VIDEO, categoryService.classify("http://example.com/video.MP4"));
        assertEquals(Category.VIDEO, categoryService.classify("http://example.com/video.MKV"));
        assertEquals(Category.AUDIO, categoryService.classify("http://example.com/audio.MP3"));
        assertEquals(Category.AUDIO, categoryService.classify("http://example.com/audio.FLAC"));
        assertEquals(Category.ARCHIVE, categoryService.classify("http://example.com/file.ZIP"));
        assertEquals(Category.DOCUMENT, categoryService.classify("http://example.com/doc.PDF"));
        assertEquals(Category.PROGRAM, categoryService.classify("http://example.com/app.EXE"));
        
        // 测试混合大小写
        assertEquals(Category.VIDEO, categoryService.classify("http://example.com/video.Mp4"));
        assertEquals(Category.AUDIO, categoryService.classify("http://example.com/audio.Mp3"));
    }

    // ==================== MIME 类型分类测试 ====================

    /**
     * 测试视频 MIME 类型识别
     * 验证：视频 MIME 类型能正确识别为 VIDEO 分类
     */
    @Test
    public void testVideoMimeTypeRecognition() {
        // 测试视频 MIME 类型
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/mp4"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/mpeg"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/webm"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/quicktime"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/x-msvideo"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/x-flv"));
        
        // 测试特殊视频 MIME 类型
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("application/x-mpegURL"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("application/vnd.apple.mpegurl"));
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("application/mp4"));
    }

    /**
     * 测试音频 MIME 类型识别
     * 验证：音频 MIME 类型能正确识别为 AUDIO 分类
     */
    @Test
    public void testAudioMimeTypeRecognition() {
        // 测试音频 MIME 类型
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/mpeg"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/mp3"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/wav"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/ogg"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/aac"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/flac"));
        
        // 测试特殊音频 MIME 类型
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("application/ogg"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("application/x-flac"));
    }

    /**
     * 测试压缩包 MIME 类型识别
     * 验证：压缩包 MIME 类型能正确识别为 ARCHIVE 分类
     */
    @Test
    public void testArchiveMimeTypeRecognition() {
        // 测试压缩包 MIME 类型
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/zip"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-zip-compressed"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-rar-compressed"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-7z-compressed"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-tar"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/gzip"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-gzip"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-bzip2"));
        assertEquals(Category.ARCHIVE, ContentTypeMapping.mapContentType("application/x-xz"));
    }

    /**
     * 测试文档 MIME 类型识别
     * 验证：文档 MIME 类型能正确识别为 DOCUMENT 分类
     */
    @Test
    public void testDocumentMimeTypeRecognition() {
        // 测试文档 MIME 类型
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/pdf"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/msword"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/vnd.ms-excel"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/vnd.ms-powerpoint"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("text/plain"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("text/html"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("text/csv"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/rtf"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("application/epub+zip"));
    }

    /**
     * 测试程序 MIME 类型识别
     * 验证：程序 MIME 类型能正确识别为 PROGRAM 分类
     */
    @Test
    public void testProgramMimeTypeRecognition() {
        // 测试程序 MIME 类型
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/octet-stream"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-msdownload"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-msdos-program"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/vnd.android.package-archive"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-apple-diskimage"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-debian-package"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-redhat-package-manager"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/x-rpm"));
        assertEquals(Category.PROGRAM, ContentTypeMapping.mapContentType("application/java-archive"));
    }

    /**
     * 测试 MIME 类型带参数
     * 验证：带参数的 MIME 类型能正确处理
     */
    @Test
    public void testMimeTypeWithParameters() {
        // 测试带字符集参数的 MIME 类型
        assertEquals(Category.VIDEO, ContentTypeMapping.mapContentType("video/mp4; charset=utf-8"));
        assertEquals(Category.AUDIO, ContentTypeMapping.mapContentType("audio/mpeg; charset=utf-8"));
        assertEquals(Category.DOCUMENT, ContentTypeMapping.mapContentType("text/plain; charset=utf-8"));
    }

    /**
     * 测试空或 null MIME 类型
     * 验证：空或 null MIME 类型返回 OTHER
     */
    @Test
    public void testEmptyOrNullMimeType() {
        assertEquals(Category.OTHER, ContentTypeMapping.mapContentType(null));
        assertEquals(Category.OTHER, ContentTypeMapping.mapContentType(""));
        assertEquals(Category.OTHER, ContentTypeMapping.mapContentType("   "));
    }

    // ==================== 六大分类映射测试 ====================

    /**
     * 测试六大分类枚举完整性
     * 验证：六大分类枚举值正确
     */
    @Test
    public void testSixCategories() {
        // 验证六大分类存在
        Category[] categories = Category.values();
        assertEquals(6, categories.length);
        
        // 验证分类名称
        assertEquals("VIDEO", Category.VIDEO.name());
        assertEquals("AUDIO", Category.AUDIO.name());
        assertEquals("ARCHIVE", Category.ARCHIVE.name());
        assertEquals("DOCUMENT", Category.DOCUMENT.name());
        assertEquals("PROGRAM", Category.PROGRAM.name());
        assertEquals("OTHER", Category.OTHER.name());
    }

    /**
     * 测试后缀映射表完整性
     * 验证：后缀映射表覆盖所有六大分类
     */
    @Test
    public void testSuffixMappingCompleteness() {
        Map<String, Category> suffixMap = SuffixMapping.createSuffixMap();
        
        // 验证映射表不为空
        assertFalse(suffixMap.isEmpty());
        
        // 验证包含所有六大分类
        assertTrue(suffixMap.containsValue(Category.VIDEO));
        assertTrue(suffixMap.containsValue(Category.AUDIO));
        assertTrue(suffixMap.containsValue(Category.ARCHIVE));
        assertTrue(suffixMap.containsValue(Category.DOCUMENT));
        assertTrue(suffixMap.containsValue(Category.PROGRAM));
        // OTHER 不在映射表中，作为默认值
    }

    /**
     * 测试 MIME 类型映射表完整性
     * 验证：MIME 类型映射表覆盖主要分类
     */
    @Test
    public void testMimeMappingCompleteness() {
        Map<String, Category> mimeMap = ContentTypeMapping.getMimeCategoryMap();
        
        // 验证映射表不为空
        assertFalse(mimeMap.isEmpty());
        
        // 验证包含主要分类（不含 OTHER）
        assertTrue(mimeMap.containsValue(Category.VIDEO));
        assertTrue(mimeMap.containsValue(Category.AUDIO));
        assertTrue(mimeMap.containsValue(Category.ARCHIVE));
        assertTrue(mimeMap.containsValue(Category.DOCUMENT));
        assertTrue(mimeMap.containsValue(Category.PROGRAM));
    }

    /**
     * 测试自定义后缀映射
     * 验证：可以通过配置添加自定义后缀映射
     */
    @Test
    public void testCustomSuffixMapping() {
        // 创建自定义配置
        CategoryConfig config = new CategoryConfig();
        Map<String, Category> customMappings = new HashMap<>();
        customMappings.put("custom", Category.VIDEO);
        customMappings.put("myformat", Category.AUDIO);
        config.setCustomSuffixMappings(customMappings);
        
        // 创建带自定义配置的分类服务
        CategoryService customService = new CategoryService(config);
        
        // 测试自定义后缀
        assertEquals(Category.VIDEO, customService.classify("http://example.com/file.custom"));
        assertEquals(Category.AUDIO, customService.classify("http://example.com/file.myformat"));
        
        // 验证原有后缀仍然有效
        assertEquals(Category.VIDEO, customService.classify("http://example.com/file.mp4"));
        assertEquals(Category.AUDIO, customService.classify("http://example.com/file.mp3"));
    }

    /**
     * 测试空 URL 处理
     * 验证：空或 null URL 返回 OTHER 分类
     */
    @Test
    public void testEmptyOrNullUrl() {
        assertEquals(Category.OTHER, categoryService.classify(null));
        assertEquals(Category.OTHER, categoryService.classify(""));
        assertEquals(Category.OTHER, categoryService.classify("   "));
    }

    /**
     * 测试 ContentTypeMapping 辅助方法
     * 验证：isVideo、isAudio 等辅助方法正确工作
     */
    @Test
    public void testContentTypeMappingHelperMethods() {
        // 测试 isVideo
        assertTrue(ContentTypeMapping.isVideo("video/mp4"));
        assertFalse(ContentTypeMapping.isVideo("audio/mp3"));
        
        // 测试 isAudio
        assertTrue(ContentTypeMapping.isAudio("audio/mpeg"));
        assertFalse(ContentTypeMapping.isAudio("video/mp4"));
        
        // 测试 isArchive
        assertTrue(ContentTypeMapping.isArchive("application/zip"));
        assertFalse(ContentTypeMapping.isArchive("video/mp4"));
        
        // 测试 isDocument
        assertTrue(ContentTypeMapping.isDocument("application/pdf"));
        assertFalse(ContentTypeMapping.isDocument("video/mp4"));
        
        // 测试 isProgram
        assertTrue(ContentTypeMapping.isProgram("application/octet-stream"));
        assertFalse(ContentTypeMapping.isProgram("video/mp4"));
    }

    /**
     * 测试获取后缀映射表
     * 验证：getSuffixMap 返回映射表副本
     */
    @Test
    public void testGetSuffixMap() {
        Map<String, Category> suffixMap = categoryService.getSuffixMap();
        
        // 验证返回的是副本（修改不影响原表）
        suffixMap.put("test", Category.OTHER);
        
        // 再次获取，验证不包含 test
        Map<String, Category> newMap = categoryService.getSuffixMap();
        assertFalse(newMap.containsKey("test"));
    }

    /**
     * 测试获取配置
     * 验证：getConfig 返回当前配置
     */
    @Test
    public void testGetConfig() {
        CategoryConfig config = categoryService.getConfig();
        assertNotNull(config);
        
        // 测试自定义配置
        CategoryConfig customConfig = new CategoryConfig();
        CategoryService customService = new CategoryService(customConfig);
        assertEquals(customConfig, customService.getConfig());
    }
}