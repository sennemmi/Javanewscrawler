package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// 声明这是一个Spring的服务层组件。
@Service
// 使用Lombok为该类自动生成一个SLF4J的logger实例，变量名为log。
@Slf4j
// 定义一个名为 NewsCrawlerService 的公开类。
public class NewsCrawlerService {

    // 声明一个用于新闻数据持久化的、不可变的仓库字段。
    private final NewsDataRepository newsDataRepository;

    // 定义一个静态不可变的字符串数组，存储用于提取新闻标题的CSS选择器。
    private static final String[] TITLE_SELECTORS = {"h1.main-title"};
    
    // 定义一个静态不可变的字符串数组，存储用于提取新闻来源的CSS选择器。
    private static final String[] SOURCE_SELECTORS = {
            // 选择器1：匹配 ".top-bar-inner .date-source .author a" 元素。
            ".top-bar-inner .date-source .author a",
            // 选择器2：匹配 ".date-source a.source" 元素。
            ".date-source a.source",
            // 选择器3：匹配 ".top-bar-inner .date-source a.ent-source" 元素。
            ".top-bar-inner .date-source a.ent-source"
    };
    
    // 定义一个静态不可变的字符串数组，存储用于提取发布时间的CSS选择器。
    private static final String[] TIME_SELECTORS = {
            // 选择器1：匹配 ".date-source .date" 元素。
            ".date-source .date",
            // 选择器2：匹配 ".top-bar-inner .date-source .date" 元素。
            ".top-bar-inner .date-source .date"
    };
    
    // 定义一个静态不可变的字符串数组，存储用于提取新闻正文的CSS选择器。
    private static final String[] CONTENT_SELECTORS = {"div#article"};

    // 定义一个静态不可变的正则表达式模式，用于匹配第一种新浪新闻URL格式。
    private static final Pattern SINA_NEWS_PATTERN_1 = Pattern.compile("^https://news\\.sina\\.com\\.cn/\\w/\\d{4}-\\d{2}-\\d{2}/doc-[a-z0-9]+\\.shtml$");
    
    // 定义一个静态不可变的正则表达式模式，用于匹配第二种新浪新闻URL格式。
    private static final Pattern SINA_NEWS_PATTERN_2 = Pattern.compile("^https://k\\.sina\\.com\\.cn/article_\\w+\\.html$");

    // 定义类的构造函数，通过它注入NewsDataRepository依赖。
    public NewsCrawlerService(NewsDataRepository newsDataRepository) {
        // 将注入的仓库实例赋值给类成员变量。
        this.newsDataRepository = newsDataRepository;
    } // 构造函数结束。

    // 定义从入口页爬取新闻的方法，可能抛出IOException。
    public List<NewsData> crawlNewsFromIndexPage(String indexUrl) throws IOException {
        // 使用log记录二级爬取任务的开始信息。
        log.info("开始二级爬取任务，入口页面: {}", indexUrl);
        // 初始化一个列表，用于存储爬取到的新闻数据。
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 使用Jsoup连接到指定的入口URL。
        Connection.Response response = Jsoup.connect(indexUrl)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为20秒。
                .timeout(20000)
                // 执行连接请求并获取响应。
                .execute();
        // 解析响应体为Jsoup的Document对象。
        Document indexDoc = response.parse();
        // 从文档中选择所有带有href属性的<a>标签。
        Elements links = indexDoc.select("a[href]");
        // 记录在入口页面找到的链接总数。
        log.info("在入口页面找到 {} 个链接，开始筛选...", links.size());

        // 创建一个Set来存储有效的、待爬取的URL，以自动去重。
        Set<String> validUrlsToCrawl = new HashSet<>();
        // 遍历所有找到的链接元素。
        for (Element link : links) {
            // 获取链接的绝对URL并去除首尾空格。
            String absUrl = link.absUrl("href").trim();
            // 查找URL中'?'字符的位置。
            int queryPos = absUrl.indexOf('?');
            // 如果找到'?'，则截取其之前的部分，以移除查询参数。
            if (queryPos != -1) {
                // 更新absUrl为不含查询参数的URL。
                absUrl = absUrl.substring(0, queryPos);
            } // if条件结束。
            // 查找URL中'#'字符的位置。
            int hashPos = absUrl.indexOf('#');
            // 如果找到'#'，则截取其之前的部分，以移除哈希片段。
            if(hashPos != -1) {
                // 更新absUrl为不含哈希片段的URL。
                absUrl = absUrl.substring(0, hashPos);
            } // if条件结束。

            // 检查清理后的URL是否为有效的新浪新闻URL。
            if (isSinaNewsUrl(absUrl)) {
                // 如果是，则将其添加到待爬取URL的集合中。
                validUrlsToCrawl.add(absUrl);
            } // if条件结束。
        } // for循环结束。
        
        // 记录筛选出的有效新闻URL数量。
        log.info("筛选出 {} 个有效的新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 初始化一个计数器，用于跟踪处理进度。
        int count = 0;
        // 初始化一个计数器，用于记录因内容提取失败而跳过的URL数量。
        int skippedCount = 0;
        // 遍历所有有效的待爬取URL。
        for (String urlToCrawl : validUrlsToCrawl) {
            // 进度计数器加一。
            count++;
            // 记录当前爬取进度和正在处理的URL。
            log.info("二级爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            // 开始一个try块，以捕获单个URL处理中可能发生的异常。
            try {
                // 调用单页爬取方法，并获取返回的Optional对象。
                Optional<NewsData> newsDataOpt = crawlAndSaveSinaNews(urlToCrawl);
                // 检查Optional对象是否包含新闻数据。
                if (newsDataOpt.isPresent()) {
                    // 如果包含，则将新闻数据添加到结果列表中。
                    crawledNewsList.add(newsDataOpt.get());
                } else { // 如果Optional为空。
                    // 跳过计数器加一。
                    skippedCount++;
                    // 记录该URL因内容提取失败而被跳过。
                    log.info("二级爬取过程中，URL {} 的内容提取失败，已跳过", urlToCrawl);
                } // if-else结束。
                // 让当前线程暂停500毫秒，以避免请求过于频繁。
                Thread.sleep(500);
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录处理特定URL时发生的错误信息。
                log.error("二级爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
                // 注释：单个页面失败不影响整体任务，程序将继续处理下一个URL。
            } // try-catch结束。
        } // for循环结束。

        // 记录二级爬取任务完成后的总结信息。
        log.info("二级爬取任务完成，共成功爬取并保存了 {} 条新闻，跳过了 {} 条内容提取失败的新闻。", crawledNewsList.size(), skippedCount);
        // 返回包含所有成功爬取并保存的新闻数据的列表。
        return crawledNewsList;
    } // crawlNewsFromIndexPage方法结束。
    
    // 定义从入口页爬取新闻并关联历史记录的方法。
    public List<NewsData> crawlNewsFromIndexPage(String indexUrl, CrawlHistory crawlHistory) throws IOException {
        // 记录关联历史的二级爬取任务开始信息。
        log.info("开始二级爬取任务，入口页面: {}", indexUrl);
        // 初始化一个列表，用于存储爬取到的新闻数据。
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 使用Jsoup连接到指定的入口URL。
        Connection.Response response = Jsoup.connect(indexUrl)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为20秒。
                .timeout(20000)
                // 执行连接请求并获取响应。
                .execute();
        // 解析响应体为Jsoup的Document对象。
        Document indexDoc = response.parse();

        // 从文档中选择所有带有href属性的<a>标签。
        Elements links = indexDoc.select("a[href]");
        // 记录在入口页面找到的链接总数。
        log.info("在入口页面找到 {} 个链接，开始筛选...", links.size());

        // 创建一个Set来存储有效的、待爬取的URL，以自动去重。
        Set<String> validUrlsToCrawl = new HashSet<>();
        // 遍历所有找到的链接元素。
        for (Element link : links) {
            // 获取链接的绝对URL并去除首尾空格。
            String absUrl = link.absUrl("href").trim();
            // 查找URL中'?'字符的位置。
            int queryPos = absUrl.indexOf('?');
            // 如果找到'?'，则截取其之前的部分，以移除查询参数。
            if (queryPos != -1) {
                // 更新absUrl为不含查询参数的URL。
                absUrl = absUrl.substring(0, queryPos);
            } // if条件结束。
            // 查找URL中'#'字符的位置。
            int hashPos = absUrl.indexOf('#');
            // 如果找到'#'，则截取其之前的部分，以移除哈希片段。
            if(hashPos != -1) {
                // 更新absUrl为不含哈希片段的URL。
                absUrl = absUrl.substring(0, hashPos);
            } // if条件结束。

            // 检查清理后的URL是否为有效的新浪新闻URL。
            if (isSinaNewsUrl(absUrl)) {
                // 如果是，则将其添加到待爬取URL的集合中。
                validUrlsToCrawl.add(absUrl);
            } // if条件结束。
        } // for循环结束。
        
        // 记录筛选出的有效新闻URL数量。
        log.info("筛选出 {} 个有效的新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 初始化一个计数器，用于跟踪处理进度。
        int count = 0;
        // 初始化一个计数器，用于记录因内容提取失败而跳过的URL数量。
        int skippedCount = 0;
        // 遍历所有有效的待爬取URL。
        for (String urlToCrawl : validUrlsToCrawl) {
            // 进度计数器加一。
            count++;
            // 记录当前爬取进度和正在处理的URL。
            log.info("二级爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            // 开始一个try块，以捕获单个URL处理中可能发生的异常。
            try {
                // 调用带有crawlHistory参数的单页爬取方法。
                Optional<NewsData> newsDataOpt = crawlAndSaveSinaNews(urlToCrawl, crawlHistory);
                // 检查Optional对象是否包含新闻数据。
                if (newsDataOpt.isPresent()) {
                    // 如果包含，则将新闻数据添加到结果列表中。
                    crawledNewsList.add(newsDataOpt.get());
                } else { // 如果Optional为空。
                    // 跳过计数器加一。
                    skippedCount++;
                    // 记录该URL因内容提取失败而被跳过。
                    log.info("二级爬取过程中，URL {} 的内容提取失败，已跳过", urlToCrawl);
                } // if-else结束。
                // 让当前线程暂停500毫秒，以避免请求过于频繁。
                Thread.sleep(500);
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录处理特定URL时发生的错误信息。
                log.error("二级爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
                // 注释：单个页面失败不影响整体任务，程序将继续处理下一个URL。
            } // try-catch结束。
        } // for循环结束。

        // 记录二级爬取任务完成后的总结信息。
        log.info("二级爬取任务完成，共成功爬取并保存了 {} 条新闻，跳过了 {} 条内容提取失败的新闻。", crawledNewsList.size(), skippedCount);
        // 返回包含所有成功爬取并保存的新闻数据的列表。
        return crawledNewsList;
    } // 带历史记录的crawlNewsFromIndexPage方法结束。

    // 定义一个便捷方法，使用默认入口页按关键词爬取新闻。
    public List<NewsData> crawlNewsByKeyword(String keyword) throws IOException {
        // 调用完整的关键词爬取方法，并传入默认的新浪新闻首页URL。
        return crawlNewsByKeyword(keyword, "https://news.sina.com.cn/");
    } // crawlNewsByKeyword便捷方法结束。

    // 定义按关键词和指定入口页爬取新闻的方法。
    public List<NewsData> crawlNewsByKeyword(String keyword, String indexUrl) throws IOException {
        // 记录按关键词爬取任务的开始信息。
        log.info("开始按关键词 '{}' 爬取任务，入口页面: {}", keyword, indexUrl);
        // 初始化一个列表，用于存储爬取到的新闻数据。
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 使用Jsoup连接到指定的入口URL。
        Document indexDoc = Jsoup.connect(indexUrl)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为20秒。
                .timeout(20000)
                // 使用GET方法获取页面文档。
                .get();

        // 从文档中选择所有带有href属性的<a>标签。
        Elements links = indexDoc.select("a[href]");
        // 记录找到的链接总数和用于筛选的关键词。
        log.info("在入口页面找到 {} 个链接，开始根据关键词 '{}' 进行筛选...", links.size(), keyword);

        // 创建一个Set来存储有效的、待爬取的URL，以自动去重。
        Set<String> validUrlsToCrawl = new HashSet<>();
        // 遍历所有找到的链接元素。
        for (Element link : links) {
            // 获取链接的文本内容并去除首尾空格。
            String linkTitle = link.text().trim();
            // 检查链接文本（忽略大小写）是否包含指定的关键词。
            if (linkTitle.toLowerCase().contains(keyword.toLowerCase())) {
                // 如果包含，则获取该链接的绝对URL并去除首尾空格。
                String absUrl = link.absUrl("href").trim();
                // 查找URL中'?'字符的位置。
                int queryPos = absUrl.indexOf('?');
                // 如果找到'?'，则截取其之前的部分，以移除查询参数。
                if (queryPos != -1) {
                    // 更新absUrl为不含查询参数的URL。
                    absUrl = absUrl.substring(0, queryPos);
                } // if条件结束。
                // 查找URL中'#'字符的位置。
                int hashPos = absUrl.indexOf('#');
                // 如果找到'#'，则截取其之前的部分，以移除哈希片段。
                if(hashPos != -1) {
                    // 更新absUrl为不含哈希片段的URL。
                    absUrl = absUrl.substring(0, hashPos);
                } // if条件结束。

                // 检查清理后的URL是否为有效的新浪新闻URL。
                if (isSinaNewsUrl(absUrl)) {
                    // 如果是，则将其添加到待爬取URL的集合中。
                    validUrlsToCrawl.add(absUrl);
                } // if条件结束。
            } // if条件结束。
        } // for循环结束。

        // 记录筛选出的标题含关键词的有效新闻URL数量。
        log.info("筛选出 {} 个标题含关键词的有效新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 初始化一个计数器，用于跟踪处理进度。
        int count = 0;
        // 初始化一个计数器，用于记录因内容提取失败而跳过的URL数量。
        int skippedCount = 0;
        // 遍历所有有效的待爬取URL。
        for (String urlToCrawl : validUrlsToCrawl) {
            // 进度计数器加一。
            count++;
            // 记录当前关键词爬取进度和正在处理的URL。
            log.info("关键词爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            // 开始一个try块，以捕获单个URL处理中可能发生的异常。
            try {
                // 调用单页爬取方法，并获取返回的Optional对象。
                Optional<NewsData> newsDataOpt = crawlAndSaveSinaNews(urlToCrawl);
                // 检查Optional对象是否包含新闻数据。
                if (newsDataOpt.isPresent()) {
                    // 如果包含，则将新闻数据添加到结果列表中。
                    crawledNewsList.add(newsDataOpt.get());
                } else { // 如果Optional为空。
                    // 跳过计数器加一。
                    skippedCount++;
                    // 记录该URL因内容提取失败而被跳过。
                    log.info("关键词爬取过程中，URL {} 的内容提取失败，已跳过", urlToCrawl);
                } // if-else结束。
                // 让当前线程暂停500毫秒，以避免请求过于频繁。
                Thread.sleep(500);
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录处理特定URL时发生的错误信息。
                log.error("关键词爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
            } // try-catch结束。
        } // for循环结束。

        // 记录关键词爬取任务完成后的总结信息。
        log.info("关键词 '{}' 爬取任务完成，共成功爬取并保存了 {} 条新闻，跳过了 {} 条内容提取失败的新闻。", keyword, crawledNewsList.size(), skippedCount);
        // 返回包含所有成功爬取并保存的新闻数据的列表。
        return crawledNewsList;
    } // crawlNewsByKeyword方法结束。

    // 定义按关键词爬取并关联历史记录的方法。
    public List<NewsData> crawlNewsByKeyword(String keyword, String indexUrl, CrawlHistory crawlHistory) throws IOException {
        // 记录关联历史的关键词爬取任务开始信息。
        log.info("开始按关键词 '{}' 爬取任务，入口页面: {}", keyword, indexUrl);
        // 初始化一个列表，用于存储爬取到的新闻数据。
        List<NewsData> crawledNewsList = new ArrayList<>();

        // 使用Jsoup连接到指定的入口URL。
        Document indexDoc = Jsoup.connect(indexUrl)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为20秒。
                .timeout(20000)
                // 使用GET方法获取页面文档。
                .get();

        // 从文档中选择所有带有href属性的<a>标签。
        Elements links = indexDoc.select("a[href]");
        // 记录找到的链接总数和用于筛选的关键词。
        log.info("在入口页面找到 {} 个链接，开始根据关键词 '{}' 进行筛选...", links.size(), keyword);

        // 创建一个Set来存储有效的、待爬取的URL，以自动去重。
        Set<String> validUrlsToCrawl = new HashSet<>();
        // 遍历所有找到的链接元素。
        for (Element link : links) {
            // 获取链接的文本内容并去除首尾空格。
            String linkTitle = link.text().trim();
            // 检查链接文本（忽略大小写）是否包含指定的关键词。
            if (linkTitle.toLowerCase().contains(keyword.toLowerCase())) {
                // 如果包含，则获取该链接的绝对URL并去除首尾空格。
                String absUrl = link.absUrl("href").trim();
                // 查找URL中'?'字符的位置。
                int queryPos = absUrl.indexOf('?');
                // 如果找到'?'，则截取其之前的部分，以移除查询参数。
                if (queryPos != -1) {
                    // 更新absUrl为不含查询参数的URL。
                    absUrl = absUrl.substring(0, queryPos);
                } // if条件结束。
                // 查找URL中'#'字符的位置。
                int hashPos = absUrl.indexOf('#');
                // 如果找到'#'，则截取其之前的部分，以移除哈希片段。
                if(hashPos != -1) {
                    // 更新absUrl为不含哈希片段的URL。
                    absUrl = absUrl.substring(0, hashPos);
                } // if条件结束。

                // 检查清理后的URL是否为有效的新浪新闻URL。
                if (isSinaNewsUrl(absUrl)) {
                    // 如果是，则将其添加到待爬取URL的集合中。
                    validUrlsToCrawl.add(absUrl);
                } // if条件结束。
            } // if条件结束。
        } // for循环结束。

        // 记录筛选出的标题含关键词的有效新闻URL数量。
        log.info("筛选出 {} 个标题含关键词的有效新闻详情页URL准备爬取。", validUrlsToCrawl.size());

        // 初始化一个计数器，用于跟踪处理进度。
        int count = 0;
        // 初始化一个计数器，用于记录因内容提取失败而跳过的URL数量。
        int skippedCount = 0;
        // 遍历所有有效的待爬取URL。
        for (String urlToCrawl : validUrlsToCrawl) {
            // 进度计数器加一。
            count++;
            // 记录当前关键词爬取进度和正在处理的URL。
            log.info("关键词爬取进度: {}/{}, 正在处理URL: {}", count, validUrlsToCrawl.size(), urlToCrawl);
            // 开始一个try块，以捕获单个URL处理中可能发生的异常。
            try {
                // 调用带有crawlHistory参数的单页爬取方法。
                Optional<NewsData> newsDataOpt = crawlAndSaveSinaNews(urlToCrawl, crawlHistory);
                // 检查Optional对象是否包含新闻数据。
                if (newsDataOpt.isPresent()) {
                    // 如果包含，则将新闻数据添加到结果列表中。
                    crawledNewsList.add(newsDataOpt.get());
                } else { // 如果Optional为空。
                    // 跳过计数器加一。
                    skippedCount++;
                    // 记录该URL因内容提取失败而被跳过。
                    log.info("关键词爬取过程中，URL {} 的内容提取失败，已跳过", urlToCrawl);
                } // if-else结束。
                // 让当前线程暂停500毫秒，以避免请求过于频繁。
                Thread.sleep(500);
            } catch (Exception e) { // 捕获在try块中发生的任何异常。
                // 记录处理特定URL时发生的错误信息。
                log.error("关键词爬取过程中，处理URL {} 失败: {}", urlToCrawl, e.getMessage());
            } // try-catch结束。
        } // for循环结束。

        // 记录关键词爬取任务完成后的总结信息。
        log.info("关键词 '{}' 爬取任务完成，共成功爬取并保存了 {} 条新闻，跳过了 {} 条内容提取失败的新闻。", keyword, crawledNewsList.size(), skippedCount);
        // 返回包含所有成功爬取并保存的新闻数据的列表。
        return crawledNewsList;
    } // 带历史记录的crawlNewsByKeyword方法结束。

    // 定义一个私有方法，用于检查URL是否为新浪新闻URL。
    private boolean isSinaNewsUrl(String url) {
        // 检查URL是否为null或空字符串。
        if (url == null || url.isEmpty()) {
            // 如果是，则返回false。
            return false;
        } // if条件结束。
        // 使用两个正则表达式模式匹配URL，只要有一个匹配成功就返回true。
        return SINA_NEWS_PATTERN_1.matcher(url).matches() || SINA_NEWS_PATTERN_2.matcher(url).matches();
    } // isSinaNewsUrl方法结束。

    // 声明此方法需要在一个事务中执行。
    @Transactional
    // 定义爬取并保存单个新浪新闻的方法。
    public Optional<NewsData> crawlAndSaveSinaNews(String url) throws IOException {
        // 根据URL在数据库中查找是否已存在该新闻。
        Optional<NewsData> existingNews = newsDataRepository.findByUrl(url);
        // 检查查询结果是否存在。
        if (existingNews.isPresent()) {
            // 如果存在，则记录日志并跳过爬取。
            log.info("新闻已存在于数据库，跳过爬取: {}", url);
            // 返回已存在的新闻数据。
            return existingNews;
        } // if条件结束。

        // 记录开始爬取新新闻的日志。
        log.info("开始爬取新闻: {}", url);
        // 使用Jsoup连接到指定的URL。
        Document doc = Jsoup.connect(url)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为15秒。
                .timeout(15000)
                // 使用GET方法获取页面文档。
                .get();

        // 使用选择器数组尝试提取标题。
        String title = getTextBySelectors(doc, TITLE_SELECTORS);
        // 如果使用主要选择器未能提取到标题。
        if (title.isEmpty()) {
            // 则尝试从页面的<title>标签获取，并进行清理。
            title = doc.title().replace("_新浪新闻", "").replace("_新浪网", "").trim();
        } // if条件结束。
        
        // 使用选择器数组尝试提取来源，如果失败则使用默认值"未知来源"。
        String source = getTextBySelectors(doc, SOURCE_SELECTORS, "未知来源");
        // 使用选择器数组尝试提取发布时间字符串。
        String publishTimeStr = getTextBySelectors(doc, TIME_SELECTORS);

        // 使用选择器数组尝试获取包含正文内容的元素。
        Element articleContentElement = getElementBySelectors(doc, CONTENT_SELECTORS);
        // 初始化内容字符串为“内容提取失败”。
        String content = "内容提取失败";
        // 检查是否成功获取到正文元素。
        if (articleContentElement != null) {
            // 从正文元素中移除不必要的子元素（如作者信息、广告等）。
            articleContentElement.select("p.show_author, .wap_special, .article-notice, div[id^=ad_], ins.sinaads").remove();
            // 移除被列入黑名单的图片及其父元素。
            articleContentElement.select("img[black-list=y]").parents().remove();
            // 获取清理后元素的HTML内容作为新闻正文。
            content = articleContentElement.html();
        } // if条件结束。

        // 如果内容仍然是初始的失败状态。
        if ("内容提取失败".equals(content)) {
            // 记录警告日志，说明内容提取失败。
            log.warn("新闻内容提取失败，跳过保存: {}", url);
            // 返回一个空的Optional对象，表示不保存此新闻。
            return Optional.empty();
        } // if条件结束。

        // 从页面的meta标签中提取关键词。
        String keywords = doc.select("meta[name=keywords]").attr("content");

        // 创建一个新的NewsData实体对象。
        NewsData newsData = new NewsData();
        // 设置新闻的URL。
        newsData.setUrl(url);
        // 设置新闻的标题，如果标题为空则设置为"无标题"。
        newsData.setTitle(title.isEmpty() ? "无标题" : title);
        // 设置新闻的来源。
        newsData.setSource(source);
        // 设置新闻的正文内容。
        newsData.setContent(content);
        // 设置新闻的关键词。
        newsData.setKeywords(keywords);

        // 调用私有方法来解析并设置发布时间。
        parseAndSetPublishTime(newsData, doc, publishTimeStr);

        // 记录新闻爬取成功并准备保存到数据库。
        log.info("新闻爬取成功，正在保存到数据库: {}", newsData.getTitle());
        // 保存新闻实体到数据库，并将其包装在Optional中返回。
        return Optional.of(newsDataRepository.save(newsData));
    } // crawlAndSaveSinaNews方法结束。

    // 声明此方法需要在一个事务中执行。
    @Transactional
    // 定义爬取并保存单个新闻并关联历史记录的方法。
    public Optional<NewsData> crawlAndSaveSinaNews(String url, CrawlHistory crawlHistory) throws IOException {
        // 根据URL在数据库中查找是否已存在该新闻。
        Optional<NewsData> existingNews = newsDataRepository.findByUrl(url);
        // 检查查询结果是否存在。
        if (existingNews.isPresent()) {
            // 如果存在，则记录日志并跳过爬取。
            log.info("新闻已存在于数据库，跳过爬取: {}", url);
            // 获取已存在的新闻数据实体。
            NewsData newsData = existingNews.get();
            // 如果传入了有效的爬取历史记录，且该新闻当前未关联任何历史记录。
            if (crawlHistory != null && newsData.getCrawlHistory() == null) {
                // 将新闻与该爬取历史记录关联。
                newsData.setCrawlHistory(crawlHistory);
                // 保存更新后的新闻实体，并将其包装在Optional中返回。
                return Optional.of(newsDataRepository.save(newsData));
            } // if条件结束。
            // 返回已存在的新闻数据。
            return existingNews;
        } // if条件结束。

        // 记录开始爬取新新闻的日志。
        log.info("开始爬取新闻: {}", url);
        // 使用Jsoup连接到指定的URL。
        Document doc = Jsoup.connect(url)
                // 设置User-Agent模拟浏览器访问。
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                // 设置连接超时时间为15秒。
                .timeout(15000)
                // 使用GET方法获取页面文档。
                .get();

        // 使用选择器数组尝试提取标题。
        String title = getTextBySelectors(doc, TITLE_SELECTORS);
        // 如果使用主要选择器未能提取到标题。
        if (title.isEmpty()) {
            // 则尝试从页面的<title>标签获取，并进行清理。
            title = doc.title().replace("_新浪新闻", "").replace("_新浪网", "").trim();
        } // if条件结束。
        
        // 使用选择器数组尝试提取来源，如果失败则使用默认值"未知来源"。
        String source = getTextBySelectors(doc, SOURCE_SELECTORS, "未知来源");
        // 使用选择器数组尝试提取发布时间字符串。
        String publishTimeStr = getTextBySelectors(doc, TIME_SELECTORS);

        // 使用选择器数组尝试获取包含正文内容的元素。
        Element articleContentElement = getElementBySelectors(doc, CONTENT_SELECTORS);
        // 初始化内容字符串为“内容提取失败”。
        String content = "内容提取失败";
        // 检查是否成功获取到正文元素。
        if (articleContentElement != null) {
            // 从正文元素中移除不必要的子元素（如作者信息、广告等）。
            articleContentElement.select("p.show_author, .wap_special, .article-notice, div[id^=ad_], ins.sinaads").remove();
            // 移除被列入黑名单的图片及其父元素。
            articleContentElement.select("img[black-list=y]").parents().remove();
            // 获取清理后元素的HTML内容作为新闻正文。
            content = articleContentElement.html();
        } // if条件结束。

        // 如果内容仍然是初始的失败状态。
        if ("内容提取失败".equals(content)) {
            // 记录警告日志，说明内容提取失败。
            log.warn("新闻内容提取失败，跳过保存: {}", url);
            // 返回一个空的Optional对象，表示不保存此新闻。
            return Optional.empty();
        } // if条件结束。

        // 从页面的meta标签中提取关键词。
        String keywords = doc.select("meta[name=keywords]").attr("content");

        // 创建一个新的NewsData实体对象。
        NewsData newsData = new NewsData();
        // 设置新闻的URL。
        newsData.setUrl(url);
        // 设置新闻的标题，如果标题为空则设置为"无标题"。
        newsData.setTitle(title.isEmpty() ? "无标题" : title);
        // 设置新闻的来源。
        newsData.setSource(source);
        // 设置新闻的正文内容。
        newsData.setContent(content);
        // 设置新闻的关键词。
        newsData.setKeywords(keywords);
        // 将新闻与传入的爬取历史记录关联。
        newsData.setCrawlHistory(crawlHistory);

        // 调用私有方法来解析并设置发布时间。
        parseAndSetPublishTime(newsData, doc, publishTimeStr);

        // 记录新闻爬取成功并准备保存到数据库。
        log.info("新闻爬取成功，正在保存到数据库: {}", newsData.getTitle());
        // 保存新闻实体到数据库，并将其包装在Optional中返回。
        return Optional.of(newsDataRepository.save(newsData));
    } // 带历史记录的crawlAndSaveSinaNews方法结束。

    // 定义一个私有方法，用于解析并设置新闻的发布时间。
    private void parseAndSetPublishTime(NewsData newsData, Document doc, String timeStr) {
        // 检查从页面文本提取的时间字符串是否有效。
        if (timeStr != null && !timeStr.isEmpty()) {
            // 开始一个try块，尝试使用标准格式解析时间。
            try {
                // 定义一个日期时间格式化器。
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
                // 解析时间字符串并设置到newsData对象中。
                newsData.setPublishTime(LocalDateTime.parse(timeStr.trim(), formatter));
                // 解析成功，直接返回。
                return;
            } catch (Exception e) { // 如果解析失败。
                // 记录警告日志，说明标准格式解析失败。
                log.warn("使用标准格式'yyyy年MM月dd日 HH:mm'解析时间 '{}' 失败, 尝试备用方案...", timeStr);
            } // try-catch结束。
        } // if条件结束。
        
        // 开始一个try块，尝试从meta标签中获取时间。
        try {
            // 选择具有'article:published_time'属性的meta标签，并获取其content属性值。
            String metaTime = doc.selectFirst("meta[property=article:published_time]").attr("content");
            // 检查获取到的meta时间字符串是否有效。
            if (metaTime != null && !metaTime.isEmpty()) {
                // 使用ISO_OFFSET_DATE_TIME格式化器解析时间，并设置到newsData对象中。
                newsData.setPublishTime(LocalDateTime.parse(metaTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                // 解析成功，直接返回。
                return;
            } // if条件结束。
        } catch (Exception ex) { // 如果选择或解析meta标签失败。
            // 注释：忽略异常，程序将继续尝试下一个方案或使用默认值。
        } // try-catch结束。

        // 如果所有方案都失败，记录警告日志。
        log.warn("所有时间解析方案均失败，将使用当前时间。URL: {}", newsData.getUrl());
        // 使用当前的系统时间作为发布时间。
        newsData.setPublishTime(LocalDateTime.now());
    } // parseAndSetPublishTime方法结束。

    // 定义一个私有辅助方法，使用多个选择器尝试获取文本。
    private String getTextBySelectors(Element element, String... selectors) {
        // 调用重载方法，并传入一个空字符串作为默认值。
        return getTextBySelectors(element, selectors, "");
    } // getTextBySelectors方法结束。

    // 定义一个私有辅助方法，使用多个选择器尝试获取文本，并提供默认值。
    private String getTextBySelectors(Element element, String[] selectors, String defaultValue) {
        // 遍历传入的所有CSS选择器。
        for (String selector : selectors) {
            // 使用当前选择器在指定元素下查找第一个匹配的子元素。
            Element found = element.selectFirst(selector);
            // 检查是否找到了元素。
            if (found != null) {
                // 如果找到，返回该元素的文本内容并去除首尾空格。
                return found.text().trim();
            } // if条件结束。
        } // for循环结束。
        // 如果所有选择器都未找到匹配元素，则返回指定的默认值。
        return defaultValue;
    } // 带默认值的getTextBySelectors方法结束。

    // 定义一个私有辅助方法，使用多个选择器尝试获取元素。
    private Element getElementBySelectors(Element element, String... selectors) {
        // 遍历传入的所有CSS选择器。
        for (String selector : selectors) {
            // 使用当前选择器在指定元素下查找第一个匹配的子元素。
            Element found = element.selectFirst(selector);
            // 检查是否找到了元素。
            if (found != null) {
                // 如果找到，直接返回该元素。
                return found;
            } // if条件结束。
        } // for循环结束。
        // 如果所有选择器都未找到匹配元素，则返回null。
        return null;
    } // getElementBySelectors方法结束。

    // 定义根据URL从数据库查找新闻数据的方法。
    public Optional<NewsData> findNewsByUrl(String url) {
        // 记录从数据库查询新闻的日志。
        log.info("从数据库查询URL对应的新闻数据: {}", url);
        // 调用仓库的findByUrl方法并返回结果。
        return newsDataRepository.findByUrl(url);
    } // findNewsByUrl方法结束。

    // 定义根据爬取历史ID查找关联新闻数据的方法。
    public List<NewsData> findNewsByCrawlHistoryId(Long historyId) {
        // 记录查询关联新闻的日志。
        log.info("查询爬取历史ID {} 关联的新闻数据", historyId);
        // 调用仓库的findByCrawlHistoryId方法并返回结果。
        return newsDataRepository.findByCrawlHistoryId(historyId);
    } // findNewsByCrawlHistoryId方法结束。
} // NewsCrawlerService类定义结束。