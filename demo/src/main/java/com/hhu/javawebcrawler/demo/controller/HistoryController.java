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
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 声明这是一个RESTful风格的控制器。
@RestController
// 将此控制器下的所有请求路径映射到"/api"下。
@RequestMapping("/api")
// 定义一个名为 HistoryController 的公开类。
public class HistoryController {

    // 创建一个静态不可变的Logger实例，用于记录日志。
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    
    // 声明一个不可变的爬取历史服务字段。
    private final CrawlHistoryService crawlHistoryService;
    // 声明一个不可变的用户服务字段。
    private final UserService userService;
    // 声明一个不可变的新闻爬虫服务字段。
    private final NewsCrawlerService newsCrawlerService;

    // 定义类的构造函数，通过它注入服务依赖。
    public HistoryController(CrawlHistoryService crawlHistoryService, UserService userService, NewsCrawlerService newsCrawlerService) {
        // 将注入的爬取历史服务实例赋值给类成员变量。
        this.crawlHistoryService = crawlHistoryService;
        // 将注入的用户服务实例赋值给类成员变量。
        this.userService = userService;
        // 将注入的新闻爬虫服务实例赋值给类成员变量。
        this.newsCrawlerService = newsCrawlerService;
    } // 构造函数结束。

    // 将此方法映射到HTTP GET请求的"/history"路径。
    @GetMapping("/history")
    // 定义获取用户历史记录的API端点。
    public ResponseEntity<List<CrawlHistory>> getHistory() {
        // 记录收到获取历史记录请求的日志。
        logger.info("收到获取爬取历史记录请求");
        
        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试获取爬取历史记录");
            // 返回401未授权状态。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
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
            // 返回500服务器内部错误状态。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } // try-catch结束。

        // 调用爬取历史服务获取该用户的历史记录列表。
        List<CrawlHistory> history = crawlHistoryService.getUserHistory(userId);
        // 记录成功获取用户历史记录的日志。
        logger.info("成功获取用户 [{}] 的爬取历史记录，共 {} 条", username, history.size());
        // 返回200 OK状态以及历史记录列表。
        return ResponseEntity.ok(history);
    } // getHistory方法结束。
    
    // 将此方法映射到HTTP DELETE请求的"/history/{id}"路径。
    @DeleteMapping("/history/{id}")
    // 定义删除单个历史记录的API端点。
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable Long id) {
        // 记录收到删除单个历史记录请求的日志。
        logger.info("收到删除单个历史记录请求，ID: {}", id);
        
        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试删除历史记录");
            // 返回401未授权状态。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                // 在响应体中设置success为false。
                "success", false,
                // 在响应体中设置错误消息。
                "message", "获取用户信息失败"
            )); // body构造结束。
        } // try-catch结束。
        
        // 调用服务尝试删除属于该用户的历史记录，并获取删除结果。
        boolean deleted = crawlHistoryService.deleteHistoryIfBelongsToUser(id, userId);
        // 检查是否删除成功。
        if (deleted) {
            // 如果成功，则记录成功日志。
            logger.info("用户 [{}] 成功删除历史记录 ID: {}", username, id);
            // 返回200 OK状态和成功信息。
            return ResponseEntity.ok(Map.of(
                // 在响应体中设置success为true。
                "success", true,
                // 在响应体中设置成功消息。
                "message", "历史记录已删除"
            )); // body构造结束。
        } else { // 如果删除失败。
            // 记录删除失败的警告日志。
            logger.warn("用户 [{}] 尝试删除不存在或不属于该用户的历史记录 ID: {}", username, id);
            // 返回403禁止访问状态和错误信息。
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                // 在响应体中设置success为false。
                "success", false,
                // 在响应体中设置错误消息。
                "message", "无法删除该历史记录，记录不存在或不属于当前用户"
            )); // body构造结束。
        } // if-else结束。
    } // deleteHistory方法结束。
    
    // 将此方法映射到HTTP DELETE请求的"/history/batch"路径。
    @DeleteMapping("/history/batch")
    // 定义批量删除历史记录的API端点。
    public ResponseEntity<Map<String, Object>> batchDeleteHistory(@RequestBody Map<String, List<Long>> payload) {
        // 从请求体中获取ID列表。
        List<Long> ids = payload.get("ids");
        // 检查ID列表是否为null或空。
        if (ids == null || ids.isEmpty()) {
            // 如果是，则返回400错误请求状态和错误信息。
            return ResponseEntity.badRequest().body(Map.of(
                // 在响应体中设置success为false。
                "success", false,
                // 在响应体中设置错误消息。
                "message", "未提供要删除的ID列表"
            )); // body构造结束。
        } // if条件结束。
        
        // 记录收到批量删除请求的日志。
        logger.info("收到批量删除历史记录请求，ID数量: {}", ids.size());
        
        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试批量删除历史记录");
            // 返回401未授权状态。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                // 在响应体中设置success为false。
                "success", false,
                // 在响应体中设置错误消息。
                "message", "获取用户信息失败"
            )); // body构造结束。
        } // try-catch结束。
        
        // 调用服务批量删除属于该用户的历史记录，并获取成功删除的数量。
        int deletedCount = crawlHistoryService.batchDeleteHistoryIfBelongsToUser(ids, userId);
        // 记录批量删除完成的日志。
        logger.info("用户 [{}] 批量删除历史记录完成，成功删除 {}/{} 条记录", username, deletedCount, ids.size());
        
        // 返回200 OK状态和操作结果信息。
        return ResponseEntity.ok(Map.of(
            // 在响应体中设置success为true。
            "success", true,
            // 在响应体中设置格式化的成功消息。
            "message", String.format("成功删除 %d 条历史记录", deletedCount),
            // 在响应体中包含成功删除的数量。
            "deletedCount", deletedCount,
            // 在响应体中包含请求删除的总数。
            "totalRequested", ids.size()
        )); // body构造结束。
    } // batchDeleteHistory方法结束。
    
    // 将此方法映射到HTTP DELETE请求的"/history/all"路径。
    @DeleteMapping("/history/all")
    // 定义清空所有历史记录的API端点。
    public ResponseEntity<Map<String, Object>> clearAllHistory() {
        // 记录收到清空所有历史记录请求的日志。
        logger.info("收到清空所有历史记录请求");
        
        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试清空历史记录");
            // 返回401未授权状态。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                // 在响应体中设置success为false。
                "success", false,
                // 在响应体中设置错误消息。
                "message", "获取用户信息失败"
            )); // body构造结束。
        } // try-catch结束。
        
        // 调用服务删除该用户的所有历史记录，并获取删除的数量。
        int deletedCount = crawlHistoryService.deleteAllHistoryByUserId(userId);
        // 记录清空历史记录完成的日志。
        logger.info("用户 [{}] 清空历史记录完成，共删除 {} 条记录", username, deletedCount);
        
        // 返回200 OK状态和操作结果信息。
        return ResponseEntity.ok(Map.of(
            // 在响应体中设置success为true。
            "success", true,
            // 在响应体中设置格式化的成功消息。
            "message", String.format("已清空所有历史记录，共删除 %d 条", deletedCount),
            // 在响应体中包含删除的数量。
            "deletedCount", deletedCount
        )); // body构造结束。
    } // clearAllHistory方法结束。

    // 将此方法映射到HTTP GET请求的"/history/{historyId}/news"路径。
    @GetMapping("/history/{historyId}/news")
    // 定义获取历史记录关联新闻的API端点。
    public ResponseEntity<List<NewsData>> getNewsByHistoryId(@PathVariable Long historyId) {
        // 记录收到获取关联新闻数据请求的日志。
        logger.info("收到获取爬取历史关联新闻数据请求，历史ID: {}", historyId);
        
        // 从Spring Security上下文中获取当前的认证信息。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证。
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果未认证，则记录警告日志。
            logger.warn("未认证用户尝试获取爬取历史关联新闻数据");
            // 返回401未授权状态。
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
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
            // 返回500服务器内部错误状态。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } // try-catch结束。
        
        // 调用服务根据ID查找历史记录，以验证所有权。
        Optional<CrawlHistory> historyOpt = crawlHistoryService.findById(historyId);
        // 检查历史记录是否存在，并且其用户ID是否与当前登录用户匹配。
        if (!historyOpt.isPresent() || !historyOpt.get().getUserId().equals(userId)) {
            // 如果不匹配，则记录警告日志。
            logger.warn("用户 [{}] 尝试访问不存在或不属于该用户的历史记录 ID: {}", username, historyId);
            // 返回403禁止访问状态。
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } // if条件结束。
        
        // 调用新闻服务根据历史记录ID查找关联的新闻列表。
        List<NewsData> newsList = newsCrawlerService.findNewsByCrawlHistoryId(historyId);
        // 记录成功获取关联新闻数据的日志。
        logger.info("成功获取历史记录 ID: {} 关联的新闻数据，共 {} 条", historyId, newsList.size());
        // 返回200 OK状态以及新闻数据列表。
        return ResponseEntity.ok(newsList);
    } // getNewsByHistoryId方法结束。
} // HistoryController类定义结束。