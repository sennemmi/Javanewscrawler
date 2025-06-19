package com.hhu.javawebcrawler.demo.entity;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 爬取历史实体类，映射 t_crawl_history 表
 */
@Entity
@Table(name = "t_crawl_history")
@Data
public class CrawlHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 存储关联的用户ID

    @Column(name = "crawl_type", nullable = false, length = 20)
    private String crawlType;

    @Lob //大对象字段标记
    @Column(columnDefinition = "TEXT")
    private String url;//新闻原始url

    private String title;

    @Lob 
    @Column(columnDefinition = "TEXT") 
    private String params;//存储复杂参数

    @Column(name = "crawl_time", nullable = false, updatable = false)
    private LocalDateTime crawlTime;//爬取时间

    //生成时间
    @PrePersist
    protected void onCreate() {
        this.crawlTime = LocalDateTime.now();
    }
}
