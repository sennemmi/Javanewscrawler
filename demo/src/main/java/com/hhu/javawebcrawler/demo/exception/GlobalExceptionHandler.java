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

/**
 * 全局异常处理器
 * <p>
 * 统一处理应用程序中抛出的异常，提供一致的错误响应格式。
 * </p>
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理认证异常
     * 
     * @param ex 认证异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Map<String, Object> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("认证异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", HttpStatus.UNAUTHORIZED.value());
        response.put("message", ex.getMessage());
        
        return response;
    }
    
    /**
     * 处理授权异常
     * 
     * @param ex 授权异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public Map<String, Object> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("授权异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", HttpStatus.FORBIDDEN.value());
        response.put("message", "没有权限执行此操作");
        
        return response;
    }
    
    /**
     * 处理爬虫异常
     * 
     * @param ex 爬虫异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(com.hhu.javawebcrawler.demo.exception.CrawlerException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleCrawlerException(com.hhu.javawebcrawler.demo.exception.CrawlerException ex) {
        logger.error("爬虫异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", ex.getStatusCode().value());
        response.put("message", ex.getMessage());
        
        if (ex.getDetails() != null) {
            response.put("details", ex.getDetails());
        }
        
        return new ResponseEntity<>(response, ex.getStatusCode());
    }
    
    /**
     * 处理IO异常
     * 
     * @param ex IO异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> handleIOException(IOException ex) {
        logger.error("IO异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "操作过程中发生IO错误: " + ex.getMessage());
        
        return response;
    }
    
    /**
     * 处理所有其他未处理的异常
     * 
     * @param ex 异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> handleGenericException(Exception ex) {
        logger.error("未处理的异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "服务器内部错误: " + ex.getMessage());
        
        return response;
    }
}