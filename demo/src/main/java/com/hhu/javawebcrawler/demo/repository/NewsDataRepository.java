package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsDataRepository extends JpaRepository<NewsData, Long> {
    // 通过 URL 查找新闻，用于去重
    Optional<NewsData> findByUrl(String url);
}
