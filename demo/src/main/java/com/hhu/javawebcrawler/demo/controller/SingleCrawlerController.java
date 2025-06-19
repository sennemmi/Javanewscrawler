package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.controller.base.BaseController;
import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * 单个URL爬取控制器
 * <p>
 * 负责处理单个新闻URL的爬取请求，提供单个URL的爬取功能。
 * </p>
 */
@RestController
@RequestMapping("/api")
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
    @PostMapping("/crawl/single")
    public ResponseEntity<Object> crawlSingleUrl(@RequestBody Map<String, String> payload) {
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
                    // 先创建爬取历史记录
                    CrawlHistory crawlHistory = new CrawlHistory();
                    crawlHistory.setUserId(userId);
                    crawlHistory.setCrawlType("SINGLE_URL");
                    crawlHistory.setUrl(url);
                    crawlHistory.setTitle("单URL爬取任务: " + url);
                    
                    // 保存爬取历史记录，获取ID
                    crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
                    
                    // 爬取新闻，并关联爬取历史
                    Optional<NewsData> newsDataOpt = newsCrawlerService.crawlAndSaveSinaNews(url, crawlHistory);
                    
                    if (newsDataOpt.isPresent()) {
                        NewsData newsData = newsDataOpt.get();
                        
                        // 更新爬取历史记录的标题
                        crawlHistory.setTitle(newsData.getTitle());
                        crawlHistoryService.saveHistory(crawlHistory);
                        
                        logger.info("成功爬取URL: {}, 标题: {}", url, newsData.getTitle());
                        return newsData;
                    } else {
                        // 爬取失败，更新历史记录
                        crawlHistory.setTitle("爬取失败: 内容提取失败");
                        crawlHistoryService.saveHistory(crawlHistory);
                        
                        logger.warn("爬取URL成功，但内容提取失败，跳过保存和记录历史: {}", url);
                        return new ResponseEntity<>("内容提取失败", HttpStatus.NO_CONTENT).getBody();
                    }
                } catch (IOException e) {
                    logger.error("爬取URL失败: {} - {}", url, e.getMessage());
                    throw new RuntimeException("爬取URL失败: " + e.getMessage(), e);
                }
            })
        );
    }
    
    /**
     * 根据URL获取已爬取的新闻详情
     * <p>
     * 该接口用于从数据库中获取已经爬取过的新闻数据，不会触发新的爬取操作。
     * 主要用于查看历史记录中的新闻详情。
     * </p>
     * 
     * @param url 新闻URL
     * @return 新闻详情数据
     */
    @GetMapping("/news/detail")
    public ResponseEntity<NewsData> getNewsDetail(@RequestParam String url) {
        logger.info("收到获取新闻详情请求: {}", url);
        
        // 验证认证状态
        validateAuthentication();
        
        // 验证参数
        validateStringParam(url, "URL");
        
        try {
            // 尝试URL解码，处理可能的编码问题
            String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            logger.info("解码后的URL: {}", decodedUrl);
            
            // 先查询原始URL
            Optional<NewsData> newsDataOpt = newsCrawlerService.findNewsByUrl(url);
            
            // 如果找不到，尝试用解码后的URL查询
            if (!newsDataOpt.isPresent()) {
                newsDataOpt = newsCrawlerService.findNewsByUrl(decodedUrl);
                logger.info("使用解码后URL重新查询");
            }
            
            if (newsDataOpt.isPresent()) {
                logger.info("成功获取新闻详情: {}", newsDataOpt.get().getTitle());
                return ResponseEntity.ok(newsDataOpt.get());
            } else {
                logger.warn("未找到URL对应的新闻详情，原始URL: {}, 解码URL: {}", url, decodedUrl);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            logger.error("获取新闻详情时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
} 