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

// 声明这是一个RESTful风格的控制器。
@RestController
// 将此控制器下的所有请求路径映射到"/api"下。
@RequestMapping("/api")
// 定义一个名为 ExportController 的公开类。
public class ExportController {

    // 创建一个静态不可变的Logger实例，用于记录日志。
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    
    // 声明一个不可变的新闻爬虫服务字段。
    private final NewsCrawlerService newsCrawlerService;
    // 声明一个不可变的文件导出服务字段。
    private final FileExportService fileExportService;
    // 声明一个不可变的爬取历史服务字段。
    private final CrawlHistoryService crawlHistoryService;
    // 声明一个不可变的用户服务字段。
    private final UserService userService;

    // 定义类的构造函数，通过它注入服务依赖。
    public ExportController(NewsCrawlerService newsCrawlerService, 
                            FileExportService fileExportService, 
                            CrawlHistoryService crawlHistoryService,
                            UserService userService) {
        // 将注入的新闻爬虫服务实例赋值给类成员变量。
        this.newsCrawlerService = newsCrawlerService;
        // 将注入的文件导出服务实例赋值给类成员变量。
        this.fileExportService = fileExportService;
        // 将注入的爬取历史服务实例赋值给类成员变量。
        this.crawlHistoryService = crawlHistoryService;
        // 将注入的用户服务实例赋值给类成员变量。
        this.userService = userService;
    } // 构造函数结束。

    // 将此方法映射到HTTP GET请求的"/export"路径。
    @GetMapping("/export")
    // 定义导出文件的API端点，接收多个请求参数。
    public ResponseEntity<byte[]> exportFile(
            // 定义一个必需的字符串请求参数"url"。
            @RequestParam String url,
            // 定义一个必需的字符串请求参数"format"。
            @RequestParam String format,
            // 定义一个可选的浮点数请求参数"lineSpacing"，默认值为1.5。
            @RequestParam(required = false, defaultValue = "1.5") Float lineSpacing,
            // 定义一个可选的整数请求参数"textFontSize"，默认值为14。
            @RequestParam(required = false, defaultValue = "14") Integer textFontSize,
            // 定义一个可选的整数请求参数"titleFontSize"。
            @RequestParam(required = false) Integer titleFontSize,
            // 定义一个可选的整数请求参数"h1FontSize"。
            @RequestParam(required = false) Integer h1FontSize,
            // 定义一个可选的整数请求参数"h2FontSize"。
            @RequestParam(required = false) Integer h2FontSize,
            // 定义一个可选的整数请求参数"h3FontSize"。
            @RequestParam(required = false) Integer h3FontSize,
            // 定义一个可选的整数请求参数"captionFontSize"。
            @RequestParam(required = false) Integer captionFontSize,
            // 定义一个可选的整数请求参数"footerFontSize"。
            @RequestParam(required = false) Integer footerFontSize,
            // 定义一个可选的字符串请求参数"fontFamily"。
            @RequestParam(required = false) String fontFamily
    ) { // 方法参数列表结束。
        // 记录收到文件导出请求的日志。
        logger.info("收到文件导出请求: URL={}, 格式={}, 文本大小={}, 行间距={}, 字体={}", url, format, textFontSize, lineSpacing, fontFamily);
        
        // 开始一个try块，用于捕获整个导出过程中可能发生的异常。
        try {
            // 检查基础正文字体大小是否在有效范围内。
            if (textFontSize < 8 || textFontSize > 32) {
                // 如果无效，则记录警告日志。
                logger.warn("无效的字体大小参数: {}", textFontSize);
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body("基础字体大小(textFontSize)必须在8到32之间".getBytes());
            } // if条件结束。
            // 检查行间距是否在有效范围内。
            if (lineSpacing < 1.0 || lineSpacing > 3.0) {
                // 如果无效，则记录警告日志。
                logger.warn("无效的行间距参数: {}", lineSpacing);
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body("行间距必须在1.0到3.0之间".getBytes());
            } // if条件结束。

            // 从Spring Security上下文中获取当前的认证信息。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 检查用户是否已认证。
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未认证，则记录警告日志。
                logger.warn("未认证用户尝试导出文件，URL: {}", url);
                // 返回401未授权状态。
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            } // if条件结束。

            // 获取已认证用户的用户名。
            String username = authentication.getName();
            // 调用用户服务根据用户名查找用户实体。
            User user = userService.findByUsername(username);
            // 从用户实体中获取用户ID。
            Long userId = user.getId();

            // 创建一个新的CrawlHistory实体对象。
            CrawlHistory crawlHistory = new CrawlHistory();
            // 为历史记录设置用户ID。
            crawlHistory.setUserId(userId);
            // 为历史记录设置爬取类型。
            crawlHistory.setCrawlType("SINGLE_URL");
            // 为历史记录设置被操作的URL。
            crawlHistory.setUrl(url);
            // 为历史记录设置一个临时的标题。
            crawlHistory.setTitle("导出文件: " + url);
            // 保存初始的历史记录，并获取包含数据库生成ID的返回对象。
            crawlHistory = crawlHistoryService.saveHistory(crawlHistory);
            
            // 声明一个Optional变量用于存储新闻数据。
            Optional<NewsData> newsDataOpt;
            // 调用新闻爬虫服务爬取新闻，并关联历史记录。
            newsDataOpt = newsCrawlerService.crawlAndSaveSinaNews(url, crawlHistory);
            
            // 检查是否成功获取到新闻数据。
            if (!newsDataOpt.isPresent()) {
                // 如果失败，则更新历史记录标题为失败状态。
                crawlHistory.setTitle("导出失败: 内容提取失败");
                // 保存更新后的失败历史记录。
                crawlHistoryService.saveHistory(crawlHistory);
                // 记录未能获取新闻数据的警告日志。
                logger.warn("未能获取新闻数据: {}", url);
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body("未能获取新闻数据".getBytes());
            } // if条件结束。
            // 从Optional中获取新闻数据实体。
            NewsData newsData = newsDataOpt.get();
            // 记录成功获取新闻数据的调试日志。
            logger.debug("成功获取新闻数据: {}", newsData.getTitle());

            // 基于基础正文字体大小计算各部分的默认字体大小。
            int defaultTitleSize = textFontSize + 8;
            // 计算默认的一级标题字体大小。
            int defaultH1Size = textFontSize + 6;
            // 计算默认的二级标题字体大小。
            int defaultH2Size = textFontSize + 4;
            // 计算默认的三级标题字体大小。
            int defaultH3Size = textFontSize + 2;
            // 计算默认的图片注释字体大小，确保不小于10。
            int defaultCaptionSize = Math.max(textFontSize - 2, 10);
            // 计算默认的页脚字体大小，确保不小于8。
            int defaultFooterSize = Math.max(textFontSize - 4, 8);

            // 使用用户提供的值或默认值来确定最终的字体大小。
            int finalTitleSize = (titleFontSize != null) ? titleFontSize : defaultTitleSize;
            // 确定最终的一级标题字体大小。
            int finalH1Size = (h1FontSize != null) ? h1FontSize : defaultH1Size;
            // 确定最终的二级标题字体大小。
            int finalH2Size = (h2FontSize != null) ? h2FontSize : defaultH2Size;
            // 确定最终的三级标题字体大小。
            int finalH3Size = (h3FontSize != null) ? h3FontSize : defaultH3Size;
            // 确定最终的图片注释字体大小。
            int finalCaptionSize = (captionFontSize != null) ? captionFontSize : defaultCaptionSize;
            // 确定最终的页脚字体大小。
            int finalFooterSize = (footerFontSize != null) ? footerFontSize : defaultFooterSize;
            
            // 声明一个字节数组用于存储文件内容。
            byte[] fileContent;
            // 声明一个字符串用于存储文件的MIME类型。
            String contentType;
            // 声明一个字符串用于存储文件的扩展名。
            String fileExtension;

            // 根据请求的格式生成不同类型的文件。
            if ("word".equalsIgnoreCase(format)) {
                // 记录生成Word文档的调试日志。
                logger.debug("生成Word文档，标题字体大小: {}, 正文字体大小: {}, 字体: {}", finalTitleSize, textFontSize, fontFamily);
                // 调用文件导出服务创建Word文档。
                fileContent = fileExportService.createWord(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize, fontFamily);
                // 设置Word文档的MIME类型。
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                // 设置文件扩展名为.docx。
                fileExtension = ".docx";
            } else if ("pdf".equalsIgnoreCase(format)) { // 如果请求格式是"pdf"。
                // 记录生成PDF文档的调试日志。
                logger.debug("生成PDF文档，标题字体大小: {}, 正文字体大小: {}, 字体: {}", finalTitleSize, textFontSize, fontFamily);
                // 调用文件导出服务创建PDF文档。
                fileContent = fileExportService.createPdf(newsData, lineSpacing, finalTitleSize, finalH1Size,
                        finalH2Size, finalH3Size, textFontSize, finalCaptionSize, finalFooterSize, fontFamily);
                // 设置PDF文档的MIME类型。
                contentType = "application/pdf";
                // 设置文件扩展名为.pdf。
                fileExtension = ".pdf";
            } else { // 如果格式不受支持。
                // 记录不支持的文件格式的警告日志。
                logger.warn("不支持的文件格式: {}", format);
                // 返回400错误请求状态和错误信息。
                return ResponseEntity.badRequest().body("不支持的格式，请使用 'word' 或 'pdf'".getBytes());
            } // if-else结束。

            // 将新闻标题中不适合文件名的字符替换为下划线。
            String safeFileName = newsData.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            // 对安全文件名进行URL编码，以支持HTTP头中的非ASCII字符。
            String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8.toString());

            // 创建一个新的HttpHeaders对象来设置响应头。
            HttpHeaders headers = new HttpHeaders();
            // 设置Content-Type响应头。
            headers.setContentType(MediaType.parseMediaType(contentType));
            // 设置Content-Disposition响应头，以触发浏览器下载文件。
            headers.setContentDispositionFormData("attachment", encodedFileName + fileExtension);
            // 设置缓存控制响应头。
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            // 设置Content-Length响应头。
            headers.setContentLength(fileContent.length);

            // 创建一个包含格式信息的字符串。
            String formatInfo = String.format("%s (正文:%d,行距:%.1f)",
                    format.toUpperCase(), textFontSize, lineSpacing);
            // 使用格式信息和新闻标题更新历史记录的标题。
            crawlHistory.setTitle("导出为" + formatInfo + ": " + newsData.getTitle());
            // 保存更新后的历史记录。
            crawlHistoryService.saveHistory(crawlHistory);
            
            // 记录已记录导出历史的调试日志。
            logger.debug("已记录导出历史");
            
            // 记录成功导出文件的日志。
            logger.info("成功导出文件: {}, 大小: {} 字节", encodedFileName + fileExtension, fileContent.length);
            // 返回一个包含文件内容、响应头和200 OK状态的ResponseEntity。
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (IOException e) { // 捕获IO异常。
            // 记录导出失败的错误日志。
            logger.error("导出失败 (IO异常): {}", e.getMessage());
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败：" + e.getMessage()).getBytes());
        } catch (Exception e) { // 捕获其他所有异常。
            // 记录导出失败的未知异常错误日志，并包含堆栈跟踪。
            logger.error("导出失败 (未知异常): {}", e.getMessage(), e);
            // 返回500服务器内部错误状态和错误信息。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("未知错误：" + e.getMessage()).getBytes());
        } // try-catch结束。
    } // exportFile方法结束。
} // ExportController类定义结束。