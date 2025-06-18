package com.hhu.javawebcrawler.demo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 爬虫异常类
 * <p>
 * 表示在爬虫操作过程中发生的异常，包含HTTP状态码和详细错误信息。
 * </p>
 */
public class CrawlerException extends RuntimeException {
    
    private final HttpStatus statusCode;
    private final Map<String, Object> details;
    
    /**
     * 创建爬虫异常
     * 
     * @param message 错误消息
     * @param statusCode HTTP状态码
     */
    public CrawlerException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.details = null;
    }
    
    /**
     * 创建爬虫异常
     * 
     * @param message 错误消息
     * @param statusCode HTTP状态码
     * @param details 详细错误信息
     */
    public CrawlerException(String message, HttpStatus statusCode, Map<String, Object> details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }
    
    /**
     * 创建爬虫异常
     * 
     * @param message 错误消息
     * @param cause 原始异常
     * @param statusCode HTTP状态码
     */
    public CrawlerException(String message, Throwable cause, HttpStatus statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.details = null;
    }
    
    /**
     * 创建爬虫异常
     * 
     * @param message 错误消息
     * @param cause 原始异常
     * @param statusCode HTTP状态码
     * @param details 详细错误信息
     */
    public CrawlerException(String message, Throwable cause, HttpStatus statusCode, Map<String, Object> details) {
        super(message, cause);
        this.statusCode = statusCode;
        this.details = details;
    }
    
    /**
     * 获取HTTP状态码
     * 
     * @return HTTP状态码
     */
    public HttpStatus getStatusCode() {
        return statusCode;
    }
    
    /**
     * 获取详细错误信息
     * 
     * @return 详细错误信息
     */
    public Map<String, Object> getDetails() {
        return details;
    }
    
    /**
     * 创建表示参数验证失败的异常
     * 
     * @param message 错误消息
     * @return 爬虫异常
     */
    public static CrawlerException badRequest(String message) {
        return new CrawlerException(message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 创建表示资源未找到的异常
     * 
     * @param message 错误消息
     * @return 爬虫异常
     */
    public static CrawlerException notFound(String message) {
        return new CrawlerException(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 创建表示服务器内部错误的异常
     * 
     * @param message 错误消息
     * @param cause 原始异常
     * @return 爬虫异常
     */
    public static CrawlerException serverError(String message, Throwable cause) {
        return new CrawlerException(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 