package com.hhu.javawebcrawler.demo.utils;

import com.hhu.javawebcrawler.demo.exception.CrawlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * 请求验证工具类
 * <p>
 * 提供通用的请求参数验证方法，用于控制器中的参数校验。
 * </p>
 */
public class RequestValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);
    
    /**
     * 验证Map中的必填字符串参数
     * <p>
     * 检查指定的参数是否存在且非空。
     * </p>
     * 
     * @param params 参数Map
     * @param paramName 参数名称
     * @return 参数值
     * @throws CrawlerException 如果参数不存在或为空
     */
    public static String validateRequiredParameter(Map<String, String> params, String paramName) {
        if (!params.containsKey(paramName)) {
            logger.warn("缺少必填参数: {}", paramName);
            throw CrawlerException.badRequest("缺少必填参数: " + paramName);
        }
        
        String value = params.get(paramName);
        if (value == null || value.isBlank()) {
            logger.warn("参数不能为空: {}", paramName);
            throw CrawlerException.badRequest("参数不能为空: " + paramName);
        }
        
        return value;
    }
    
    /**
     * 获取Map中的可选字符串参数
     * <p>
     * 如果参数不存在或为空，则返回默认值。
     * </p>
     * 
     * @param params 参数Map
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static String getOptionalParameter(Map<String, String> params, String paramName, String defaultValue) {
        if (!params.containsKey(paramName)) {
            return defaultValue;
        }
        
        String value = params.get(paramName);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
    
    /**
     * 验证数值参数是否在指定范围内
     * 
     * @param value 要验证的值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param paramName 参数名称
     * @throws CrawlerException 如果值不在指定范围内
     */
    public static void validateNumberInRange(Number value, Number min, Number max, String paramName) {
        if (value == null) {
            logger.warn("参数不能为空: {}", paramName);
            throw CrawlerException.badRequest("参数不能为空: " + paramName);
        }
        
        double doubleValue = value.doubleValue();
        if (doubleValue < min.doubleValue() || doubleValue > max.doubleValue()) {
            logger.warn("参数值超出范围: {} = {} (有效范围: {} - {})", paramName, value, min, max);
            throw CrawlerException.badRequest(
                    String.format("参数 %s 必须在 %s 到 %s 之间", paramName, min, max));
        }
    }
    
    /**
     * 验证URL格式是否有效
     * 
     * @param url 要验证的URL
     * @param paramName 参数名称
     * @throws CrawlerException 如果URL格式无效
     */
    public static void validateUrl(String url, String paramName) {
        if (url == null || url.isBlank()) {
            logger.warn("URL参数不能为空: {}", paramName);
            throw CrawlerException.badRequest("URL参数不能为空: " + paramName);
        }
        
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            logger.warn("无效的URL格式: {} = {}", paramName, url);
            throw CrawlerException.badRequest("无效的URL格式: " + url);
        }
    }
    
    /**
     * 尝试解析整数参数
     * 
     * @param params 参数Map
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 解析后的整数值或默认值
     */
    public static Integer parseIntParameter(Map<String, String> params, String paramName, Integer defaultValue) {
        if (!params.containsKey(paramName)) {
            return defaultValue;
        }
        
        String value = params.get(paramName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("无法解析为整数: {} = {}", paramName, value);
            throw CrawlerException.badRequest("参数 " + paramName + " 必须是一个有效的整数");
        }
    }
    
    /**
     * 尝试解析浮点数参数
     * 
     * @param params 参数Map
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 解析后的浮点数值或默认值
     */
    public static Float parseFloatParameter(Map<String, String> params, String paramName, Float defaultValue) {
        if (!params.containsKey(paramName)) {
            return defaultValue;
        }
        
        String value = params.get(paramName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            logger.warn("无法解析为浮点数: {} = {}", paramName, value);
            throw CrawlerException.badRequest("参数 " + paramName + " 必须是一个有效的浮点数");
        }
    }
} 