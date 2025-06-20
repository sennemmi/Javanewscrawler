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

// 声明这是一个RESTful风格的控制器。
@RestController
// 将此控制器下的所有请求路径映射到"/api"下。
@RequestMapping("/api")
// 定义一个名为 SingleCrawlerController 的公开类，它继承自 BaseController。
public class SingleCrawlerController extends BaseController {

    // 创建一个静态不可变的Logger实例，用于记录日志。
    private static final Logger logger = LoggerFactory.getLogger(SingleCrawlerController.class);
    
    // 声明一个不可变的新闻爬虫服务字段。
    private final NewsCrawlerService newsCrawlerService;
    // 声明一个不可变的爬取历史服务字段。
    private final CrawlHistoryService crawlHistoryService;
    // 声明一个不可变的用户服务字段。
    private final UserService userService;

    // 定义类的构造函数，通过它注入服务依赖。
    public SingleCrawlerController(NewsCrawlerService newsCrawlerService, 
                              CrawlHistoryService crawlHistoryService,
                              UserService userService) {
        // 将注入的新闻爬虫服务实例赋值给类成员变量。
        this.newsCrawlerService = newsCrawlerService;
        // 将注入的爬取历史服务实例赋值给类成员变量。
        this.crawlHistoryService = crawlHistoryService;
        // 将注入的用户服务实例赋值给类成员变量。
        this.userService = userService;
    } // 构造函数结束。

    // 将此方法映射到HTTP POST请求的"/crawl/single"路径。
    @PostMapping("/crawl/single")
    // 定义爬取单个URL的API端点，接收一个包含URL的请求体。
    public ResponseEntity<Object> crawlSingleUrl(@RequestBody Map<String, String> payload) {
        // 记录收到单个URL爬取请求的日志。
        logger.info("收到单个URL爬取请求: {}", payload.get("url"));
        
        // 调用父类方法，验证当前用户是否已认证。
        validateAuthentication();
        
        // 从请求体Map中获取"url"字段的值。
        String url = payload.get("url");
        // 调用父类方法，验证URL参数是否有效（非空）。
        validateStringParam(url, "URL");
        
        // 调用父类方法，获取当前登录用户的ID。
        Long userId = getCurrentUserId(userService);
        
        // 返回200 OK状态，响应体内容由lambda表达式的结果决定。
        return ResponseEntity.ok(
            // 调用父类方法，执行一个代码块并统一处理其中可能抛出的异常。
            executeWithExceptionHandling(() -> {
                // 开始一个try块，处理可能发生的IO异常。
                try {
                    // 创建一个新的CrawlHistory实体对象。
                    CrawlHistory crawlHistory = new CrawlHistory();
                    // 为历史记录设置用户ID。
                    crawlHistory.setUserId(userId);
                    // 为历史记录设置爬取类型为“单个URL”。
                    crawlHistory.setCrawlType("SINGLE_URL");
                    // 为历史记录设置被爬取的URL。
                    crawlHistory.setUrl(url);
                    // 为历史记录设置一个临时的标题。
                    crawlHistory.setTitle("单URL爬取任务: " + url);
                    
                    // 保存初始的历史记录，并获取包含数据库生成ID的返回对象。
                    crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
                    
                    // 调用新闻爬虫服务爬取新闻，并关联上一步创建的历史记录。
                    Optional<NewsData> newsDataOpt = newsCrawlerService.crawlAndSaveSinaNews(url, crawlHistory);
                    
                    // 检查爬取结果是否包含有效数据。
                    if (newsDataOpt.isPresent()) {
                        // 从Optional中获取新闻数据实体。
                        NewsData newsData = newsDataOpt.get();
                        
                        // 使用爬取到的新闻标题更新历史记录的标题。
                        crawlHistory.setTitle(newsData.getTitle());
                        // 再次保存历史记录以更新标题。
                        crawlHistoryService.saveHistory(crawlHistory);
                        
                        // 记录成功爬取URL的日志。
                        logger.info("成功爬取URL: {}, 标题: {}", url, newsData.getTitle());
                        // 返回成功爬取到的新闻数据作为响应体。
                        return newsData;
                    } else { // 如果爬取结果为空。
                        // 更新历史记录的标题为失败状态。
                        crawlHistory.setTitle("爬取失败: 内容提取失败");
                        // 保存更新后的失败历史记录。
                        crawlHistoryService.saveHistory(crawlHistory);
                        
                        // 记录内容提取失败的警告日志。
                        logger.warn("爬取URL成功，但内容提取失败，跳过保存和记录历史: {}", url);
                        // 返回一个表示内容提取失败的响应体。
                        return new ResponseEntity<>("内容提取失败", HttpStatus.NO_CONTENT).getBody();
                    } // if-else结束。
                } catch (IOException e) { // 捕获IO异常。
                    // 记录爬取失败的错误日志。
                    logger.error("爬取URL失败: {} - {}", url, e.getMessage());
                    // 将IO异常包装成运行时异常向上抛出，由executeWithExceptionHandling处理。
                    throw new RuntimeException("爬取URL失败: " + e.getMessage(), e);
                } // try-catch结束。
            }) // lambda表达式结束。
        ); // ResponseEntity构造结束。
    } // crawlSingleUrl方法结束。
    
    // 将此方法映射到HTTP GET请求的"/news/detail"路径。
    @GetMapping("/news/detail")
    // 定义获取已爬取新闻详情的API端点，接收URL作为请求参数。
    public ResponseEntity<NewsData> getNewsDetail(@RequestParam String url) {
        // 记录收到获取新闻详情请求的日志。
        logger.info("收到获取新闻详情请求: {}", url);
        
        // 调用父类方法，验证当前用户是否已认证。
        validateAuthentication();
        
        // 调用父类方法，验证URL参数是否有效。
        validateStringParam(url, "URL");
        
        // 开始一个try块，处理URL解码和数据库查询中可能发生的异常。
        try {
            // 对传入的URL进行UTF-8解码，以处理可能被编码的字符。
            String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            // 记录解码后的URL。
            logger.info("解码后的URL: {}", decodedUrl);
            
            // 首先使用原始（可能编码的）URL从数据库查询新闻。
            Optional<NewsData> newsDataOpt = newsCrawlerService.findNewsByUrl(url);
            
            // 如果使用原始URL未找到新闻。
            if (!newsDataOpt.isPresent()) {
                // 则尝试使用解码后的URL再次查询。
                newsDataOpt = newsCrawlerService.findNewsByUrl(decodedUrl);
                // 记录使用解码后URL进行查询的日志。
                logger.info("使用解码后URL重新查询");
            } // if条件结束。
            
            // 检查最终是否找到了新闻数据。
            if (newsDataOpt.isPresent()) {
                // 记录成功获取新闻详情的日志。
                logger.info("成功获取新闻详情: {}", newsDataOpt.get().getTitle());
                // 返回200 OK状态以及找到的新闻数据。
                return ResponseEntity.ok(newsDataOpt.get());
            } else { // 如果两种方式都未找到新闻。
                // 记录未找到新闻详情的警告日志。
                logger.warn("未找到URL对应的新闻详情，原始URL: {}, 解码URL: {}", url, decodedUrl);
                // 返回404 Not Found状态。
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            } // if-else结束。
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取新闻详情时发生的错误日志。
            logger.error("获取新闻详情时发生错误: {}", e.getMessage(), e);
            // 返回500服务器内部错误状态。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } // try-catch结束。
    } // getNewsDetail方法结束。
} // SingleCrawlerController类定义结束。