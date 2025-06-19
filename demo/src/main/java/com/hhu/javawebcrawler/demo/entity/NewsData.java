package com.hhu.javawebcrawler.demo.entity;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "t_news_data", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"url"})
})
@Data
public class NewsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 768, nullable = false)
    private String url;

    @Column(length = 255)
    private String title;

    @Column(length = 100)
    private String source;

    private LocalDateTime publishTime;

    @Lob // 表示这是一个大对象，映射到 LONGTEXT
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 255)
    private String keywords;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fetchTime = LocalDateTime.now();
    
    @JsonIgnore // Ignore this field during JSON serialization to prevent lazy loading issues
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_history_id")
    private CrawlHistory crawlHistory;
    
    // 添加一个字段用于在JSON序列化时保留历史记录ID
    @Transient // 不映射到数据库列
    private Long crawlHistoryId;
    
    // 在获取实体前设置crawlHistoryId
    @PostLoad
    private void onLoad() {
        if (crawlHistory != null) {
            this.crawlHistoryId = crawlHistory.getId();
        }
    }
}
