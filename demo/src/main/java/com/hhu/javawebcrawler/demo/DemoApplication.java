package com.hhu.javawebcrawler.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.lang.NonNull;
import java.io.File;

// @SpringBootApplication 注解标识这是一个 Spring Boot 应用程序的主类
@SpringBootApplication
// 实现 WebMvcConfigurer 接口以自定义 Spring MVC 的配置
public class DemoApplication implements WebMvcConfigurer {

	// 创建一个静态的、最终的 Logger 实例，用于记录该类的日志
	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	// Java 应用程序的主入口方法
	public static void main(String[] args) {
		// 运行 Spring Boot 应用程序，DemoApplication.class 是主配置类
		SpringApplication.run(DemoApplication.class, args);
	}

	// @PostConstruct 注解确保此方法在服务器加载 Servlet 之后、构造函数之后执行
	@PostConstruct
	// 定义一个初始化方法
	public void init() {
		// 记录一条信息级别的日志，表示系统开始初始化
		logger.info("Java Web爬虫系统初始化...");

		// 调用方法来检查必要的字体文件是否存在
		checkFontFiles();

		// 记录一条信息级别的日志，表示文档导出功能已完成初始化
		logger.info("文档导出功能初始化完成");
	}

	// 定义一个私有方法，用于检查字体文件
	private void checkFontFiles() {
		// 定义一个字符串数组，存放需要检查的字体文件的相对路径
		String[] fontPaths = {
			// 方正黑体-简体字体文件路径
			"static/fonts/方正黑体-简体.TTF",
			// 方正楷体-简体字体文件路径
			"static/fonts/方正楷体-简体.ttf",
			// 方正仿宋-简体字体文件路径
			"static/fonts/方正仿宋-简体.TTF",
			// 方正书宋-简体字体文件路径
			"static/fonts/方正书宋-简体.ttf"
		};

		// 遍历字体文件路径数组
		for (String fontPath : fontPaths) {
			// 使用 try-catch 块来处理可能发生的异常，如文件未找到
			try {
				// 通过类加载器获取资源的URL，并从中创建文件对象
				File fontFile = new File(getClass().getClassLoader().getResource(fontPath).getFile());
				// 检查文件是否存在于文件系统中
				if (fontFile.exists()) {
					// 如果文件存在，记录一条信息日志
					logger.info("字体文件已找到: {}", fontPath);
				// 如果文件不存在
				} else {
					// 记录一条警告日志
					logger.warn("字体文件不存在: {}", fontPath);
				}
			// 捕获在尝试获取文件时可能发生的任何异常
			} catch (Exception e) {
				// 记录一条错误日志，说明检查特定字体文件时出错，并附带异常信息
				logger.error("检查字体文件时出错 {}: {}", fontPath, e.getMessage());
			}
		}
	}

	// @Override 注解表示此方法覆盖了 WebMvcConfigurer 接口中的方法
	@Override
	// 配置静态资源处理器
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		// 添加一个资源处理器，将 URL 路径 /static/** 映射到静态资源
		registry.addResourceHandler("/static/**")
				// 指定这些静态资源位于 classpath 的 /static/ 目录下
				.addResourceLocations("classpath:/static/");
	}
}