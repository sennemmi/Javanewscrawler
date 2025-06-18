package com.hhu.javawebcrawler.demo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 日志记录切面
 * <p>
 * 使用AOP方式记录控制器方法调用日志，包括请求参数、耗时和异常信息。
 * </p>
 */
@Aspect
@Component
public class LoggingAspect {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /**
     * 定义切入点：所有控制器
     */
    @Pointcut("execution(* com.hhu.javawebcrawler.demo.controller..*.*(..))")
    public void controllerMethods() {}
    
    /**
     * 环绕通知：记录方法调用前后的日志
     * 
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 如果方法执行过程中发生异常
     */
    @Around("controllerMethods()")
    public Object logAroundControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        HttpServletRequest request = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }
        } catch (Exception e) {
            // 忽略异常，请求信息不是必须的
        }
        
        // 记录请求开始的日志
        if (request != null) {
            logger.info("请求开始 - {} {} ({})", request.getMethod(), request.getRequestURI(), joinPoint.getSignature().toShortString());
        } else {
            logger.info("方法调用开始 - {}", joinPoint.getSignature().toShortString());
        }
        
        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录方法执行完成的日志
            if (request != null) {
                logger.info("请求完成 - {} {} ({}) - 耗时: {}ms", 
                        request.getMethod(), request.getRequestURI(), 
                        joinPoint.getSignature().toShortString(), executionTime);
            } else {
                logger.info("方法调用完成 - {} - 耗时: {}ms", 
                        joinPoint.getSignature().toShortString(), executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录方法执行异常的日志
            if (request != null) {
                logger.error("请求异常 - {} {} ({}) - 耗时: {}ms - 异常: {}", 
                        request.getMethod(), request.getRequestURI(), 
                        joinPoint.getSignature().toShortString(), executionTime, e.getMessage());
            } else {
                logger.error("方法调用异常 - {} - 耗时: {}ms - 异常: {}", 
                        joinPoint.getSignature().toShortString(), executionTime, e.getMessage());
            }
            
            throw e;
        }
    }
    
    /**
     * 异常通知：记录方法抛出异常的详细信息
     * 
     * @param joinPoint 连接点
     * @param e 异常对象
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        logger.error("异常详情 - 方法: {}.{}() - 参数: {} - 异常类型: {} - 异常消息: {}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()),
                e.getClass().getName(),
                e.getMessage());
    }
} 