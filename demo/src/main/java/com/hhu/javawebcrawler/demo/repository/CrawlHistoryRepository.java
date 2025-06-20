package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository // 声明这是一个Spring的仓库（Repository）组件，用于数据访问。
public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> { 
    // 定义一个接口，继承自JpaRepository，提供对CrawlHistory实体的基本CRUD操作。
    //Spring Data JPA 框架会在运行时根据在接口中定义的方法名称来自动生成实际的 SQL 查询语句和方法实现
    // 根据用户ID查询爬取历史，并按爬取时间（CrawlTime）降序（Desc）排列。
    List<CrawlHistory> findByUserIdOrderByCrawlTimeDesc(Long userId);

    // 根据用户ID（UserId）查询所有相关的爬取历史记录。
    List<CrawlHistory> findByUserId(Long userId);

    // 根据用户ID（UserId）和主键ID列表（IdIn）查询匹配的爬取历史记录。
    List<CrawlHistory> findByUserIdAndIdIn(Long userId, List<Long> ids);

    @Transactional // 声明此方法需要在一个事务中执行，以确保数据操作的原子性。
    // 根据用户ID（UserId）删除所有相关的爬取历史记录。
    void deleteByUserId(Long userId);
} // CrawlHistoryRepository 接口定义结束。