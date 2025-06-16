package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsDataRepository extends JpaRepository<NewsData, Long> {
    /**
     * 根据URL判断新闻是否存在，用于数据去重。
     */
    boolean existsByUrl(String url);
}
