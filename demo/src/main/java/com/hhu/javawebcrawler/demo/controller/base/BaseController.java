package com.hhu.javawebcrawler.demo.controller.base;

import com.hhu.javawebcrawler.demo.exception.CrawlerException;
import com.hhu.javawebcrawler.demo.service.UserService;
import com.hhu.javawebcrawler.demo.utils.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// 定义一个抽象的基类控制器，提供通用功能。
public abstract class BaseController {
    
    // 为每个子类创建一个受保护的、不可变的Logger实例，用于记录日志。
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    // 定义一个受保护的方法，用于验证用户是否已认证。
    protected void validateAuthentication() {
        // 调用AuthUtils工具类检查当前用户是否已认证。
        if (!AuthUtils.isAuthenticated()) {
            // 如果未认证，则抛出一个自定义的“错误请求”异常。
            throw CrawlerException.badRequest("用户未认证");
        } // if条件结束。
    } // validateAuthentication方法结束。
    
    // 定义一个受保护的方法，用于获取当前登录用户的ID。
    protected Long getCurrentUserId(UserService userService) {
        // 开始一个try块，捕获获取用户ID时可能发生的认证异常。
        try {
            // 调用AuthUtils工具类获取当前用户的ID。
            return AuthUtils.getCurrentUserId(userService);
        } catch (AuthUtils.AuthenticationException e) { // 捕获自定义的认证异常。
            // 将认证异常包装成自定义的“错误请求”异常并抛出。
            throw CrawlerException.badRequest(e.getMessage());
        } // try-catch结束。
    } // getCurrentUserId方法结束。
    
    // 定义一个受保护的方法，用于验证字符串参数是否有效。
    protected void validateStringParam(String param, String paramName) {
        // 检查参数是否为null或仅包含空白字符。
        if (param == null || param.isBlank()) {
            // 如果是，则记录参数验证失败的警告日志。
            logger.warn("参数验证失败: {} 为空", paramName);
            // 抛出一个自定义的“错误请求”异常，并提供清晰的错误消息。
            throw CrawlerException.badRequest(paramName + "不能为空");
        } // if条件结束。
    } // validateStringParam方法结束。
    
    // 定义一个受保护的方法，用于验证数值是否在指定范围内。
    protected void validateNumberInRange(Number value, Number min, Number max, String paramName) {
        // 检查数值参数是否为null。
        if (value == null) {
            // 如果是，则记录参数验证失败的警告日志。
            logger.warn("参数验证失败: {} 为空", paramName);
            // 抛出一个自定义的“错误请求”异常。
            throw CrawlerException.badRequest(paramName + "不能为空");
        } // if条件结束。
        
        // 将传入的数值转换为double类型以便比较。
        double doubleValue = value.doubleValue();
        // 检查该值是否小于最小值或大于最大值。
        if (doubleValue < min.doubleValue() || doubleValue > max.doubleValue()) {
            // 如果超出范围，则记录参数验证失败的警告日志。
            logger.warn("参数验证失败: {} = {} 不在有效范围内 ({} - {})", paramName, value, min, max);
            // 抛出一个自定义的“错误请求”异常，并提供详细的范围信息。
            throw CrawlerException.badRequest(paramName + "必须在" + min + "到" + max + "之间");
        } // if条件结束。
    } // validateNumberInRange方法结束。
    
    // 定义一个受保护的泛型方法，用于执行操作并统一处理异常。
    protected <T> T executeWithExceptionHandling(Supplier<T> operation) {
        // 开始一个try块，用于执行传入的操作并捕获异常。
        try {
            // 执行由Supplier提供的get()方法，并返回其结果。
            return operation.get();
        } catch (CrawlerException e) { // 捕获已知的、自定义的爬虫异常。
            // 如果是已知的CrawlerException，则直接重新抛出，不进行包装。
            throw e;
        } catch (Exception e) { // 捕获其他所有未知异常。
            // 记录未知异常的错误日志，并包含完整的堆栈跟踪。
            logger.error("操作执行失败: {}", e.getMessage(), e);
            // 将未知异常包装成一个自定义的“服务器错误”异常并抛出。
            throw CrawlerException.serverError("操作执行失败: " + e.getMessage(), e);
        } // try-catch结束。
    } // executeWithExceptionHandling方法结束。
    
    // 定义一个受保护的泛型方法，用于创建标准的成功响应体。
    protected <T> Map<String, Object> createSuccessResponse(T data) {
        // 创建一个新的HashMap来组织响应数据。
        Map<String, Object> response = new HashMap<>();
        // 在响应中放入一个表示成功的"status"字段。
        response.put("status", "success");
        // 在响应中放入实际的业务数据。
        response.put("data", data);
        // 返回构建好的响应Map。
        return response;
    } // createSuccessResponse方法结束。
    
    // 定义一个受保护的方法，用于创建标准的错误响应体。
    protected Map<String, Object> createErrorResponse(String message) {
        // 创建一个新的HashMap来组织响应数据。
        Map<String, Object> response = new HashMap<>();
        // 在响应中放入一个表示错误的"status"字段。
        response.put("status", "error");
        // 在响应中放入具体的错误消息。
        response.put("message", message);
        // 返回构建好的响应Map。
        return response;
    } // createErrorResponse方法结束。
} // BaseController类定义结束。