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
} 