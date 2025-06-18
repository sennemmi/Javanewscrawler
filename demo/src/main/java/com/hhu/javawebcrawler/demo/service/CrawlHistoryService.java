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

@Service
public class CrawlHistoryService {

    private final CrawlHistoryRepository crawlHistoryRepository;
    private final ObjectMapper objectMapper;

    public CrawlHistoryService(CrawlHistoryRepository crawlHistoryRepository) {
        this.crawlHistoryRepository = crawlHistoryRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 记录单URL爬取历史
     */
    public void recordSingleUrlCrawl(Long userId, String url, String title) {
        CrawlHistory history = new CrawlHistory();
        history.setUserId(userId);
        history.setCrawlType("SINGLE_URL");
        history.setUrl(url);
        history.setTitle(title);
        // params 在这种类型下可以为 null
        crawlHistoryRepository.save(history);
    }

    /**
     * 记录二级爬取历史（从入口页爬取多个新闻）
     *
     * @param userId 用户ID
     * @param indexUrl 入口页URL
     * @param title 任务标题
     * @param params 爬取参数和结果信息
     */
    public void recordIndexCrawl(Long userId, String indexUrl, String title, Map<String, Object> params) {
        CrawlHistory history = new CrawlHistory();
        history.setUserId(userId);
        history.setCrawlType("INDEX_CRAWL");
        history.setUrl(indexUrl);
        history.setTitle(title);
        
        try {
            // 将Map转换为JSON字符串
            String paramsJson = objectMapper.writeValueAsString(params);
            history.setParams(paramsJson);
        } catch (Exception e) {
            // 转换失败时，记录错误
            history.setParams("{\"error\":\"转换参数时出错\"}");
        }
        
        crawlHistoryRepository.save(history);
    }

    /**
     * 直接保存爬取历史对象
     * 
     * @param history 已经构建好的CrawlHistory对象
     * @return 保存后的对象
     */
    public CrawlHistory saveHistory(CrawlHistory history) {
        return crawlHistoryRepository.save(history);
    }

    /**
     * 获取用户的爬取历史
     */
    public List<CrawlHistory> getUserHistory(Long userId) {
        return crawlHistoryRepository.findByUserIdOrderByCrawlTimeDesc(userId);
    }
    
    /**
     * 删除单个历史记录，确保只能删除自己的记录
     * 
     * @param historyId 要删除的历史记录ID
     * @param userId 当前用户ID
     * @return 是否成功删除
     */
    @Transactional
    public boolean deleteHistoryIfBelongsToUser(Long historyId, Long userId) {
        Optional<CrawlHistory> historyOpt = crawlHistoryRepository.findById(historyId);
        
        if (historyOpt.isPresent() && historyOpt.get().getUserId().equals(userId)) {
            crawlHistoryRepository.deleteById(historyId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 批量删除历史记录，确保只能删除自己的记录
     * 
     * @param historyIds 要删除的历史记录ID列表
     * @param userId 当前用户ID
     * @return 成功删除的记录数量
     */
    @Transactional
    public int batchDeleteHistoryIfBelongsToUser(List<Long> historyIds, Long userId) {
        List<CrawlHistory> userHistories = crawlHistoryRepository.findByUserIdAndIdIn(userId, historyIds);
        
        if (userHistories.isEmpty()) {
            return 0;
        }
        
        List<Long> userHistoryIds = userHistories.stream()
                .map(CrawlHistory::getId)
                .collect(Collectors.toList());
        
        crawlHistoryRepository.deleteAllById(userHistoryIds);
        return userHistoryIds.size();
    }
    
    /**
     * 删除用户的所有历史记录
     * 
     * @param userId 用户ID
     * @return 删除的记录数量
     */
    @Transactional
    public int deleteAllHistoryByUserId(Long userId) {
        List<CrawlHistory> userHistories = crawlHistoryRepository.findByUserId(userId);
        int count = userHistories.size();
        
        if (count > 0) {
            crawlHistoryRepository.deleteByUserId(userId);
        }
        
        return count;
    }
}
