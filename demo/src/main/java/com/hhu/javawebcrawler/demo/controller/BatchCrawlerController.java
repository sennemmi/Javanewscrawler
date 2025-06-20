package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
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
import com.fasterxml.jackson.databind.ObjectMapper;

// 声明这是一个RESTful风格的控制器。
@RestController
// 将此控制器下的所有请求路径映射到"/api/crawl"下。
@RequestMapping("/api/crawl")
// 定义一个名为 BatchCrawlerController 的公开类。
public class BatchCrawlerController {

    // 创建一个静态不可变的Logger实例，用于记录日志。
    private static final Logger logger = LoggerFactory.getLogger(BatchCrawlerController.class);
    
    // 声明一个不可变的新闻爬虫服务字段。
    private final NewsCrawlerService newsCrawlerService;
    // 声明一个不可变的爬取历史服务字段。
    private final CrawlHistoryService crawlHistoryService;
    // 声明一个不可变的用户服务字段。
    private final UserService userService;
    // 声明一个用于处理JSON转换的ObjectMapper字段。
    private final ObjectMapper objectMapper;

    // 定义类的构造函数，通过它注入服务依赖。
    public BatchCrawlerController(NewsCrawlerService newsCrawlerService, 
                             CrawlHistoryService crawlHistoryService,
                             UserService userService) {
        // 将注入的新闻爬虫服务实例赋值给类成员变量。
        this.newsCrawlerService = newsCrawlerService;
        // 将注入的爬取历史服务实例赋值给类成员变量。
        this.crawlHistoryService = crawlHistoryService;
        // 将注入的用户服务实例赋值给类成员变量。
        this.userService = userService;
        // 创建并初始化一个ObjectMapper实例。
        this.objectMapper = new ObjectMapper();
    } // 构造函数结束。

    // 将此方法映射到HTTP POST请求的"/from-index"路径。
    @PostMapping("/from-index")
    // 定义从入口页进行二级爬取的API端点。
    public ResponseEntity<Map<String, Object>> crawlFromIndex(@RequestBody Map<String, String> payload) {
        // 从请求体Map中获取"url"字段的值。
        String indexUrl = payload.get("url");
        // 记录收到二级爬取请求的日志。
        logger.info("收到二级爬取请求，入口页面: {}", indexUrl);

        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试执行二级爬取，入口URL: {}", indexUrl);
            // 返回401未授权状态和错误信息。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未认证"));
        } // if条件结束。

        // 检查入口URL是否为null或空。
        if (indexUrl == null || indexUrl.isBlank()) {
            // 如果是，则记录警告日志。
            logger.warn("二级爬取请求中入口URL为空");
            // 返回400错误请求状态和错误信息。
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "入口URL不能为空"));
        } // if条件结束。

        // 获取已认证用户的用户名。
        String username = authentication.getName();
        // 声明一个长整型变量用于存储用户ID。
        Long userId;
        // 开始一个try块，用于捕获获取用户信息时可能发生的异常。
        try {
            // 调用用户服务根据用户名查找用户实体。
            User user = userService.findByUsername(username);
            // 从用户实体中获取用户ID。
            userId = user.getId();
            // 记录调试日志，显示用户名和对应的ID。
            logger.debug("用户 [{}] ID: {}", username, userId);
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取用户ID失败的错误日志。
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "获取用户信息失败"));
        } // try-catch结束。

        // 开始一个try块，处理爬取过程中可能发生的IO异常。
        try {
            // 创建一个新的CrawlHistory实体对象。
            CrawlHistory crawlHistory = new CrawlHistory();
            // 为历史记录设置用户ID。
            crawlHistory.setUserId(userId);
            // 为历史记录设置爬取类型。
            crawlHistory.setCrawlType("INDEX_CRAWL");
            // 为历史记录设置被爬取的入口URL。
            crawlHistory.setUrl(indexUrl);
            // 为历史记录设置一个临时的标题。
            crawlHistory.setTitle("二级爬取任务，入口页面: " + indexUrl);
            
            // 保存初始的历史记录，并获取包含数据库生成ID的返回对象。
            crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
            
            // 执行二级爬取，并传递CrawlHistory对象以建立关联。
            List<NewsData> crawledNews = newsCrawlerService.crawlNewsFromIndexPage(indexUrl, crawlHistory);
            
            // 根据爬取结果更新历史记录的标题。
            crawlHistory.setTitle("二级爬取，成功获取 " + crawledNews.size() + " 条新闻");
            
            // 创建一个Map来存储详细的爬取参数。
            Map<String, Object> params = new HashMap<>();
            // 将入口URL存入参数Map。
            params.put("entryUrl", indexUrl);
            // 将爬取到的总数存入参数Map。
            params.put("totalCount", crawledNews.size());
            // 将最多5个样本URL存入参数Map。
            params.put("sampleUrls", crawledNews.stream()
                    // 限制流中最多有5个元素。
                    .limit(5)
                    // 提取每个NewsData对象的URL。
                    .map(NewsData::getUrl)
                    // 将提取出的URL收集到一个新的列表中。
                    .collect(Collectors.toList()));
            
            // 开始一个try块，处理JSON转换时可能发生的异常。
            try {
                // 将参数Map转换为JSON字符串并设置到历史记录中。
                crawlHistory.setParams(objectMapper.writeValueAsString(params));
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录参数转换失败的警告日志。
                logger.warn("转换参数时出错: {}", e.getMessage());
                // 设置一个表示错误的JSON字符串。
                crawlHistory.setParams("{\"error\":\"转换参数时出错\"}");
            } // try-catch结束。
            
            // 再次保存历史记录以更新标题和参数。
            crawlHistoryService.saveHistory(crawlHistory);
            
            // 创建一个Map来组织返回给前端的结果。
            Map<String, Object> result = new HashMap<>();
            // 设置成功消息。
            result.put("message", "二级爬取任务完成");
            // 设置爬取到的数量。
            result.put("crawledCount", crawledNews.size());
            // 设置入口URL。
            result.put("entryUrl", indexUrl);
            // 设置所有爬取到的新闻标题列表。
            result.put("titles", crawledNews.stream().map(NewsData::getTitle).collect(Collectors.toList()));
            
            // 记录二级爬取完成的日志。
            logger.info("二级爬取完成，从 {} 获取了 {} 条新闻", indexUrl, crawledNews.size());
            // 返回200 OK状态以及包含结果的Map。
            return ResponseEntity.ok(result);
            
        } catch (IOException e) { // 捕获IO异常。
            // 记录二级爬取过程中发生的错误日志。
            logger.error("二级爬取过程中发生错误: {}", e.getMessage());
            // 创建一个Map来组织错误返回信息。
            Map<String, Object> errorResult = new HashMap<>();
            // 设置错误消息。
            errorResult.put("error", "爬取过程中发生错误: " + e.getMessage());
            // 设置入口URL。
            errorResult.put("entryUrl", indexUrl);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        } // try-catch结束。
    } // crawlFromIndex方法结束。

    // 将此方法映射到HTTP POST请求的"/by-keyword"路径。
    @PostMapping("/by-keyword")
    // 定义按关键词爬取的API端点。
    public ResponseEntity<Map<String, Object>> crawlByKeyword(@RequestBody Map<String, String> payload) {
        // 从请求体Map中获取"keyword"字段的值。
        String keyword = payload.get("keyword");
        // 从请求体Map中获取"url"字段的值，如果不存在则使用默认值。
        String indexUrl = payload.getOrDefault("url", "https://news.sina.com.cn/");
        
        // 记录收到关键词爬取请求的日志。
        logger.info("收到关键词爬取请求，关键词: {}, 入口页面: {}", keyword, indexUrl);

        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试执行关键词爬取，关键词: {}", keyword);
            // 返回401未授权状态和错误信息。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未认证"));
        } // if条件结束。

        // 检查关键词是否为null或空。
        if (keyword == null || keyword.isBlank()) {
            // 如果是，则记录警告日志。
            logger.warn("关键词爬取请求中关键词为空");
            // 返回400错误请求状态和错误信息。
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "关键词不能为空"));
        } // if条件结束。

        // 获取已认证用户的用户名。
        String username = authentication.getName();
        // 声明一个长整型变量用于存储用户ID。
        Long userId;
        // 开始一个try块，用于捕获获取用户信息时可能发生的异常。
        try {
            // 调用用户服务根据用户名查找用户实体。
            User user = userService.findByUsername(username);
            // 从用户实体中获取用户ID。
            userId = user.getId();
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取用户ID失败的错误日志。
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "获取用户信息失败"));
        } // try-catch结束。

        // 开始一个try块，处理爬取过程中可能发生的IO异常。
        try {
            // 创建一个新的CrawlHistory实体对象。
            CrawlHistory crawlHistory = new CrawlHistory();
            // 为历史记录设置用户ID。
            crawlHistory.setUserId(userId);
            // 为历史记录设置爬取类型。
            crawlHistory.setCrawlType("INDEX_CRAWL");
            // 为历史记录设置被爬取的入口URL。
            crawlHistory.setUrl(indexUrl);
            // 为历史记录设置一个临时的标题。
            crawlHistory.setTitle("关键词爬取任务: " + keyword);
            
            // 保存初始的历史记录，并获取包含数据库生成ID的返回对象。
            crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
            
            // 执行关键词爬取，并传递CrawlHistory对象以建立关联。
            List<NewsData> crawledNews = newsCrawlerService.crawlNewsByKeyword(keyword, indexUrl, crawlHistory);
            
            // 根据爬取结果更新历史记录的标题。
            crawlHistory.setTitle("关键词爬取: " + keyword + "，成功获取 " + crawledNews.size() + " 条新闻");
            
            // 创建一个Map来存储详细的爬取参数。
            Map<String, Object> params = new HashMap<>();
            // 将关键词存入参数Map。
            params.put("keyword", keyword);
            // 将入口URL存入参数Map。
            params.put("entryUrl", indexUrl);
            // 将爬取到的总数存入参数Map。
            params.put("totalCount", crawledNews.size());
            // 将最多5个样本URL存入参数Map。
            params.put("sampleUrls", crawledNews.stream()
                    // 限制流中最多有5个元素。
                    .limit(5)
                    // 提取每个NewsData对象的URL。
                    .map(NewsData::getUrl)
                    // 将提取出的URL收集到一个新的列表中。
                    .collect(Collectors.toList()));
            
            // 开始一个try块，处理JSON转换时可能发生的异常。
            try {
                // 将参数Map转换为JSON字符串并设置到历史记录中。
                crawlHistory.setParams(objectMapper.writeValueAsString(params));
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录参数转换失败的警告日志。
                logger.warn("转换参数时出错: {}", e.getMessage());
                // 设置一个表示错误的JSON字符串。
                crawlHistory.setParams("{\"error\":\"转换参数时出错\"}");
            } // try-catch结束。
            
            // 再次保存历史记录以更新标题和参数。
            crawlHistoryService.saveHistory(crawlHistory);
            
            // 创建一个Map来组织返回给前端的结果。
            Map<String, Object> result = new HashMap<>();
            // 设置成功消息。
            result.put("message", "关键词爬取任务完成");
            // 设置爬取到的数量。
            result.put("crawledCount", crawledNews.size());
            // 设置关键词。
            result.put("keyword", keyword);
            // 设置入口URL。
            result.put("entryUrl", indexUrl);
            // 设置所有爬取到的新闻标题列表。
            result.put("titles", crawledNews.stream().map(NewsData::getTitle).collect(Collectors.toList()));
            
            // 记录关键词爬取完成的日志。
            logger.info("关键词爬取完成，关键词: {}, 获取了 {} 条新闻", keyword, crawledNews.size());
            // 返回200 OK状态以及包含结果的Map。
            return ResponseEntity.ok(result);
            
        } catch (IOException e) { // 捕获IO异常。
            // 记录关键词爬取过程中发生的错误日志。
            logger.error("关键词爬取过程中发生错误: {}", e.getMessage());
            // 创建一个Map来组织错误返回信息。
            Map<String, Object> errorResult = new HashMap<>();
            // 设置错误消息。
            errorResult.put("error", "爬取过程中发生错误: " + e.getMessage());
            // 设置关键词。
            errorResult.put("keyword", keyword);
            // 设置入口URL。
            errorResult.put("entryUrl", indexUrl);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        } // try-catch结束。
    } // crawlByKeyword方法结束。
} // BatchCrawlerController类定义结束。