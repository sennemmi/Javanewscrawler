// 定义包名，表示该类属于项目的工具类集合
package com.hhu.javawebcrawler.demo.utils;

// 导入项目内部的实体类，用于封装用户信息
import com.hhu.javawebcrawler.demo.entity.User;
// 导入项目内部的服务接口，用于操作用户数据
import com.hhu.javawebcrawler.demo.service.UserService;
// 导入SLF4J库，用于日志记录
import org.slf4j.Logger;
// 导入SLF4J库，用于获取Logger实例
import org.slf4j.LoggerFactory;
// 导入Spring Security的核心接口，代表用户的认证信息
import org.springframework.security.core.Authentication;
// 导入Spring Security的上下文持有者，用于获取当前安全上下文
import org.springframework.security.core.context.SecurityContextHolder;


// 定义一个公共的认证工具类
public class AuthUtils {
    
    // 创建一个静态的Logger实例，用于记录该类的日志
    private static final Logger logger = LoggerFactory.getLogger(AuthUtils.class);
    
    // 定义一个静态公共方法，用于获取当前认证用户的ID
    public static Long getCurrentUserId(UserService userService) throws AuthenticationException {
        // 从安全上下文持有者中获取当前的认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 检查认证对象是否存在、是否已认证，以及是否为匿名用户
        if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
            // 如果用户未认证，记录一条警告日志
            logger.warn("尝试获取未认证用户的ID");
            // 抛出自定义的认证异常
            throw new AuthenticationException("用户未认证");
        }
        
        // 从认证对象中获取用户名
        String username = authentication.getName();
        // 使用try-catch块处理可能发生的异常
        try {
            // 通过用户服务根据用户名查找用户实体
            User user = userService.findByUsername(username);
            // 记录调试日志，显示成功获取到的用户ID
            logger.debug("获取到用户 [{}] 的ID: {}", username, user.getId());
            // 返回用户的ID
            return user.getId();
        // 捕获在获取用户信息过程中可能发生的任何异常
        } catch (Exception e) {
            // 记录获取用户ID失败的错误日志
            logger.error("获取用户ID失败: {} - {}", username, e.getMessage());
            // 抛出包含原始异常信息的认证异常
            throw new AuthenticationException("获取用户信息失败: " + e.getMessage(), e);
        }
    }
    
    // 定义一个静态公共方法，用于检查当前用户是否已认证
    public static boolean isAuthenticated() {
        // 从安全上下文持有者中获取当前的认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 返回一个布尔值，表示用户是否不为null、已认证且不是匿名用户
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    // 定义一个静态公共方法，用于获取当前认证用户的用户名
    public static String getCurrentUsername() {
        // 从安全上下文持有者中获取当前的认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查用户是否已认证
        if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
            // 如果已认证，返回用户名
            return authentication.getName();
        }
        // 如果未认证，返回null
        return null;
    }
    
    // 定义一个静态公共内部类，表示认证相关的异常
    public static class AuthenticationException extends Exception {
        // 定义一个构造函数，接收一个错误消息字符串
        public AuthenticationException(String message) {
            // 调用父类（Exception）的构造函数
            super(message);
        }
        
        // 定义一个重载的构造函数，接收错误消息和根本原因（cause）
        public AuthenticationException(String message, Throwable cause) {
            // 调用父类（Exception）的相应构造函数
            super(message, cause);
        }
    }
}