package com.hhu.javawebcrawler.demo.controller.base;

import com.hhu.javawebcrawler.demo.exception.CrawlerException;
import com.hhu.javawebcrawler.demo.service.UserService;
import com.hhu.javawebcrawler.demo.utils.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 控制器基类
 * <p>
 * 提供共用的控制器功能，如用户认证、参数验证、日志记录等。
 * 所有控制器应该继承此基类，以实现代码复用和统一的错误处理。
 * </p>
 */
public abstract class BaseController {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 验证用户是否已认证，如果未认证则抛出异常
     * 
     * @throws CrawlerException 如果用户未认证
     */
    protected void validateAuthentication() {
        if (!AuthUtils.isAuthenticated()) {
            throw CrawlerException.badRequest("用户未认证");
        }
    }
    
    /**
     * 获取当前已认证用户的ID
     * 
     * @param userService 用户服务
     * @return 用户ID
     * @throws CrawlerException 如果获取用户ID失败
     */
    protected Long getCurrentUserId(UserService userService) {
        try {
            return AuthUtils.getCurrentUserId(userService);
        } catch (AuthUtils.AuthenticationException e) {
            throw CrawlerException.badRequest(e.getMessage());
        }
    }
    
    /**
     * 检查字符串参数是否为空或空白
     * 
     * @param param 参数值
     * @param paramName 参数名称（用于错误消息）
     * @throws CrawlerException 如果参数为空或空白
     */
    protected void validateStringParam(String param, String paramName) {
        if (param == null || param.isBlank()) {
            logger.warn("参数验证失败: {} 为空", paramName);
            throw CrawlerException.badRequest(paramName + "不能为空");
        }
    }
    
    /**
     * 验证数值是否在指定范围内
     * 
     * @param value 要验证的值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param paramName 参数名称（用于错误消息）
     * @throws CrawlerException 如果值不在指定范围内
     */
    protected void validateNumberInRange(Number value, Number min, Number max, String paramName) {
        if (value == null) {
            logger.warn("参数验证失败: {} 为空", paramName);
            throw CrawlerException.badRequest(paramName + "不能为空");
        }
        
        double doubleValue = value.doubleValue();
        if (doubleValue < min.doubleValue() || doubleValue > max.doubleValue()) {
            logger.warn("参数验证失败: {} = {} 不在有效范围内 ({} - {})", paramName, value, min, max);
            throw CrawlerException.badRequest(paramName + "必须在" + min + "到" + max + "之间");
        }
    }
    
    /**
     * 执行操作并处理异常
     * <p>
     * 通用的异常处理逻辑，用于包装服务方法调用。
     * </p>
     * 
     * @param operation 要执行的操作
     * @param <T> 操作返回类型
     * @return 操作结果
     * @throws CrawlerException 如果操作失败
     */
    protected <T> T executeWithExceptionHandling(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (CrawlerException e) {
            // 直接传递已经包装好的CrawlerException
            throw e;
        } catch (Exception e) {
            logger.error("操作执行失败: {}", e.getMessage(), e);
            throw CrawlerException.serverError("操作执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建标准的响应结构
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 包含状态和数据的Map
     */
    protected <T> Map<String, Object> createSuccessResponse(T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }
    
    /**
     * 创建错误响应结构
     * 
     * @param message 错误消息
     * @return 包含错误信息的Map
     */
    protected Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}