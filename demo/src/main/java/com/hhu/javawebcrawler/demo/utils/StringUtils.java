package com.hhu.javawebcrawler.demo.utils;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 提供字符串处理相关的通用方法，封装了常用的字符串操作。
 * </p>
 */
public class StringUtils {
    
    // 文件名不允许包含的字符的正则表达式
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    
    /**
     * 清理URL
     * <p>
     * 移除URL中的查询参数和哈希部分，返回干净的URL。
     * </p>
     * 
     * @param url 原始URL
     * @return 清理后的URL
     */
    public static String cleanUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // 移除查询参数
        int queryPos = url.indexOf('?');
        if (queryPos != -1) {
            url = url.substring(0, queryPos);
        }
        
        // 移除哈希部分
        int hashPos = url.indexOf('#');
        if (hashPos != -1) {
            url = url.substring(0, hashPos);
        }
        
        return url;
    }
    
    /**
     * 生成安全的文件名
     * <p>
     * 将原始文件名中的非法字符替换为下划线，确保文件名符合系统要求。
     * </p>
     * 
     * @param originalName 原始文件名
     * @return 安全的文件名
     */
    public static String safeFileName(String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            return "unnamed";
        }
        
        return INVALID_FILENAME_CHARS.matcher(originalName).replaceAll("_");
    }
    
    /**
     * URL编码
     * <p>
     * 对字符串进行URL编码，使用UTF-8字符集。
     * </p>
     * 
     * @param str 需要编码的字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            // 如果编码失败，返回原始字符串
            return str;
        }
    }
    
    /**
     * 检查字符串是否为空或空白
     * <p>
     * 判断字符串是否为null、空字符串或仅包含空白字符。
     * </p>
     * 
     * @param str 要检查的字符串
     * @return 如果字符串为空或空白则返回true，否则返回false
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 获取字符串的安全长度
     * <p>
     * 如果字符串为null，返回0，否则返回字符串的长度。
     * </p>
     * 
     * @param str 要检查的字符串
     * @return 字符串的长度，如果为null则返回0
     */
    public static int safeLength(String str) {
        return str == null ? 0 : str.length();
    }
    
    /**
     * 截断字符串
     * <p>
     * 如果字符串长度超过指定的最大长度，则截断并添加省略号。
     * </p>
     * 
     * @param str 要截断的字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串，如果原字符串为null则返回null
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength) + "...";
    }
} 