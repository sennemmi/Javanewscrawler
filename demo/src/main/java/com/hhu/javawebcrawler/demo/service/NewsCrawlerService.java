package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.repository.NewsDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;


@Service
@Slf4j
public class NewsCrawlerService {

    private final NewsDataRepository newsDataRepository;

    // --- 为不同页面结构定义选择器常量 ---
    private static final String[] TITLE_SELECTORS = {"h1.main-title"};
    private static final String[] SOURCE_SELECTORS = {
            ".top-bar-inner .date-source .author a",
            ".date-source a.source",
            ".top-bar-inner .date-source a.ent-source"
    };
    private static final String[] TIME_SELECTORS = {
            ".date-source .date",
            ".top-bar-inner .date-source .date"
    };
    private static final String[] CONTENT_SELECTORS = {"div#article"};

    // --- 新增：用于二级爬取的链接筛选正则表达式 ---
    // 匹配如: https://news.sina.com.cn/c/2025-06-17/doc-infaismq0100292.shtml
    private static final Pattern SINA_NEWS_PATTERN_1 = Pattern.compile("^https://news\\.sina\\.com\\.cn/\\w/\\d{4}-\\d{2}-\\d{2}/doc-[a-z0-9]+\\.shtml$");
    // 匹配如: https://k.sina.com.cn/article_6105713761_16bedcc6102001otd2.html
    private static final Pattern SINA_NEWS_PATTERN_2 = Pattern.compile("^https://k\\.sina\\.com\\.cn/article_\\w+\\.html$");


    public NewsCrawlerService(NewsDataRepository newsDataRepository) {
        this.newsDataRepository = newsDataRepository;
    }

    /**
     * 【新增】二级爬取功能：从一个入口页面（如新闻首页）爬取所有符合条件的新闻详情页。
     *
     * @param indexUrl 入口页面的URL，例如 "https://news.sina.com.cn/"
     * @return 成功爬取并入库的所有新闻数据列表
     * @throws IOException 当爬取入口页面失败时抛出
     */
    public List<NewsData> crawlNewsFromIndexPage(String indexUrl) throws IOException {
        log.info("开始二级爬取任务，入口页面: {}", indexUrl);
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 1. 爬取入口页面
        Connection.Response response = Jsoup.connect(indexUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(20000)
                .execute();
        Document indexDoc = response.parse();
        URL baseUrl = response.url(); // 获取最终的URL，处理重定向

        // 2. 提取并筛选所有符合条件的链接
        Elements links = indexDoc.select("a[href]");
        log.info("在入口页面找到 {} 个链接，开始筛选...", links.size());

        Set<String> validUrlsToCrawl = new HashSet<>();
        for (Element link : links) {
            String absUrl = link.absUrl("href").trim();
            // 清理URL，移除查询参数和哈希
            int queryPos = absUrl.indexOf('?');
            if (queryPos != -1) {
                absUrl = absUrl.substring(0, queryPos);
            }
            int hashPos = absUrl.indexOf('#');
            if(hashPos != -1) {
                absUrl = absUrl.substring(0, hashPos);
            }

            if (isSinaNewsUrl(absUrl)) {
                validUrlsToCrawl.add(absUrl);
            }
        }
        
        log.info("筛选出 {} 个有效的新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 3. 遍历有效链接，调用单页爬虫进行爬取
        int count = 0;
        for (String urlToCrawl : validUrlsToCrawl) {
            count++;
            log.info("二级爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            try {
                // 调用现有的单页爬取和保存方法
                NewsData newsData = crawlAndSaveSinaNews(urlToCrawl);
                crawledNewsList.add(newsData);
                // 可以加个延时，防止请求过于频繁
                Thread.sleep(500); // 暂停500毫秒
            } catch (Exception e) {
                log.error("二级爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
                // 某个页面失败不影响整体任务，继续下一个
            }
        }

        log.info("二级爬取任务完成，共成功爬取并保存了 {} 条新闻。", crawledNewsList.size());
        return crawledNewsList;
    }

    /**
     * 检查给定的URL是否是目标新浪新闻详情页
     * @param url 待检查的URL
     * @return 如果是目标URL则返回true
     */
    private boolean isSinaNewsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return SINA_NEWS_PATTERN_1.matcher(url).matches() || SINA_NEWS_PATTERN_2.matcher(url).matches();
    }


    /**
     * 爬取单个新浪新闻 URL 并存入数据库。
     * 该方法已适配多种新浪新闻页面结构。
     *
     * @param url 新闻链接
     * @return 爬取并保存后的新闻数据实体
     * @throws IOException 爬取失败时抛出
     */
    @Transactional
    public NewsData crawlAndSaveSinaNews(String url) throws IOException {
        // 1. 检查数据库中是否已存在该URL
        Optional<NewsData> existingNews = newsDataRepository.findByUrl(url);
        if (existingNews.isPresent()) {
            log.info("新闻已存在于数据库，跳过爬取: {}", url);
            return existingNews.get();
        }

        // 2. 使用 Jsoup 连接并获取页面文档
        log.info("开始爬取新闻: {}", url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000)
                .get();

        // 3. 解析页面元素
        String title = getTextBySelectors(doc, TITLE_SELECTORS);
        if (title.isEmpty()) {
            // 如果主要选择器失败，尝试从 <title> 标签获取，并做一些清理
            title = doc.title().replace("_新浪新闻", "").replace("_新浪网", "").trim();
        }
        
        String source = getTextBySelectors(doc, SOURCE_SELECTORS, "未知来源");
        String publishTimeStr = getTextBySelectors(doc, TIME_SELECTORS);

        // 正文内容提取与清理
        Element articleContentElement = getElementBySelectors(doc, CONTENT_SELECTORS);
        String content = "内容提取失败";
        if (articleContentElement != null) {
            articleContentElement.select("p.show_author, .wap_special, .article-notice, div[id^=ad_], ins.sinaads").remove();
            articleContentElement.select("img[black-list=y]").parents().remove();
            content = articleContentElement.html();
        }

        String keywords = doc.select("meta[name=keywords]").attr("content");

        // 4. 创建 NewsData 实体
        NewsData newsData = new NewsData();
        newsData.setUrl(url);
        newsData.setTitle(title.isEmpty() ? "无标题" : title);
        newsData.setSource(source);
        newsData.setContent(content);
        newsData.setKeywords(keywords);

        // 解析发布时间（包含备用方案）
        parseAndSetPublishTime(newsData, doc, publishTimeStr);

        // 5. 保存到数据库
        log.info("新闻爬取成功，正在保存到数据库: {}", newsData.getTitle());
        return newsDataRepository.save(newsData);
    }

    /**
     * 解析并设置新闻的发布时间，包含多种备用方案。
     * @param newsData 要设置时间的实体
     * @param doc Jsoup 文档对象，用于获取 meta 标签
     * @param timeStr 从页面可见文本中提取的时间字符串
     */
    private void parseAndSetPublishTime(NewsData newsData, Document doc, String timeStr) {
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
                newsData.setPublishTime(LocalDateTime.parse(timeStr.trim(), formatter));
                return;
            } catch (Exception e) {
                log.warn("使用标准格式'yyyy年MM月dd日 HH:mm'解析时间 '{}' 失败, 尝试备用方案...", timeStr);
            }
        }
        
        try {
            String metaTime = doc.selectFirst("meta[property=article:published_time]").attr("content");
            if (metaTime != null && !metaTime.isEmpty()) {
                newsData.setPublishTime(LocalDateTime.parse(metaTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                return;
            }
        } catch (Exception ex) {
            // 忽略，继续尝试下一个方案
        }

        log.warn("所有时间解析方案均失败，将使用当前时间。URL: {}", newsData.getUrl());
        newsData.setPublishTime(LocalDateTime.now());
    }

    private String getTextBySelectors(Element element, String... selectors) {
        return getTextBySelectors(element, selectors, "");
    }

    private String getTextBySelectors(Element element, String[] selectors, String defaultValue) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null) {
                return found.text().trim();
            }
        }
        return defaultValue;
    }

    private Element getElementBySelectors(Element element, String... selectors) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}