package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsDataRepository extends JpaRepository<NewsData, Long> {
    // 通过 URL 查找新闻，用于去重
    Optional<NewsData> findByUrl(String url);
    
    // 通过爬取历史ID查找关联的新闻数据
    @Query("SELECT n FROM NewsData n WHERE n.crawlHistory.id = :historyId")
    List<NewsData> findByCrawlHistoryId(@Param("historyId") Long historyId);
}
