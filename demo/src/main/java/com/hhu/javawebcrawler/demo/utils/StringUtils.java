// 定义了该Java文件所在的包名
package com.hhu.javawebcrawler.demo.utils;

// 导入 Java 标准库中的类，用于字符集处理
import java.nio.charset.StandardCharsets;
// 导入 Java 标准库中的类，用于 URL 编码
import java.net.URLEncoder;
// 导入 Java 标准库中的类，用于正则表达式模式匹配
import java.util.regex.Pattern;

// 定义一个公共的 StringUtils 工具类
public class StringUtils {

    // 定义一个私有的、静态的、最终的 Pattern 对象，用于匹配文件名中的非法字符
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    // 定义一个公共的静态方法，用于清理 URL 字符串
    public static String cleanUrl(String url) {
        // 检查传入的URL是否为null或空字符串
        if (url == null || url.isEmpty()) {
            // 如果是，则直接返回原始URL
            return url;
        }

        // 查找URL中问号（?）首次出现的位置，它标志着查询参数的开始
        int queryPos = url.indexOf('?');
        // 如果找到了问号
        if (queryPos != -1) {
            // 则截取从开头到问号之前的部分，即移除查询参数
            url = url.substring(0, queryPos);
        }

        // 查找URL中哈希符号（#）首次出现的位置，它标志着片段标识符的开始
        int hashPos = url.indexOf('#');
        // 如果找到了哈希符号
        if (hashPos != -1) {
            // 则截取从开头到哈希符号之前的部分，即移除哈希部分
            url = url.substring(0, hashPos);
        }

        // 返回清理后的URL字符串
        return url;
    }

    // 定义一个公共的静态方法，用于生成一个安全的文件名
    public static String safeFileName(String originalName) {
        // 检查原始文件名是否为null或空
        if (originalName == null || originalName.isEmpty()) {
            // 如果是，则返回一个默认的文件名 "unnamed"
            return "unnamed";
        }

        // 使用预定义的正则表达式匹配器，将原始文件名中的所有非法字符替换为下划线 "_"
        return INVALID_FILENAME_CHARS.matcher(originalName).replaceAll("_");
    }

    // 定义一个公共的静态方法，用于对字符串进行URL编码
    public static String urlEncode(String str) {
        // 开始一个try-catch块，以处理可能发生的编码异常
        try {
            // 使用URLEncoder对字符串进行编码，指定字符集为UTF-8，并返回编码后的字符串
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        // 捕获在编码过程中可能发生的任何异常
        } catch (Exception e) {
            // 如果编码失败，则记录异常（此处未实现日志记录）并返回原始字符串
            return str;
        }
    }

    // 定义一个公共的静态方法，用于检查字符串是否为null或仅包含空白字符
    public static boolean isNullOrBlank(String str) {
        // 如果字符串为null，或者去除首尾空白后为空字符串，则返回true
        return str == null || str.trim().isEmpty();
    }

    // 定义一个公共的静态方法，用于安全地获取字符串的长度
    public static int safeLength(String str) {
        // 使用三元运算符：如果字符串为null，则返回0；否则返回其长度
        return str == null ? 0 : str.length();
    }

    // 定义一个公共的静态方法，用于截断字符串到指定的最大长度
    public static String truncate(String str, int maxLength) {
        // 检查字符串是否为null，或者其长度是否已经小于或等于最大长度
        if (str == null || str.length() <= maxLength) {
            // 如果是，则无需截断，直接返回原始字符串
            return str;
        }

        // 如果字符串长度超过最大长度，则截取从开头到最大长度的子串，并在末尾添加省略号 "..."
        return str.substring(0, maxLength) + "...";
    }
}