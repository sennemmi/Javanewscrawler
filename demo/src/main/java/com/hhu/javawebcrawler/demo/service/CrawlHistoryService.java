package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.repository.CrawlHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CrawlHistoryService {

    private final CrawlHistoryRepository crawlHistoryRepository;

    public CrawlHistoryService(CrawlHistoryRepository crawlHistoryRepository) {
        this.crawlHistoryRepository = crawlHistoryRepository;
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
     * 获取用户的爬取历史
     */
    public List<CrawlHistory> getUserHistory(Long userId) {
        return crawlHistoryRepository.findByUserIdOrderByCrawlTimeDesc(userId);
    }
}
