// 定义了该Java文件所在的包名
package com.hhu.javawebcrawler.demo.config;

// 导入 SLF4J 日志接口
import org.slf4j.Logger;
// 导入 SLF4J 日志工厂类
import org.slf4j.LoggerFactory;
// 导入 Spring 的 Bean 注解，用于声明一个Bean
import org.springframework.context.annotation.Bean;
// 导入 Spring 的 Configuration 注解，标记这是一个配置类
import org.springframework.context.annotation.Configuration;
// 导入 Spring 的 ClassPathResource 类，用于访问类路径下的资源
import org.springframework.core.io.ClassPathResource;

// 导入 Java 工具类 HashMap
import java.util.HashMap;
// 导入 Java 工具类 Map
import java.util.Map;

// 标记这个类是一个Spring配置类
@Configuration
// 定义一个公共类 DocumentExportConfig
public class DocumentExportConfig {

    // 创建一个静态的、最终的 Logger 实例，用于记录该类的日志
    private static final Logger logger = LoggerFactory.getLogger(DocumentExportConfig.class);

    // 定义一个公共静态最终字符串常量，表示黑体字体的路径
    public static final String HEITI_FONT_PATH = "static/fonts/方正黑体-简体.TTF";
    // 定义一个公共静态最终字符串常量，表示楷体字体的路径
    public static final String KAITI_FONT_PATH = "static/fonts/方正楷体-简体.ttf";
    // 定义一个公共静态最终字符串常量，表示仿宋字体的路径
    public static final String FANGSONG_FONT_PATH = "static/fonts/方正仿宋-简体.TTF";
    // 定义一个公共静态最终字符串常量，表示宋体字体的路径
    public static final String SONGTI_FONT_PATH = "static/fonts/方正书宋-简体.ttf";

    // 定义一个公共静态最终字符串常量，表示默认的字体族
    public static final String DEFAULT_FONT_FAMILY = "宋体";
    // 定义一个公共静态最终字符串常量，表示默认的标题字体族
    public static final String DEFAULT_TITLE_FONT_FAMILY = "黑体";

    // 将该方法的返回值注册为一个Spring Bean，并指定名称为 "fontPathMappings"
    @Bean(name = "fontPathMappings")
    // 定义一个公共方法，返回一个字体名称到路径的映射Map
    public Map<String, String> fontMappings() {
        // 创建一个新的HashMap实例来存储字体映射
        Map<String, String> fontMap = new HashMap<>();
        // 将“黑体”名称与对应的字体文件路径关联起来
        fontMap.put("黑体", HEITI_FONT_PATH);
        // 将“楷体”名称与对应的字体文件路径关联起来
        fontMap.put("楷体", KAITI_FONT_PATH);
        // 将“仿宋”名称与对应的字体文件路径关联起来
        fontMap.put("仿宋", FANGSONG_FONT_PATH);
        // 将“宋体”名称与对应的字体文件路径关联起来
        fontMap.put("宋体", SONGTI_FONT_PATH);
        
        // 遍历字体映射Map中的每一个条目以验证文件是否存在
        for (Map.Entry<String, String> entry : fontMap.entrySet()) {
            // 开始一个try-catch块，处理加载资源时可能发生的异常
            try {
                // 创建一个ClassPathResource对象来表示类路径下的字体文件
                ClassPathResource resource = new ClassPathResource(entry.getValue());
                // 检查字体资源是否存在
                if (resource.exists()) {
                    // 如果存在，记录一条信息日志
                    logger.info("字体文件已加载: {} -> {}", entry.getKey(), entry.getValue());
                // 否则（如果资源不存在）
                } else {
                    // 记录一条警告日志
                    logger.warn("字体文件不存在: {} -> {}", entry.getKey(), entry.getValue());
                }
            // 捕获在try块中可能发生的任何异常
            } catch (Exception e) {
                // 记录一条错误日志，并包含失败的详细信息
                logger.error("字体文件加载失败: {} -> {}: {}", 
                           entry.getKey(), entry.getValue(), e.getMessage());
            }
        }
        
        // 返回填充并验证后的字体映射Map
        return fontMap;
    }
    
    // 将该方法的返回值注册为一个Spring Bean，并指定名称为 "documentStyleConfig"
    @Bean(name = "documentStyleConfig")
    // 定义一个公共方法，返回一个包含文档样式配置的Map
    public Map<String, Object> documentExportConfig() {
        // 创建一个新的HashMap实例来存储配置项
        Map<String, Object> config = new HashMap<>();
        
        // 配置默认标题的字体大小
        config.put("defaultTitleFontSize", 22);
        // 配置一级标题的字体大小
        config.put("defaultHeading1FontSize", 20);
        // 配置二级标题的字体大小
        config.put("defaultHeading2FontSize", 18);
        // 配置三级标题的字体大小
        config.put("defaultHeading3FontSize", 16);
        // 配置正文文本的字体大小
        config.put("defaultTextFontSize", 14);
        // 配置图表标题的字体大小
        config.put("defaultCaptionFontSize", 12);
        // 配置页脚的字体大小
        config.put("defaultFooterFontSize", 10);
        
        // 配置默认行间距
        config.put("defaultLineSpacing", 1.5f);
        
        // 配置PDF文档的上边距
        config.put("pdfMarginTop", 36f);
        // 配置PDF文档的下边距
        config.put("pdfMarginBottom", 36f);
        // 配置PDF文档的左边距
        config.put("pdfMarginLeft", 36f);
        // 配置PDF文档的右边距
        config.put("pdfMarginRight", 36f);
        
        // 记录一条信息日志，表示配置加载完成
        logger.info("文档导出配置已加载");
        // 返回包含所有配置项的Map
        return config;
    }
}