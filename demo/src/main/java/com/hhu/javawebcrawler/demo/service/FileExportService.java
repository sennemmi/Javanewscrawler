package com.hhu.javawebcrawler.demo.service;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;

@Service
public class FileExportService {

    // 默认字体路径
    private static final String FONT_PATH = "static/fonts/simsun.ttf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 默认字体大小
    private static final int DEFAULT_TITLE_FONT_SIZE = 22;
    private static final int DEFAULT_HEADING1_FONT_SIZE = 20;
    private static final int DEFAULT_HEADING2_FONT_SIZE = 18;
    private static final int DEFAULT_HEADING3_FONT_SIZE = 16;
    private static final int DEFAULT_TEXT_FONT_SIZE = 14;
    private static final int DEFAULT_CAPTION_FONT_SIZE = 12;
    private static final int DEFAULT_FOOTER_FONT_SIZE = 10;

    // 默认行间距 (相对于字体大小的倍数)
    private static final float DEFAULT_LINE_SPACING = 1.5f;

    /**
     * 使用默认字体和间距创建 Word 文档 (.docx)
     * @param newsData 新闻数据
     * @return Word 文件的字节数组
     */
    public byte[] createWord(NewsData newsData) throws IOException {
        return createWord(newsData, DEFAULT_LINE_SPACING, DEFAULT_TITLE_FONT_SIZE, DEFAULT_HEADING1_FONT_SIZE,
                DEFAULT_HEADING2_FONT_SIZE, DEFAULT_HEADING3_FONT_SIZE, DEFAULT_TEXT_FONT_SIZE,
                DEFAULT_CAPTION_FONT_SIZE, DEFAULT_FOOTER_FONT_SIZE);
    }

    /**
     * 创建 Word 文档 (.docx)，可自定义基础正文字体大小和行间距，其他元素字体大小将相对调整
     * @param newsData 新闻数据
     * @param textFontSize 正文字体大小
     * @param lineSpacing 行间距倍数
     * @return Word 文件的字节数组
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
     * @param newsData 新闻数据
     * @param lineSpacing 行间距倍数
     * @param titleFontSize 标题字体大小
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param textFontSize 正文字体大小
     * @param captionFontSize 图片/元数据字体大小
     * @param footerFontSize 页脚字体大小
     * @return Word 文件的字节数组
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
     * 处理段落文本
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
     * 处理标题
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
     * 处理列表
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
     * 处理图片
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
     * @param newsData 新闻数据
     * @return PDF 文件的字节数组
     */
    public byte[] createPdf(NewsData newsData) throws IOException {
        return createPdf(newsData, DEFAULT_LINE_SPACING, DEFAULT_TITLE_FONT_SIZE, DEFAULT_HEADING1_FONT_SIZE,
                DEFAULT_HEADING2_FONT_SIZE, DEFAULT_HEADING3_FONT_SIZE, DEFAULT_TEXT_FONT_SIZE,
                DEFAULT_CAPTION_FONT_SIZE, DEFAULT_FOOTER_FONT_SIZE);
    }

    /**
     * 创建 PDF 文档，可自定义基础正文字体大小和行间距，其他元素字体大小将相对调整
     * @param newsData 新闻数据
     * @param textFontSize 正文字体大小
     * @param lineSpacing 行间距倍数
     * @return PDF 文件的字节数组
     */
    public byte[] createPdf(NewsData newsData, int textFontSize, float lineSpacing) throws IOException {
        int titleFontSize = textFontSize + 8;
        int heading1FontSize = textFontSize + 6;
        int heading2FontSize = textFontSize + 4;
        int heading3FontSize = textFontSize + 2;
        int captionFontSize = Math.max(textFontSize - 2, 10);
        int footerFontSize = Math.max(textFontSize - 4, 8);

        return createPdf(newsData, lineSpacing, titleFontSize, heading1FontSize, heading2FontSize,
                heading3FontSize, textFontSize, captionFontSize, footerFontSize);
    }

    /**
     * 创建 PDF 文档，支持完全自定义所有元素的字体大小和行间距
     * @param newsData 新闻数据
     * @param lineSpacing 行间距倍数
     * @param titleFontSize 标题字体大小
     * @param heading1FontSize 一级标题字体大小
     * @param heading2FontSize 二级标题字体大小
     * @param heading3FontSize 三级标题字体大小
     * @param textFontSize 正文字体大小
     * @param captionFontSize 图片/元数据字体大小
     * @param footerFontSize 页脚字体大小
     * @return PDF 文件的字节数组
     */
    public byte[] createPdf(NewsData newsData, float lineSpacing, int titleFontSize, int heading1FontSize,
                            int heading2FontSize, int heading3FontSize, int textFontSize,
                            int captionFontSize, int footerFontSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        try {
            PdfFont font = loadChineseFont();
            PdfFont boldFont = loadChineseBoldFont();

            Paragraph title = new Paragraph(newsData.getTitle())
                    .setFont(boldFont)
                    .setFontSize(titleFontSize)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(titleFontSize * 1.2f);
            document.add(title);

            String metaInfo = String.format("来源: %s   发布时间: %s",
                    newsData.getSource(),
                    newsData.getPublishTime().format(DATE_FORMATTER));
            Paragraph meta = new Paragraph(metaInfo)
                    .setFont(font)
                    .setFontSize(captionFontSize)
                    .setItalic()
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(textFontSize * 1.5f);
            document.add(meta);

            document.add(new Paragraph("\n").setMarginBottom(textFontSize * 0.5f));

            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(newsData.getContent());

            processHtmlContentForPdf(document, htmlDoc, font, boldFont, lineSpacing,
                    heading1FontSize, heading2FontSize, heading3FontSize, textFontSize, captionFontSize);

            Paragraph footer = new Paragraph("——— 由Java Web爬虫系统生成 ———")
                    .setFont(font)
                    .setFontSize(footerFontSize)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(textFontSize * 2);
            document.add(footer);

        } catch (Exception e) {
            document.add(new Paragraph("文档生成过程中发生错误: " + e.getMessage()));
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    /**
     * 处理HTML内容，转换为PDF文档格式
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
     * 处理段落文本
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
     * 处理标题
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
     * 处理列表
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
     * 处理图片
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
     */
    private PdfFont loadChineseFont() throws IOException {
        try {
            try {
                ClassPathResource resource = new ClassPathResource(FONT_PATH);
                if (resource.exists()) {
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            } catch (Exception ignored) {
                // 忽略异常，继续使用系统字体
            }
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new IOException("无法加载任何字体", ex);
            }
        }
    }

    /**
     * 加载中文粗体字体
     */
    private PdfFont loadChineseBoldFont() throws IOException {
        try {
            // 注意: simsun.ttf 本身没有粗体变体，这里我们仍然加载它，并通过iText的API设置粗体样式。
            // 如果需要真正的粗体效果，应提供一个粗体字文件，如 simhei.ttf
            try {
                ClassPathResource resource = new ClassPathResource(FONT_PATH);
                if (resource.exists()) {
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            } catch (Exception ignored) {
                 // 忽略异常，继续使用系统字体
            }
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new IOException("无法加载任何字体", ex);
            }
        }
    }
}