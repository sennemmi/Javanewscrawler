package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.ScheduledCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 定时爬取任务控制器
 * 提供管理定时爬取任务的API接口
 */
@RestController
@RequestMapping("/api/scheduled")
public class ScheduledCrawlerController {

    private final ScheduledCrawlerService scheduledCrawlerService;
    private final UserService userService;
    public ScheduledCrawlerController(ScheduledCrawlerService scheduledCrawlerService, UserService userService) {
        this.scheduledCrawlerService = scheduledCrawlerService;
        this.userService = userService;
    }

    /**
     * 【高级】手动触发定时爬取新浪新闻首页的任务
     * 
     * @return 爬取结果信息
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerScheduledCrawl() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户未认证"));
        }
        
        try {
            // 获取用户ID
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            Long userId = user.getId();
            
            // 触发爬取任务
            Map<String, Object> result = scheduledCrawlerService.manualCrawlSinaHomepage(userId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "触发爬取任务失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 【管理】获取定时爬取任务的状态信息
     * 
     * @return 定时任务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getScheduledTaskStatus() {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "用户未认证"));
        }
        
        // 这里只是返回一个简单的状态信息
        // 实际项目中可能需要从数据库或缓存中获取更详细的状态
        Map<String, Object> status = new HashMap<>();
        status.put("isEnabled", true);
        status.put("scheduledTimes", "每日8:00和16:00");
        status.put("targetUrl", "https://news.sina.com.cn/");
        status.put("lastUpdateTime", "获取最新一次执行记录"); // 实际项目中应从数据库获取
        
        return ResponseEntity.ok(status);
    }
} 