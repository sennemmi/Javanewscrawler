package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.service.DataAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

// 声明这是一个RESTful风格的控制器。
@RestController
// 将此控制器下的所有请求路径映射到"/api/analysis"下。
@RequestMapping("/api/analysis")
// 定义一个名为 DataAnalysisController 的公开类。
public class DataAnalysisController {

    // 创建一个静态不可变的Logger实例，用于记录日志。
    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisController.class);
    
    // 声明一个不可变的数据分析服务字段。
    private final DataAnalysisService dataAnalysisService;

    // 定义类的构造函数，通过它注入数据分析服务依赖。
    public DataAnalysisController(DataAnalysisService dataAnalysisService) {
        // 将注入的数据分析服务实例赋值给类成员变量。
        this.dataAnalysisService = dataAnalysisService;
    } // 构造函数结束。

    // 将此方法映射到HTTP POST请求的"/word-cloud"路径。
    @PostMapping("/word-cloud")
    // 定义获取词云数据的API端点。
    public ResponseEntity<?> getWordCloudData(@RequestBody Map<String, Object> params) {
        // 开始一个try块，用于捕获整个处理过程中可能发生的异常。
        try {
            // 记录收到词云数据请求的日志，并包含请求参数。
            logger.info("收到词云数据请求: {}", params);
            
            // 从Spring Security上下文中获取当前的认证信息。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 检查用户是否已认证。
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未认证，则记录警告日志。
                logger.warn("未认证用户尝试访问词云API");
                // 返回401未授权状态和错误信息。
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            } // if条件结束。
            
            // 定义分析源，此处固定为"keywords"。
            String source = "keywords";
            // 声明一个整数变量用于存储词云数量限制。
            Integer limit;
            // 开始一个try块，用于处理可能发生的数字格式化异常。
            try {
                // 从请求参数中获取"limit"值，如果不存在则默认为"50"，并转换为整数。
                limit = Integer.parseInt(params.getOrDefault("limit", "50").toString());
            } catch (NumberFormatException e) { // 捕获数字格式化异常。
                // 记录无效limit参数的警告日志。
                logger.warn("无效的limit参数: {}", params.get("limit"));
                // 使用默认值50。
                limit = 50;
            } // try-catch结束。
            
            // 声明一个长整型变量用于存储历史记录ID。
            Long historyId = null;
            // 检查请求参数中是否包含有效的"historyId"。
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                // 开始一个try块，处理可能发生的数字格式化异常。
                try {
                    // 将"historyId"参数转换为长整型。
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) { // 捕获数字格式化异常。
                    // 记录无效historyId参数的警告日志。
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    // 返回400错误请求状态和错误信息。
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                } // try-catch结束。
            } else { // 如果不包含"historyId"。
                // 记录缺少historyId参数的警告日志。
                logger.warn("缺少historyId参数");
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            } // if-else结束。
            
            // 记录最终用于分析的参数。
            logger.info("词云分析参数: source={}, limit={}, historyId={}", 
                       source, limit, historyId);
            
            // 调用数据分析服务生成词云数据。
            List<Map<String, Object>> wordCloudData = dataAnalysisService.generateWordCloudData(source, limit, historyId);
            
            // 创建一个新的HashMap来组织响应数据。
            Map<String, Object> response = new HashMap<>();
            // 将词云数据放入响应Map。
            response.put("wordCloud", wordCloudData);
            // 将词云数据的总数放入响应Map。
            response.put("total", wordCloudData.size());
            
            // 记录成功生成词云数据的日志。
            logger.info("成功生成词云数据，共 {} 条", wordCloudData.size());
            // 返回200 OK状态以及包含结果的响应体。
            return ResponseEntity.ok(response);
            
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录生成词云数据时发生的错误日志。
            logger.error("生成词云数据时发生错误: {}", e.getMessage(), e);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(500).body(Map.of("error", "生成词云数据失败: " + e.getMessage()));
        } // try-catch结束。
    } // getWordCloudData方法结束。

    // 将此方法映射到HTTP POST请求的"/hot-words"路径。
    @PostMapping("/hot-words")
    // 定义获取热词排行榜的API端点。
    public ResponseEntity<?> getHotWords(@RequestBody Map<String, Object> params) {
        // 开始一个try块，用于捕获整个处理过程中可能发生的异常。
        try {
            // 记录收到热词排行榜请求的日志。
            logger.info("收到热词排行榜请求: {}", params);
            
            // 从Spring Security上下文中获取当前的认证信息。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 检查用户是否已认证。
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未认证，则记录警告日志。
                logger.warn("未认证用户尝试访问热词API");
                // 返回401未授权状态和错误信息。
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            } // if条件结束。
            
            // 声明一个整数变量用于存储热词数量限制。
            Integer limit;
            // 开始一个try块，用于处理可能发生的数字格式化异常。
            try {
                // 从请求参数中获取"limit"值，如果不存在则默认为"20"，并转换为整数。
                limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
            } catch (NumberFormatException e) { // 捕获数字格式化异常。
                // 记录无效limit参数的警告日志。
                logger.warn("无效的limit参数: {}", params.get("limit"));
                // 使用默认值20。
                limit = 20;
            } // try-catch结束。
            
            // 声明一个长整型变量用于存储历史记录ID。
            Long historyId = null;
            // 检查请求参数中是否包含有效的"historyId"。
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                // 开始一个try块，处理可能发生的数字格式化异常。
                try {
                    // 将"historyId"参数转换为长整型。
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) { // 捕获数字格式化异常。
                    // 记录无效historyId参数的警告日志。
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    // 返回400错误请求状态和错误信息。
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                } // try-catch结束。
            } else { // 如果不包含"historyId"。
                // 记录缺少historyId参数的警告日志。
                logger.warn("缺少historyId参数");
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            } // if-else结束。
            
            // 记录最终用于分析的参数。
            logger.info("热词分析参数: limit={}, historyId={}", 
                       limit, historyId);
            
            // 调用数据分析服务获取热词数据。
            List<Map<String, Object>> hotWords = dataAnalysisService.getHotWords(limit, historyId);
            
            // 创建一个新的HashMap来组织响应数据。
            Map<String, Object> response = new HashMap<>();
            // 将热词数据放入响应Map。
            response.put("hotWords", hotWords);
            // 将热词数据的总数放入响应Map。
            response.put("total", hotWords.size());
            
            // 记录成功生成热词数据的日志。
            logger.info("成功生成热词数据，共 {} 条", hotWords.size());
            // 返回200 OK状态以及包含结果的响应体。
            return ResponseEntity.ok(response);
            
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取热词排行榜时发生的错误日志。
            logger.error("获取热词排行榜时发生错误: {}", e.getMessage(), e);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(500).body(Map.of("error", "获取热词排行榜失败: " + e.getMessage()));
        } // try-catch结束。
    } // getHotWords方法结束。

    // 将此方法映射到HTTP POST请求的"/time-trend"路径。
    @PostMapping("/time-trend")
    // 定义获取时间趋势分析数据的API端点。
    public ResponseEntity<?> getTimeTrend(@RequestBody Map<String, Object> params) {
        // 开始一个try块，用于捕获整个处理过程中可能发生的异常。
        try {
            // 记录收到时间趋势分析请求的日志。
            logger.info("收到时间趋势分析请求: {}", params);
            
            // 从Spring Security上下文中获取当前的认证信息。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 检查用户是否已认证。
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未认证，则记录警告日志。
                logger.warn("未认证用户尝试访问时间趋势API");
                // 返回401未授权状态和错误信息。
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            } // if条件结束。
            
            // 从请求参数中获取"keyword"值，如果不存在则默认为空字符串。
            String keyword = params.getOrDefault("keyword", "").toString();
            // 检查关键词是否为空。
            if (keyword.isEmpty()) {
                // 如果是，则记录警告日志。
                logger.warn("缺少关键词参数");
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body(Map.of("error", "关键词不能为空"));
            } // if条件结束。
            
            // 从请求参数中获取"timeUnit"值，如果不存在则默认为"day"。
            String timeUnit = params.getOrDefault("timeUnit", "day").toString();
            
            // 声明一个长整型变量用于存储历史记录ID。
            Long historyId = null;
            // 检查请求参数中是否包含有效的"historyId"。
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                // 开始一个try块，处理可能发生的数字格式化异常。
                try {
                    // 将"historyId"参数转换为长整型。
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) { // 捕获数字格式化异常。
                    // 记录无效historyId参数的警告日志。
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    // 返回400错误请求状态和错误信息。
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                } // try-catch结束。
            } else { // 如果不包含"historyId"。
                // 记录缺少historyId参数的警告日志。
                logger.warn("缺少historyId参数");
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            } // if-else结束。
            
            // 记录最终用于分析的参数。
            logger.info("时间趋势分析参数: keyword={}, timeUnit={}, historyId={}", 
                       keyword, timeUnit, historyId);
            
            // 调用数据分析服务获取关键词的时间趋势数据。
            List<Map<String, Object>> trendData = dataAnalysisService.getKeywordTimeTrend(keyword, timeUnit, historyId);
            
            // 创建一个新的HashMap来组织响应数据。
            Map<String, Object> response = new HashMap<>();
            // 将关键词放入响应Map。
            response.put("keyword", keyword);
            // 将时间单位放入响应Map。
            response.put("timeUnit", timeUnit);
            // 将趋势数据放入响应Map。
            response.put("trendData", trendData);
            
            // 记录成功生成时间趋势数据的日志。
            logger.info("成功生成时间趋势数据，共 {} 条", trendData.size());
            // 返回200 OK状态以及包含结果的响应体。
            return ResponseEntity.ok(response);
            
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取时间趋势分析数据时发生的错误日志。
            logger.error("获取时间趋势分析数据时发生错误: {}", e.getMessage(), e);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(500).body(Map.of("error", "获取时间趋势分析数据失败: " + e.getMessage()));
        } // try-catch结束。
    } // getTimeTrend方法结束。

    // 将此方法映射到HTTP POST请求的"/source-distribution"路径。
    @PostMapping("/source-distribution")
    // 定义获取内容来源分布的API端点。
    public ResponseEntity<?> getSourceDistribution(@RequestBody Map<String, Object> params) {
        // 开始一个try块，用于捕获整个处理过程中可能发生的异常。
        try {
            // 记录收到内容来源分布请求的日志。
            logger.info("收到内容来源分布请求: {}", params);
            
            // 从Spring Security上下文中获取当前的认证信息。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 检查用户是否已认证。
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未认证，则记录警告日志。
                logger.warn("未认证用户尝试访问来源分布API");
                // 返回401未授权状态和错误信息。
                return ResponseEntity.status(401).body(Map.of("error", "用户未认证"));
            } // if条件结束。
            
            // 声明一个长整型变量用于存储历史记录ID。
            Long historyId = null;
            // 检查请求参数中是否包含有效的"historyId"。
            if (params.containsKey("historyId") && params.get("historyId") != null) {
                // 开始一个try块，处理可能发生的数字格式化异常。
                try {
                    // 将"historyId"参数转换为长整型。
                    historyId = Long.parseLong(params.get("historyId").toString());
                } catch (NumberFormatException e) { // 捕获数字格式化异常。
                    // 记录无效historyId参数的警告日志。
                    logger.warn("无效的historyId参数: {}", params.get("historyId"));
                    // 返回400错误请求状态和错误信息。
                    return ResponseEntity.badRequest().body(Map.of("error", "historyId参数无效或缺失"));
                } // try-catch结束。
            } else { // 如果不包含"historyId"。
                // 记录缺少historyId参数的警告日志。
                logger.warn("缺少historyId参数");
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body(Map.of("error", "historyId参数缺失"));
            } // if-else结束。
            
            // 记录最终用于分析的参数。
            logger.info("来源分布分析参数: historyId={}", 
                       historyId);
            
            // 调用数据分析服务获取来源分布数据。
            List<Map<String, Object>> sourceData = dataAnalysisService.getSourceDistribution(historyId);
            
            // 创建一个新的HashMap来组织响应数据。
            Map<String, Object> response = new HashMap<>();
            // 将来源分布数据放入响应Map。
            response.put("sourceDistribution", sourceData);
            // 将来源总数放入响应Map。
            response.put("total", sourceData.size());
            
            // 记录成功生成来源分布数据的日志。
            logger.info("成功生成来源分布数据，共 {} 条", sourceData.size());
            // 返回200 OK状态以及包含结果的响应体。
            return ResponseEntity.ok(response);
            
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            // 记录获取内容来源分布时发生的错误日志。
            logger.error("获取内容来源分布时发生错误: {}", e.getMessage(), e);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(500).body(Map.of("error", "获取内容来源分布失败: " + e.getMessage()));
        } // try-catch结束。
    } // getSourceDistribution方法结束。
} // DataAnalysisController类定义结束。