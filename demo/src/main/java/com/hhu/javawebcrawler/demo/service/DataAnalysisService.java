package com.hhu.javawebcrawler.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 * <p>
 * 提供新闻文本数据分析功能，包括词云生成、热词统计和时间趋势分析
 * </p>
 */
@Service
public class DataAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisService.class);
    
    // 停用词列表：常见的没有分析价值的词
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "和", "是", "在", "我", "有", "你", "他", "她", "它", "这", "那", "都", "也",
        "就", "要", "会", "到", "可以", "被", "等", "与", "以", "及", "但", "但是", "而", "或", "则",
        "因为", "所以", "如果", "只要", "只有", "不", "没有", "一个", "一种", "一样", "一点", "一些",
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", 
        "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "日", "月", "年", "最", "多",
        "人", "从", "对", "能", "为", "地", "得", "着", "说", "上", "下", "中", "前", "后", "里",
        "中国", "记者", "报道", "新闻", "来源", "时间", "今天", "记者", "编辑", "频道", "评论"
    ));
    
    // 用于分词的简单正则表达式模式
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+|[a-zA-Z]+");
    
    private final JdbcTemplate jdbcTemplate;

    public DataAnalysisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成词云数据
     *
     * @param source 词源 (title或content)
     * @param limit 返回结果数量限制
     * @param startTime 开始时间 (可选)
     * @param endTime 结束时间 (可选)
     * @return 词云数据列表，每个元素包含词语(text)和权重(weight)
     */
    public List<Map<String, Object>> generateWordCloudData(String source, Integer limit, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("生成词云数据，数据源: {}, 限制: {}, 时间范围: {} 至 {}", source, limit, startTime, endTime);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        if (startTime != null) {
            whereClause.append(" AND fetch_time >= ?");
            params.add(startTime);
        }
        
        if (endTime != null) {
            whereClause.append(" AND fetch_time <= ?");
            params.add(endTime);
        }
        
        // 根据数据源选择不同的查询
        String dataColumn = "title"; // 默认使用标题
        if ("content".equals(source)) {
            dataColumn = "content";
        } else if ("keywords".equals(source)) {
            dataColumn = "keywords";
        }
        
        // 获取数据
        String sql = "SELECT " + dataColumn + " FROM t_news_data WHERE " + dataColumn + " IS NOT NULL" + whereClause;
        List<String> rawTexts = jdbcTemplate.queryForList(sql, params.toArray(), String.class);
        
        // 分词并统计词频
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        for (String text : rawTexts) {
            // 关键词已经是逗号分隔的词组，直接分割
            if ("keywords".equals(source)) {
                String[] keywords = text.split(",");
                for (String keyword : keywords) {
                    String trimmed = keyword.trim();
                    if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                        wordFrequency.put(trimmed, wordFrequency.getOrDefault(trimmed, 0) + 1);
                    }
                }
            } else {
                // 标题或内容需要分词
                List<String> words = segmentText(text);
                for (String word : words) {
                    if (!STOP_WORDS.contains(word)) {
                        wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }
        
        // 排序并限制结果数量
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("text", entry.getKey());
                    wordData.put("weight", entry.getValue());
                    return wordData;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取热词排行榜
     *
     * @param limit 返回结果数量限制
     * @param startTime 开始时间 (可选)
     * @param endTime 结束时间 (可选)
     * @return 热词列表，每个元素包含词语(word)和出现次数(count)
     */
    public List<Map<String, Object>> getHotWords(Integer limit, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("获取热词排行榜，限制: {}, 时间范围: {} 至 {}", limit, startTime, endTime);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        
        if (startTime != null) {
            whereClause.append(" AND fetch_time >= ?");
            params.add(startTime);
        }
        
        if (endTime != null) {
            whereClause.append(" AND fetch_time <= ?");
            params.add(endTime);
        }
        
        // 查询所有关键词
        String sql = "SELECT keywords FROM t_news_data WHERE keywords IS NOT NULL" + whereClause;
        List<String> allKeywords = jdbcTemplate.queryForList(sql, params.toArray(), String.class);
        
        // 统计词频
        Map<String, Integer> wordCount = new HashMap<>();
        for (String keywordsStr : allKeywords) {
            String[] keywords = keywordsStr.split(",");
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                    wordCount.put(trimmed, wordCount.getOrDefault(trimmed, 0) + 1);
                }
            }
        }
        
        // 排序并限制结果数量
        return wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("word", entry.getKey());
                    wordData.put("count", entry.getValue());
                    return wordData;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取关键词时间趋势数据
     *
     * @param keyword 要分析的关键词
     * @param timeUnit 时间单位 (day, week, month)
     * @param startTime 开始时间 (可选)
     * @param endTime 结束时间 (可选)
     * @return 时间趋势数据，按时间点分组的关键词出现次数
     */
    public List<Map<String, Object>> getKeywordTimeTrend(String keyword, String timeUnit, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("获取关键词时间趋势，关键词: {}, 时间单位: {}, 时间范围: {} 至 {}", keyword, timeUnit, startTime, endTime);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE keywords LIKE ?");
        List<Object> params = new ArrayList<>();
        params.add("%" + keyword + "%");
        
        if (startTime != null) {
            whereClause.append(" AND publish_time >= ?");
            params.add(startTime);
        }
        
        if (endTime != null) {
            whereClause.append(" AND publish_time <= ?");
            params.add(endTime);
        }
        
        // 根据时间单位选择不同的时间格式
        String timeFormat;
        switch (timeUnit.toLowerCase()) {
            case "week":
                timeFormat = "%Y-%u"; // ISO周格式 (年-周数)
                break;
            case "month":
                timeFormat = "%Y-%m"; // 年-月格式
                break;
            case "day":
            default:
                timeFormat = "%Y-%m-%d"; // 年-月-日格式
                break;
        }
        
        // 查询按时间分组的关键词出现次数
        String sql = "SELECT DATE_FORMAT(publish_time, ?) as time_point, COUNT(*) as count " +
                     "FROM t_news_data" + whereClause + 
                     " GROUP BY time_point ORDER BY MIN(publish_time)";
        
        params.add(0, timeFormat);
        
        return jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .map(row -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timePoint", row.get("time_point"));
                    point.put("count", row.get("count"));
                    return point;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取新闻来源分布数据
     *
     * @param startTime 开始时间 (可选)
     * @param endTime 结束时间 (可选)
     * @return 来源分布数据，每个元素包含来源名称(source)和数量(count)
     */
    public List<Map<String, Object>> getSourceDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("获取新闻来源分布，时间范围: {} 至 {}", startTime, endTime);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE source IS NOT NULL");
        List<Object> params = new ArrayList<>();
        
        if (startTime != null) {
            whereClause.append(" AND fetch_time >= ?");
            params.add(startTime);
        }
        
        if (endTime != null) {
            whereClause.append(" AND fetch_time <= ?");
            params.add(endTime);
        }
        
        // 查询各来源的新闻数量
        String sql = "SELECT source, COUNT(*) as count FROM t_news_data" + 
                     whereClause + " GROUP BY source ORDER BY count DESC";
        
        return jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .map(row -> {
                    Map<String, Object> sourceData = new HashMap<>();
                    sourceData.put("source", row.get("source"));
                    sourceData.put("count", row.get("count"));
                    return sourceData;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 对文本进行简单分词
     * 注意：这是一个非常简单的分词方法，仅用于演示
     * 实际应用中应该使用专业的中文分词库如Ansj、HanLP等
     *
     * @param text 要分词的文本
     * @return 分词结果列表
     */
    private List<String> segmentText(String text) {
        List<String> words = new ArrayList<>();
        
        // 使用正则表达式提取中文词和英文词
        java.util.regex.Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            // 只保留长度>=2的词
            if (word.length() >= 2) {
                words.add(word);
            }
        }
        
        return words;
    }
} 