package com.hhu.javawebcrawler.demo.utils;

import com.hhu.javawebcrawler.demo.exception.CrawlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

// 定义一个公共类 RequestValidator，用于封装请求验证逻辑
public class RequestValidator {

    // 创建一个静态的、最终的 Logger 实例，用于记录该类的日志
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    // 定义一个公共的静态方法，用于验证Map中必须存在的字符串参数
    public static String validateRequiredParameter(Map<String, String> params, String paramName) {
        // 检查参数Map中是否不包含指定的参数名
        if (!params.containsKey(paramName)) {
            // 如果缺少参数，记录一条警告日志
            logger.warn("缺少必填参数: {}", paramName);
            // 抛出一个自定义的 CrawlerException 异常，指示是错误的请求
            throw CrawlerException.badRequest("缺少必填参数: " + paramName);
        }

        // 从Map中获取指定参数名的值
        String value = params.get(paramName);
        // 检查获取到的值是否为null或者仅包含空白字符
        if (value == null || value.isBlank()) {
            // 如果参数为空，记录一条警告日志
            logger.warn("参数不能为空: {}", paramName);
            // 抛出异常，指示参数不能为空
            throw CrawlerException.badRequest("参数不能为空: " + paramName);
        }

        // 如果验证通过，返回参数的值
        return value;
    }

    // 定义一个公共的静态方法，用于获取Map中的可选字符串参数
    public static String getOptionalParameter(Map<String, String> params, String paramName, String defaultValue) {
        // 检查参数Map中是否不包含指定的参数名
        if (!params.containsKey(paramName)) {
            // 如果不包含，则返回提供的默认值
            return defaultValue;
        }

        // 从Map中获取指定参数名的值
        String value = params.get(paramName);
        // 如果值是null或空白，返回默认值；否则返回该值
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    // 定义一个公共的静态方法，验证一个数值是否在指定的最小和最大值之间
    public static void validateNumberInRange(Number value, Number min, Number max, String paramName) {
        // 检查传入的数值是否为null
        if (value == null) {
            // 如果为null，记录警告日志
            logger.warn("参数不能为空: {}", paramName);
            // 并抛出异常，指示参数不能为空
            throw CrawlerException.badRequest("参数不能为空: " + paramName);
        }

        // 将要验证的值转换为double类型以便比较
        double doubleValue = value.doubleValue();
        // 检查该值是否小于最小值或大于最大值
        if (doubleValue < min.doubleValue() || doubleValue > max.doubleValue()) {
            // 如果超出范围，记录警告日志，并包含参数名、值和有效范围
            logger.warn("参数值超出范围: {} = {} (有效范围: {} - {})", paramName, value, min, max);
            // 抛出异常，提示用户参数值超出范围
            throw CrawlerException.badRequest(
                    // 使用String.format格式化错误消息
                    String.format("参数 %s 必须在 %s 到 %s 之间", paramName, min, max));
        }
    }

    // 定义一个公共的静态方法，用于验证一个字符串是否是有效的URL格式
    public static void validateUrl(String url, String paramName) {
        // 检查URL字符串是否为null或空白
        if (url == null || url.isBlank()) {
            // 如果是，记录警告日志
            logger.warn("URL参数不能为空: {}", paramName);
            // 并抛出异常，指示URL参数不能为空
            throw CrawlerException.badRequest("URL参数不能为空: " + paramName);
        }

        // 开始一个try-catch块来处理URL格式可能引发的异常
        try {
            // 尝试创建一个新的URL对象，如果字符串格式无效，会抛出MalformedURLException
            new URL(url);
        // 捕获URL格式错误的异常
        } catch (MalformedURLException e) {
            // 记录警告日志，说明URL格式无效
            logger.warn("无效的URL格式: {} = {}", paramName, url);
            // 抛出异常，告知用户提供的URL格式无效
            throw CrawlerException.badRequest("无效的URL格式: " + url);
        }
    }

    // 定义一个公共的静态方法，用于尝试从参数Map中解析一个整数值
    public static Integer parseIntParameter(Map<String, String> params, String paramName, Integer defaultValue) {
        // 检查Map中是否不包含指定的参数名
        if (!params.containsKey(paramName)) {
            // 如果不包含，返回默认值
            return defaultValue;
        }

        // 从Map中获取参数值字符串
        String value = params.get(paramName);
        // 检查值是否为null或空白
        if (value == null || value.isBlank()) {
            // 如果是，返回默认值
            return defaultValue;
        }

        // 开始一个try-catch块来处理字符串到整数的转换可能引发的异常
        try {
            // 尝试将字符串解析为整数并返回
            return Integer.parseInt(value);
        // 捕获数字格式化异常
        } catch (NumberFormatException e) {
            // 如果解析失败，记录警告日志
            logger.warn("无法解析为整数: {} = {}", paramName, value);
            // 抛出异常，告知用户参数必须是有效的整数
            throw CrawlerException.badRequest("参数 " + paramName + " 必须是一个有效的整数");
        }
    }

    // 定义一个公共的静态方法，用于尝试从参数Map中解析一个浮点数值
    public static Float parseFloatParameter(Map<String, String> params, String paramName, Float defaultValue) {
        // 检查Map中是否不包含指定的参数名
        if (!params.containsKey(paramName)) {
            // 如果不包含，返回默认值
            return defaultValue;
        }

        // 从Map中获取参数值字符串
        String value = params.get(paramName);
        // 检查值是否为null或空白
        if (value == null || value.isBlank()) {
            // 如果是，返回默认值
            return defaultValue;
        }

        // 开始一个try-catch块来处理字符串到浮点数的转换可能引发的异常
        try {
            // 尝试将字符串解析为浮点数并返回
            return Float.parseFloat(value);
        // 捕获数字格式化异常
        } catch (NumberFormatException e) {
            // 如果解析失败，记录警告日志
            logger.warn("无法解析为浮点数: {} = {}", paramName, value);
            // 抛出异常，告知用户参数必须是有效的浮点数
            throw CrawlerException.badRequest("参数 " + paramName + " 必须是一个有效的浮点数");
        }
    }
}