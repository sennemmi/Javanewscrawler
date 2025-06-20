package com.hhu.javawebcrawler.demo.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

// 定义一个名为CrawlerException的公共类，它继承自RuntimeException，表示这是一个非受检异常
public class CrawlerException extends RuntimeException {
    
    // 声明一个私有的、最终的HttpStatus类型字段，用于存储HTTP状态码
    private final HttpStatus statusCode;
    // 声明一个私有的、最终的Map类型字段，用于存储错误的详细信息
    private final Map<String, Object> details;
    
    // 定义一个公共构造函数，接收错误消息和HTTP状态码作为参数
    public CrawlerException(String message, HttpStatus statusCode) {
        // 调用父类RuntimeException的构造函数，并传入错误消息
        super(message);
        // 初始化statusCode字段
        this.statusCode = statusCode;
        // 将details字段初始化为null，因为此构造函数不接收详细信息
        this.details = null;
    // 构造函数结束
    }
    
    // 定义一个重载的公共构造函数，额外接收一个包含详细信息的Map
    public CrawlerException(String message, HttpStatus statusCode, Map<String, Object> details) {
        // 调用父类RuntimeException的构造函数，并传入错误消息
        super(message);
        // 初始化statusCode字段
        this.statusCode = statusCode;
        // 使用传入的参数初始化details字段
        this.details = details;
    // 构造函数结束
    }
    
    // 定义一个重载的公共构造函数，额外接收一个原始异常（cause）
    public CrawlerException(String message, Throwable cause, HttpStatus statusCode) {
        // 调用父类RuntimeException的构造函数，并传入错误消息和原始异常
        super(message, cause);
        // 初始化statusCode字段
        this.statusCode = statusCode;
        // 将details字段初始化为null
        this.details = null;
    // 构造函数结束
    }
    
    // 定义一个最完整的公共构造函数，接收所有可能的参数
    public CrawlerException(String message, Throwable cause, HttpStatus statusCode, Map<String, Object> details) {
        // 调用父类RuntimeException的构造函数，并传入错误消息和原始异常
        super(message, cause);
        // 初始化statusCode字段
        this.statusCode = statusCode;
        // 使用传入的参数初始化details字段
        this.details = details;
    // 构造函数结束
    }
    
    // 定义一个公共的getter方法，用于获取HTTP状态码
    public HttpStatus getStatusCode() {
        // 返回statusCode字段的值
        return statusCode;
    // getStatusCode方法结束
    }
    
    // 定义一个公共的getter方法，用于获取详细错误信息
    public Map<String, Object> getDetails() {
        // 返回details字段的值
        return details;
    // getDetails方法结束
    }
    
    // 定义一个静态工厂方法，用于快速创建表示“错误请求”（400）的异常
    public static CrawlerException badRequest(String message) {
        // 创建并返回一个新的CrawlerException实例，状态码为BAD_REQUEST
        return new CrawlerException(message, HttpStatus.BAD_REQUEST);
    // badRequest方法结束
    }
    
    // 定义一个静态工厂方法，用于快速创建表示“未找到”（404）的异常
    public static CrawlerException notFound(String message) {
        // 创建并返回一个新的CrawlerException实例，状态码为NOT_FOUND
        return new CrawlerException(message, HttpStatus.NOT_FOUND);
    // notFound方法结束
    }
    
    // 定义一个静态工厂方法，用于快速创建表示“服务器内部错误”（500）的异常
    public static CrawlerException serverError(String message, Throwable cause) {
        // 创建并返回一个新的CrawlerException实例，状态码为INTERNAL_SERVER_ERROR，并包含原始异常
        return new CrawlerException(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    // serverError方法结束
    }
// CrawlerException类结束
} 