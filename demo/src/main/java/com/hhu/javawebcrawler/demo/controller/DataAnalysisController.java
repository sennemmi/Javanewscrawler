package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.service.DataAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * 数据分析控制器
 * <p>
 * 提供新闻文本数据分析功能，如词云、热词统计和时间趋势分析
 * </p>
 */
@RestController
@RequestMapping("/api/analysis")
public class DataAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisController.class);
    
    private final DataAnalysisService dataAnalysisService;

    public DataAnalysisController(DataAnalysisService dataAnalysisService) {
        this.dataAnalysisService = dataAnalysisService;
    }

    /**
     * 获取词云数据
     * <p>
     * 分析指定时间范围内的新闻数据，生成词云所需的词频统计结果
     * </p>
     * 
     * @param params 包含分析参数的请求体
     * @return 词云数据（词语及其权重）
     */
    @PostMapping("/word-cloud")
    public ResponseEntity<?> getWordCloudData(@RequestBody Map<String, Object> params) {
        try {
            logger.info("收到词云数据请求: {}", params);
            
            // 确认用户已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("未认证用户尝试访问词云API");
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            }
            
            // 获取请求参数
            String source = "keywords"; // 固定为keywords
            Integer limit;
            try {
                limit = Integer.parseInt(params.getOrDefault("limit", "50").toString());
            } catch (NumberFormatException e) {
                logger.warn("无效的limit参数: {}", params.get("limit"));
                limit = 50; // 默认值
            }
            
            // 获取历史ID参数（必填）
            Long historyId = null;
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                try {
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) {
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                }
            } else {
                logger.warn("缺少historyId参数");
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            }
            
            logger.info("词云分析参数: source={}, limit={}, historyId={}", 
                       source, limit, historyId);
            
            // 获取词云数据
            List<Map<String, Object>> wordCloudData = dataAnalysisService.generateWordCloudData(source, limit, historyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("wordCloud", wordCloudData);
            response.put("total", wordCloudData.size());
            
            logger.info("成功生成词云数据，共 {} 条", wordCloudData.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("生成词云数据时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "生成词云数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热词排行榜
     * <p>
     * 分析指定时间范围内的新闻关键词，生成热词排行榜
     * </p>
     * 
     * @param params 包含分析参数的请求体
     * @return 热词排行榜数据
     */
    @PostMapping("/hot-words")
    public ResponseEntity<?> getHotWords(@RequestBody Map<String, Object> params) {
        try {
            logger.info("收到热词排行榜请求: {}", params);
            
            // 确认用户已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("未认证用户尝试访问热词API");
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            }
            
            // 获取请求参数
            Integer limit;
            try {
                limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
            } catch (NumberFormatException e) {
                logger.warn("无效的limit参数: {}", params.get("limit"));
                limit = 20; // 默认值
            }
            
            // 获取历史ID参数（必填）
            Long historyId = null;
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                try {
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) {
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                }
            } else {
                logger.warn("缺少historyId参数");
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            }
            
            logger.info("热词分析参数: limit={}, historyId={}", 
                       limit, historyId);
            
            // 获取热词数据
            List<Map<String, Object>> hotWords = dataAnalysisService.getHotWords(limit, historyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hotWords", hotWords);
            response.put("total", hotWords.size());
            
            logger.info("成功生成热词数据，共 {} 条", hotWords.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取热词排行榜时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取热词排行榜失败: " + e.getMessage()));
        }
    }

    /**
     * 获取时间趋势分析数据
     * <p>
     * 分析指定关键词在一段时间内的出现频率变化
     * </p>
     * 
     * @param params 包含分析参数的请求体
     * @return 时间趋势数据
     */
    @PostMapping("/time-trend")
    public ResponseEntity<?> getTimeTrend(@RequestBody Map<String, Object> params) {
        try {
            logger.info("收到时间趋势分析请求: {}", params);
            
            // 确认用户已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("未认证用户尝试访问时间趋势API");
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            }
            
            // 获取请求参数
            String keyword = params.getOrDefault("keyword", "").toString();
            if (keyword.isEmpty()) {
                logger.warn("缺少关键词参数");
                return ResponseEntity.badRequest().body(Map.of("error", "关键词不能为空"));
            }
            
            String timeUnit = params.getOrDefault("timeUnit", "day").toString();
            
            // 获取历史ID参数（必填）
            Long historyId = null;
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                try {
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) {
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                }
            } else {
                logger.warn("缺少historyId参数");
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            }
            
            logger.info("时间趋势分析参数: keyword={}, timeUnit={}, historyId={}", 
                       keyword, timeUnit, historyId);
            
            // 获取时间趋势数据
            List<Map<String, Object>> trendData = dataAnalysisService.getKeywordTimeTrend(keyword, timeUnit, historyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("timeUnit", timeUnit);
            response.put("trendData", trendData);
            
            logger.info("成功生成时间趋势数据，共 {} 条", trendData.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取时间趋势分析数据时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取时间趋势分析数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取内容来源分布
     * <p>
     * 分析新闻的来源分布情况
     * </p>
     * 
     * @param params 包含分析参数的请求体
     * @return 来源分布数据
     */
    @PostMapping("/source-distribution")
    public ResponseEntity<?> getSourceDistribution(@RequestBody Map<String, Object> params) {
        try {
            logger.info("收到内容来源分布请求: {}", params);
            
            // 确认用户已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("未认证用户尝试访问来源分布API");
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            }
            
            // 获取历史ID参数（必填）
            Long historyId = null;
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                try {
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) {
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                }
            } else {
                logger.warn("缺少historyId参数");
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            }
            
            logger.info("来源分布分析参数: historyId={}", 
                       historyId);
            
            // 获取来源分布数据
            List<Map<String, Object>> sourceData = dataAnalysisService.getSourceDistribution(historyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sourceDistribution", sourceData);
            response.put("total", sourceData.size());
            
            logger.info("成功生成来源分布数据，共 {} 条", sourceData.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取内容来源分布时发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "获取内容来源分布失败: " + e.getMessage()));
        }
    }
} 