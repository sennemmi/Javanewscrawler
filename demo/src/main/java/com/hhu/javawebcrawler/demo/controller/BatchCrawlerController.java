package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量爬取控制器
 * <p>
 * 负责处理批量爬取功能，包括二级爬取和关键词爬取功能。
 * </p>
 */
@RestController
@RequestMapping("/api/crawl")
public class BatchCrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(BatchCrawlerController.class);
    
    private final NewsCrawlerService newsCrawlerService;
    private final CrawlHistoryService crawlHistoryService;
    private final UserService userService;

    public BatchCrawlerController(NewsCrawlerService newsCrawlerService, 
                             CrawlHistoryService crawlHistoryService,
                             UserService userService) {
        this.newsCrawlerService = newsCrawlerService;
        this.crawlHistoryService = crawlHistoryService;
        this.userService = userService;
    }

    /**
     * 从指定的入口页面进行二级爬取
     * <p>
     * 该方法会从指定的入口页面获取所有符合条件的新闻链接，并逐一爬取。
     * </p>
     * 
     * @param payload 请求体，必须包含"url"字段，值为新闻列表页或首页的URL
     * @return 包含操作结果信息的响应体
     */
    @PostMapping("/from-index")
    public ResponseEntity<Map<String, Object>> crawlFromIndex(@RequestBody Map<String, String> payload) {
        String indexUrl = payload.get("url");
        logger.info("收到二级爬取请求，入口页面: {}", indexUrl);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试执行二级爬取，入口URL: {}", indexUrl);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未认证"));
        }

        if (indexUrl == null || indexUrl.isBlank()) {
            logger.warn("二级爬取请求中入口URL为空");
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "入口URL不能为空"));
        }

        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
            logger.debug("用户 [{}] ID: {}", username, userId);
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "获取用户信息失败"));
        }

        try {
            // 执行二级爬取
            List<NewsData> crawledNews = newsCrawlerService.crawlNewsFromIndexPage(indexUrl);
            
            // 记录爬取历史
            Map<String, Object> params = new HashMap<>();
            params.put("entryUrl", indexUrl);
            
            crawlHistoryService.recordIndexCrawl(userId, indexUrl, "二级爬取，成功获取 " + crawledNews.size() + " 条新闻", params);
            
            // 整理返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("message", "二级爬取任务完成");
            result.put("crawledCount", crawledNews.size());
            result.put("entryUrl", indexUrl);
            result.put("titles", crawledNews.stream().map(NewsData::getTitle).collect(Collectors.toList()));
            
            logger.info("二级爬取完成，从 {} 获取了 {} 条新闻", indexUrl, crawledNews.size());
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("二级爬取过程中发生错误: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "爬取过程中发生错误: " + e.getMessage());
            errorResult.put("entryUrl", indexUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * 按关键词爬取新闻
     * <p>
     * 从指定入口页面爬取所有标题包含关键词的新闻。
     * </p>
     * 
     * @param payload 请求体，必须包含"keyword"字段，可选包含"url"字段
     * @return 包含操作结果信息的响应体
     */
    @PostMapping("/by-keyword")
    public ResponseEntity<Map<String, Object>> crawlByKeyword(@RequestBody Map<String, String> payload) {
        String keyword = payload.get("keyword");
        String indexUrl = payload.getOrDefault("url", "https://news.sina.com.cn/");
        
        logger.info("收到关键词爬取请求，关键词: {}, 入口页面: {}", keyword, indexUrl);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试执行关键词爬取，关键词: {}", keyword);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未认证"));
        }

        if (keyword == null || keyword.isBlank()) {
            logger.warn("关键词爬取请求中关键词为空");
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "关键词不能为空"));
        }

        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "获取用户信息失败"));
        }

        try {
            // 执行关键词爬取
            List<NewsData> crawledNews = newsCrawlerService.crawlNewsByKeyword(keyword, indexUrl);
            
            // 记录爬取历史
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("entryUrl", indexUrl);
            
            crawlHistoryService.recordIndexCrawl(userId, indexUrl, "关键词爬取: " + keyword + "，成功获取 " + crawledNews.size() + " 条新闻", params);
            
            // 整理返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("message", "关键词爬取任务完成");
            result.put("crawledCount", crawledNews.size());
            result.put("keyword", keyword);
            result.put("entryUrl", indexUrl);
            result.put("titles", crawledNews.stream().map(NewsData::getTitle).collect(Collectors.toList()));
            
            logger.info("关键词爬取完成，关键词: {}, 获取了 {} 条新闻", keyword, crawledNews.size());
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("关键词爬取过程中发生错误: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "爬取过程中发生错误: " + e.getMessage());
            errorResult.put("keyword", keyword);
            errorResult.put("entryUrl", indexUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
} 