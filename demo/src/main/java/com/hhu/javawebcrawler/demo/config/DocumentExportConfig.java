package com.hhu.javawebcrawler.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档导出配置类
 * <p>
 * 此配置类负责配置与文档导出相关的Bean和属性，包括字体配置和文档导出选项。
 * </p>
 */
@Configuration
public class DocumentExportConfig {

    private static final Logger logger = LoggerFactory.getLogger(DocumentExportConfig.class);

    /**
     * 字体路径常量
     */
    public static final String HEITI_FONT_PATH = "static/fonts/方正黑体-简体.TTF";
    public static final String KAITI_FONT_PATH = "static/fonts/方正楷体-简体.ttf";
    public static final String FANGSONG_FONT_PATH = "static/fonts/方正仿宋-简体.TTF";
    public static final String SONGTI_FONT_PATH = "static/fonts/方正书宋-简体.ttf";

    /**
     * 默认字体配置
     */
    public static final String DEFAULT_FONT_FAMILY = "宋体";
    public static final String DEFAULT_TITLE_FONT_FAMILY = "黑体";

    /**
     * 字体映射Bean
     * <p>
     * 提供字体名称到字体文件路径的映射，供文档生成服务使用。
     * </p>
     * 
     * @return 字体名称到字体路径的映射
     */
    @Bean(name = "fontPathMappings")
    public Map<String, String> fontMappings() {
        Map<String, String> fontMap = new HashMap<>();
        fontMap.put("黑体", HEITI_FONT_PATH);
        fontMap.put("楷体", KAITI_FONT_PATH);
        fontMap.put("仿宋", FANGSONG_FONT_PATH);
        fontMap.put("宋体", SONGTI_FONT_PATH);
        
        // 验证字体文件是否存在
        for (Map.Entry<String, String> entry : fontMap.entrySet()) {
            try {
                ClassPathResource resource = new ClassPathResource(entry.getValue());
                if (resource.exists()) {
                    logger.info("字体文件已加载: {} -> {}", entry.getKey(), entry.getValue());
                } else {
                    logger.warn("字体文件不存在: {} -> {}", entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                logger.error("字体文件加载失败: {} -> {}: {}", 
                           entry.getKey(), entry.getValue(), e.getMessage());
            }
        }
        
        return fontMap;
    }
    
    /**
     * 文档导出配置
     * <p>
     * 提供文档导出的默认配置选项，如行间距和默认字体大小。
     * </p>
     * 
     * @return 文档导出配置的映射
     */
    @Bean(name = "documentStyleConfig")
    public Map<String, Object> documentExportConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 默认字体大小
        config.put("defaultTitleFontSize", 22);
        config.put("defaultHeading1FontSize", 20);
        config.put("defaultHeading2FontSize", 18);
        config.put("defaultHeading3FontSize", 16);
        config.put("defaultTextFontSize", 14);
        config.put("defaultCaptionFontSize", 12);
        config.put("defaultFooterFontSize", 10);
        
        // 默认行间距
        config.put("defaultLineSpacing", 1.5f);
        
        // PDF特有配置
        config.put("pdfMarginTop", 36f);
        config.put("pdfMarginBottom", 36f);
        config.put("pdfMarginLeft", 36f);
        config.put("pdfMarginRight", 36f);
        
        logger.info("文档导出配置已加载");
        return config;
    }
} 