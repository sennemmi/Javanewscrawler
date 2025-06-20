package com.hhu.javawebcrawler.demo.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity // 声明这个类是一个JPA实体，将映射到数据库表。
@Table(name = "t_news_data", uniqueConstraints = { // 指定映射的表名为 "t_news_data"，并定义约束。
    @UniqueConstraint(columnNames = {"url"}) // 在 "url" 列上添加一个唯一性约束，确保URL不重复。
}) // @Table 注解的结束括号。
@Data // Lombok注解，自动生成getter、setter、toString等常用方法。
public class NewsData { // 定义一个名为 NewsData 的公开类。

    @Id // 声明这个字段是表的主键。
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 指定主键的生成策略为数据库自增。
    private Long id; // 定义主键ID字段，类型为长整型。

    @Column(length = 768, nullable = false) // 映射到数据库列，设置最大长度为768，且不可为空。
    private String url; // 定义URL字段，用于存储新闻的链接地址。

    @Column(length = 255) // 映射到数据库列，设置最大长度为255。
    private String title; // 定义标题字段，用于存储新闻的标题。

    @Column(length = 100) // 映射到数据库列，设置最大长度为100。
    private String source; // 定义来源字段，用于存储新闻的来源或发布者。

    private LocalDateTime publishTime; // 定义发布时间字段，存储新闻的发布日期和时间。

    @Lob // 表示这是一个大对象（Large Object）字段。
    @Column(columnDefinition = "LONGTEXT") // 明确指定数据库中此列的类型为LONGTEXT，用于存储非常长的文本。
    private String content; // 定义内容字段，用于存储新闻的正文。

    @Column(length = 255) // 映射到数据库列，设置最大长度为255。
    private String keywords; // 定义关键词字段，用于存储新闻的关键词。

    @Column(nullable = false, updatable = false) // 映射到数据库列，设置不可为空，并且在更新时此字段的值不会被改变。
    private LocalDateTime fetchTime = LocalDateTime.now(); // 定义抓取时间字段，并默认为当前时间。

    @JsonIgnore // Jackson注解，在将对象序列化为JSON时忽略此字段，以防止懒加载异常。
    @ManyToOne(fetch = FetchType.LAZY) // 定义多对一关系，并设置加载策略为懒加载（LAZY）。
    //检索相关的爬虫历史记录时，不会立即加载CrawlerHistory对象，只有访问CrawlHistory对象时才会进行加载。
    @JoinColumn(name = "crawl_history_id") // 指定外键列的名称为 "crawl_history_id"。
    private CrawlHistory crawlHistory; // 定义关联的爬取历史记录实体对象。

    // 添加此字段是为了在JSON序列化时保留历史记录的ID。
    @Transient // JPA注解，表示此字段不映射到数据库的任何列。
    private Long crawlHistoryId; // 定义一个临时字段，用于在JSON中传递 crawlHistory 的ID。

    // 此方法在获取实体后设置crawlHistoryId。
    @PostLoad // JPA生命周期回调注解，在实体从数据库加载后执行此方法。
    private void onLoad() { // 定义一个私有方法，在加载实体后被调用。
        if (crawlHistory != null) { // 检查关联的 crawlHistory 对象是否存在。
            this.crawlHistoryId = crawlHistory.getId(); // 如果存在，则将其ID赋值给临时的 crawlHistoryId 字段。
        } // if语句结束。
    } // onLoad 方法结束。
} // NewsData 类定义结束。