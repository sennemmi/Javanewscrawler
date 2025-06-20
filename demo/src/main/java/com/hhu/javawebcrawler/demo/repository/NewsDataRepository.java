package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsDataRepository extends JpaRepository<NewsData, Long> { 
    //定义新闻数据仓库接口，继承JpaRepository以管理NewsData实体，其主键类型为Long。
    
    // 用于通过URL查找新闻，以实现数据去重，数据重复则覆盖原有记录。
    Optional<NewsData> findByUrl(String url); 
    
    // 用于查找与特定爬取历史相关联的新闻数据。
    @Query("SELECT n FROM NewsData n WHERE n.crawlHistory.id = :historyId") 
    // 使用自定义JPQL查询，查询实体对象及其属性，避免歧义，根据crawlHistory的ID来查找NewsData实体。
    List<NewsData> findByCrawlHistoryId(@Param("historyId") Long historyId);
     // 定义方法，通过爬取历史ID查找新闻列表，并使用@Param注解将方法参数绑定到JPQL查询中的命名参数。
} // NewsDataRepository 接口定义结束。