package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.controller.base.BaseController;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 单个URL爬取控制器
 * <p>
 * 负责处理单个新闻URL的爬取请求，提供单个URL的爬取功能。
 * </p>
 */
@RestController
@RequestMapping("/api/crawl")
public class SingleCrawlerController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(SingleCrawlerController.class);
    
    private final NewsCrawlerService newsCrawlerService;
    private final CrawlHistoryService crawlHistoryService;
    private final UserService userService;

    public SingleCrawlerController(NewsCrawlerService newsCrawlerService, 
                              CrawlHistoryService crawlHistoryService,
                              UserService userService) {
        this.newsCrawlerService = newsCrawlerService;
        this.crawlHistoryService = crawlHistoryService;
        this.userService = userService;
    }

    /**
     * 根据URL爬取单个新闻，同时记录爬取历史
     * 
     * @param payload 请求体，必须包含 "url" 字段
     * @return 爬取到的新闻数据，包含标题、内容、发布时间等信息
     */
    @PostMapping("/single")
    public ResponseEntity<NewsData> crawlSingleUrl(@RequestBody Map<String, String> payload) {
        logger.info("收到单个URL爬取请求: {}", payload.get("url"));
        
        // 验证认证状态
        validateAuthentication();
        
        // 验证参数
        String url = payload.get("url");
        validateStringParam(url, "URL");
        
        // 获取用户ID
        Long userId = getCurrentUserId(userService);
        
        // 执行爬取并处理异常
        return ResponseEntity.ok(
            executeWithExceptionHandling(() -> {
                try {
                    // 爬取新闻
                    NewsData newsData = newsCrawlerService.crawlAndSaveSinaNews(url);
                    
                    // 记录爬取历史
                    crawlHistoryService.recordSingleUrlCrawl(userId, url, newsData.getTitle());
                    
                    logger.info("成功爬取URL: {}, 标题: {}", url, newsData.getTitle());
                    return newsData;
                } catch (IOException e) {
                    logger.error("爬取URL失败: {} - {}", url, e.getMessage());
                    throw new RuntimeException("爬取URL失败: " + e.getMessage(), e);
                }
            })
        );
    }
} 