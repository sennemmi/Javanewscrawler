package com.hhu.javawebcrawler.demo.utils;

import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 认证工具类
 * <p>
 * 提供用户认证相关的通用方法，封装了从Security上下文获取用户信息的逻辑。
 * </p>
 */
public class AuthUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthUtils.class);
    
    /**
     * 获取当前认证用户的ID
     * <p>
     * 从Spring Security上下文中获取当前用户的身份信息，并通过UserService获取对应的用户ID。
     * </p>
     * 
     * @param userService 用户服务
     * @return 用户ID
     * @throws AuthenticationException 如果用户未登录或无法获取用户信息
     */
    public static Long getCurrentUserId(UserService userService) throws AuthenticationException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
            logger.warn("尝试获取未认证用户的ID");
            throw new AuthenticationException("用户未认证");
        }
        
        String username = authentication.getName();
        try {
            User user = userService.findByUsername(username);
            logger.debug("获取到用户 [{}] 的ID: {}", username, user.getId());
            return user.getId();
        } catch (Exception e) {
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            throw new AuthenticationException("获取用户信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查当前用户是否已认证
     * <p>
     * 从Spring Security上下文中获取当前用户的认证状态。
     * </p>
     * 
     * @return 如果用户已认证则返回true，否则返回false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * 获取当前认证用户的用户名
     * <p>
     * 从Spring Security上下文中获取当前用户的用户名。
     * </p>
     * 
     * @return 用户名，如果用户未认证则返回null
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }
    
    /**
     * 认证异常
     * <p>
     * 表示在获取用户认证信息过程中发生的异常。
     * </p>
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
        
        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 