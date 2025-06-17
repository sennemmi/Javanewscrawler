package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时爬虫服务
 * 负责定期执行新闻爬取任务
 */
@Service
public class ScheduledCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledCrawlerService.class);
    private static final String SINA_NEWS_HOMEPAGE = "https://news.sina.com.cn/";
    private static final String SCHEDULED_CRAWL_TYPE = "SCHEDULED_CRAWL";
    
    // 系统管理员ID，用于记录定时任务的爬取历史
    private static final Long SYSTEM_ADMIN_ID = 1L;

    private final NewsCrawlerService newsCrawlerService;
    private final CrawlHistoryService crawlHistoryService;

    public ScheduledCrawlerService(NewsCrawlerService newsCrawlerService, CrawlHistoryService crawlHistoryService) {
        this.newsCrawlerService = newsCrawlerService;
        this.crawlHistoryService = crawlHistoryService;
    }

    /**
     * 定时爬取新浪新闻首页
     * 每天早上8点和下午4点执行
     */
    @Scheduled(cron = "0 0 8,16 * * ?")
    public void scheduledCrawlSinaHomepage() {
        logger.info("开始执行定时爬取任务: {}", SINA_NEWS_HOMEPAGE);
        
        try {
            // 执行爬取
            List<NewsData> crawledList = newsCrawlerService.crawlNewsFromIndexPage(SINA_NEWS_HOMEPAGE);
            int crawledCount = crawledList.size();
            
            // 记录爬取结果
            logger.info("定时爬取成功，共爬取 {} 条新闻", crawledCount);
            
            // 收集爬取的URL样本（最多10个）
            List<String> sampleUrls = crawledList.stream()
                .map(NewsData::getUrl)
                .limit(10)
                .collect(Collectors.toList());
            
            // 构建历史记录参数
            Map<String, Object> params = new HashMap<>();
            params.put("totalCount", crawledCount);
            params.put("sampleUrls", sampleUrls);
            params.put("executionTime", LocalDateTime.now().toString());
            params.put("isScheduled", true);
            
            // 记录历史
            String title = String.format("[定时任务] 从新浪新闻首页爬取了 %d 条新闻", crawledCount);
            recordScheduledCrawlHistory(SINA_NEWS_HOMEPAGE, title, params);
            
        } catch (Exception e) {
            logger.error("定时爬取任务执行失败", e);
            
            // 记录失败历史
            Map<String, Object> params = new HashMap<>();
            params.put("error", e.getMessage());
            params.put("executionTime", LocalDateTime.now().toString());
            params.put("isScheduled", true);
            
            String title = "[定时任务] 爬取新浪新闻首页失败";
            recordScheduledCrawlHistory(SINA_NEWS_HOMEPAGE, title, params);
        }
    }
    
    /**
     * 手动触发定时爬取任务
     * 
     * @param userId 触发爬取的用户ID
     * @return 爬取结果信息
     */
    public Map<String, Object> manualCrawlSinaHomepage(Long userId) {
        logger.info("用户 {} 手动触发爬取任务: {}", userId, SINA_NEWS_HOMEPAGE);
        
        try {
            // 执行爬取
            List<NewsData> crawledList = newsCrawlerService.crawlNewsFromIndexPage(SINA_NEWS_HOMEPAGE);
            int crawledCount = crawledList.size();
            
            // 收集爬取的URL样本（最多10个）
            List<String> sampleUrls = crawledList.stream()
                .map(NewsData::getUrl)
                .limit(10)
                .collect(Collectors.toList());
            
            // 构建历史记录参数
            Map<String, Object> params = new HashMap<>();
            params.put("totalCount", crawledCount);
            params.put("sampleUrls", sampleUrls);
            params.put("executionTime", LocalDateTime.now().toString());
            params.put("isScheduled", false);
            params.put("triggeredByUserId", userId);
            
            // 记录历史（使用触发用户的ID）
            String title = String.format("手动触发从新浪新闻首页爬取，获取了 %d 条新闻", crawledCount);
            
            // 使用INDEX_CRAWL类型而不是SCHEDULED_CRAWL，因为这是手动触发的
            crawlHistoryService.recordIndexCrawl(userId, SINA_NEWS_HOMEPAGE, title, params);
            
            // 返回结果
            return Map.of(
                "success", true,
                "message", "手动爬取任务完成",
                "crawledCount", crawledCount,
                "url", SINA_NEWS_HOMEPAGE
            );
            
        } catch (Exception e) {
            logger.error("手动触发爬取任务执行失败", e);
            
            // 返回错误信息
            return Map.of(
                "success", false,
                "message", "爬取失败: " + e.getMessage(),
                "url", SINA_NEWS_HOMEPAGE
            );
        }
    }
    
    /**
     * 记录定时爬取的历史
     */
    private void recordScheduledCrawlHistory(String url, String title, Map<String, Object> params) {
        // 创建爬取历史记录
        CrawlHistory history = new CrawlHistory();
        history.setUserId(SYSTEM_ADMIN_ID); // 使用系统管理员ID
        history.setCrawlType(SCHEDULED_CRAWL_TYPE);
        history.setUrl(url);
        history.setTitle(title);
        
        try {
            // 将参数转换为JSON字符串并设置
            String paramsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
            history.setParams(paramsJson);
        } catch (Exception e) {
            logger.error("转换参数为JSON失败", e);
            history.setParams("{\"error\":\"转换参数时出错\"}");
        }
        
        // 保存历史记录
        crawlHistoryService.saveHistory(history);
    }
} 