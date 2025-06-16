package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {

    /**
     * 根据用户ID查找其所有的爬取历史，并按时间降序排列。
     */
    List<CrawlHistory> findByUserIdOrderByCrawlTimeDesc(Long userId);
}
