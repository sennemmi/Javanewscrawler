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

/**
 * 新闻爬虫服务
 * <p>
 * 本服务提供多种方式的新闻爬取功能，支持单页爬取、二级爬取和关键词爬取等方式。
 * 主要针对新浪新闻网站进行内容抓取，支持多种页面结构的解析，能够自适应不同的新闻页面格式。
 * 爬取的内容包括新闻标题、来源、发布时间、正文内容和关键词等信息。
 * </p>
 * 
 * @author JavaWebCrawler团队
 */
@Service
@Slf4j
public class NewsCrawlerService {

    private final NewsDataRepository newsDataRepository;

    /** 标题选择器数组，用于从不同格式的页面中提取新闻标题 */
    private static final String[] TITLE_SELECTORS = {"h1.main-title"};
    
    /** 来源选择器数组，用于从不同格式的页面中提取新闻来源 */
    private static final String[] SOURCE_SELECTORS = {
            ".top-bar-inner .date-source .author a",
            ".date-source a.source",
            ".top-bar-inner .date-source a.ent-source"
    };
    
    /** 时间选择器数组，用于从不同格式的页面中提取发布时间 */
    private static final String[] TIME_SELECTORS = {
            ".date-source .date",
            ".top-bar-inner .date-source .date"
    };
    
    /** 内容选择器数组，用于从不同格式的页面中提取新闻正文 */
    private static final String[] CONTENT_SELECTORS = {"div#article"};

    /** 
     * 新浪新闻URL匹配模式1
     * 匹配如: https://news.sina.com.cn/c/2025-06-17/doc-infaismq0100292.shtml 
     */
    private static final Pattern SINA_NEWS_PATTERN_1 = Pattern.compile("^https://news\\.sina\\.com\\.cn/\\w/\\d{4}-\\d{2}-\\d{2}/doc-[a-z0-9]+\\.shtml$");
    
    /** 
     * 新浪新闻URL匹配模式2
     * 匹配如: https://k.sina.com.cn/article_6105713761_16bedcc6102001otd2.html 
     */
    private static final Pattern SINA_NEWS_PATTERN_2 = Pattern.compile("^https://k\\.sina\\.com\\.cn/article_\\w+\\.html$");

    /**
     * 构造函数
     * 
     * @param newsDataRepository 新闻数据存储库，用于数据持久化
     */
    public NewsCrawlerService(NewsDataRepository newsDataRepository) {
        this.newsDataRepository = newsDataRepository;
    }

    /**
     * 二级爬取功能：从一个入口页面（如新闻首页）爬取所有符合条件的新闻详情页
     * <p>
     * 该方法首先访问入口页面，提取所有链接，然后筛选出符合新浪新闻URL格式的链接，
     * 最后逐个爬取这些链接对应的新闻详情页并保存到数据库。
     * </p>
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
     * 按关键词爬取新闻：从新浪新闻首页爬取所有标题包含关键词的新闻
     * <p>
     * 该方法是{@link #crawlNewsByKeyword(String, String)}的便捷版本，默认使用新浪新闻首页作为入口。
     * </p>
     *
     * @param keyword 要搜索的关键词
     * @return 成功爬取并入库的所有新闻数据列表
     * @throws IOException 当爬取入口页面失败时抛出
     */
    public List<NewsData> crawlNewsByKeyword(String keyword) throws IOException {
        return crawlNewsByKeyword(keyword, "https://news.sina.com.cn/");
    }

    /**
     * 按关键词爬取新闻：从指定的入口页面爬取所有标题包含关键词的新闻
     * <p>
     * 该方法首先访问入口页面，提取所有链接及其文本，筛选出文本包含指定关键词且URL符合新浪新闻格式的链接，
     * 然后逐个爬取这些链接对应的新闻详情页并保存到数据库。
     * </p>
     *
     * @param keyword 要搜索的关键词
     * @param indexUrl 入口页面的URL，例如 "https://news.sina.com.cn/"
     * @return 成功爬取并入库的所有新闻数据列表
     * @throws IOException 当爬取入口页面失败时抛出
     */
    public List<NewsData> crawlNewsByKeyword(String keyword, String indexUrl) throws IOException {
        log.info("开始按关键词 '{}' 爬取任务，入口页面: {}", keyword, indexUrl);
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 1. 爬取入口页面
        Document indexDoc = Jsoup.connect(indexUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(20000)
                .get();

        // 2. 提取并筛选所有符合条件的链接
        Elements links = indexDoc.select("a[href]");
        log.info("在入口页面找到 {} 个链接，开始根据关键词 '{}' 进行筛选...", links.size(), keyword);

        Set<String> validUrlsToCrawl = new HashSet<>();
        for (Element link : links) {
            String linkTitle = link.text().trim();
            // 筛选条件：链接文本必须包含关键词 (忽略大小写)
            if (linkTitle.toLowerCase().contains(keyword.toLowerCase())) {
                String absUrl = link.absUrl("href").trim();
                // 清理URL
                int queryPos = absUrl.indexOf('?');
                if (queryPos != -1) {
                    absUrl = absUrl.substring(0, queryPos);
                }
                int hashPos = absUrl.indexOf('#');
                if(hashPos != -1) {
                    absUrl = absUrl.substring(0, hashPos);
                }

                // 筛选条件：URL必须是有效的新浪新闻详情页
                if (isSinaNewsUrl(absUrl)) {
                    validUrlsToCrawl.add(absUrl);
                }
            }
        }

        log.info("筛选出 {} 个标题含关键词的有效新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 3. 遍历有效链接，调用单页爬虫进行爬取
        int count = 0;
        for (String urlToCrawl : validUrlsToCrawl) {
            count++;
            log.info("关键词爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            try {
                // 调用现有的单页爬取和保存方法
                NewsData newsData = crawlAndSaveSinaNews(urlToCrawl);
                crawledNewsList.add(newsData);
                // 暂停，避免请求过于频繁
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("关键词爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
            }
        }

        log.info("关键词 '{}' 爬取任务完成，共成功爬取并保存了 {} 条新闻。", keyword, crawledNewsList.size());
        return crawledNewsList;
    }

    /**
     * 检查给定的URL是否是目标新浪新闻详情页
     * <p>
     * 使用预定义的正则表达式模式匹配URL，以确定其是否为有效的新浪新闻详情页URL。
     * </p>
     * 
     * @param url 待检查的URL
     * @return 如果是目标URL则返回true，否则返回false
     */
    private boolean isSinaNewsUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return SINA_NEWS_PATTERN_1.matcher(url).matches() || SINA_NEWS_PATTERN_2.matcher(url).matches();
    }

    /**
     * 爬取单个新浪新闻URL并存入数据库
     * <p>
     * 该方法首先检查数据库中是否已存在该URL对应的新闻，如果存在则直接返回已有数据。
     * 否则，使用Jsoup获取页面内容，解析出标题、来源、发布时间、正文等信息，
     * 创建NewsData实体并保存到数据库。该方法已适配多种新浪新闻页面结构。
     * </p>
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
     * 解析并设置新闻的发布时间，包含多种备用方案
     * <p>
     * 该方法首先尝试使用从页面提取的时间字符串解析发布时间，如果失败，
     * 则尝试从meta标签中获取时间信息。如果所有方案都失败，则使用当前时间作为发布时间。
     * </p>
     * 
     * @param newsData 要设置时间的新闻实体
     * @param doc Jsoup文档对象，用于获取meta标签
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

    /**
     * 使用多个选择器依次尝试获取元素文本
     * <p>
     * 该方法会依次尝试使用提供的选择器查找元素，返回第一个成功匹配的元素的文本内容。
     * 如果所有选择器都无法匹配到元素，则返回空字符串。
     * </p>
     * 
     * @param element 要在其中查找的父元素
     * @param selectors 要尝试的CSS选择器数组
     * @return 找到的元素文本，如果未找到则返回空字符串
     */
    private String getTextBySelectors(Element element, String... selectors) {
        return getTextBySelectors(element, selectors, "");
    }

    /**
     * 使用多个选择器依次尝试获取元素文本，带默认值
     * <p>
     * 该方法会依次尝试使用提供的选择器查找元素，返回第一个成功匹配的元素的文本内容。
     * 如果所有选择器都无法匹配到元素，则返回指定的默认值。
     * </p>
     * 
     * @param element 要在其中查找的父元素
     * @param selectors 要尝试的CSS选择器数组
     * @param defaultValue 未找到元素时返回的默认值
     * @return 找到的元素文本，如果未找到则返回默认值
     */
    private String getTextBySelectors(Element element, String[] selectors, String defaultValue) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null) {
                return found.text().trim();
            }
        }
        return defaultValue;
    }

    /**
     * 使用多个选择器依次尝试获取元素
     * <p>
     * 该方法会依次尝试使用提供的选择器查找元素，返回第一个成功匹配的元素。
     * 如果所有选择器都无法匹配到元素，则返回null。
     * </p>
     * 
     * @param element 要在其中查找的父元素
     * @param selectors 要尝试的CSS选择器数组
     * @return 找到的元素，如果未找到则返回null
     */
    private Element getElementBySelectors(Element element, String... selectors) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 根据URL查找已爬取的新闻数据
     * <p>
     * 该方法从数据库中查询指定URL的新闻数据，不会触发新的爬取操作。
     * 主要用于查看历史记录中的新闻详情。
     * </p>
     * 
     * @param url 新闻URL
     * @return 包含新闻数据的Optional对象，如果未找到则为empty
     */
    public Optional<NewsData> findNewsByUrl(String url) {
        log.info("从数据库查询URL对应的新闻数据: {}", url);
        return newsDataRepository.findByUrl(url);
    }
}