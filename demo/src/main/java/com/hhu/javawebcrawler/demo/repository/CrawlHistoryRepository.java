package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {
    
    /**
     * 查询指定用户的所有爬取历史记录，按时间倒序排列
     * 
     * @param userId 用户ID
     * @return 爬取历史记录列表
     */
    List<CrawlHistory> findByUserIdOrderByCrawlTimeDesc(Long userId);
    
    /**
     * 查询指定用户的所有爬取历史记录
     * 
     * @param userId 用户ID
     * @return 爬取历史记录列表
     */
    List<CrawlHistory> findByUserId(Long userId);
    
    /**
     * 查询指定用户的指定ID列表中的爬取历史记录
     * 
     * @param userId 用户ID
     * @param ids 历史记录ID列表
     * @return 爬取历史记录列表
     */
    List<CrawlHistory> findByUserIdAndIdIn(Long userId, List<Long> ids);
    
    /**
     * 删除指定用户的所有爬取历史记录
     * 
     * @param userId 用户ID
     */
    @Transactional
    void deleteByUserId(Long userId);
}
