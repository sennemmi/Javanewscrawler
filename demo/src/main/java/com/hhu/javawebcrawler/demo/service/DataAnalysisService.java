package com.hhu.javawebcrawler.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// @Service注解，将这个类标记为Spring容器中的一个服务组件
@Service
// 定义一个名为DataAnalysisService的公共类
public class DataAnalysisService {

    // 获取一个私有的、静态的、最终的Logger实例，用于记录日志
    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisService.class);
    
    // 定义一个私有的、静态的、最终的Set集合，用于存储停用词
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        // 中文常用虚词、代词、连词等
        "的", "了", "和", "是", "在", "我", "有", "你", "他", "她", "它", "这", "那", "都", "也",
        // 中文常用副词、介词等
        "就", "要", "会", "到", "可以", "被", "等", "与", "以", "及", "但", "但是", "而", "或", "则",
        // 中文因果、条件连词等
        "因为", "所以", "如果", "只要", "只有", "不", "没有", "一个", "一种", "一样", "一点", "一些",
        // 数字字符
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", 
        // 中文数字和时间单位等
        "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "日", "月", "年", "最", "多",
        // 常见无意义实体词
        "人", "从", "对", "能", "为", "地", "得", "着", "说", "上", "下", "中", "前", "后", "里",
        // 新闻领域常见无意义词
        "中国", "记者", "报道", "新闻", "来源", "时间", "今天", "记者", "编辑", "频道", "评论"
    // 列表结束
    ));
    
    // 定义一个私有的、静态的、最终的Pattern对象，用于匹配长度至少为2的中文词或英文词
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z]{2,}");
    
    // 声明一个私有的、最终的JdbcTemplate成员变量，用于数据库操作
    private final JdbcTemplate jdbcTemplate;

    // 定义DataAnalysisService的公共构造函数，通过依赖注入接收JdbcTemplate实例
    public DataAnalysisService(JdbcTemplate jdbcTemplate) {
        // 将注入的JdbcTemplate实例赋值给本类的成员变量
        this.jdbcTemplate = jdbcTemplate;
    // 构造函数结束
    }

    // 定义一个公共方法，用于生成词云数据
    public List<Map<String, Object>> generateWordCloudData(String source, Integer limit, Long historyId) {
        // 记录生成词云数据的日志信息，包含参数
        logger.info("生成词云数据，数据源: {}, 限制: {}, 历史ID: {}", source, limit, historyId);
        
        // 使用StringBuilder构建SQL查询的WHERE子句
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        // 创建一个ArrayList用于存放SQL查询的参数
        List<Object> params = new ArrayList<>();
        
        // 检查是否提供了爬取历史ID
        if (historyId != null) {
            // 如果提供了，向WHERE子句中追加ID过滤条件
            whereClause.append(" AND crawl_history_id = ?");
            // 将历史ID添加到参数列表中
            params.add(historyId);
        // if语句结束
        }
        
        // 声明一个字符串变量，用于存储要查询的数据列名，默认为"title"
        String dataColumn = "title";
        // 如果请求的数据源是"content"
        if ("content".equals(source)) {
            // 将查询列名设置为"content"
            dataColumn = "content";
        // 如果请求的数据源是"keywords"
        } else if ("keywords".equals(source)) {
            // 将查询列名设置为"keywords"
            dataColumn = "keywords";
        // if-else if语句结束
        }
        
        // 构建完整的SQL查询语句
        String sql = "SELECT " + dataColumn + " FROM t_news_data " + whereClause + " AND " + dataColumn + " IS NOT NULL";
        // 记录将要执行的SQL语句
        logger.debug("执行SQL: {}", sql);
        // 执行查询，将结果映射为字符串列表
        List<String> rawTexts = jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> rs.getString(1));
        // 记录查询到的原始数据条数
        logger.debug("查询到 {} 条原始数据", rawTexts.size());
        
        // 创建一个HashMap用于存储词语及其出现的频率
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        // 遍历从数据库获取的每一段文本
        for (String text : rawTexts) {
            // 如果文本为空或只包含空白字符，则跳过
            if (text == null || text.trim().isEmpty()) {
                // 继续下一次循环
                continue;
            // if语句结束
            }
            
            // 如果数据源是"keywords"
            if ("keywords".equals(source)) {
                // 按逗号分割关键词字符串
                String[] keywords = text.split(",");
                // 遍历分割后的每个关键词
                for (String keyword : keywords) {
                    // 去除关键词两端的空白
                    String trimmed = keyword.trim();
                    // 如果关键词非空且不是停用词
                    if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                        // 更新该关键词的词频统计，如果不存在则设为1，存在则加1
                        wordFrequency.put(trimmed, wordFrequency.getOrDefault(trimmed, 0) + 1);
                    // if语句结束
                    }
                // 内层for循环结束
                }
            // 如果数据源是标题或内容
            } else {
                // 对文本进行分词
                List<String> words = segmentText(text);
                // 遍历分词后的每个词语
                for (String word : words) {
                    // 如果词语不是停用词
                    if (!STOP_WORDS.contains(word)) {
                        // 更新该词语的词频统计
                        wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                    // if语句结束
                    }
                // 内层for循环结束
                }
            // if-else语句结束
            }
        // 外层for循环结束
        }
        
        // 记录分词后独立词语的数量
        logger.debug("分词后得到 {} 个不同的词", wordFrequency.size());
        
        // 对词频Map进行处理，以生成最终结果
        List<Map<String, Object>> result = wordFrequency.entrySet().stream()
                // 按词频（Map的值）降序排序
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                // 限制结果列表的大小
                .limit(limit)
                // 将每个Map条目转换为包含"text"和"weight"键的Map
                .map(entry -> {
                    // 创建一个新的HashMap用于存放单个词云数据
                    Map<String, Object> wordData = new HashMap<>();
                    // 将词语存入"text"键
                    wordData.put("text", entry.getKey());
                    // 将词频存入"weight"键
                    wordData.put("weight", entry.getValue());
                    // 返回构建好的Map
                    return wordData;
                // map操作结束
                })
                // 将处理后的流元素收集到一个列表中
                .collect(Collectors.toList());
        
        // 记录返回的词云数据条数
        logger.debug("返回词云数据 {} 条", result.size());
        // 返回最终的词云数据列表
        return result;
    // generateWordCloudData方法结束
    }
    
    // 定义一个公共方法，用于获取热词排行榜
    public List<Map<String, Object>> getHotWords(Integer limit, Long historyId) {
        // 记录获取热词的日志信息
        logger.info("获取热词排行榜，限制: {}, 历史ID: {}", limit, historyId);
        
        // 使用StringBuilder构建SQL查询的WHERE子句
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        // 创建一个ArrayList用于存放SQL查询的参数
        List<Object> params = new ArrayList<>();
        
        // 检查是否提供了爬取历史ID
        if (historyId != null) {
            // 如果提供了，向WHERE子句中追加ID过滤条件
            whereClause.append(" AND crawl_history_id = ?");
            // 将历史ID添加到参数列表中
            params.add(historyId);
        // if语句结束
        }
        
        // 构建查询关键词的SQL语句
        String keywordSql = "SELECT keywords FROM t_news_data " + whereClause + " AND keywords IS NOT NULL";
        // 构建查询标题的SQL语句
        String titleSql = "SELECT title FROM t_news_data " + whereClause + " AND title IS NOT NULL";
        
        // 记录将要执行的关键词SQL
        logger.debug("执行关键词SQL: {}", keywordSql);
        // 执行查询获取所有关键词字符串
        List<String> allKeywords = jdbcTemplate.query(keywordSql, params.toArray(), (rs, rowNum) -> rs.getString(1));
        // 记录将要执行的标题SQL
        logger.debug("执行标题SQL: {}", titleSql);
        // 执行查询获取所有标题
        List<String> allTitles = jdbcTemplate.query(titleSql, params.toArray(), (rs, rowNum) -> rs.getString(1));
        
        // 创建一个HashMap用于统计词频
        Map<String, Integer> wordCount = new HashMap<>();
        
        // 遍历所有关键词字符串
        for (String keywordsStr : allKeywords) {
            // 如果关键词字符串为空或只包含空白，则跳过
            if (keywordsStr == null || keywordsStr.trim().isEmpty()) {
                // 继续下一次循环
                continue;
            // if语句结束
            }
            
            // 按逗号分割关键词
            String[] keywords = keywordsStr.split(",");
            // 遍历每个关键词
            for (String keyword : keywords) {
                // 去除关键词两端的空白
                String trimmed = keyword.trim();
                // 如果关键词非空且不是停用词
                if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                    // 更新词频，关键词权重加2
                    wordCount.put(trimmed, wordCount.getOrDefault(trimmed, 0) + 2);
                // if语句结束
                }
            // 内层for循环结束
            }
        // 外层for循环结束
        }
        
        // 遍历所有标题
        for (String title : allTitles) {
            // 如果标题为空或只包含空白，则跳过
            if (title == null || title.trim().isEmpty()) {
                // 继续下一次循环
                continue;
            // if语句结束
            }
            
            // 对标题进行分词
            List<String> words = segmentText(title);
            // 遍历分词后的每个词语
            for (String word : words) {
                // 如果词语不是停用词
                if (!STOP_WORDS.contains(word)) {
                    // 更新词频，标题词权重加1
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                // if语句结束
                }
            // 内层for循环结束
            }
        // 外层for循环结束
        }
        
        // 记录处理后的独立词语数量
        logger.debug("关键词处理后得到 {} 个不同的词", wordCount.size());
        
        // 对词频Map进行处理，以生成最终结果
        List<Map<String, Object>> result = wordCount.entrySet().stream()
                // 按词频（Map的值）降序排序
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                // 限制结果列表的大小
                .limit(limit)
                // 将每个Map条目转换为包含"word"和"count"键的Map
                .map(entry -> {
                    // 创建一个新的HashMap用于存放单个热词数据
                    Map<String, Object> wordData = new HashMap<>();
                    // 将词语存入"word"键
                    wordData.put("word", entry.getKey());
                    // 将词频存入"count"键
                    wordData.put("count", entry.getValue());
                    // 返回构建好的Map
                    return wordData;
                // map操作结束
                })
                // 将处理后的流元素收集到一个列表中
                .collect(Collectors.toList());
        
        // 记录返回的热词数据条数
        logger.debug("返回热词数据 {} 条", result.size());
        // 返回最终的热词列表
        return result;
    // getHotWords方法结束
    }
    
    // 定义一个公共方法，用于获取关键词的时间趋势
    public List<Map<String, Object>> getKeywordTimeTrend(String keyword, String timeUnit, Long historyId) {
        // 记录获取时间趋势的日志信息
        logger.info("获取关键词时间趋势，关键词: {}, 时间单位: {}, 历史ID: {}", keyword, timeUnit, historyId);
        
        // 构建SQL的WHERE子句，查询关键词、标题或内容中包含指定关键词的记录
        StringBuilder whereClause = new StringBuilder(" WHERE (keywords LIKE ? OR title LIKE ? OR content LIKE ?)");
        // 创建参数列表
        List<Object> params = new ArrayList<>();
        // 添加关键词参数，用于三个LIKE匹配
        params.add("%" + keyword + "%");
        // 添加关键词参数
        params.add("%" + keyword + "%");
        // 添加关键词参数
        params.add("%" + keyword + "%");
        
        // 检查是否提供了爬取历史ID
        if (historyId != null) {
            // 如果提供了，向WHERE子句中追加ID过滤条件
            whereClause.append(" AND crawl_history_id = ?");
            // 将历史ID添加到参数列表中
            params.add(historyId);
        // if语句结束
        }
        
        // 声明时间格式化字符串变量
        String timeFormat;
        // 声明SQL查询字符串变量
        String sql;
        
        // 使用switch语句根据时间单位选择不同的SQL查询逻辑
        switch (timeUnit.toLowerCase()) {
            // 如果时间单位是"hour6"
            case "hour6":
                // 设置时间格式（未使用，但为说明）
                timeFormat = "%Y-%m-%d %H";
                // 构建按6小时分组的SQL
                sql = "SELECT CONCAT(DATE_FORMAT(publish_time, '%Y-%m-%d '), FLOOR(HOUR(publish_time)/6)*6) as time_point, COUNT(*) as count " +
                      "FROM t_news_data" + whereClause + 
                      " GROUP BY time_point ORDER BY MIN(publish_time)";
                // 结束case
                break;
            // 如果时间单位是"hour12"
            case "hour12":
                // 设置时间格式（未使用，但为说明）
                timeFormat = "%Y-%m-%d %H";
                // 构建按12小时分组的SQL
                sql = "SELECT CONCAT(DATE_FORMAT(publish_time, '%Y-%m-%d '), FLOOR(HOUR(publish_time)/12)*12) as time_point, COUNT(*) as count " +
                      "FROM t_news_data" + whereClause + 
                      " GROUP BY time_point ORDER BY MIN(publish_time)";
                // 结束case
                break;
            // 如果时间单位是"day"或默认情况
            case "day":
            // 默认情况
            default:
                // 设置按天分组的时间格式
                timeFormat = "%Y-%m-%d";
                // 构建按天分组的SQL
                sql = "SELECT DATE_FORMAT(publish_time, ?) as time_point, COUNT(*) as count " +
                      "FROM t_news_data" + whereClause + 
                      " GROUP BY time_point ORDER BY MIN(publish_time)";
                // 将时间格式作为第一个参数添加到参数列表
                params.add(0, timeFormat);
                // 结束case
                break;
        // switch语句结束
        }
        
        // 记录将要执行的SQL语句
        logger.debug("执行SQL: {}", sql);
        
        // 声明一个列表用于存储结果
        List<Map<String, Object>> result;
        
        // 如果时间单位是小时级别
        if (timeUnit.equals("hour6") || timeUnit.equals("hour12")) {
            // 执行查询并处理结果流
            result = jdbcTemplate.queryForList(sql, params.toArray()).stream()
                    // 对每一行结果进行映射
                    .map(row -> {
                        // 创建一个新的HashMap存放单个时间点的数据
                        Map<String, Object> point = new HashMap<>();
                        // 存入时间点
                        point.put("timePoint", row.get("time_point"));
                        // 存入数量
                        point.put("count", row.get("count"));
                        // 返回构建好的Map
                        return point;
                    // map操作结束
                    })
                    // 将流收集为列表
                    .collect(Collectors.toList());
        // 如果是天级别
        } else {
            // 执行查询并处理结果流
            result = jdbcTemplate.queryForList(sql, params.toArray()).stream()
                // 对每一行结果进行映射
                .map(row -> {
                    // 创建一个新的HashMap存放单个时间点的数据
                    Map<String, Object> point = new HashMap<>();
                    // 存入时间点
                    point.put("timePoint", row.get("time_point"));
                    // 存入数量
                    point.put("count", row.get("count"));
                    // 返回构建好的Map
                    return point;
                // map操作结束
                })
                // 将流收集为列表
                .collect(Collectors.toList());
        // if-else结束
        }
        
        // 记录返回的时间趋势数据条数
        logger.debug("返回时间趋势数据 {} 条", result.size());
        // 返回最终的时间趋势数据列表
        return result;
    // getKeywordTimeTrend方法结束
    }
    
    // 定义一个公共方法，用于获取新闻来源分布
    public List<Map<String, Object>> getSourceDistribution(Long historyId) {
        // 记录获取来源分布的日志信息
        logger.info("获取新闻来源分布，历史ID: {}", historyId);
        
        // 构建WHERE子句，过滤掉来源为空的记录
        StringBuilder whereClause = new StringBuilder(" WHERE source IS NOT NULL");
        // 创建参数列表
        List<Object> params = new ArrayList<>();
        
        // 检查是否提供了爬取历史ID
        if (historyId != null) {
            // 如果提供了，向WHERE子句中追加ID过滤条件
            whereClause.append(" AND crawl_history_id = ?");
            // 将历史ID添加到参数列表中
            params.add(historyId);
        // if语句结束
        }
        
        // 构建SQL语句，按来源分组统计数量，并按数量降序排序
        String sql = "SELECT source, COUNT(*) as count FROM t_news_data" + 
                     whereClause + " GROUP BY source ORDER BY count DESC";
        
        // 记录将要执行的SQL语句
        logger.debug("执行SQL: {}", sql);
        // 执行查询并处理结果流
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, params.toArray()).stream()
                // 对每一行结果进行映射
                .map(row -> {
                    // 创建一个新的HashMap存放单个来源数据
                    Map<String, Object> sourceData = new HashMap<>();
                    // 存入来源名称
                    sourceData.put("source", row.get("source"));
                    // 存入数量
                    sourceData.put("count", row.get("count"));
                    // 返回构建好的Map
                    return sourceData;
                // map操作结束
                })
                // 将流收集为列表
                .collect(Collectors.toList());
        
        // 记录返回的来源分布数据条数
        logger.debug("返回来源分布数据 {} 条", result.size());
        // 返回最终的来源分布数据列表
        return result;
    // getSourceDistribution方法结束
    }
    
    // 定义一个私有的辅助方法，用于对文本进行简单分词
    private List<String> segmentText(String text) {
        // 创建一个ArrayList用于存放分词结果
        List<String> words = new ArrayList<>();
        
        // 使用预定义的正则表达式模式在文本中查找匹配项
        java.util.regex.Matcher matcher = WORD_PATTERN.matcher(text);
        // 循环查找匹配项
        while (matcher.find()) {
            // 获取匹配到的词语
            String word = matcher.group();
            // 检查词语长度是否大于等于2
            if (word.length() >= 2) {
                // 如果是，则添加到结果列表中
                words.add(word);
            // if语句结束
            }
        // while循环结束
        }
        
        // 返回分词结果列表
        return words;
    // segmentText方法结束
    }
// DataAnalysisService类结束
} 