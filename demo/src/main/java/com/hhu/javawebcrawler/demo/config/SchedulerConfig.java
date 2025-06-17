package com.hhu.javawebcrawler.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 * 启用Spring的定时任务功能
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring会自动加载标记了@Scheduled注解的任务方法
} 