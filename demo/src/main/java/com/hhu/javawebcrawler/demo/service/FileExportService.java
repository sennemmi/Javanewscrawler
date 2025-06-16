package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.NewsData;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class FileExportService {

    // 确保在 src/main/resources/fonts/ 目录下有这个字体文件
    public static final String FONT_PATH = "fonts/simsun.ttf";

    /**
     * 创建 Word 文档 (.docx)，支持段落排版
     * @param newsData 新闻数据
     * @return Word 文件的字节数组
     */
    public byte[] createWord(NewsData newsData) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(newsData.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("黑体");

            // 副标题 (来源和时间)
            XWPFParagraph metaParagraph = document.createParagraph();
            metaParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun metaRun = metaParagraph.createRun();
            String metaInfo = String.format("来源: %s   发布时间: %s",
                newsData.getSource(),
                newsData.getPublishTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            metaRun.setText(metaInfo);
            metaRun.setFontSize(12);
            metaRun.setItalic(true);
            metaRun.setColor("808080"); // 灰色

            // 空一行
            document.createParagraph();

            // 正文内容 (解析HTML并保留段落)
            Document contentDoc = Jsoup.parse(newsData.getContent());
            for (Element p : contentDoc.select("p, div.img_wrapper")) {
                // 如果是图片容器
                if (p.is("div.img_wrapper")) {
                    Element img = p.selectFirst("img");
                    if (img != null) {
                        String imgSrc = img.attr("abs:src"); // 获取绝对路径
                        XWPFParagraph imgParagraph = document.createParagraph();
                        XWPFRun imgRun = imgParagraph.createRun();
                        imgRun.setText("[图片: " + imgSrc + "]"); // 简单地将图片URL作为文本插入
                        imgRun.addBreak();
                        // 提示：完整的图片插入需要下载图片流并使用 addPicture() 方法，会更复杂
                    }
                }
                // 如果是文本段落
                else if (p.is("p")) {
                    XWPFParagraph contentParagraph = document.createParagraph();
                    contentParagraph.setIndentationFirstLine(600); // 首行缩进
                    XWPFRun contentRun = contentParagraph.createRun();
                    contentRun.setText(p.text());
                    contentRun.setFontSize(14);
                    contentRun.setFontFamily("宋体");
                }
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建 PDF 文档，支持段落排版
     * @param newsData 新闻数据
     * @return PDF 文件的字节数组
     * @throws DocumentException 
     */
    public byte[] createPdf(NewsData newsData) throws IOException, DocumentException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            try {
                PdfWriter.getInstance(document, out);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // 设置中文字体
            BaseFont bfChinese = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 22, Font.BOLD);
            Font metaFont = new Font(bfChinese, 12, Font.ITALIC, java.awt.Color.GRAY);
            Font contentFont = new Font(bfChinese, 14, Font.NORMAL);

            document.open();

            // 标题
            Paragraph titlePara = new Paragraph(newsData.getTitle(), titleFont);
            titlePara.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(titlePara);
            
            // 副标题
            String metaInfo = String.format("来源: %s   发布时间: %s",
                newsData.getSource(),
                newsData.getPublishTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            Paragraph metaPara = new Paragraph(metaInfo, metaFont);
            metaPara.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(metaPara);
            
            // 添加空行
            document.add(new Paragraph(" "));

            // 正文 (解析HTML并保留段落)
            Document contentDoc = Jsoup.parse(newsData.getContent());
            for (Element p : contentDoc.select("p, div.img_wrapper")) {
                 if (p.is("div.img_wrapper")) {
                    Element img = p.selectFirst("img");
                    if (img != null) {
                        String imgSrc = img.attr("abs:src");
                        Paragraph imgPara = new Paragraph("[图片: " + imgSrc + "]", metaFont);
                        document.add(imgPara);
                    }
                }
                else if (p.is("p")) {
                    Paragraph contentPara = new Paragraph(p.text(), contentFont);
                    contentPara.setFirstLineIndent(28); // 首行缩进
                    document.add(contentPara);
                }
            }

            document.close();
            return out.toByteArray();
        }
    }
}
