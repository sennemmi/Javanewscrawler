package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.repository.NewsDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class NewsCrawlerService {

    private final NewsDataRepository newsDataRepository;

    public NewsCrawlerService(NewsDataRepository newsDataRepository) {
        this.newsDataRepository = newsDataRepository;
    }

    /**
     * 爬取单个新浪新闻 URL 并存入数据库 (根据提供的HTML结构优化)
     * @param url 新闻链接
     * @return 爬取并保存后的新闻数据实体
     * @throws IOException 爬取失败时抛出
     */
    @Transactional // 保证操作的原子性
    public NewsData crawlAndSaveSinaNews(String url) throws IOException {
        // 1. 检查数据库中是否已存在该URL，避免重复爬取
        Optional<NewsData> existingNews = newsDataRepository.findByUrl(url);
        if (existingNews.isPresent()) {
            log.info("新闻已存在于数据库，跳过爬取: {}", url);
            return existingNews.get();
        }

        // 2. 使用 Jsoup 连接并获取页面文档
        log.info("开始爬取新闻: {}", url);
        Document doc = Jsoup.connect(url)
                // 模拟浏览器访问，防止被屏蔽
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000) // 15秒超时
                .get();

        // 3. 解析页面元素
        
        // 标题: <h1 class="main-title">...</h1>
        String title = doc.selectFirst("h1.main-title").text();

        // 来源和发布时间: 在 <div class="date-source"> 下
        Element dateSourceElement = doc.selectFirst(".date-source");
        String source = "未知来源";
        String publishTimeStr = "";
        if (dateSourceElement != null) {
            // 来源: <a class="source">...</a>
            Element sourceLink = dateSourceElement.selectFirst("a.source");
            if (sourceLink != null) {
                source = sourceLink.text();
            }
            // 发布时间: <span class="date">...</span>
            Element dateSpan = dateSourceElement.selectFirst(".date");
            if (dateSpan != null) {
                publishTimeStr = dateSpan.text();
            }
        }
        
        // 正文: <div id="article">...</div>
        // 保留HTML结构以便于后续排版，同时要移除末尾不必要的内容
        Element articleContentElement = doc.selectFirst("div#article");
        String content = "内容提取失败";
        if (articleContentElement != null) {
            // 创建一个内容的副本进行清理，避免影响原始文档树
            Element clonedArticle = articleContentElement.clone();
            // 移除末尾的责任编辑、专题链接等不需要的部分
            clonedArticle.select("p.show_author").remove();
            clonedArticle.select(".wap_special").remove();
            content = clonedArticle.html();
        }
        
        // 关键词: <meta name="keywords" content="..." />
        String keywords = doc.select("meta[name=keywords]").attr("content");

        // 4. 创建 NewsData 实体
        NewsData newsData = new NewsData();
        newsData.setUrl(url);
        newsData.setTitle(title);
        newsData.setSource(source);
        newsData.setContent(content);
        newsData.setKeywords(keywords); // meta标签里的关键词已经是逗号分隔的
        
        // 解析发布时间
        if (!publishTimeStr.isEmpty()) {
            try {
                // 格式如："2025年06月16日 10:01"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
                newsData.setPublishTime(LocalDateTime.parse(publishTimeStr, formatter));
            } catch (Exception e) {
                log.warn("解析发布时间失败 '{}', 尝试备用方案...", publishTimeStr);
                // 备用方案: 从 meta 标签获取
                // <meta property="article:published_time" content="2025-06-16T10:03:21+08:00" />
                try {
                    String metaTime = doc.select("meta[property=article:published_time]").attr("content");
                    newsData.setPublishTime(LocalDateTime.parse(metaTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                } catch (Exception ex) {
                    log.error("所有时间解析方案均失败，使用当前时间。URL: {}", url, ex);
                    newsData.setPublishTime(LocalDateTime.now());
                }
            }
        } else {
             log.warn("未找到发布时间元素，使用当前时间。URL: {}", url);
             newsData.setPublishTime(LocalDateTime.now());
        }
        
        // 5. 保存到数据库
        log.info("新闻爬取成功，正在保存到数据库: {}", title);
        return newsDataRepository.save(newsData);
    }
}
