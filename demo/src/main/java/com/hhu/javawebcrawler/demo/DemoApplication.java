package com.hhu.javawebcrawler.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.lang.NonNull;
import java.io.File;

@SpringBootApplication
@EnableScheduling
public class DemoApplication implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * 应用程序启动后初始化
	 */
	@PostConstruct
	public void init() {
		logger.info("Java Web爬虫系统初始化...");
		
		// 检查字体文件是否存在
		checkFontFiles();
		
		logger.info("文档导出功能初始化完成");
	}
	
	/**
	 * 检查必要的字体文件是否存在
	 */
	private void checkFontFiles() {
		String[] fontPaths = {
			"static/fonts/方正黑体-简体.TTF",
			"static/fonts/方正楷体-简体.ttf",
			"static/fonts/方正仿宋-简体.TTF",
			"static/fonts/方正书宋-简体.ttf"
		};
		
		for (String fontPath : fontPaths) {
			try {
				File fontFile = new File(getClass().getClassLoader().getResource(fontPath).getFile());
				if (fontFile.exists()) {
					logger.info("字体文件已找到: {}", fontPath);
				} else {
					logger.warn("字体文件不存在: {}", fontPath);
				}
			} catch (Exception e) {
				logger.error("检查字体文件时出错 {}: {}", fontPath, e.getMessage());
			}
		}
	}
	
	/**
	 * 配置静态资源处理
	 */
	@Override
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**")
				.addResourceLocations("classpath:/static/");
	}
}
