package com.hhu.javawebcrawler.demo.exception;

import com.hhu.javawebcrawler.demo.utils.AuthUtils.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// @ControllerAdvice注解，表明这是一个全局控制器增强器，用于统一处理异常
@ControllerAdvice
// 定义一个名为GlobalExceptionHandler的公共类，继承自ResponseEntityExceptionHandler以提供对Spring MVC异常的基本处理
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    // 获取一个私有的、静态的、最终的Logger实例，用于记录日志
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // @ExceptionHandler注解，指定该方法处理AuthenticationException类型的异常
    @ExceptionHandler(AuthenticationException.class)
    // @ResponseStatus注解，设置此处理器返回的HTTP状态码为UNAUTHORIZED (401)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    // @ResponseBody注解，表示该方法的返回值将直接作为HTTP响应体，并自动序列化为JSON
    @ResponseBody
    // 定义一个公共方法来处理认证异常，接收抛出的异常作为参数
    public Map<String, Object> handleAuthenticationException(AuthenticationException ex) {
        // 使用warn级别记录认证异常信息
        logger.warn("认证异常: {}", ex.getMessage());
        
        // 创建一个HashMap来构建响应体内容
        Map<String, Object> response = new HashMap<>();
        // 向响应体中添加状态字段
        response.put("status", "error");
        // 向响应体中添加HTTP状态码
        response.put("code", HttpStatus.UNAUTHORIZED.value());
        // 向响应体中添加具体的错误消息
        response.put("message", ex.getMessage());
        
        // 返回包含错误信息的Map
        return response;
    // handleAuthenticationException方法结束
    }
    
    // @ExceptionHandler注解，指定该方法处理AccessDeniedException类型的异常
    @ExceptionHandler(AccessDeniedException.class)
    // @ResponseStatus注解，设置此处理器返回的HTTP状态码为FORBIDDEN (403)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    // @ResponseBody注解，表示方法的返回值将作为HTTP响应体
    @ResponseBody
    // 定义一个公共方法来处理授权（访问被拒绝）异常
    public Map<String, Object> handleAccessDeniedException(AccessDeniedException ex) {
        // 使用warn级别记录授权异常信息
        logger.warn("授权异常: {}", ex.getMessage());
        
        // 创建一个HashMap来构建响应体内容
        Map<String, Object> response = new HashMap<>();
        // 向响应体中添加状态字段
        response.put("status", "error");
        // 向响应体中添加HTTP状态码
        response.put("code", HttpStatus.FORBIDDEN.value());
        // 向响应体中添加一个通用的、对用户友好的权限错误消息
        response.put("message", "没有权限执行此操作");
        
        // 返回包含错误信息的Map
        return response;
    // handleAccessDeniedException方法结束
    }
    
    // @ExceptionHandler注解，指定该方法处理自定义的CrawlerException类型的异常
    @ExceptionHandler(com.hhu.javawebcrawler.demo.exception.CrawlerException.class)
    // @ResponseBody注解，表示方法的返回值将作为HTTP响应体
    @ResponseBody
    // 定义一个公共方法来处理爬虫异常，返回一个ResponseEntity以灵活控制响应状态码和内容
    public ResponseEntity<Map<String, Object>> handleCrawlerException(com.hhu.javawebcrawler.demo.exception.CrawlerException ex) {
        // 使用error级别记录爬虫异常信息
        logger.error("爬虫异常: {}", ex.getMessage());
        
        // 创建一个HashMap来构建响应体内容
        Map<String, Object> response = new HashMap<>();
        // 向响应体中添加状态字段
        response.put("status", "error");
        // 从异常对象中获取状态码并添加到响应体
        response.put("code", ex.getStatusCode().value());
        // 从异常对象中获取错误消息并添加到响应体
        response.put("message", ex.getMessage());
        
        // 检查异常对象中是否包含详细信息
        if (ex.getDetails() != null) {
            // 如果有，将详细信息添加到响应体
            response.put("details", ex.getDetails());
        // if语句结束
        }
        
        // 创建并返回一个ResponseEntity对象，包含响应体和从异常中获取的HTTP状态码
        return new ResponseEntity<>(response, ex.getStatusCode());
    // handleCrawlerException方法结束
    }
    
    // @ExceptionHandler注解，指定该方法处理IOException类型的异常
    @ExceptionHandler(IOException.class)
    // @ResponseStatus注解，设置此处理器返回的HTTP状态码为INTERNAL_SERVER_ERROR (500)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    // @ResponseBody注解，表示方法的返回值将作为HTTP响应体
    @ResponseBody
    // 定义一个公共方法来处理IO异常
    public Map<String, Object> handleIOException(IOException ex) {
        // 使用error级别记录IO异常信息，并包含异常堆栈
        logger.error("IO异常: {}", ex.getMessage(), ex);
        
        // 创建一个HashMap来构建响应体内容
        Map<String, Object> response = new HashMap<>();
        // 向响应体中添加状态字段
        response.put("status", "error");
        // 向响应体中添加HTTP状态码
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        // 向响应体中添加一个描述IO错误的具体消息
        response.put("message", "操作过程中发生IO错误: " + ex.getMessage());
        
        // 返回包含错误信息的Map
        return response;
    // handleIOException方法结束
    }
    
    // @ExceptionHandler注解，指定该方法处理所有未被前面处理器捕获的Exception子类异常
    @ExceptionHandler(Exception.class)
    // @ResponseStatus注解，设置此处理器返回的HTTP状态码为INTERNAL_SERVER_ERROR (500)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    // @ResponseBody注解，表示方法的返回值将作为HTTP响应体
    @ResponseBody
    // 定义一个通用的异常处理方法
    public Map<String, Object> handleGenericException(Exception ex) {
        // 使用error级别记录未被捕获的异常信息，并包含异常堆栈
        logger.error("未处理的异常: {}", ex.getMessage(), ex);
        
        // 创建一个HashMap来构建响应体内容
        Map<String, Object> response = new HashMap<>();
        // 向响应体中添加状态字段
        response.put("status", "error");
        // 向响应体中添加HTTP状态码
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        // 向响应体中添加一个通用的服务器内部错误消息
        response.put("message", "服务器内部错误: " + ex.getMessage());
        
        // 返回包含错误信息的Map
        return response;
    // handleGenericException方法结束
    }
// GlobalExceptionHandler类结束
}