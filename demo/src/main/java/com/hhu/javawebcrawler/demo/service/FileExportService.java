package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.config.DocumentExportConfig;
import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 文件导出服务
 * <p>
 * 本服务提供将新闻数据导出为不同格式（包括PDF和Word文档）的功能。
 * 支持自定义文档的字体大小、行间距等样式属性，以满足不同的导出需求。
 * 能够处理新闻中的文本、图片、标题和列表等各种HTML元素，并保持合理的排版。
 * </p>
 * 
 * @author JavaWebCrawler团队
 */
@Service
public class FileExportService {

    private static final Logger logger = LoggerFactory.getLogger(FileExportService.class);

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 默认字体大小常量
    /** 默认标题字体大小 */
    private static final int DEFAULT_TITLE_FONT_SIZE = 22;
    /** 默认一级标题字体大小 */
    private static final int DEFAULT_HEADING1_FONT_SIZE = 20;
    /** 默认二级标题字体大小 */
    private static final int DEFAULT_HEADING2_FONT_SIZE = 18;
    /** 默认三级标题字体大小 */
    private static final int DEFAULT_HEADING3_FONT_SIZE = 16;
    /** 默认正文字体大小 */
    private static final int DEFAULT_TEXT_FONT_SIZE = 14;
    /** 默认图片说明字体大小 */
    private static final int DEFAULT_CAPTION_FONT_SIZE = 12;
    /** 默认页脚字体大小 */
    private static final int DEFAULT_FOOTER_FONT_SIZE = 10;

    /** 默认行间距 (相对于字体大小的倍数) */
    private static final float DEFAULT_LINE_SPACING = 1.5f;
    
    private final Map<String, String> fontMappings;
    private final Map<String, Object> documentConfig;
    
    // 使用命名的bean注入
    public FileExportService(
            @org.springframework.beans.factory.annotation.Qualifier("fontPathMappings") Map<String, String> fontMappings,
            @org.springframework.beans.factory.annotation.Qualifier("documentStyleConfig") Map<String, Object> documentExportConfig) {
        this.fontMappings = fontMappings;
        this.documentConfig = documentExportConfig;
        logger.info("文件导出服务已初始化，加载了{}种字体", fontMappings.size());
    }

    /**
     * 使用默认字体和间距创建 Word 文档 (.docx)
     * <p>
     * 将新闻数据转换为Word文档，使用预设的默认样式参数。
     * 适合快速导出不需要特殊格式要求的文档。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @return Word文档的字节数组，可直接用于下载或保存
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createWord(NewsData newsData) throws IOException {
        // 使用配置中的默认值
        float defaultLineSpacing = (Float)documentConfig.getOrDefault("defaultLineSpacing", DEFAULT_LINE_SPACING);
        int defaultTitleFontSize = (Integer)documentConfig.getOrDefault("defaultTitleFontSize", DEFAULT_TITLE_FONT_SIZE);
        int defaultHeading1FontSize = (Integer)documentConfig.getOrDefault("defaultHeading1FontSize", DEFAULT_HEADING1_FONT_SIZE);
        int defaultHeading2FontSize = (Integer)documentConfig.getOrDefault("defaultHeading2FontSize", DEFAULT_HEADING2_FONT_SIZE);
        int defaultHeading3FontSize = (Integer)documentConfig.getOrDefault("defaultHeading3FontSize", DEFAULT_HEADING3_FONT_SIZE);
        int defaultTextFontSize = (Integer)documentConfig.getOrDefault("defaultTextFontSize", DEFAULT_TEXT_FONT_SIZE);
        int defaultCaptionFontSize = (Integer)documentConfig.getOrDefault("defaultCaptionFontSize", DEFAULT_CAPTION_FONT_SIZE);
        int defaultFooterFontSize = (Integer)documentConfig.getOrDefault("defaultFooterFontSize", DEFAULT_FOOTER_FONT_SIZE);
        
        return createWord(newsData, defaultLineSpacing, defaultTitleFontSize, defaultHeading1FontSize,
                defaultHeading2FontSize, defaultHeading3FontSize, defaultTextFontSize,
                defaultCaptionFontSize, defaultFooterFontSize);
    }

    /**
     * 创建 Word 文档 (.docx)，可自定义基础正文字体大小和行间距
     * <p>
     * 通过指定正文字体大小和行间距，其他元素字体大小将相对于正文字体自动调整。
     * 适合需要调整基本阅读体验但不需要精细控制的场景。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @param textFontSize 正文字体大小（像素）
     * @param lineSpacing 行间距倍数（相对于字体大小）
     * @return Word文档的字节数组
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createWord(NewsData newsData, int textFontSize, float lineSpacing) throws IOException {
        // 为保持旧API兼容性，根据正文字体大小计算其他元素字体
        int titleFontSize = textFontSize + 8;
        int heading1FontSize = textFontSize + 6;
        int heading2FontSize = textFontSize + 4;
        int heading3FontSize = textFontSize + 2;
        int captionFontSize = Math.max(textFontSize - 2, 10);
        int footerFontSize = Math.max(textFontSize - 4, 8);

        return createWord(newsData, lineSpacing, titleFontSize, heading1FontSize, heading2FontSize,
                heading3FontSize, textFontSize, captionFontSize, footerFontSize);
    }

    /**
     * 创建 Word 文档 (.docx)，支持完全自定义所有元素的字体大小和行间距
     * <p>
     * 提供最大的灵活性，允许单独设置每种元素的字体大小。
     * 适合对文档格式有精细要求的场景。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @param lineSpacing 行间距倍数
     * @param titleFontSize 标题字体大小（像素）
     * @param heading1FontSize 一级标题字体大小（像素）
     * @param heading2FontSize 二级标题字体大小（像素）
     * @param heading3FontSize 三级标题字体大小（像素）
     * @param textFontSize 正文字体大小（像素）
     * @param captionFontSize 图片说明/元数据字体大小（像素）
     * @param footerFontSize 页脚字体大小（像素）
     * @return Word文档的字节数组
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createWord(NewsData newsData, float lineSpacing, int titleFontSize, int heading1FontSize,
                             int heading2FontSize, int heading3FontSize, int textFontSize,
                             int captionFontSize, int footerFontSize) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 添加文档标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            titleParagraph.setSpacingAfter((int) (titleFontSize * lineSpacing * 20)); // 设置段后间距
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(newsData.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(titleFontSize);
            titleRun.setFontFamily("黑体");
            titleRun.addBreak();

            // 添加元数据（来源和发布时间）
            XWPFParagraph metaParagraph = document.createParagraph();
            metaParagraph.setAlignment(ParagraphAlignment.CENTER);
            metaParagraph.setSpacingAfter((int) (captionFontSize * lineSpacing * 30)); // 设置段后间距
            XWPFRun metaRun = metaParagraph.createRun();
            String metaInfo = String.format("来源: %s   发布时间: %s",
                    newsData.getSource(),
                    newsData.getPublishTime().format(DATE_FORMATTER));
            metaRun.setText(metaInfo);
            metaRun.setFontSize(captionFontSize);
            metaRun.setItalic(true);
            metaRun.setColor("808080");
            metaRun.addBreak();
            metaRun.addBreak();

            // 解析HTML内容
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(newsData.getContent());

            // 处理正文段落和图片
            processHtmlContentForWord(document, htmlDoc, lineSpacing, heading1FontSize, heading2FontSize,
                    heading3FontSize, textFontSize, captionFontSize);

            // 添加页脚
            XWPFParagraph footerParagraph = document.createParagraph();
            footerParagraph.setAlignment(ParagraphAlignment.CENTER);
            footerParagraph.setSpacingBefore((int) (footerFontSize * lineSpacing * 20)); // 设置段前间距
            XWPFRun footerRun = footerParagraph.createRun();
            footerRun.setText("——— 由Java Web爬虫系统生成 ———");
            footerRun.setFontSize(footerFontSize);
            footerRun.setColor("A9A9A9");

            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 处理HTML内容，转换为Word文档格式
     * <p>
     * 解析HTML内容中的各种元素，并将其转换为Word文档对应的格式。
     * </p>
     * 
     * @param document Word文档对象
     * @param htmlDoc 已解析的HTML文档
     * @param lineSpacing 行间距倍数
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param textFontSize 正文字体大小
     * @param captionFontSize 图片说明字体大小
     */
    private void processHtmlContentForWord(XWPFDocument document, org.jsoup.nodes.Document htmlDoc,
                                           float lineSpacing, int heading1FontSize, int heading2FontSize,
                                           int heading3FontSize, int textFontSize, int captionFontSize) {
        Elements elements = htmlDoc.select("p, div.img_wrapper, h1, h2, h3, h4, h5, h6, ul, ol, li");

        for (Element element : elements) {
            if (element.is("div.img_wrapper")) {
                processImageForWord(document, element, captionFontSize, lineSpacing);
            } else if (element.is("p")) {
                processParagraphForWord(document, element, textFontSize, lineSpacing);
            } else if (element.is("h1, h2, h3, h4, h5, h6")) {
                processHeadingForWord(document, element, heading1FontSize, heading2FontSize, heading3FontSize, lineSpacing);
            } else if (element.is("ul, ol")) {
                processListForWord(document, element, textFontSize, lineSpacing);
            }
        }
    }

    /**
     * 处理段落文本为Word格式
     * <p>
     * 将HTML段落元素转换为Word文档的段落，设置首行缩进和段落间距。
     * </p>
     * 
     * @param document Word文档对象
     * @param element HTML段落元素
     * @param textFontSize 文本字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processParagraphForWord(XWPFDocument document, Element element, int textFontSize, float lineSpacing) {
        String text = element.text().trim();
        if (!text.isEmpty()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationFirstLine(600); // 首行缩进
            paragraph.setSpacingBetween(lineSpacing);
            paragraph.setSpacingAfter((int) (textFontSize * 20));
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            run.setFontSize(textFontSize);
            run.setFontFamily("宋体");
            run.addBreak();
        }
    }

    /**
     * 处理标题为Word格式
     * <p>
     * 将HTML标题元素(h1-h6)转换为Word文档的标题段落，根据标题级别设置不同的字体大小。
     * </p>
     * 
     * @param document Word文档对象
     * @param element HTML标题元素
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processHeadingForWord(XWPFDocument document, Element element,
                                       int heading1FontSize, int heading2FontSize,
                                       int heading3FontSize, float lineSpacing) {
        String text = element.text().trim();
        if (!text.isEmpty()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            paragraph.setSpacingBetween(lineSpacing);
            paragraph.setSpacingAfter((int) (heading3FontSize * 20));
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            run.setBold(true);

            if (element.is("h1")) {
                run.setFontSize(heading1FontSize);
                paragraph.setSpacingAfter((int) (heading1FontSize * 30));
            } else if (element.is("h2")) {
                run.setFontSize(heading2FontSize);
                paragraph.setSpacingAfter((int) (heading2FontSize * 25));
            } else if (element.is("h3")) {
                run.setFontSize(heading3FontSize);
            } else {
                run.setFontSize(heading3FontSize - 1);
            }

            run.setFontFamily("黑体");
            run.addBreak();
        }
    }

    /**
     * 处理列表为Word格式
     * <p>
     * 将HTML列表元素(ul/ol)转换为Word文档的列表，支持有序列表和无序列表。
     * </p>
     * 
     * @param document Word文档对象
     * @param listElement HTML列表元素
     * @param textFontSize 文本字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processListForWord(XWPFDocument document, Element listElement, int textFontSize, float lineSpacing) {
        Elements items = listElement.select("li");
        boolean isOrdered = listElement.is("ol");
        int counter = 1;

        for (Element item : items) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setIndentationLeft(900);
            paragraph.setSpacingBetween(lineSpacing);
            paragraph.setSpacingAfter((int) (textFontSize * 15));
            XWPFRun run = paragraph.createRun();

            if (isOrdered) {
                run.setText(counter++ + ". " + item.text().trim());
            } else {
                run.setText("• " + item.text().trim());
            }

            run.setFontSize(textFontSize);
            run.setFontFamily("宋体");
            run.addBreak();
        }
    }

    /**
     * 处理图片为Word格式
     * <p>
     * 将HTML图片元素转换为Word文档中的图片，并添加图片说明。
     * 如果图片无法加载，将显示错误信息。
     * </p>
     * 
     * @param document Word文档对象
     * @param imgWrapper HTML图片容器元素
     * @param captionFontSize 图片说明字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processImageForWord(XWPFDocument document, Element imgWrapper, int captionFontSize, float lineSpacing) {
        Element img = imgWrapper.selectFirst("img");
        if (img != null) {
            String imgSrc = img.attr("src");
            String imgAlt = img.attr("alt");

            if (!imgSrc.startsWith("http")) {
                imgSrc = "https:" + imgSrc;
            }

            try {
                XWPFParagraph imgParagraph = document.createParagraph();
                imgParagraph.setAlignment(ParagraphAlignment.CENTER);
                imgParagraph.setSpacingAfter(200);
                XWPFRun imgRun = imgParagraph.createRun();

                try (InputStream imageStream = URI.create(imgSrc).toURL().openStream()) {
                    byte[] imageBytes = imageStream.readAllBytes();
                    int pictureType;
                    if (imgSrc.toLowerCase().endsWith(".png")) {
                        pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                    } else if (imgSrc.toLowerCase().endsWith(".gif")) {
                        pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                    } else {
                        pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                    }

                    imgRun.addPicture(new ByteArrayInputStream(imageBytes), pictureType, imgSrc, 450, 300);
                    imgRun.addBreak();
                } catch (Exception e) {
                    imgRun.setText("[图片: " + imgSrc + "]");
                    imgRun.addBreak();
                    e.printStackTrace();
                }

                if (imgAlt != null && !imgAlt.isEmpty()) {
                    XWPFParagraph captionParagraph = document.createParagraph();
                    captionParagraph.setAlignment(ParagraphAlignment.CENTER);
                    captionParagraph.setSpacingAfter((int) (captionFontSize * lineSpacing * 20));
                    XWPFRun captionRun = captionParagraph.createRun();
                    captionRun.setText(imgAlt);
                    captionRun.setItalic(true);
                    captionRun.setFontSize(captionFontSize);
                    captionRun.setColor("505050");
                    captionRun.addBreak();
                }
            } catch (Exception e) {
                XWPFParagraph errorParagraph = document.createParagraph();
                XWPFRun errorRun = errorParagraph.createRun();
                errorRun.setText("[无法加载图片: " + imgSrc + "]");
                errorRun.addBreak();
            }
        }
    }

    /**
     * 使用默认字体和间距创建 PDF 文档
     * <p>
     * 将新闻数据转换为PDF文档，使用预设的默认样式参数。
     * 适合快速导出不需要特殊格式要求的文档。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @return PDF文档的字节数组，可直接用于下载或保存
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createPdf(NewsData newsData) throws IOException {
        // 使用配置中的默认值
        float defaultLineSpacing = (Float)documentConfig.getOrDefault("defaultLineSpacing", DEFAULT_LINE_SPACING);
        int defaultTitleFontSize = (Integer)documentConfig.getOrDefault("defaultTitleFontSize", DEFAULT_TITLE_FONT_SIZE);
        int defaultHeading1FontSize = (Integer)documentConfig.getOrDefault("defaultHeading1FontSize", DEFAULT_HEADING1_FONT_SIZE);
        int defaultHeading2FontSize = (Integer)documentConfig.getOrDefault("defaultHeading2FontSize", DEFAULT_HEADING2_FONT_SIZE);
        int defaultHeading3FontSize = (Integer)documentConfig.getOrDefault("defaultHeading3FontSize", DEFAULT_HEADING3_FONT_SIZE);
        int defaultTextFontSize = (Integer)documentConfig.getOrDefault("defaultTextFontSize", DEFAULT_TEXT_FONT_SIZE);
        int defaultCaptionFontSize = (Integer)documentConfig.getOrDefault("defaultCaptionFontSize", DEFAULT_CAPTION_FONT_SIZE);
        int defaultFooterFontSize = (Integer)documentConfig.getOrDefault("defaultFooterFontSize", DEFAULT_FOOTER_FONT_SIZE);
        
        return createPdf(newsData, defaultLineSpacing, defaultTitleFontSize, defaultHeading1FontSize,
                defaultHeading2FontSize, defaultHeading3FontSize, defaultTextFontSize,
                defaultCaptionFontSize, defaultFooterFontSize);
    }

    /**
     * 创建 PDF 文档，可自定义基础正文字体大小和行间距
     * <p>
     * 通过指定正文字体大小和行间距，其他元素字体大小将相对于正文字体自动调整。
     * 适合需要调整基本阅读体验但不需要精细控制的场景。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @param textFontSize 正文字体大小（像素）
     * @param lineSpacing 行间距倍数（相对于字体大小）
     * @return PDF文档的字节数组
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createPdf(NewsData newsData, int textFontSize, float lineSpacing) throws IOException {
        // 为保持旧API兼容性，根据正文字体大小计算其他元素字体
        int titleFontSize = textFontSize + 8;
        int heading1FontSize = textFontSize + 6;
        int heading2FontSize = textFontSize + 4;
        int heading3FontSize = textFontSize + 2;
        int captionFontSize = Math.max(textFontSize - 2, 10);
        int footerFontSize = Math.max(textFontSize - 4, 8);

        // 使用PDF特定的配置
        Float pdfMarginTop = (Float)documentConfig.getOrDefault("pdfMarginTop", 36f);
        Float pdfMarginBottom = (Float)documentConfig.getOrDefault("pdfMarginBottom", 36f);
        Float pdfMarginLeft = (Float)documentConfig.getOrDefault("pdfMarginLeft", 36f);
        Float pdfMarginRight = (Float)documentConfig.getOrDefault("pdfMarginRight", 36f);
        
        logger.debug("使用PDF边距配置: 上={}, 下={}, 左={}, 右={}", 
                     pdfMarginTop, pdfMarginBottom, pdfMarginLeft, pdfMarginRight);

        return createPdf(newsData, lineSpacing, titleFontSize, heading1FontSize, heading2FontSize,
                heading3FontSize, textFontSize, captionFontSize, footerFontSize);
    }

    /**
     * 创建 PDF 文档，支持完全自定义所有元素的字体大小和行间距
     * <p>
     * 提供最大的灵活性，允许单独设置每种元素的字体大小。
     * 适合对文档格式有精细要求的场景。
     * </p>
     * 
     * @param newsData 要导出的新闻数据对象
     * @param lineSpacing 行间距倍数
     * @param titleFontSize 标题字体大小（像素）
     * @param heading1FontSize 一级标题字体大小（像素）
     * @param heading2FontSize 二级标题字体大小（像素）
     * @param heading3FontSize 三级标题字体大小（像素）
     * @param textFontSize 正文字体大小（像素）
     * @param captionFontSize 图片说明/元数据字体大小（像素）
     * @param footerFontSize 页脚字体大小（像素）
     * @return PDF文档的字节数组
     * @throws IOException 如果文档创建过程中出现IO错误
     */
    public byte[] createPdf(NewsData newsData, float lineSpacing, int titleFontSize, int heading1FontSize,
                             int heading2FontSize, int heading3FontSize, int textFontSize,
                             int captionFontSize, int footerFontSize) throws IOException {
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 加载中文字体
            PdfFont font = loadChineseFont();
            PdfFont boldFont = loadChineseBoldFont();
            
            // 使用PDF特定的配置
            Float pdfMarginTop = (Float)documentConfig.getOrDefault("pdfMarginTop", 36f);
            Float pdfMarginBottom = (Float)documentConfig.getOrDefault("pdfMarginBottom", 36f);
            Float pdfMarginLeft = (Float)documentConfig.getOrDefault("pdfMarginLeft", 36f);
            Float pdfMarginRight = (Float)documentConfig.getOrDefault("pdfMarginRight", 36f);
            
            // 创建PDF文档
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // 设置文档边距
            document.setMargins(pdfMarginTop, pdfMarginRight, pdfMarginBottom, pdfMarginLeft);

            // 添加文档标题
            Paragraph title = new Paragraph(newsData.getTitle())
                    .setFont(boldFont)
                    .setFontSize(titleFontSize)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(titleFontSize * 0.8f);
            document.add(title);

            // 添加元数据（来源和发布时间）
            String metaInfo = String.format("来源: %s   发布时间: %s",
                    newsData.getSource(),
                    newsData.getPublishTime().format(DATE_FORMATTER));
            Paragraph meta = new Paragraph(metaInfo)
                    .setFont(font)
                    .setFontSize(captionFontSize)
                    .setItalic()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(captionFontSize * 2.0f);
            document.add(meta);

            // 解析HTML内容
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(newsData.getContent());

            // 处理正文段落和图片
            processHtmlContentForPdf(document, htmlDoc, font, boldFont, lineSpacing, heading1FontSize,
                    heading2FontSize, heading3FontSize, textFontSize, captionFontSize);

            // 添加页脚
            document.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("——— 由Java Web爬虫系统生成 ———")
                    .setFont(font)
                    .setFontSize(footerFontSize)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            // 关闭文档
            document.close();
            return baos.toByteArray();
        }
    }

    /**
     * 处理HTML内容，转换为PDF文档格式
     * <p>
     * 解析HTML内容中的各种元素，并将其转换为PDF文档对应的格式。
     * 包括段落、标题、列表和图片等元素的处理。
     * </p>
     * 
     * @param document PDF文档对象
     * @param htmlDoc 已解析的HTML文档
     * @param font 标准字体
     * @param boldFont 粗体字体
     * @param lineSpacing 行间距倍数
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param textFontSize 正文字体大小
     * @param captionFontSize 图片说明字体大小
     */
    private void processHtmlContentForPdf(Document document, org.jsoup.nodes.Document htmlDoc,
                                          PdfFont font, PdfFont boldFont, float lineSpacing,
                                          int heading1FontSize, int heading2FontSize, int heading3FontSize,
                                          int textFontSize, int captionFontSize) {
        Elements elements = htmlDoc.select("p, div.img_wrapper, h1, h2, h3, h4, h5, h6, ul, ol, li");

        for (Element element : elements) {
            if (element.is("div.img_wrapper")) {
                processImageForPdf(document, element, font, captionFontSize, lineSpacing);
            } else if (element.is("p")) {
                processParagraphForPdf(document, element, font, textFontSize, lineSpacing);
            } else if (element.is("h1, h2, h3, h4, h5, h6")) {
                processHeadingForPdf(document, element, boldFont, heading1FontSize,
                        heading2FontSize, heading3FontSize, lineSpacing);
            } else if (element.is("ul, ol")) {
                processListForPdf(document, element, font, textFontSize, lineSpacing);
            }
        }
    }

    /**
     * 处理段落文本为PDF格式
     * <p>
     * 将HTML段落元素转换为PDF文档的段落，设置首行缩进和段落间距。
     * </p>
     * 
     * @param document PDF文档对象
     * @param element HTML段落元素
     * @param font PDF字体
     * @param textFontSize 文本字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processParagraphForPdf(Document document, Element element, PdfFont font,
                                        int textFontSize, float lineSpacing) {
        String text = element.text().trim();
        if (!text.isEmpty()) {
            Paragraph paragraph = new Paragraph(text)
                    .setFont(font)
                    .setFontSize(textFontSize)
                    .setFirstLineIndent(28)
                    .setMarginBottom(textFontSize * 0.8f)
                    .setFixedLeading(textFontSize * lineSpacing);
            document.add(paragraph);
        }
    }

    /**
     * 处理标题为PDF格式
     * <p>
     * 将HTML标题元素(h1-h6)转换为PDF文档的标题段落，根据标题级别设置不同的字体大小。
     * </p>
     * 
     * @param document PDF文档对象
     * @param element HTML标题元素
     * @param boldFont 粗体PDF字体
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processHeadingForPdf(Document document, Element element, PdfFont boldFont,
                                      int heading1FontSize, int heading2FontSize,
                                      int heading3FontSize, float lineSpacing) {
        String text = element.text().trim();
        if (!text.isEmpty()) {
            float fontSize = heading3FontSize;
            float marginBottom = fontSize * 1.0f;

            if (element.is("h1")) {
                fontSize = heading1FontSize;
                marginBottom = fontSize * 1.2f;
            } else if (element.is("h2")) {
                fontSize = heading2FontSize;
                marginBottom = fontSize * 1.0f;
            } else if (element.is("h3")) {
                fontSize = heading3FontSize;
                marginBottom = fontSize * 0.8f;
            }

            Paragraph heading = new Paragraph(text)
                    .setFont(boldFont)
                    .setFontSize(fontSize)
                    .setBold()
                    .setMarginBottom(marginBottom)
                    .setFixedLeading(fontSize * lineSpacing);
            document.add(heading);
        }
    }

    /**
     * 处理列表为PDF格式
     * <p>
     * 将HTML列表元素(ul/ol)转换为PDF文档的列表，支持有序列表和无序列表。
     * </p>
     * 
     * @param document PDF文档对象
     * @param listElement HTML列表元素
     * @param font PDF字体
     * @param textFontSize 文本字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processListForPdf(Document document, Element listElement, PdfFont font,
                                   int textFontSize, float lineSpacing) {
        Elements items = listElement.select("li");
        boolean isOrdered = listElement.is("ol");
        int counter = 1;

        for (Element item : items) {
            String prefix = isOrdered ? counter++ + ". " : "• ";
            Paragraph listItem = new Paragraph()
                    .setFont(font)
                    .setFontSize(textFontSize)
                    .setMarginLeft(28)
                    .setMarginBottom(textFontSize * 0.5f)
                    .setFixedLeading(textFontSize * lineSpacing);

            listItem.add(new Text(prefix + item.text().trim()));
            document.add(listItem);
        }
    }

    /**
     * 处理图片为PDF格式
     * <p>
     * 将HTML图片元素转换为PDF文档中的图片，并添加图片说明。
     * 如果图片无法加载，将显示错误信息。
     * </p>
     * 
     * @param document PDF文档对象
     * @param imgWrapper HTML图片容器元素
     * @param font PDF字体
     * @param captionFontSize 图片说明字体大小
     * @param lineSpacing 行间距倍数
     */
    private void processImageForPdf(Document document, Element imgWrapper, PdfFont font,
                                    int captionFontSize, float lineSpacing) {
        Element img = imgWrapper.selectFirst("img");
        if (img != null) {
            String imgSrc = img.attr("src");
            String imgAlt = img.attr("alt");

            if (!imgSrc.startsWith("http")) {
                imgSrc = "https:" + imgSrc;
            }

            try {
                Image pdfImg = new Image(ImageDataFactory.create(URI.create(imgSrc).toURL()));
                pdfImg.setWidth(400);
                pdfImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                pdfImg.setMarginBottom(captionFontSize * 0.5f);
                document.add(pdfImg);

                if (imgAlt != null && !imgAlt.isEmpty()) {
                    Paragraph caption = new Paragraph(imgAlt)
                            .setFont(font)
                            .setFontSize(captionFontSize)
                            .setItalic()
                            .setFontColor(ColorConstants.DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(captionFontSize * 1.5f)
                            .setFixedLeading(captionFontSize * lineSpacing);
                    document.add(caption);
                }

            } catch (Exception e) {
                Paragraph errorText = new Paragraph("[无法加载图片: " + imgSrc + "]")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(captionFontSize * 1.0f);
                document.add(errorText);
            }
        }
    }

    /**
     * 加载中文字体
     * <p>
     * 尝试从类路径加载中文字体，如果失败则尝试使用系统字体。
     * 按优先级顺序尝试：配置的字体映射 -> 系统中文字体 -> 默认字体。
     * </p>
     * 
     * @return 可用于PDF的中文字体
     * @throws IOException 如果所有字体加载尝试都失败
     */
    private PdfFont loadChineseFont() throws IOException {
        // 首先尝试使用配置的宋体
        String fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_FONT_FAMILY);
        if (fontPath != null) {
            try {
                ClassPathResource resource = new ClassPathResource(fontPath);
                if (resource.exists()) {
                    logger.debug("使用配置的字体: {}", fontPath);
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            } catch (Exception e) {
                logger.warn("无法加载配置的字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 其次尝试使用系统字体
        try {
            logger.debug("尝试使用系统字体: STSong-Light");
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            logger.warn("无法加载系统字体: {}", e.getMessage());
            
            // 最后尝试使用默认字体
            try {
                logger.debug("尝试使用默认字体");
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                logger.error("无法加载任何字体: {}", ex.getMessage());
                throw new IOException("无法加载任何字体", ex);
            }
        }
    }

    /**
     * 加载中文粗体字体
     * <p>
     * 尝试从类路径加载中文粗体字体，如果失败则尝试使用系统字体。
     * 优先使用黑体作为粗体字体，如果不可用，则使用宋体并通过iText的API设置粗体样式。
     * </p>
     * 
     * @return 可用于PDF的中文粗体字体
     * @throws IOException 如果所有字体加载尝试都失败
     */
    private PdfFont loadChineseBoldFont() throws IOException {
        // 首先尝试使用配置的黑体
        String fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_TITLE_FONT_FAMILY);
        if (fontPath != null) {
            try {
                ClassPathResource resource = new ClassPathResource(fontPath);
                if (resource.exists()) {
                    logger.debug("使用配置的黑体字体: {}", fontPath);
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            } catch (Exception e) {
                logger.warn("无法加载配置的黑体字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 其次尝试使用宋体作为替代
        fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_FONT_FAMILY);
        if (fontPath != null) {
            try {
                ClassPathResource resource = new ClassPathResource(fontPath);
                if (resource.exists()) {
                    logger.debug("使用配置的宋体字体作为粗体替代: {}", fontPath);
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            } catch (Exception e) {
                logger.warn("无法加载配置的宋体字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 再次尝试使用系统字体
        try {
            logger.debug("尝试使用系统字体作为粗体: STSong-Light");
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            logger.warn("无法加载系统字体作为粗体: {}", e.getMessage());
            
            // 最后尝试使用默认字体
            try {
                logger.debug("尝试使用默认字体作为粗体");
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                logger.error("无法加载任何字体作为粗体: {}", ex.getMessage());
                throw new IOException("无法加载任何字体作为粗体", ex);
            }
        }
    }
}