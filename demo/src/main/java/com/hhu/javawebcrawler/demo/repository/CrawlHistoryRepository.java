package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {
    // 查找某个用户的所有爬取历史
    List<CrawlHistory> findByUserIdOrderByCrawlTimeDesc(Long userId);
}
