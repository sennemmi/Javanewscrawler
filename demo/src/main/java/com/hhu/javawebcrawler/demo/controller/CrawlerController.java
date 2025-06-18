package com.hhu.javawebcrawler.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 原爬虫控制器 - 已弃用
 * <p>
 * 此控制器已被拆分为多个专注的控制器，请使用以下控制器替代：
 * <ul>
 *   <li>{@link SingleCrawlerController}: 处理单个URL爬取</li>
 *   <li>{@link BatchCrawlerController}: 处理批量爬取功能</li>
 *   <li>{@link ExportController}: 处理文件导出功能</li>
 *   <li>{@link HistoryController}: 处理历史记录查询</li>
 * </ul>
 * </p>
 * 
 * <p><strong>删除计划</strong>：该控制器将在2024年6月30日后移除，请在此日期前完成迁移工作。</p>
 * 
 * @deprecated 此控制器已被拆分为多个更专注的控制器，请使用新的控制器
 */
@Deprecated
@RestController
@RequestMapping("/api-deprecated")
public class CrawlerController {
    // 该类已弃用，所有功能已迁移到更专注的控制器中
}