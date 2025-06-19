package com.hhu.javawebcrawler.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z]{2,}");
    
    private final JdbcTemplate jdbcTemplate;

    public DataAnalysisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成词云数据
     *
     * @param source 词源 (title或content)
     * @param limit 返回结果数量限制
     * @param historyId 爬取历史ID (必填)
     * @return 词云数据列表，每个元素包含词语(text)和权重(weight)
     */
    public List<Map<String, Object>> generateWordCloudData(String source, Integer limit, Long historyId) {
        logger.info("生成词云数据，数据源: {}, 限制: {}, 历史ID: {}", source, limit, historyId);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1"); // 初始条件，方便后续拼接
        List<Object> params = new ArrayList<>();
        
        // 如果指定了历史ID，添加对应的过滤条件
        if (historyId != null) {
            whereClause.append(" AND crawl_history_id = ?");
            params.add(historyId);
        }
        
        // 根据数据源选择不同的查询
        String dataColumn = "title"; // 默认使用标题
        if ("content".equals(source)) {
            dataColumn = "content";
        } else if ("keywords".equals(source)) {
            dataColumn = "keywords";
        }
        
        // 获取数据
        String sql = "SELECT " + dataColumn + " FROM t_news_data " + whereClause + " AND " + dataColumn + " IS NOT NULL";
        logger.debug("执行SQL: {}", sql);
        List<String> rawTexts = jdbcTemplate.queryForList(sql, params.toArray(), String.class);
        logger.debug("查询到 {} 条原始数据", rawTexts.size());
        
        // 分词并统计词频
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        for (String text : rawTexts) {
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            
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
        
        logger.debug("分词后得到 {} 个不同的词", wordFrequency.size());
        
        // 排序并限制结果数量
        List<Map<String, Object>> result = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("text", entry.getKey());
                    wordData.put("weight", entry.getValue());
                    return wordData;
                })
                .collect(Collectors.toList());
        
        logger.debug("返回词云数据 {} 条", result.size());
        return result;
    }
    
    /**
     * 获取热词排行榜
     *
     * @param limit 返回结果数量限制
     * @param historyId 爬取历史ID (必填)
     * @return 热词列表，每个元素包含词语(word)和出现次数(count)
     */
    public List<Map<String, Object>> getHotWords(Integer limit, Long historyId) {
        logger.info("获取热词排行榜，限制: {}, 历史ID: {}", limit, historyId);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        // 如果指定了历史ID，添加对应的过滤条件
        if (historyId != null) {
            whereClause.append(" AND crawl_history_id = ?");
            params.add(historyId);
        }
        
        // 查询所有关键词和标题 - 从标题中提取关键词，而不仅仅依赖关键词字段
        String keywordSql = "SELECT keywords FROM t_news_data " + whereClause + " AND keywords IS NOT NULL";
        String titleSql = "SELECT title FROM t_news_data " + whereClause + " AND title IS NOT NULL";
        
        logger.debug("执行关键词SQL: {}", keywordSql);
        List<String> allKeywords = jdbcTemplate.queryForList(keywordSql, params.toArray(), String.class);
        logger.debug("执行标题SQL: {}", titleSql);
        List<String> allTitles = jdbcTemplate.queryForList(titleSql, params.toArray(), String.class);
        
        // 统计词频
        Map<String, Integer> wordCount = new HashMap<>();
        
        // 处理关键词
        for (String keywordsStr : allKeywords) {
            if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
                continue;
            }
            
            String[] keywords = keywordsStr.split(",");
            for (String keyword : keywords) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                    wordCount.put(trimmed, wordCount.getOrDefault(trimmed, 0) + 2); // 关键词权重加倍
                }
            }
        }
        
        // 处理标题
        for (String title : allTitles) {
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            
            List<String> words = segmentText(title);
            for (String word : words) {
                if (!STOP_WORDS.contains(word)) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        logger.debug("关键词处理后得到 {} 个不同的词", wordCount.size());
        
        // 排序并限制结果数量
        List<Map<String, Object>> result = wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("word", entry.getKey());
                    wordData.put("count", entry.getValue());
                    return wordData;
                })
                .collect(Collectors.toList());
        
        logger.debug("返回热词数据 {} 条", result.size());
        return result;
    }
    
    /**
     * 获取关键词时间趋势数据
     *
     * @param keyword 要分析的关键词
     * @param timeUnit 时间单位 (day, week, month)
     * @param historyId 爬取历史ID (必填)
     * @return 时间趋势数据，按时间点分组的关键词出现次数
     */
    public List<Map<String, Object>> getKeywordTimeTrend(String keyword, String timeUnit, Long historyId) {
        logger.info("获取关键词时间趋势，关键词: {}, 时间单位: {}, 历史ID: {}", keyword, timeUnit, historyId);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE (keywords LIKE ? OR title LIKE ? OR content LIKE ?)");
        List<Object> params = new ArrayList<>();
        params.add("%" + keyword + "%");
        params.add("%" + keyword + "%");
        params.add("%" + keyword + "%");
        
        // 如果指定了历史ID，添加对应的过滤条件
        if (historyId != null) {
            whereClause.append(" AND crawl_history_id = ?");
            params.add(historyId);
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
        
        // 查询按时间分组的关键词出现次数 - 修改表名为t_news_data
        String sql = "SELECT DATE_FORMAT(publish_time, ?) as time_point, COUNT(*) as count " +
                     "FROM t_news_data" + whereClause + 
                     " GROUP BY time_point ORDER BY MIN(publish_time)";
        
        params.add(0, timeFormat);
        logger.debug("执行SQL: {}", sql);
        
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .map(row -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timePoint", row.get("time_point"));
                    point.put("count", row.get("count"));
                    return point;
                })
                .collect(Collectors.toList());
        
        logger.debug("返回时间趋势数据 {} 条", result.size());
        return result;
    }
    
    /**
     * 获取新闻来源分布数据
     *
     * @param historyId 爬取历史ID (必填)
     * @return 来源分布数据，每个元素包含来源名称(source)和数量(count)
     */
    public List<Map<String, Object>> getSourceDistribution(Long historyId) {
        logger.info("获取新闻来源分布，历史ID: {}", historyId);
        
        // 构建查询条件
        StringBuilder whereClause = new StringBuilder(" WHERE source IS NOT NULL");
        List<Object> params = new ArrayList<>();
        
        // 如果指定了历史ID，添加对应的过滤条件
        if (historyId != null) {
            whereClause.append(" AND crawl_history_id = ?");
            params.add(historyId);
        }
        
        // 查询各来源的新闻数量 - 修改表名为t_news_data
        String sql = "SELECT source, COUNT(*) as count FROM t_news_data" + 
                     whereClause + " GROUP BY source ORDER BY count DESC";
        
        logger.debug("执行SQL: {}", sql);
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .map(row -> {
                    Map<String, Object> sourceData = new HashMap<>();
                    sourceData.put("source", row.get("source"));
                    sourceData.put("count", row.get("count"));
                    return sourceData;
                })
                .collect(Collectors.toList());
        
        logger.debug("返回来源分布数据 {} 条", result.size());
        return result;
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