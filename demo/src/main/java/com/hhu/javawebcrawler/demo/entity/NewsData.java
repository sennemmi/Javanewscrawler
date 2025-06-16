package com.hhu.javawebcrawler.demo.entity;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 新闻数据实体类，映射 t_news_data 表
 */
@Entity
@Table(name = "t_news_data")
@Data
public class NewsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 768)// 数据不能为空且必须唯一，长度为768
    private String url;

    private String title;

    @Column(length = 100)//长度为100
    private String source;//新闻来源

    @Column(name = "publish_time")
    private LocalDateTime publishTime;//新闻发布时间

    @Lob
    @Column(columnDefinition = "LONGTEXT") // 明确指定为LONGTEXT以存储长文章
    private String content;

    private String keywords;//新闻关键词

    @Column(name = "fetch_time", nullable = false, updatable = false)
    private LocalDateTime fetchTime;//抓取时间

    @PrePersist
    protected void onCreate() {
        this.fetchTime = LocalDateTime.now();
    }
}
