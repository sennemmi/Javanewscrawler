// 定义包名，表示该类属于项目的服务层
package com.hhu.javawebcrawler.demo.service;

// 导入项目内部的配置类，用于获取文档导出相关的配置信息
import com.hhu.javawebcrawler.demo.config.DocumentExportConfig;
// 导入项目内部的实体类，用于封装新闻数据
import com.hhu.javawebcrawler.demo.entity.NewsData;
// 导入iText库，用于PDF字体编码设置
import com.itextpdf.io.font.PdfEncodings;
// 导入iText库，用于创建图片数据对象
import com.itextpdf.io.image.ImageDataFactory;
// 导入iText库，用于定义颜色常量
import com.itextpdf.kernel.colors.ColorConstants;
// 导入iText库，用于处理PDF中的字体
import com.itextpdf.kernel.font.PdfFont;
// 导入iText库，用于创建PDF字体对象
import com.itextpdf.kernel.font.PdfFontFactory;
// 导入iText库，代表一个PDF文档对象
import com.itextpdf.kernel.pdf.PdfDocument;
// 导入iText库，用于将PDF文档写入输出流
import com.itextpdf.kernel.pdf.PdfWriter;
// 导入iText库，是操作PDF内容的高级API入口
import com.itextpdf.layout.Document;
// 导入iText库，用于表示PDF中的图片元素
import com.itextpdf.layout.element.Image;
// 导入iText库，用于表示PDF中的段落元素
import com.itextpdf.layout.element.Paragraph;
// 导入iText库，用于表示PDF中的文本片段
import com.itextpdf.layout.element.Text;
// 导入iText库，用于设置文本对齐方式
import com.itextpdf.layout.properties.TextAlignment;
// 导入Apache POI库，用于操作Word文档
import org.apache.poi.xwpf.usermodel.*;
// 导入Jsoup库，用于解析和操作HTML
import org.jsoup.Jsoup;
// 导入Jsoup库，用于表示HTML中的一个元素
import org.jsoup.nodes.Element;
// 导入Jsoup库，用于表示HTML元素的集合
import org.jsoup.select.Elements;
// 导入SLF4J库，用于日志记录
import org.slf4j.Logger;
// 导入SLF4J库，用于获取Logger实例
import org.slf4j.LoggerFactory;
// 导入Spring框架的资源加载类
import org.springframework.core.io.ClassPathResource;
// 导入Spring框架的Service注解，将该类标记为服务组件
import org.springframework.stereotype.Service;

// 导入Java IO类，用于处理字节数组输入流
import java.io.ByteArrayInputStream;
// 导入Java IO类，用于处理字节数组输出流
import java.io.ByteArrayOutputStream;
// 导入Java IO类，用于表示IO操作可能抛出的异常
import java.io.IOException;
// 导入Java IO类，用于处理输入流
import java.io.InputStream;
// 导入Java URI类，用于表示统一资源标识符
import java.net.URI;
// 导入Java时间格式化类
import java.time.format.DateTimeFormatter;
// 导入Java Map集合接口
import java.util.Map;


// 使用@Service注解，将此类声明为Spring容器管理的服务层Bean
@Service
// 定义文件导出服务类
public class FileExportService {

    // 创建一个静态的Logger实例，用于记录该类的日志
    private static final Logger logger = LoggerFactory.getLogger(FileExportService.class);

    // 定义一个静态的日期时间格式化器，格式为"年-月-日 时:分:秒"
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 定义默认标题字体大小常量
    private static final int DEFAULT_TITLE_FONT_SIZE = 22;
    // 定义默认一级标题字体大小常量
    private static final int DEFAULT_HEADING1_FONT_SIZE = 20;
    // 定义默认二级标题字体大小常量
    private static final int DEFAULT_HEADING2_FONT_SIZE = 18;
    // 定义默认三级标题字体大小常量
    private static final int DEFAULT_HEADING3_FONT_SIZE = 16;
    // 定义默认正文字体大小常量
    private static final int DEFAULT_TEXT_FONT_SIZE = 14;
    // 定义默认图片说明字体大小常量
    private static final int DEFAULT_CAPTION_FONT_SIZE = 12;
    // 定义默认页脚字体大小常量
    private static final int DEFAULT_FOOTER_FONT_SIZE = 10;

    // 定义默认行间距常量 (相对于字体大小的倍数)
    private static final float DEFAULT_LINE_SPACING = 1.5f;
    
    // 声明一个Map用于存储字体名称到字体文件路径的映射
    private final Map<String, String> fontMappings;
    // 声明一个Map用于存储文档样式的配置
    private final Map<String, Object> documentConfig;
    
    // 定义构造函数，用于依赖注入
    public FileExportService(
            // 使用@Qualifier注解，指定注入名为"fontPathMappings"的Bean
            @org.springframework.beans.factory.annotation.Qualifier("fontPathMappings") Map<String, String> fontMappings,
            // 使用@Qualifier注解，指定注入名为"documentStyleConfig"的Bean
            @org.springframework.beans.factory.annotation.Qualifier("documentStyleConfig") Map<String, Object> documentExportConfig) {
        // 将注入的字体映射赋值给类的成员变量
        this.fontMappings = fontMappings;
        // 将注入的文档配置赋值给类的成员变量
        this.documentConfig = documentExportConfig;
        // 记录初始化日志，显示加载的字体数量
        logger.info("文件导出服务已初始化，加载了{}种字体", fontMappings.size());
    }

    // 定义一个公共方法，用于使用默认样式创建Word文档
    public byte[] createWord(NewsData newsData) throws IOException {
        // 从配置中获取默认行间距，如果不存在则使用类中定义的常量
        float defaultLineSpacing = (Float)documentConfig.getOrDefault("defaultLineSpacing", DEFAULT_LINE_SPACING);
        // 从配置中获取默认标题字体大小，如果不存在则使用类中定义的常量
        int defaultTitleFontSize = (Integer)documentConfig.getOrDefault("defaultTitleFontSize", DEFAULT_TITLE_FONT_SIZE);
        // 从配置中获取默认一级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading1FontSize = (Integer)documentConfig.getOrDefault("defaultHeading1FontSize", DEFAULT_HEADING1_FONT_SIZE);
        // 从配置中获取默认二级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading2FontSize = (Integer)documentConfig.getOrDefault("defaultHeading2FontSize", DEFAULT_HEADING2_FONT_SIZE);
        // 从配置中获取默认三级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading3FontSize = (Integer)documentConfig.getOrDefault("defaultHeading3FontSize", DEFAULT_HEADING3_FONT_SIZE);
        // 从配置中获取默认正文字体大小，如果不存在则使用类中定义的常量
        int defaultTextFontSize = (Integer)documentConfig.getOrDefault("defaultTextFontSize", DEFAULT_TEXT_FONT_SIZE);
        // 从配置中获取默认图片说明字体大小，如果不存在则使用类中定义的常量
        int defaultCaptionFontSize = (Integer)documentConfig.getOrDefault("defaultCaptionFontSize", DEFAULT_CAPTION_FONT_SIZE);
        // 从配置中获取默认页脚字体大小，如果不存在则使用类中定义的常量
        int defaultFooterFontSize = (Integer)documentConfig.getOrDefault("defaultFooterFontSize", DEFAULT_FOOTER_FONT_SIZE);
        
        // 调用重载的createWord方法，传入所有获取到的默认参数，字体族设为null
        return createWord(newsData, defaultLineSpacing, defaultTitleFontSize, defaultHeading1FontSize,
                defaultHeading2FontSize, defaultHeading3FontSize, defaultTextFontSize,
                defaultCaptionFontSize, defaultFooterFontSize, null);
    }

    // 定义一个公共方法，用于根据指定的正文字体大小和行间距创建Word文档
    public byte[] createWord(NewsData newsData, int textFontSize, float lineSpacing) throws IOException {
        // 根据传入的正文字体大小，按比例计算标题字体大小
        int titleFontSize = textFontSize + 8;
        // 根据传入的正文字体大小，按比例计算一级标题字体大小
        int heading1FontSize = textFontSize + 6;
        // 根据传入的正文字体大小，按比例计算二级标题字体大小
        int heading2FontSize = textFontSize + 4;
        // 根据传入的正文字体大小，按比例计算三级标题字体大小
        int heading3FontSize = textFontSize + 2;
        // 根据传入的正文字体大小，按比例计算图片说明字体大小，并确保不小于10
        int captionFontSize = Math.max(textFontSize - 2, 10);
        // 根据传入的正文字体大小，按比例计算页脚字体大小，并确保不小于8
        int footerFontSize = Math.max(textFontSize - 4, 8);

        // 调用完整的createWord方法，传入所有计算出的参数，字体族设为null
        return createWord(newsData, lineSpacing, titleFontSize, heading1FontSize, heading2FontSize,
                heading3FontSize, textFontSize, captionFontSize, footerFontSize, null);
    }

    // 定义一个公共方法，用于创建可完全自定义样式的Word文档
    public byte[] createWord(NewsData newsData, float lineSpacing, int titleFontSize, int heading1FontSize,
                             int heading2FontSize, int heading3FontSize, int textFontSize,
                             int captionFontSize, int footerFontSize, String fontFamily) throws IOException {
        // 使用try-with-resources语句自动管理资源，创建一个新的Word文档对象和字节输出流
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 初始化标题字体名称为默认的"黑体"
            String titleFontName = "黑体";
            // 初始化正文字体名称为默认的"宋体"
            String textFontName = "宋体";
            
            // 检查是否提供了自定义字体族
            if (fontFamily != null && !fontFamily.isEmpty()) {
                // 清理前端传入的CSS字体值，移除引号和空格
                String cleanFontName = fontFamily.replaceAll("['\"\\s]", "")
                                               // 取逗号前的第一个字体名作为主字体
                                               .split(",")[0];
                
                // 如果清理后的字体名包含"书宋"，则将正文和标题字体都设为"宋体"
                if (cleanFontName.contains("书宋")) {
                    // 设置正文字体为宋体
                    textFontName = "宋体";
                    // 设置标题字体为宋体
                    titleFontName = "宋体";
                // 如果清理后的字体名包含"仿宋"，则将正文和标题字体都设为"仿宋"
                } else if (cleanFontName.contains("仿宋")) {
                    // 设置正文字体为仿宋
                    textFontName = "仿宋";
                    // 设置标题字体为仿宋
                    titleFontName = "仿宋";
                // 如果清理后的字体名包含"黑体"，则将正文和标题字体都设为"黑体"
                } else if (cleanFontName.contains("黑体")) {
                    // 设置正文字体为黑体
                    textFontName = "黑体";
                    // 设置标题字体为黑体
                    titleFontName = "黑体";
                // 如果清理后的字体名包含"楷体"，则将正文和标题字体都设为"楷体"
                } else if (cleanFontName.contains("楷体")) {
                    // 设置正文字体为楷体
                    textFontName = "楷体";
                    // 设置标题字体为楷体
                    titleFontName = "楷体";
                }
                
                // 记录调试日志，显示使用的自定义字体及其映射结果
                logger.debug("使用自定义字体: {}, 映射为标题字体: {}, 正文字体: {}", 
                          fontFamily, titleFontName, textFontName);
            }

            // 在文档中创建一个新段落用于标题
            XWPFParagraph titleParagraph = document.createParagraph();
            // 设置标题段落居中对齐
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            // 设置标题段落的段后间距
            titleParagraph.setSpacingAfter((int) (titleFontSize * lineSpacing * 20));
            // 在段落中创建一个文本运行(run)
            XWPFRun titleRun = titleParagraph.createRun();
            // 设置标题文本内容
            titleRun.setText(newsData.getTitle());
            // 设置标题为粗体
            titleRun.setBold(true);
            // 设置标题字体大小
            titleRun.setFontSize(titleFontSize);
            // 设置标题字体
            titleRun.setFontFamily(titleFontName);
            // 在标题后添加一个换行符
            titleRun.addBreak();

            // 在文档中创建一个新段落用于元数据（来源、时间）
            XWPFParagraph metaParagraph = document.createParagraph();
            // 设置元数据段落居中对齐
            metaParagraph.setAlignment(ParagraphAlignment.CENTER);
            // 设置元数据段落的段后间距
            metaParagraph.setSpacingAfter((int) (captionFontSize * lineSpacing * 30));
            // 在段落中创建一个文本运行
            XWPFRun metaRun = metaParagraph.createRun();
            // 格式化来源和发布时间信息
            String metaInfo = String.format("来源: %s   发布时间: %s",
                    newsData.getSource(),
                    newsData.getPublishTime().format(DATE_FORMATTER));
            // 设置元数据文本内容
            metaRun.setText(metaInfo);
            // 设置元数据字体大小
            metaRun.setFontSize(captionFontSize);
            // 设置元数据为斜体
            metaRun.setItalic(true);
            // 设置元数据文本颜色为灰色
            metaRun.setColor("808080");
            // 在元数据后添加一个换行符
            metaRun.addBreak();
            // 再次添加一个换行符以增加间距
            metaRun.addBreak();

            // 使用Jsoup解析新闻内容HTML字符串
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(newsData.getContent());

            // 调用辅助方法处理HTML内容，将其转换为Word格式
            processHtmlContentForWord(document, htmlDoc, lineSpacing, heading1FontSize, heading2FontSize,
                    heading3FontSize, textFontSize, captionFontSize, textFontName, titleFontName);

            // 创建一个新段落用于页脚
            XWPFParagraph footerParagraph = document.createParagraph();
            // 设置页脚段落居中对齐
            footerParagraph.setAlignment(ParagraphAlignment.CENTER);
            // 设置页脚段落的段前间距
            footerParagraph.setSpacingBefore((int) (footerFontSize * lineSpacing * 20));
            // 在段落中创建一个文本运行
            XWPFRun footerRun = footerParagraph.createRun();
            // 设置页脚文本内容
            footerRun.setText("——— 文档结束 ———");
            // 设置页脚字体大小
            footerRun.setFontSize(footerFontSize);
            // 设置页脚文本颜色为浅灰色
            footerRun.setColor("A9A9A9");

            // 将Word文档内容写入字节输出流
            document.write(out);
            // 返回包含Word文档数据的字节数组
            return out.toByteArray();
        }
    }

    // 定义一个私有辅助方法，用于处理HTML内容并添加到Word文档中
    private void processHtmlContentForWord(XWPFDocument document, org.jsoup.nodes.Document htmlDoc,
                                           float lineSpacing, int heading1FontSize, int heading2FontSize,
                                           int heading3FontSize, int textFontSize, int captionFontSize,
                                           String textFontName, String titleFontName) {
        // 使用Jsoup选择器选取所有段落、图片容器、标题和列表项等元素
        Elements elements = htmlDoc.select("p, div.img_wrapper, h1, h2, h3, h4, h5, h6, ul, ol, li");

        // 遍历选出的所有HTML元素
        for (Element element : elements) {
            // 如果元素是图片容器(div.img_wrapper)
            if (element.is("div.img_wrapper")) {
                // 调用处理图片的方法
                processImageForWord(document, element, captionFontSize, lineSpacing, textFontName);
            // 如果元素是段落(<p>)
            } else if (element.is("p")) {
                // 调用处理段落的方法
                processParagraphForWord(document, element, textFontSize, lineSpacing, textFontName);
            // 如果元素是标题(<h1>到<h6>)
            } else if (element.is("h1, h2, h3, h4, h5, h6")) {
                // 调用处理标题的方法
                processHeadingForWord(document, element, heading1FontSize, heading2FontSize, 
                                     heading3FontSize, lineSpacing, titleFontName);
            // 如果元素是列表(<ul>或<ol>)
            } else if (element.is("ul, ol")) {
                // 调用处理列表的方法
                processListForWord(document, element, textFontSize, lineSpacing, textFontName);
            }
        }
    }

    // 定义一个私有辅助方法，用于将HTML段落转换为Word段落
    private void processParagraphForWord(XWPFDocument document, Element element, int textFontSize, 
                                        float lineSpacing, String fontName) {
        // 获取段落元素的纯文本内容并去除首尾空格
        String text = element.text().trim();
        // 如果文本内容不为空
        if (!text.isEmpty()) {
            // 在Word文档中创建一个新段落
            XWPFParagraph paragraph = document.createParagraph();
            // 设置段落首行缩进（600磅，约等于2个字符）
            paragraph.setIndentationFirstLine(600);
            // 设置段落的行间距
            paragraph.setSpacingBetween(lineSpacing);
            // 设置段落的段后间距
            paragraph.setSpacingAfter((int) (textFontSize * 20));
            // 在段落中创建一个文本运行
            XWPFRun run = paragraph.createRun();
            // 设置文本内容
            run.setText(text);
            // 设置字体大小
            run.setFontSize(textFontSize);
            // 设置字体
            run.setFontFamily(fontName);
            // 添加一个换行符
            run.addBreak();
        }
    }

    // 定义一个私有辅助方法，用于将HTML标题转换为Word标题
    private void processHeadingForWord(XWPFDocument document, Element element,
                                       int heading1FontSize, int heading2FontSize,
                                       int heading3FontSize, float lineSpacing,
                                       String fontName) {
        // 获取标题元素的纯文本内容并去除首尾空格
        String text = element.text().trim();
        // 如果文本内容不为空
        if (!text.isEmpty()) {
            // 在Word文档中创建一个新段落
            XWPFParagraph paragraph = document.createParagraph();
            // 设置段落左对齐
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            // 设置段落的行间距
            paragraph.setSpacingBetween(lineSpacing);
            // 设置默认的段后间距
            paragraph.setSpacingAfter((int) (heading3FontSize * 20));
            // 在段落中创建一个文本运行
            XWPFRun run = paragraph.createRun();
            // 设置文本内容
            run.setText(text);
            // 设置文本为粗体
            run.setBold(true);

            // 如果是<h1>标签
            if (element.is("h1")) {
                // 设置为一级标题的字体大小
                run.setFontSize(heading1FontSize);
                // 设置一级标题的段后间距
                paragraph.setSpacingAfter((int) (heading1FontSize * 30));
            // 如果是<h2>标签
            } else if (element.is("h2")) {
                // 设置为二级标题的字体大小
                run.setFontSize(heading2FontSize);
                // 设置二级标题的段后间距
                paragraph.setSpacingAfter((int) (heading2FontSize * 25));
            // 如果是<h3>标签
            } else if (element.is("h3")) {
                // 设置为三级标题的字体大小
                run.setFontSize(heading3FontSize);
            // 对于<h4>, <h5>, <h6>标签
            } else {
                // 设置比三级标题稍小的字体大小
                run.setFontSize(heading3FontSize - 1);
            }

            // 设置标题的字体
            run.setFontFamily(fontName);
            // 添加一个换行符
            run.addBreak();
        }
    }

    // 定义一个私有辅助方法，用于将HTML列表转换为Word列表
    private void processListForWord(XWPFDocument document, Element listElement, int textFontSize, 
                                   float lineSpacing, String fontName) {
        // 选取列表中的所有<li>项
        Elements items = listElement.select("li");
        // 判断是<ol>(有序列表)还是<ul>(无序列表)
        boolean isOrdered = listElement.is("ol");
        // 初始化有序列表的计数器
        int counter = 1;

        // 遍历每一个列表项
        for (Element item : items) {
            // 为每个列表项创建一个新段落
            XWPFParagraph paragraph = document.createParagraph();
            // 设置段落左侧缩进
            paragraph.setIndentationLeft(900);
            // 设置段落行间距
            paragraph.setSpacingBetween(lineSpacing);
            // 设置段落的段后间距
            paragraph.setSpacingAfter((int) (textFontSize * 15));
            // 在段落中创建一个文本运行
            XWPFRun run = paragraph.createRun();

            // 如果是有序列表
            if (isOrdered) {
                // 添加"数字. "前缀
                run.setText(counter++ + ". " + item.text().trim());
            // 如果是无序列表
            } else {
                // 添加"• "前缀
                run.setText("• " + item.text().trim());
            }

            // 设置列表项的字体大小
            run.setFontSize(textFontSize);
            // 设置列表项的字体
            run.setFontFamily(fontName);
            // 添加一个换行符
            run.addBreak();
        }
    }

    // 定义一个私有辅助方法，用于将HTML图片转换为Word图片
    private void processImageForWord(XWPFDocument document, Element imgWrapper, int captionFontSize, 
                                    float lineSpacing, String fontName) {
        // 从图片容器中选取第一个<img>元素
        Element img = imgWrapper.selectFirst("img");
        // 如果找到了<img>元素
        if (img != null) {
            // 获取图片的src属性（URL）
            String imgSrc = img.attr("src");
            // 获取图片的alt属性（替代文本/说明）
            String imgAlt = img.attr("alt");

            // 如果图片URL不是以"http"开头
            if (!imgSrc.startsWith("http")) {
                // 假设是协议相对URL，为其添加"https:"前缀
                imgSrc = "https:" + imgSrc;
            }

            // 使用try-catch块处理可能发生的异常
            try {
                // 创建一个居中对齐的段落用于放置图片
                XWPFParagraph imgParagraph = document.createParagraph();
                // 设置段落居中对齐
                imgParagraph.setAlignment(ParagraphAlignment.CENTER);
                // 设置图片后的间距
                imgParagraph.setSpacingAfter(200);
                // 在段落中创建一个文本运行
                XWPFRun imgRun = imgParagraph.createRun();

                // 使用try-with-resources语句自动关闭图片输入流
                try (InputStream imageStream = URI.create(imgSrc).toURL().openStream()) {
                    // 读取图片URL的全部字节
                    byte[] imageBytes = imageStream.readAllBytes();
                    // 声明图片类型变量
                    int pictureType;
                    // 如果图片URL以.png结尾
                    if (imgSrc.toLowerCase().endsWith(".png")) {
                        // 设置图片类型为PNG
                        pictureType = XWPFDocument.PICTURE_TYPE_PNG;
                    // 如果图片URL以.gif结尾
                    } else if (imgSrc.toLowerCase().endsWith(".gif")) {
                        // 设置图片类型为GIF
                        pictureType = XWPFDocument.PICTURE_TYPE_GIF;
                    // 否则默认为JPEG
                    } else {
                        // 设置图片类型为JPEG
                        pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
                    }

                    // 将图片添加到Word文档中，指定尺寸
                    imgRun.addPicture(new ByteArrayInputStream(imageBytes), pictureType, imgSrc, 450, 300);
                    // 在图片后添加一个换行符
                    imgRun.addBreak();
                // 捕获下载或处理图片时可能发生的异常
                } catch (Exception e) {
                    // 如果图片加载失败，则插入提示文本
                    imgRun.setText("[图片: " + imgSrc + "]");
                    // 添加换行符
                    imgRun.addBreak();
                    // 打印异常堆栈信息
                    e.printStackTrace();
                }

                // 如果图片有alt文本（说明）
                if (imgAlt != null && !imgAlt.isEmpty()) {
                    // 创建一个新段落用于图片说明
                    XWPFParagraph captionParagraph = document.createParagraph();
                    // 设置说明段落居中对齐
                    captionParagraph.setAlignment(ParagraphAlignment.CENTER);
                    // 设置说明段落的段后间距
                    captionParagraph.setSpacingAfter((int) (captionFontSize * lineSpacing * 20));
                    // 在段落中创建一个文本运行
                    XWPFRun captionRun = captionParagraph.createRun();
                    // 设置说明的文本内容
                    captionRun.setText(imgAlt);
                    // 设置说明为斜体
                    captionRun.setItalic(true);
                    // 设置说明的字体大小
                    captionRun.setFontSize(captionFontSize);
                    // 设置说明的字体
                    captionRun.setFontFamily(fontName);
                    // 设置说明的文本颜色
                    captionRun.setColor("505050");
                    // 添加一个换行符
                    captionRun.addBreak();
                }
            // 捕获处理图片时可能发生的其他异常
            } catch (Exception e) {
                // 创建一个错误提示段落
                XWPFParagraph errorParagraph = document.createParagraph();
                // 在段落中创建一个文本运行
                XWPFRun errorRun = errorParagraph.createRun();
                // 设置错误提示文本
                errorRun.setText("[无法加载图片: " + imgSrc + "]");
                // 添加一个换行符
                errorRun.addBreak();
            }
        }
    }

    // 定义一个公共方法，用于使用默认样式创建PDF文档
    public byte[] createPdf(NewsData newsData) throws IOException {
        // 从配置中获取默认行间距，如果不存在则使用类中定义的常量
        float defaultLineSpacing = (Float)documentConfig.getOrDefault("defaultLineSpacing", DEFAULT_LINE_SPACING);
        // 从配置中获取默认标题字体大小，如果不存在则使用类中定义的常量
        int defaultTitleFontSize = (Integer)documentConfig.getOrDefault("defaultTitleFontSize", DEFAULT_TITLE_FONT_SIZE);
        // 从配置中获取默认一级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading1FontSize = (Integer)documentConfig.getOrDefault("defaultHeading1FontSize", DEFAULT_HEADING1_FONT_SIZE);
        // 从配置中获取默认二级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading2FontSize = (Integer)documentConfig.getOrDefault("defaultHeading2FontSize", DEFAULT_HEADING2_FONT_SIZE);
        // 从配置中获取默认三级标题字体大小，如果不存在则使用类中定义的常量
        int defaultHeading3FontSize = (Integer)documentConfig.getOrDefault("defaultHeading3FontSize", DEFAULT_HEADING3_FONT_SIZE);
        // 从配置中获取默认正文字体大小，如果不存在则使用类中定义的常量
        int defaultTextFontSize = (Integer)documentConfig.getOrDefault("defaultTextFontSize", DEFAULT_TEXT_FONT_SIZE);
        // 从配置中获取默认图片说明字体大小，如果不存在则使用类中定义的常量
        int defaultCaptionFontSize = (Integer)documentConfig.getOrDefault("defaultCaptionFontSize", DEFAULT_CAPTION_FONT_SIZE);
        // 从配置中获取默认页脚字体大小，如果不存在则使用类中定义的常量
        int defaultFooterFontSize = (Integer)documentConfig.getOrDefault("defaultFooterFontSize", DEFAULT_FOOTER_FONT_SIZE);
        
        // 调用重载的createPdf方法，传入所有获取到的默认参数，字体族设为null
        return createPdf(newsData, defaultLineSpacing, defaultTitleFontSize, defaultHeading1FontSize,
                defaultHeading2FontSize, defaultHeading3FontSize, defaultTextFontSize,
                defaultCaptionFontSize, defaultFooterFontSize, null);
    }

    // 定义一个公共方法，用于根据指定的正文字体大小和行间距创建PDF文档
    public byte[] createPdf(NewsData newsData, int textFontSize, float lineSpacing) throws IOException {
        // 根据传入的正文字体大小，按比例计算标题字体大小
        int titleFontSize = textFontSize + 8;
        // 根据传入的正文字体大小，按比例计算一级标题字体大小
        int heading1FontSize = textFontSize + 6;
        // 根据传入的正文字体大小，按比例计算二级标题字体大小
        int heading2FontSize = textFontSize + 4;
        // 根据传入的正文字体大小，按比例计算三级标题字体大小
        int heading3FontSize = textFontSize + 2;
        // 根据传入的正文字体大小，按比例计算图片说明字体大小，并确保不小于10
        int captionFontSize = Math.max(textFontSize - 2, 10);
        // 根据传入的正文字体大小，按比例计算页脚字体大小，并确保不小于8
        int footerFontSize = Math.max(textFontSize - 4, 8);

        // 从配置中获取PDF上边距，如果不存在则使用默认值36f
        Float pdfMarginTop = (Float)documentConfig.getOrDefault("pdfMarginTop", 36f);
        // 从配置中获取PDF下边距，如果不存在则使用默认值36f
        Float pdfMarginBottom = (Float)documentConfig.getOrDefault("pdfMarginBottom", 36f);
        // 从配置中获取PDF左边距，如果不存在则使用默认值36f
        Float pdfMarginLeft = (Float)documentConfig.getOrDefault("pdfMarginLeft", 36f);
        // 从配置中获取PDF右边距，如果不存在则使用默认值36f
        Float pdfMarginRight = (Float)documentConfig.getOrDefault("pdfMarginRight", 36f);
        
        // 记录调试日志，显示使用的PDF边距配置
        logger.debug("使用PDF边距配置: 上={}, 下={}, 左={}, 右={}", 
                     pdfMarginTop, pdfMarginBottom, pdfMarginLeft, pdfMarginRight);

        // 调用完整的createPdf方法，传入所有计算出的参数，字体族设为null
        return createPdf(newsData, lineSpacing, titleFontSize, heading1FontSize, heading2FontSize,
                heading3FontSize, textFontSize, captionFontSize, footerFontSize, null);
    }

    // 定义一个公共方法，用于创建可完全自定义样式的PDF文档
    public byte[] createPdf(NewsData newsData, float lineSpacing, int titleFontSize, int heading1FontSize,
                             int heading2FontSize, int heading3FontSize, int textFontSize,
                             int captionFontSize, int footerFontSize, String fontFamily) throws IOException {
        
        // 使用try-with-resources语句自动关闭字节输出流
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 声明标准字体变量
            PdfFont font;
            // 声明粗体字体变量
            PdfFont boldFont;
            
            // 检查是否提供了自定义字体族
            if (fontFamily != null && !fontFamily.isEmpty()) {
                // 清理前端传入的CSS字体值，移除引号和空格并取第一个
                String cleanFontName = fontFamily.replaceAll("['\"\\s]", "").split(",")[0];
                
                // 声明字体文件路径变量
                String fontPath = null;
                // 如果清理后的字体名包含"书宋"
                if (cleanFontName.contains("书宋")) {
                    // 从字体映射中获取"宋体"的路径
                    fontPath = fontMappings.get("宋体");
                // 如果清理后的字体名包含"仿宋"
                } else if (cleanFontName.contains("仿宋")) {
                    // 从字体映射中获取"仿宋"的路径
                    fontPath = fontMappings.get("仿宋");
                // 如果清理后的字体名包含"黑体"
                } else if (cleanFontName.contains("黑体")) {
                    // 从字体映射中获取"黑体"的路径
                    fontPath = fontMappings.get("黑体");
                // 如果清理后的字体名包含"楷体"
                } else if (cleanFontName.contains("楷体")) {
                    // 从字体映射中获取"楷体"的路径
                    fontPath = fontMappings.get("楷体");
                }
                
                // 记录调试日志，显示尝试使用的自定义字体及其映射路径
                logger.debug("尝试使用自定义字体: {}, 映射路径: {}", cleanFontName, fontPath);
                
                // 使用try-catch块处理字体加载可能发生的异常
                try {
                    // 如果找到了字体路径
                    if (fontPath != null) {
                        // 创建一个类路径资源对象
                        ClassPathResource resource = new ClassPathResource(fontPath);
                        // 如果资源存在
                        if (resource.exists()) {
                            // 从资源URL创建支持中文的PDF字体
                            font = PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                            // 将粗体字体也设置为该字体（iText可以程序化加粗）
                            boldFont = font;
                            // 记录成功加载自定义字体的日志
                            logger.debug("成功加载自定义字体: {}", fontPath);
                        // 如果资源文件不存在
                        } else {
                            // 记录警告日志，并回退到默认字体
                            logger.warn("自定义字体文件不存在: {}, 使用默认字体", fontPath);
                            // 加载默认中文字体
                            font = loadChineseFont();
                            // 加载默认中文粗体字体
                            boldFont = loadChineseBoldFont();
                        }
                    // 如果没有找到匹配的字体映射
                    } else {
                        // 记录警告日志，并回退到默认字体
                        logger.warn("未找到与 {} 匹配的字体映射, 使用默认字体", cleanFontName);
                        // 加载默认中文字体
                        font = loadChineseFont();
                        // 加载默认中文粗体字体
                        boldFont = loadChineseBoldFont();
                    }
                // 捕获字体加载过程中的任何异常
                } catch (Exception e) {
                    // 记录警告日志，并回退到默认字体
                    logger.warn("加载自定义字体失败: {}, 使用默认字体", e.getMessage());
                    // 加载默认中文字体
                    font = loadChineseFont();
                    // 加载默认中文粗体字体
                    boldFont = loadChineseBoldFont();
                }
            // 如果没有提供自定义字体
            } else {
                // 加载默认中文字体
                font = loadChineseFont();
                // 加载默认中文粗体字体
                boldFont = loadChineseBoldFont();
            }
            
            // 从配置中获取PDF上边距，如果不存在则使用默认值36f
            Float pdfMarginTop = (Float)documentConfig.getOrDefault("pdfMarginTop", 36f);
            // 从配置中获取PDF下边距，如果不存在则使用默认值36f
            Float pdfMarginBottom = (Float)documentConfig.getOrDefault("pdfMarginBottom", 36f);
            // 从配置中获取PDF左边距，如果不存在则使用默认值36f
            Float pdfMarginLeft = (Float)documentConfig.getOrDefault("pdfMarginLeft", 36f);
            // 从配置中获取PDF右边距，如果不存在则使用默认值36f
            Float pdfMarginRight = (Float)documentConfig.getOrDefault("pdfMarginRight", 36f);
            
            // 创建一个PdfWriter，将其与字节输出流关联
            PdfWriter writer = new PdfWriter(baos);
            // 创建一个PdfDocument对象
            PdfDocument pdf = new PdfDocument(writer);
            // 创建一个Document对象，用于操作PDF内容
            Document document = new Document(pdf);
            
            // 设置PDF文档的页边距
            document.setMargins(pdfMarginTop, pdfMarginRight, pdfMarginBottom, pdfMarginLeft);

            // 创建一个标题段落
            Paragraph title = new Paragraph(newsData.getTitle())
                    // 设置标题字体
                    .setFont(boldFont)
                    // 设置标题字体大小
                    .setFontSize(titleFontSize)
                    // 设置标题为粗体
                    .setBold()
                    // 设置标题居中对齐
                    .setTextAlignment(TextAlignment.CENTER)
                    // 设置标题的下边距
                    .setMarginBottom(titleFontSize * 0.8f);
            // 将标题段落添加到文档中
            document.add(title);

            // 格式化来源和发布时间信息
            String metaInfo = String.format("来源: %s   发布时间: %s",
                    newsData.getSource(),
                    newsData.getPublishTime().format(DATE_FORMATTER));
            // 创建一个元数据段落
            Paragraph meta = new Paragraph(metaInfo)
                    // 设置元数据字体
                    .setFont(font)
                    // 设置元数据字体大小
                    .setFontSize(captionFontSize)
                    // 设置元数据为斜体
                    .setItalic()
                    // 设置元数据文本颜色为深灰色
                    .setFontColor(ColorConstants.DARK_GRAY)
                    // 设置元数据居中对齐
                    .setTextAlignment(TextAlignment.CENTER)
                    // 设置元数据的下边距
                    .setMarginBottom(captionFontSize * 2.0f);
            // 将元数据段落添加到文档中
            document.add(meta);

            // 使用Jsoup解析新闻内容HTML字符串
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(newsData.getContent());

            // 调用辅助方法处理HTML内容，将其转换为PDF格式
            processHtmlContentForPdf(document, htmlDoc, font, boldFont, lineSpacing, heading1FontSize,
                    heading2FontSize, heading3FontSize, textFontSize, captionFontSize);

            // 添加一个空行作为分隔
            document.add(new Paragraph("\n"));
            // 创建一个页脚段落
            Paragraph footer = new Paragraph("——— 文档结束 ———")
                    // 设置页脚字体
                    .setFont(font)
                    // 设置页脚字体大小
                    .setFontSize(footerFontSize)
                    // 设置页脚文本颜色为深灰色
                    .setFontColor(ColorConstants.DARK_GRAY)
                    // 设置页脚居中对齐
                    .setTextAlignment(TextAlignment.CENTER);
            // 将页脚段落添加到文档中
            document.add(footer);

            // 关闭文档，完成PDF的生成
            document.close();
            // 返回包含PDF数据的字节数组
            return baos.toByteArray();
        }
    }

    // 定义一个私有辅助方法，用于处理HTML内容并添加到PDF文档中
    private void processHtmlContentForPdf(Document document, org.jsoup.nodes.Document htmlDoc,
                                          PdfFont font, PdfFont boldFont, float lineSpacing,
                                          int heading1FontSize, int heading2FontSize, int heading3FontSize,
                                          int textFontSize, int captionFontSize) {
        // 使用Jsoup选择器选取所有段落、图片容器、标题和列表项等元素
        Elements elements = htmlDoc.select("p, div.img_wrapper, h1, h2, h3, h4, h5, h6, ul, ol, li");

        // 遍历选出的所有HTML元素
        for (Element element : elements) {
            // 如果元素是图片容器(div.img_wrapper)
            if (element.is("div.img_wrapper")) {
                // 调用处理图片的方法
                processImageForPdf(document, element, font, captionFontSize, lineSpacing);
            // 如果元素是段落(<p>)
            } else if (element.is("p")) {
                // 调用处理段落的方法
                processParagraphForPdf(document, element, font, textFontSize, lineSpacing);
            // 如果元素是标题(<h1>到<h6>)
            } else if (element.is("h1, h2, h3, h4, h5, h6")) {
                // 调用处理标题的方法
                processHeadingForPdf(document, element, boldFont, heading1FontSize,
                        heading2FontSize, heading3FontSize, lineSpacing);
            // 如果元素是列表(<ul>或<ol>)
            } else if (element.is("ul, ol")) {
                // 调用处理列表的方法
                processListForPdf(document, element, font, textFontSize, lineSpacing);
            }
        }
    }

    // 定义一个私有辅助方法，用于将HTML段落转换为PDF段落
    private void processParagraphForPdf(Document document, Element element, PdfFont font,
                                        int textFontSize, float lineSpacing) {
        // 获取段落元素的纯文本内容并去除首尾空格
        String text = element.text().trim();
        // 如果文本内容不为空
        if (!text.isEmpty()) {
            // 创建一个新的PDF段落
            Paragraph paragraph = new Paragraph(text)
                    // 设置段落字体
                    .setFont(font)
                    // 设置段落字体大小
                    .setFontSize(textFontSize)
                    // 设置首行缩进
                    .setFirstLineIndent(28)
                    // 设置段落下边距
                    .setMarginBottom(textFontSize * 0.8f)
                    // 设置固定的行间距
                    .setFixedLeading(textFontSize * lineSpacing);
            // 将段落添加到文档中
            document.add(paragraph);
        }
    }

    // 定义一个私有辅助方法，用于将HTML标题转换为PDF标题
    private void processHeadingForPdf(Document document, Element element, PdfFont boldFont,
                                      int heading1FontSize, int heading2FontSize,
                                      int heading3FontSize, float lineSpacing) {
        // 获取标题元素的纯文本内容并去除首尾空格
        String text = element.text().trim();
        // 如果文本内容不为空
        if (!text.isEmpty()) {
            // 初始化字体大小为三级标题大小
            float fontSize = heading3FontSize;
            // 初始化下边距
            float marginBottom = fontSize * 1.0f;

            // 如果是<h1>标签
            if (element.is("h1")) {
                // 设置为一级标题的字体大小
                fontSize = heading1FontSize;
                // 设置一级标题的下边距
                marginBottom = fontSize * 1.2f;
            // 如果是<h2>标签
            } else if (element.is("h2")) {
                // 设置为二级标题的字体大小
                fontSize = heading2FontSize;
                // 设置二级标题的下边距
                marginBottom = fontSize * 1.0f;
            // 如果是<h3>标签
            } else if (element.is("h3")) {
                // 设置为三级标题的字体大小
                fontSize = heading3FontSize;
                // 设置三级标题的下边距
                marginBottom = fontSize * 0.8f;
            }

            // 创建一个新的PDF标题段落
            Paragraph heading = new Paragraph(text)
                    // 设置标题字体
                    .setFont(boldFont)
                    // 设置标题字体大小
                    .setFontSize(fontSize)
                    // 设置标题为粗体
                    .setBold()
                    // 设置标题下边距
                    .setMarginBottom(marginBottom)
                    // 设置固定的行间距
                    .setFixedLeading(fontSize * lineSpacing);
            // 将标题段落添加到文档中
            document.add(heading);
        }
    }

    // 定义一个私有辅助方法，用于将HTML列表转换为PDF列表
    private void processListForPdf(Document document, Element listElement, PdfFont font,
                                   int textFontSize, float lineSpacing) {
        // 选取列表中的所有<li>项
        Elements items = listElement.select("li");
        // 判断是<ol>(有序列表)还是<ul>(无序列表)
        boolean isOrdered = listElement.is("ol");
        // 初始化有序列表的计数器
        int counter = 1;

        // 遍历每一个列表项
        for (Element item : items) {
            // 根据列表类型生成前缀
            String prefix = isOrdered ? counter++ + ". " : "• ";
            // 为每个列表项创建一个新的PDF段落
            Paragraph listItem = new Paragraph()
                    // 设置列表项字体
                    .setFont(font)
                    // 设置列表项字体大小
                    .setFontSize(textFontSize)
                    // 设置列表项左边距（用于缩进）
                    .setMarginLeft(28)
                    // 设置列表项下边距
                    .setMarginBottom(textFontSize * 0.5f)
                    // 设置固定的行间距
                    .setFixedLeading(textFontSize * lineSpacing);

            // 将带前缀的列表项文本添加到段落中
            listItem.add(new Text(prefix + item.text().trim()));
            // 将列表项段落添加到文档中
            document.add(listItem);
        }
    }

    // 定义一个私有辅助方法，用于将HTML图片转换为PDF图片
    private void processImageForPdf(Document document, Element imgWrapper, PdfFont font,
                                    int captionFontSize, float lineSpacing) {
        // 从图片容器中选取第一个<img>元素
        Element img = imgWrapper.selectFirst("img");
        // 如果找到了<img>元素
        if (img != null) {
            // 获取图片的src属性（URL）
            String imgSrc = img.attr("src");
            // 获取图片的alt属性（替代文本/说明）
            String imgAlt = img.attr("alt");

            // 如果图片URL不是以"http"开头
            if (!imgSrc.startsWith("http")) {
                // 假设是协议相对URL，为其添加"https:"前缀
                imgSrc = "https:" + imgSrc;
            }

            // 使用try-catch块处理可能发生的异常
            try {
                // 从URL创建图片数据，并构建一个Image对象
                Image pdfImg = new Image(ImageDataFactory.create(URI.create(imgSrc).toURL()));
                // 设置图片宽度
                pdfImg.setWidth(400);
                // 设置图片水平居中对齐
                pdfImg.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                // 设置图片下边距
                pdfImg.setMarginBottom(captionFontSize * 0.5f);
                // 将图片添加到文档中
                document.add(pdfImg);

                // 如果图片有alt文本（说明）
                if (imgAlt != null && !imgAlt.isEmpty()) {
                    // 创建一个新的PDF段落用于图片说明
                    Paragraph caption = new Paragraph(imgAlt)
                            // 设置说明字体
                            .setFont(font)
                            // 设置说明字体大小
                            .setFontSize(captionFontSize)
                            // 设置说明为斜体
                            .setItalic()
                            // 设置说明文本颜色为深灰色
                            .setFontColor(ColorConstants.DARK_GRAY)
                            // 设置说明居中对齐
                            .setTextAlignment(TextAlignment.CENTER)
                            // 设置说明下边距
                            .setMarginBottom(captionFontSize * 1.5f)
                            // 设置固定的行间距
                            .setFixedLeading(captionFontSize * lineSpacing);
                    // 将说明段落添加到文档中
                    document.add(caption);
                }

            // 捕获下载或处理图片时可能发生的异常
            } catch (Exception e) {
                // 如果图片加载失败，则创建一个错误提示段落
                Paragraph errorText = new Paragraph("[无法加载图片: " + imgSrc + "]")
                        // 设置错误提示居中对齐
                        .setTextAlignment(TextAlignment.CENTER)
                        // 设置错误提示下边距
                        .setMarginBottom(captionFontSize * 1.0f);
                // 将错误提示段落添加到文档中
                document.add(errorText);
            }
        }
    }

    // 定义一个私有方法，用于加载中文字体
    private PdfFont loadChineseFont() throws IOException {
        // 首先尝试使用配置中指定的默认字体（通常是宋体）
        String fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_FONT_FAMILY);
        // 如果配置中存在该字体
        if (fontPath != null) {
            // 使用try-catch块处理加载异常
            try {
                // 创建一个类路径资源对象
                ClassPathResource resource = new ClassPathResource(fontPath);
                // 如果资源文件存在
                if (resource.exists()) {
                    // 记录调试日志
                    logger.debug("使用配置的字体: {}", fontPath);
                    // 从资源URL创建支持中文的PDF字体并返回
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            // 捕获异常
            } catch (Exception e) {
                // 记录加载失败的警告日志
                logger.warn("无法加载配置的字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 如果配置字体加载失败，其次尝试使用iText内置的亚洲字体STSong-Light
        try {
            // 记录调试日志
            logger.debug("尝试使用系统字体: STSong-Light");
            // 创建并返回系统字体
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        // 捕获异常
        } catch (Exception e) {
            // 记录加载失败的警告日志
            logger.warn("无法加载系统字体: {}", e.getMessage());
            
            // 如果系统字体也加载失败，最后尝试使用iText的默认字体
            try {
                // 记录调试日志
                logger.debug("尝试使用默认字体");
                // 创建并返回默认字体
                return PdfFontFactory.createFont();
            // 捕获异常
            } catch (Exception ex) {
                // 记录加载所有字体都失败的错误日志
                logger.error("无法加载任何字体: {}", ex.getMessage());
                // 抛出IOException，表示字体加载失败
                throw new IOException("无法加载任何字体", ex);
            }
        }
    }

    // 定义一个私有方法，用于加载中文粗体字体
    private PdfFont loadChineseBoldFont() throws IOException {
        // 首先尝试使用配置中指定的默认标题字体（通常是黑体）
        String fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_TITLE_FONT_FAMILY);
        // 如果配置中存在该字体
        if (fontPath != null) {
            // 使用try-catch块处理加载异常
            try {
                // 创建一个类路径资源对象
                ClassPathResource resource = new ClassPathResource(fontPath);
                // 如果资源文件存在
                if (resource.exists()) {
                    // 记录调试日志
                    logger.debug("使用配置的黑体字体: {}", fontPath);
                    // 从资源URL创建支持中文的PDF字体并返回
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            // 捕获异常
            } catch (Exception e) {
                // 记录加载失败的警告日志
                logger.warn("无法加载配置的黑体字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 如果黑体加载失败，其次尝试使用配置的宋体作为替代
        fontPath = fontMappings.get(DocumentExportConfig.DEFAULT_FONT_FAMILY);
        // 如果配置中存在宋体
        if (fontPath != null) {
            // 使用try-catch块处理加载异常
            try {
                // 创建一个类路径资源对象
                ClassPathResource resource = new ClassPathResource(fontPath);
                // 如果资源文件存在
                if (resource.exists()) {
                    // 记录调试日志
                    logger.debug("使用配置的宋体字体作为粗体替代: {}", fontPath);
                    // 从资源URL创建支持中文的PDF字体并返回
                    return PdfFontFactory.createFont(resource.getURL().toString(), PdfEncodings.IDENTITY_H);
                }
            // 捕获异常
            } catch (Exception e) {
                // 记录加载失败的警告日志
                logger.warn("无法加载配置的宋体字体 {}: {}", fontPath, e.getMessage());
            }
        }
        
        // 如果配置字体都失败，再次尝试使用系统字体STSong-Light
        try {
            // 记录调试日志
            logger.debug("尝试使用系统字体作为粗体: STSong-Light");
            // 创建并返回系统字体
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
        // 捕获异常
        } catch (Exception e) {
            // 记录加载失败的警告日志
            logger.warn("无法加载系统字体作为粗体: {}", e.getMessage());
            
            // 如果系统字体也失败，最后尝试使用默认字体
            try {
                // 记录调试日志
                logger.debug("尝试使用默认字体作为粗体");
                // 创建并返回默认字体
                return PdfFontFactory.createFont();
            // 捕获异常
            } catch (Exception ex) {
                // 记录加载所有字体都失败的错误日志
                logger.error("无法加载任何字体作为粗体: {}", ex.getMessage());
                // 抛出IOException，表示字体加载失败
                throw new IOException("无法加载任何字体作为粗体", ex);
            }
        }
    }
}