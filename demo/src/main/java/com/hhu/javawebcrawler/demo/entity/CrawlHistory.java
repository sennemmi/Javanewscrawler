package com.hhu.javawebcrawler.demo.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;


//爬取历史实体类，映射 t_crawl_history 表
@Entity // 声明这个类是一个JPA实体，它会映射到数据库中的一个表。
@Table(name = "t_crawl_history") // 指定该实体映射的数据库表的名称为 "t_crawl_history"。
@Data // Lombok注解，自动为所有字段生成getter、setter等方法。
public class CrawlHistory { // 定义一个名为 CrawlHistory 的公开类。

    @Id // 声明这个字段是表的主键。
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 指定主键的生成策略为自增（由数据库管理）。
    private Long id; // 定义主键字段，类型为长整型(Long)。

    @Column(name = "user_id", nullable = false) // 将此字段映射到名为 "user_id" 的列，并设置该列为不可为空。
    private Long userId; // 定义用户ID字段，用于存储关联的用户ID。

    @Column(name = "crawl_type", nullable = false, length = 20) // 映射到 "crawl_type" 列，不可为空，最大长度为20个字符。
    private String crawlType; // 定义爬取类型字段，例如 "单URL"、"二级爬取" 等。

    @Lob // 表示这是一个大对象（Large Object）字段，适合存储大量数据。
    @Column(columnDefinition = "TEXT") // 指定数据库中此列的具体类型为 TEXT，用于存储长文本。
    private String url; // 定义URL字段，用于存储被爬取页面的原始URL。

    private String title; // 定义标题字段，用于存储爬取内容的标题。

    @Lob // 同样，表示这是一个大对象字段。
    @Column(columnDefinition = "TEXT") // 明确指定数据库列类型为TEXT。
    private String params; // 定义参数字段，用于以文本格式（如JSON）存储复杂的请求参数。

    @Column(name = "crawl_time", nullable = false, updatable = false) // 映射到 "crawl_time" 列，不可为空，并且在更新时此字段的值不会被改变。
    private LocalDateTime crawlTime; // 定义爬取时间字段，类型为Java 8的日期时间对象。

    // JPA生命周期回调方法的注释，说明其作用是生成时间。
    @PrePersist // JPA注解，表示在实体第一次被持久化（保存）到数据库之前，会执行此方法。
    protected void onCreate() { // 定义一个受保护的方法，在创建实体时调用。
        this.crawlTime = LocalDateTime.now(); // 将当前爬取时间设置为当前的系统时间。
    } // onCreate 方法结束。
} // CrawlHistory 类定义结束。