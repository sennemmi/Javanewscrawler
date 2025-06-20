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

// @Aspect注解将这个类标识为一个切面
@Aspect
// @Component注解让Spring容器能够扫描并管理这个切面Bean
@Component
// 定义一个名为LoggingAspect的公共类
public class LoggingAspect {
    
    // 为当前类创建一个私有的、最终的Logger实例，用于记录日志
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // @Pointcut注解定义一个切入点，名为controllerMethods
    // execution表达式指定了切入点的位置：com.hhu.javawebcrawler.demo.controller包及其子包下的所有类的所有方法
    @Pointcut("execution(* com.hhu.javawebcrawler.demo.controller..*.*(..))")
    // 定义一个空方法体的方法，作为切入点的签名
    public void controllerMethods() {}
    
    // @Around注解声明这是一个环绕通知，它作用于controllerMethods()切入点
    @Around("controllerMethods()")
    // 定义环绕通知的方法，它接收一个ProceedingJoinPoint参数，用于控制目标方法的执行
    public Object logAroundControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录方法开始执行时的时间戳
        long startTime = System.currentTimeMillis();
        
        // 初始化HttpServletRequest对象为null
        HttpServletRequest request = null;
        // 开始一个try-catch块，以安全地获取HTTP请求对象
        try {
            // 从RequestContextHolder获取当前请求的属性
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            // 如果成功获取到属性
            if (attributes != null) {
                // 从属性中获取HttpServletRequest对象
                request = attributes.getRequest();
            }
        // 捕获可能发生的异常（例如，在非Web环境下调用）
        } catch (Exception e) {
            // 在此忽略异常，因为请求信息对于日志记录来说不是强制性的
        }
        
        // 如果成功获取了request对象
        if (request != null) {
            // 记录包含HTTP方法、URI和方法签名的请求开始日志
            logger.info("请求开始 - {} {} ({})", request.getMethod(), request.getRequestURI(), joinPoint.getSignature().toShortString());
        // 如果没有获取到request对象
        } else {
            // 记录一个更通用的方法调用开始日志
            logger.info("方法调用开始 - {}", joinPoint.getSignature().toShortString());
        }
        
        // 声明一个Object类型的变量来存储目标方法的返回值
        Object result;
        // 开始一个try-catch块，以执行目标方法并处理其成功或异常退出的情况
        try {
            // 调用proceed()方法执行目标方法，并获取其返回值
            result = joinPoint.proceed();
            // 计算方法的执行耗时
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 如果存在request对象
            if (request != null) {
                // 记录包含HTTP方法、URI、方法签名和耗时的请求完成日志
                logger.info("请求完成 - {} {} ({}) - 耗时: {}ms", 
                        request.getMethod(), request.getRequestURI(), 
                        joinPoint.getSignature().toShortString(), executionTime);
            // 如果不存在request对象
            } else {
                // 记录一个更通用的方法调用完成日志，包含耗时
                logger.info("方法调用完成 - {} - 耗时: {}ms", 
                        joinPoint.getSignature().toShortString(), executionTime);
            }
            
            // 返回目标方法的执行结果
            return result;
        // 捕获目标方法执行过程中抛出的任何异常
        } catch (Exception e) {
            // 计算从开始到抛出异常的耗时
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 如果存在request对象
            if (request != null) {
                // 记录包含详细信息的请求异常日志
                logger.error("请求异常 - {} {} ({}) - 耗时: {}ms - 异常: {}", 
                        request.getMethod(), request.getRequestURI(), 
                        joinPoint.getSignature().toShortString(), executionTime, e.getMessage());
            // 如果不存在request对象
            } else {
                // 记录一个更通用的方法调用异常日志
                logger.error("方法调用异常 - {} - 耗时: {}ms - 异常: {}", 
                        joinPoint.getSignature().toShortString(), executionTime, e.getMessage());
            }
            
            // 将捕获到的异常重新抛出，以确保上层调用者能处理它
            throw e;
        }
    }
    
    // @AfterThrowing注解声明这是一个异常通知，当匹配的方法抛出异常时执行
    // throwing = "e" 将方法抛出的异常对象绑定到通知方法的参数e上
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "e")
    // 定义异常通知的方法，接收JoinPoint和Throwable作为参数
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // 记录一条详细的错误日志
        logger.error("异常详情 - 方法: {}.{}() - 参数: {} - 异常类型: {} - 异常消息: {}", 
                // 获取方法所在的类的类型名称
                joinPoint.getSignature().getDeclaringTypeName(),
                // 获取方法的名称
                joinPoint.getSignature().getName(),
                // 获取并格式化方法的参数数组为字符串
                Arrays.toString(joinPoint.getArgs()),
                // 获取异常对象的类名
                e.getClass().getName(),
                // 获取异常对象的消息
                e.getMessage());
    }
}