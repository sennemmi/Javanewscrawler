package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.repository.CrawlHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service // 声明这个类是一个Spring的服务层组件。
public class CrawlHistoryService { // 定义一个名为 CrawlHistoryService 的公开类。

    private final CrawlHistoryRepository crawlHistoryRepository; // 声明一个用于数据访问的、不可变的爬取历史仓库字段。
    private final ObjectMapper objectMapper; // 声明一个用于处理JSON转换的、不可变的ObjectMapper字段。

    public CrawlHistoryService(CrawlHistoryRepository crawlHistoryRepository) { // 定义类的构造函数，通过它注入CrawlHistoryRepository依赖。
        this.crawlHistoryRepository = crawlHistoryRepository; // 将注入的仓库实例赋值给类成员变量。
        this.objectMapper = new ObjectMapper(); // 创建并初始化一个ObjectMapper实例。
    } // 构造函数结束。

    public void recordSingleUrlCrawl(Long userId, String url, String title) { // 定义一个记录单个URL爬取历史的方法。
        CrawlHistory history = new CrawlHistory(); // 创建一个新的CrawlHistory实体对象。
        history.setUserId(userId); // 为历史记录设置用户ID。
        history.setCrawlType("SINGLE_URL"); // 为历史记录设置爬取类型为“单个URL”。
        history.setUrl(url); // 为历史记录设置被爬取的URL。
        history.setTitle(title); // 为历史记录设置标题。
        // 注释：在此模式下，params字段可以为null，因此不进行设置。
        crawlHistoryRepository.save(history); // 调用仓库的save方法将历史记录持久化到数据库。
    } // recordSingleUrlCrawl 方法结束。

    public void recordIndexCrawl(Long userId, String indexUrl, String title, Map<String, Object> params) { // 定义一个记录二级爬取历史的方法。
        CrawlHistory history = new CrawlHistory(); // 创建一个新的CrawlHistory实体对象。
        history.setUserId(userId); // 为历史记录设置用户ID。
        history.setCrawlType("INDEX_CRAWL"); // 为历史记录设置爬取类型为“索引页爬取”。
        history.setUrl(indexUrl); // 为历史记录设置被爬取的入口页URL。
        history.setTitle(title); // 为历史记录设置任务标题。
        
        try { // 开始一个try块，用于捕获JSON转换时可能发生的异常。
            String paramsJson = objectMapper.writeValueAsString(params); // 使用ObjectMapper将参数Map对象转换为JSON字符串。
            history.setParams(paramsJson); // 将转换后的JSON字符串设置到历史记录的params字段。
        } catch (Exception e) { // 捕获在try块中发生的任何异常。
            history.setParams("{\"error\":\"转换参数时出错\"}"); // 如果转换失败，则设置一个表示错误的JSON字符串。
        } // try-catch 块结束。
        
        crawlHistoryRepository.save(history); // 调用仓库的save方法将历史记录持久化到数据库。
    } // recordIndexCrawl 方法结束。

    public CrawlHistory saveHistory(CrawlHistory history) { // 定义一个直接保存CrawlHistory对象的方法。
        return crawlHistoryRepository.save(history); // 调用仓库的save方法保存传入的对象，并返回保存后的实体。
    } // saveHistory 方法结束。

    public List<CrawlHistory> getUserHistory(Long userId) { // 定义一个获取指定用户所有爬取历史的方法。
        return crawlHistoryRepository.findByUserIdOrderByCrawlTimeDesc(userId); // 调用仓库方法查询并返回按时间降序排列的历史记录列表。
    } // getUserHistory 方法结束。
    
    @Transactional // 声明此方法应在数据库事务中执行。
    public boolean deleteHistoryIfBelongsToUser(Long historyId, Long userId) { // 定义一个有条件地删除单条历史记录的方法。
        Optional<CrawlHistory> historyOpt = crawlHistoryRepository.findById(historyId); // 根据ID从数据库中查找历史记录。
        
        if (historyOpt.isPresent() && historyOpt.get().getUserId().equals(userId)) { // 检查记录是否存在且其用户ID与当前用户ID匹配。
            crawlHistoryRepository.deleteById(historyId); // 如果条件满足，则根据ID删除该记录。
            return true; // 返回true表示删除成功。
        } // if 条件块结束。
        
        return false; // 如果记录不存在或不属于该用户，则返回false。
    } // deleteHistoryIfBelongsToUser 方法结束。
    
    @Transactional // 声明此方法应在数据库事务中执行。
    public int batchDeleteHistoryIfBelongsToUser(List<Long> historyIds, Long userId) { // 定义一个有条件地批量删除历史记录的方法。
        List<CrawlHistory> userHistories = crawlHistoryRepository.findByUserIdAndIdIn(userId, historyIds); // 查询属于指定用户且ID在给定列表中的所有记录。
        
        if (userHistories.isEmpty()) { // 检查查询结果是否为空。
            return 0; // 如果没有找到匹配的记录，则返回删除数量0。
        } // if 条件块结束。
        
        List<Long> userHistoryIds = userHistories.stream() // 将找到的实体列表转换为一个流。
                .map(CrawlHistory::getId) // 使用方法引用从每个CrawlHistory对象中提取ID。
                .collect(Collectors.toList()); // 将提取出的ID收集到一个新的列表中。
        
        crawlHistoryRepository.deleteAllById(userHistoryIds); // 调用仓库方法批量删除所有ID在列表中的记录。
        return userHistoryIds.size(); // 返回成功删除的记录数量。
    } // batchDeleteHistoryIfBelongsToUser 方法结束。
    
    @Transactional // 声明此方法应在数据库事务中执行。
    public int deleteAllHistoryByUserId(Long userId) { // 定义一个删除指定用户所有历史记录的方法。
        List<CrawlHistory> userHistories = crawlHistoryRepository.findByUserId(userId); // 根据用户ID查询其所有的历史记录。
        int count = userHistories.size(); // 获取查询到的记录总数。
        
        if (count > 0) { // 检查是否有记录需要删除。
            crawlHistoryRepository.deleteByUserId(userId); // 如果有，则调用仓库方法删除该用户的所有记录。
        } // if 条件块结束。
        
        return count; // 返回被删除的记录总数。
    } // deleteAllHistoryByUserId 方法结束。

    public Optional<CrawlHistory> findById(Long id) { // 定义一个根据ID查找单个爬取历史记录的方法。
        return crawlHistoryRepository.findById(id); // 调用仓库的findById方法，并返回一个可能包含结果的Optional对象。
    } // findById 方法结束。
} // CrawlHistoryService 类定义结束。