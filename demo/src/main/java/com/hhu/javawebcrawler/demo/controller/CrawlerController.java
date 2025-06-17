package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.FileExportService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import com.hhu.javawebcrawler.demo.entity.User;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CrawlerController {

    private final NewsCrawlerService newsCrawlerService;
    private final FileExportService fileExportService;
    private final CrawlHistoryService crawlHistoryService;
    private final UserService userService;

    public CrawlerController(NewsCrawlerService newsCrawlerService, FileExportService fileExportService, CrawlHistoryService crawlHistoryService, UserService userService) {
        this.newsCrawlerService = newsCrawlerService;
        this.fileExportService = fileExportService;
        this.crawlHistoryService = crawlHistoryService;
        this.userService = userService;
    }

    /**
     * 【基础】根据URL爬取单个新闻，同时记录爬取历史
     * @param payload 请求体，必须包含 "url" 字段
     * @return 爬取到的新闻数据，包含标题、内容、发布时间等信息
     * @throws IOException 如果爬取过程中发生IO异常
     * @apiNote 需要用户已登录认证，否则返回401错误
     */
    @PostMapping("/crawl/single")
    public ResponseEntity<NewsData> crawlSingleUrl(@RequestBody Map<String, String> payload) {
        String url = payload.get("url");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            System.err.println("Error retrieving user ID for username: " + username + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            NewsData newsData = newsCrawlerService.crawlAndSaveSinaNews(url);
            crawlHistoryService.recordSingleUrlCrawl(userId, url, newsData.getTitle());
            return ResponseEntity.ok(newsData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 【高级】导出新闻为 Word 或 PDF，支持基础和高级字体自定义
     *
     * @param url 新闻的 URL，必填参数
     * @param format "word" 或 "pdf"，必填参数
     * @param lineSpacing 行间距倍数，默认为 1.5，取值范围 1.0-3.0
     * @param textFontSize 基础正文字体大小，默认为 14，取值范围 8-32。其他字体大小将基于此相对计算
     * @param titleFontSize (可选) 覆盖主标题字体大小，默认为 textFontSize + 8
     * @param h1FontSize (可选) 覆盖一级标题字体大小，默认为 textFontSize + 6
     * @param h2FontSize (可选) 覆盖二级标题字体大小，默认为 textFontSize + 4
     * @param h3FontSize (可选) 覆盖三级标题字体大小，默认为 textFontSize + 2
     * @param captionFontSize (可选) 覆盖图片注释和元数据字体大小，默认为 max(textFontSize - 2, 10)
     * @param footerFontSize (可选) 覆盖页脚字体大小，默认为 max(textFontSize - 4, 8)
     * @return 文件下载流，附带适当的 Content-Type 和 Content-Disposition 头
     * @throws IOException 如果爬取或导出过程中发生IO异常
     * @apiNote 需要用户已登录认证，否则返回401错误；操作会记录到用户的爬取历史中
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFile(
            @RequestParam String url,
            @RequestParam String format,
            @RequestParam(required = false, defaultValue = "1.5") Float lineSpacing,
            @RequestParam(required = false, defaultValue = "14") Integer textFontSize,
            // --- 新增的可选覆盖参数 ---
            @RequestParam(required = false) Integer titleFontSize,
            @RequestParam(required = false) Integer h1FontSize,
            @RequestParam(required = false) Integer h2FontSize,
            @RequestParam(required = false) Integer h3FontSize,
            @RequestParam(required = false) Integer captionFontSize,
            @RequestParam(required = false) Integer footerFontSize
    ) {
        try {
            // 验证参数
            if (textFontSize < 8 || textFontSize > 32) {
                return ResponseEntity.badRequest().body("基础字体大小(textFontSize)必须在8到32之间".getBytes());
            }
            if (lineSpacing < 1.0 || lineSpacing > 3.0) {
                return ResponseEntity.badRequest().body("行间距必须在1.0到3.0之间".getBytes());
            }

            // 验证用户是否已登录 (这部分逻辑保持不变)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // 爬取或获取已有的新闻数据 (这部分逻辑保持不变)
            NewsData newsData = newsCrawlerService.crawlAndSaveSinaNews(url);

            // 1. 根据基础正文字号，计算出各部分的默认字号
            int defaultTitleSize = textFontSize + 8;
            int defaultH1Size = textFontSize + 6;
            int defaultH2Size = textFontSize + 4;
            int defaultH3Size = textFontSize + 2;
            int defaultCaptionSize = Math.max(textFontSize - 2, 10);
            int defaultFooterSize = Math.max(textFontSize - 4, 8);

            // 2. 如果用户提供了覆盖值，则使用用户的值，否则使用默认值
            int finalTitleSize = (titleFontSize != null) ? titleFontSize : defaultTitleSize;
            int finalH1Size = (h1FontSize != null) ? h1FontSize : defaultH1Size;
            int finalH2Size = (h2FontSize != null) ? h2FontSize : defaultH2Size;
            int finalH3Size = (h3FontSize != null) ? h3FontSize : defaultH3Size;
            int finalCaptionSize = (captionFontSize != null) ? captionFontSize : defaultCaptionSize;
            int finalFooterSize = (footerFontSize != null) ? footerFontSize : defaultFooterSize;
            
            byte[] fileContent;
            String contentType;
            String fileExtension;

            // 根据格式生成不同类型的文件，并调用新的、参数完整的方法
            if ("word".equalsIgnoreCase(format)) {
                fileContent = fileExportService.createWord(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                fileExtension = ".docx";
            } else if ("pdf".equalsIgnoreCase(format)) {
                fileContent = fileExportService.createPdf(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize);
                contentType = "application/pdf";
                fileExtension = ".pdf";
            } else {
                return ResponseEntity.badRequest().body("不支持的格式，请使用 'word' 或 'pdf'".getBytes());
            }

            // 设置文件名和响应头 (这部分逻辑保持不变)
            String safeFileName = newsData.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", encodedFileName + fileExtension);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(fileContent.length);

            // 记录导出历史 (这部分逻辑保持不变)
            // 可选：可以增强日志信息，记录所有自定义的字体大小
            String formatInfo = String.format("%s (正文:%d,行距:%.1f)",
                    format.toUpperCase(), textFontSize, lineSpacing);
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            crawlHistoryService.recordSingleUrlCrawl(user.getId(), url, "导出为" + formatInfo + ": " + newsData.getTitle());

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败：" + e.getMessage()).getBytes());
        } catch (Exception e) {
            e.printStackTrace(); // 在服务器日志中打印完整堆栈，便于调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("未知错误：" + e.getMessage()).getBytes());
        }
    }


    /**
     * 【基础】获取当前登录用户的爬取历史记录
     * @return 爬取历史记录列表，按时间倒序排列
     * @throws Exception 如果查询用户信息或爬取历史时发生异常
     * @apiNote 需要用户已登录认证，否则返回401错误；每条历史记录包含爬取的URL、标题和时间
     */
    @GetMapping("/history")
    public ResponseEntity<List<CrawlHistory>> getHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            System.err.println("Error retrieving user ID for username: " + username + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        List<CrawlHistory> history = crawlHistoryService.getUserHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * 【新增功能】从指定的入口页面（如新闻首页）进行二级爬取
     *
     * @param payload 请求体，必须包含 "url" 字段，值为新闻列表页或首页的URL
     * @return 包含操作结果信息的响应体
     * @apiNote 需要用户已登录认证。该操作会遍历入口页面所有符合条件的链接并逐一爬取，耗时可能较长。
     */
    @PostMapping("/crawl/from-index")
    public ResponseEntity<Map<String, Object>> crawlFromIndex(@RequestBody Map<String, String> payload) {
        String indexUrl = payload.get("url");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未认证"));
        }

        if (indexUrl == null || indexUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "入口URL不能为空"));
        }

        String username = authentication.getName();
        Long userId;
        try {
            User user = userService.findByUsername(username);
            userId = user.getId();
        } catch (Exception e) {
            System.err.println("Error retrieving user ID for username: " + username + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "获取用户信息失败"));
        }

        try {
            List<NewsData> crawledList = newsCrawlerService.crawlNewsFromIndexPage(indexUrl);
            int crawledCount = crawledList.size();
            
            // 收集爬取的新闻URL列表（最多存储前10个，避免JSON过大）
            List<String> sampleUrls = crawledList.stream()
                    .map(NewsData::getUrl)
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
            
            // 构建爬取历史记录所需的参数
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("totalCount", crawledCount);
            params.put("sampleUrls", sampleUrls);
            params.put("crawlTime", java.time.LocalDateTime.now().toString());
            
            // 记录爬取历史，使用INDEX_CRAWL类型
            String title = String.format("从入口页 %s 爬取了 %d 条新闻", indexUrl, crawledCount);
            crawlHistoryService.recordIndexCrawl(userId, indexUrl, title, params);

            Map<String, Object> response = Map.of(
                    "message", "二级爬取任务完成",
                    "crawledCount", crawledCount,
                    "entryUrl", indexUrl
            );
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "爬取入口页面失败: " + e.getMessage()));
        }
    }
}
