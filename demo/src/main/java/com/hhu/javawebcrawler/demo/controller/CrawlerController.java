package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.FileExportService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import com.hhu.javawebcrawler.demo.entity.User;
import com.lowagie.text.DocumentException;

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
     * 【基础】根据URL爬取单个新闻
     * @param payload 请求体，包含 "url"
     * @return 爬取到的新闻数据
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
     * 【基础】导出新闻为 Word 或 PDF
     * @param url 新闻的 URL
     * @param format "word" 或 "pdf"
     * @return 文件流
     * @throws DocumentException
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFile(@RequestParam String url, @RequestParam String format) throws DocumentException {
        try {
            NewsData newsData = newsCrawlerService.crawlAndSaveSinaNews(url);
            
            byte[] fileContent;
            String contentType;
            String fileExtension;

            if ("word".equalsIgnoreCase(format)) {
                fileContent = fileExportService.createWord(newsData);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                fileExtension = ".docx";
            } else if ("pdf".equalsIgnoreCase(format)) {
                fileContent = fileExportService.createPdf(newsData);
                contentType = "application/pdf";
                fileExtension = ".pdf";
            } else {
                return ResponseEntity.badRequest().build();
            }

            String encodedFileName = URLEncoder.encode(newsData.getTitle(), StandardCharsets.UTF_8.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", encodedFileName + fileExtension);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 【基础】获取指定用户的爬取历史
     * @return 历史记录列表
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
}
