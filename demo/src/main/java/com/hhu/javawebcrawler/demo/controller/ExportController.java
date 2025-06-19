package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.entity.CrawlHistory;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.CrawlHistoryService;
import com.hhu.javawebcrawler.demo.service.FileExportService;
import com.hhu.javawebcrawler.demo.service.NewsCrawlerService;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

/**
 * 文件导出控制器
 * <p>
 * 负责处理将新闻导出为不同格式文件的请求，支持Word和PDF格式，提供灵活的格式和样式控制选项。
 * </p>
 */
@RestController
@RequestMapping("/api")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    
    private final NewsCrawlerService newsCrawlerService;
    private final FileExportService fileExportService;
    private final CrawlHistoryService crawlHistoryService;
    private final UserService userService;

    public ExportController(NewsCrawlerService newsCrawlerService, 
                            FileExportService fileExportService, 
                            CrawlHistoryService crawlHistoryService,
                            UserService userService) {
        this.newsCrawlerService = newsCrawlerService;
        this.fileExportService = fileExportService;
        this.crawlHistoryService = crawlHistoryService;
        this.userService = userService;
    }

    /**
     * 导出新闻为Word或PDF，支持基础和高级字体自定义
     *
     * @param url 新闻的URL，必填参数
     * @param format "word"或"pdf"，必填参数
     * @param lineSpacing 行间距倍数，默认为1.5，取值范围1.0-3.0
     * @param textFontSize 基础正文字体大小，默认为14，取值范围8-32。其他字体大小将基于此相对计算
     * @param titleFontSize (可选)覆盖主标题字体大小，默认为textFontSize+8
     * @param h1FontSize (可选)覆盖一级标题字体大小，默认为textFontSize+6
     * @param h2FontSize (可选)覆盖二级标题字体大小，默认为textFontSize+4
     * @param h3FontSize (可选)覆盖三级标题字体大小，默认为textFontSize+2
     * @param captionFontSize (可选)覆盖图片注释和元数据字体大小，默认为max(textFontSize-2, 10)
     * @param footerFontSize (可选)覆盖页脚字体大小，默认为max(textFontSize-4, 8)
     * @return 文件下载流，附带适当的Content-Type和Content-Disposition头
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFile(
            @RequestParam String url,
            @RequestParam String format,
            @RequestParam(required = false, defaultValue = "1.5") Float lineSpacing,
            @RequestParam(required = false, defaultValue = "14") Integer textFontSize,
            @RequestParam(required = false) Integer titleFontSize,
            @RequestParam(required = false) Integer h1FontSize,
            @RequestParam(required = false) Integer h2FontSize,
            @RequestParam(required = false) Integer h3FontSize,
            @RequestParam(required = false) Integer captionFontSize,
            @RequestParam(required = false) Integer footerFontSize
    ) {
        logger.info("收到文件导出请求: URL={}, 格式={}, 文本大小={}, 行间距={}", url, format, textFontSize, lineSpacing);
        
        try {
            // 验证参数
            if (textFontSize < 8 || textFontSize > 32) {
                logger.warn("无效的字体大小参数: {}", textFontSize);
                return ResponseEntity.badRequest().body("基础字体大小(textFontSize)必须在8到32之间".getBytes());
            }
            if (lineSpacing < 1.0 || lineSpacing > 3.0) {
                logger.warn("无效的行间距参数: {}", lineSpacing);
                return ResponseEntity.badRequest().body("行间距必须在1.0到3.0之间".getBytes());
            }

            // 验证用户是否已登录
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("未认证用户尝试导出文件，URL: {}", url);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // 获取用户ID
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            Long userId = user.getId();

            // 创建爬取历史记录
            CrawlHistory crawlHistory = new CrawlHistory();
            crawlHistory.setUserId(userId);
            crawlHistory.setCrawlType("SINGLE_URL");
            crawlHistory.setUrl(url);
            crawlHistory.setTitle("导出文件: " + url);
            crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
            
            // 爬取或获取已有的新闻数据
            Optional<NewsData> newsDataOpt;
            newsDataOpt = newsCrawlerService.crawlAndSaveSinaNews(url, crawlHistory);
            
            if (!newsDataOpt.isPresent()) {
                // 如果内容提取失败，更新历史记录
                crawlHistory.setTitle("导出失败: 内容提取失败");
                crawlHistoryService.saveHistory(crawlHistory);
                logger.warn("未能获取新闻数据: {}", url);
                return ResponseEntity.badRequest().body("未能获取新闻数据".getBytes());
            }
            NewsData newsData = newsDataOpt.get();
            logger.debug("成功获取新闻数据: {}", newsData.getTitle());

            // 计算字体大小
            int defaultTitleSize = textFontSize + 8;
            int defaultH1Size = textFontSize + 6;
            int defaultH2Size = textFontSize + 4;
            int defaultH3Size = textFontSize + 2;
            int defaultCaptionSize = Math.max(textFontSize - 2, 10);
            int defaultFooterSize = Math.max(textFontSize - 4, 8);

            // 使用用户提供的值或默认值
            int finalTitleSize = (titleFontSize != null) ? titleFontSize : defaultTitleSize;
            int finalH1Size = (h1FontSize != null) ? h1FontSize : defaultH1Size;
            int finalH2Size = (h2FontSize != null) ? h2FontSize : defaultH2Size;
            int finalH3Size = (h3FontSize != null) ? h3FontSize : defaultH3Size;
            int finalCaptionSize = (captionFontSize != null) ? captionFontSize : defaultCaptionSize;
            int finalFooterSize = (footerFontSize != null) ? footerFontSize : defaultFooterSize;
            
            byte[] fileContent;
            String contentType;
            String fileExtension;

            // 根据格式生成不同类型的文件
            if ("word".equalsIgnoreCase(format)) {
                logger.debug("生成Word文档，标题字体大小: {}, 正文字体大小: {}", finalTitleSize, textFontSize);
                fileContent = fileExportService.createWord(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                fileExtension = ".docx";
            } else if ("pdf".equalsIgnoreCase(format)) {
                logger.debug("生成PDF文档，标题字体大小: {}, 正文字体大小: {}", finalTitleSize, textFontSize);
                fileContent = fileExportService.createPdf(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize);
                contentType = "application/pdf";
                fileExtension = ".pdf";
            } else {
                logger.warn("不支持的文件格式: {}", format);
                return ResponseEntity.badRequest().body("不支持的格式，请使用 'word' 或 'pdf'".getBytes());
            }

            // 设置文件名和响应头
            String safeFileName = newsData.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", encodedFileName + fileExtension);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(fileContent.length);

            // 更新爬取历史记录的标题
            String formatInfo = String.format("%s (正文:%d,行距:%.1f)",
                    format.toUpperCase(), textFontSize, lineSpacing);
            crawlHistory.setTitle("导出为" + formatInfo + ": " + newsData.getTitle());
            crawlHistoryService.saveHistory(crawlHistory);
            
            logger.debug("已记录导出历史");
            
            logger.info("成功导出文件: {}, 大小: {} 字节", encodedFileName + fileExtension, fileContent.length);
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("导出失败 (IO异常): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败：" + e.getMessage()).getBytes());
        } catch (Exception e) {
            logger.error("导出失败 (未知异常): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("未知错误：" + e.getMessage()).getBytes());
        }
    }
} 