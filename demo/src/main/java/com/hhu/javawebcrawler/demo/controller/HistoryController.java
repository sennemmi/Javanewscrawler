package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
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

/**
 * 历史记录控制器
 * <p>
 * 负责处理爬取历史记录的查询功能。
 * </p>
 */
@RestController
@RequestMapping("/api")
public class HistoryController {

    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    
    private final CrawlHistoryService crawlHistoryService;
    private final UserService userService;

    public HistoryController(CrawlHistoryService crawlHistoryService, UserService userService) {
        this.crawlHistoryService = crawlHistoryService;
        this.userService = userService;
    }

    /**
     * 获取当前登录用户的爬取历史记录
     * <p>
     * 返回用户的所有爬取历史记录，按时间倒序排列。
     * </p>
     * 
     * @return 爬取历史记录列表
     */
    @GetMapping("/history")
    public ResponseEntity<List<CrawlHistory>> getHistory() {
        logger.info("收到获取爬取历史记录请求");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试获取爬取历史记录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
            logger.debug("用户 [{}] ID: {}", username, userId);
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        List<CrawlHistory> history = crawlHistoryService.getUserHistory(userId);
        logger.info("成功获取用户 [{}] 的爬取历史记录，共 {} 条", username, history.size());
        return ResponseEntity.ok(history);
    }
    
    /**
     * 删除单个历史记录
     * <p>
     * 删除指定ID的历史记录，确保只能删除自己的记录。
     * </p>
     * 
     * @param id 要删除的历史记录ID
     * @return 删除结果
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable Long id) {
        logger.info("收到删除单个历史记录请求，ID: {}", id);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试删除历史记录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "获取用户信息失败"
            ));
        }
        
        boolean deleted = crawlHistoryService.deleteHistoryIfBelongsToUser(id, userId);
        if (deleted) {
            logger.info("用户 [{}] 成功删除历史记录 ID: {}", username, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "历史记录已删除"
            ));
        } else {
            logger.warn("用户 [{}] 尝试删除不存在或不属于该用户的历史记录 ID: {}", username, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "message", "无法删除该历史记录，记录不存在或不属于当前用户"
            ));
        }
    }
    
    /**
     * 批量删除历史记录
     * <p>
     * 删除指定ID列表中的历史记录，确保只能删除自己的记录。
     * </p>
     * 
     * @param ids 要删除的历史记录ID列表
     * @return 删除结果
     */
    @DeleteMapping("/history/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteHistory(@RequestBody Map<String, List<Long>> payload) {
        List<Long> ids = payload.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "未提供要删除的ID列表"
            ));
        }
        
        logger.info("收到批量删除历史记录请求，ID数量: {}", ids.size());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试批量删除历史记录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "获取用户信息失败"
            ));
        }
        
        int deletedCount = crawlHistoryService.batchDeleteHistoryIfBelongsToUser(ids, userId);
        logger.info("用户 [{}] 批量删除历史记录完成，成功删除 {}/{} 条记录", username, deletedCount, ids.size());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", String.format("成功删除 %d 条历史记录", deletedCount),
            "deletedCount", deletedCount,
            "totalRequested", ids.size()
        ));
    }
    
    /**
     * 清空所有历史记录
     * <p>
     * 删除当前用户的所有历史记录。
     * </p>
     * 
     * @return 删除结果
     */
    @DeleteMapping("/history/all")
    public ResponseEntity<Map<String, Object>> clearAllHistory() {
        logger.info("收到清空所有历史记录请求");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("未认证用户尝试清空历史记录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "获取用户信息失败"
            ));
        }
        
        int deletedCount = crawlHistoryService.deleteAllHistoryByUserId(userId);
        logger.info("用户 [{}] 清空历史记录完成，共删除 {} 条记录", username, deletedCount);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", String.format("已清空所有历史记录，共删除 %d 条", deletedCount),
            "deletedCount", deletedCount
        ));
    }
} 