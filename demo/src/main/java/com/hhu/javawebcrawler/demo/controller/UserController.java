package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.DTO.RegistrationRequest;
import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

// @RestController 注解，表明这是一个RESTful风格的控制器，方法返回值默认序列化为JSON
@RestController
// @RequestMapping 注解，定义该控制器所有请求的基础路径为 /api/user
@RequestMapping("/api/user")
// 定义一个名为 UserController 的公共类
public class UserController {

    // @Autowired 注解，自动从Spring容器中注入UserService的实例
    @Autowired
    // 声明一个私有的 UserService 成员变量
    private UserService userService;

    // @PostMapping 注解，将HTTP POST请求映射到 /api/user/register 路径
    @PostMapping("/register")
    // 定义一个公开的注册用户方法，返回类型为ResponseEntity，接收一个RegistrationRequest对象作为请求体
    public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest registrationRequest) {
        // 开始一个try-catch块，用于捕获和处理可能发生的异常
        try {
            // 创建一个新的User实体对象
            User newUser = new User();
            // 从请求对象中获取用户名并设置给新用户
            newUser.setUsername(registrationRequest.getUsername());
            // 从请求对象中获取密码并设置给新用户
            newUser.setPassword(registrationRequest.getPassword());
            
            // 调用userService的register方法来处理用户注册逻辑
            userService.register(newUser);

            // 如果注册成功，返回一个HTTP状态码为200 OK的响应，并附带成功消息
            return ResponseEntity.ok("注册成功！");
        // 捕获在try块中可能抛出的任何Exception
        } catch (Exception e) {
            // 如果发生异常，返回一个HTTP状态码为400 Bad Request的响应，并附带异常信息
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        // try-catch块结束
        }
    // registerUser方法结束
    }

    // @GetMapping 注解，将HTTP GET请求映射到 /api/user/current 路径
    @GetMapping("/current")
    // 定义一个公开的获取当前用户信息的方法，返回类型为ResponseEntity
    public ResponseEntity<?> getCurrentUser() {
        // 从Spring Security的上下文中获取当前的Authentication对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查Authentication对象是否存在、是否已认证，并且认证主体不是匿名用户
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // 从Authentication对象中获取用户名
            String username = authentication.getName();
            // 创建一个HashMap来存储要返回的用户信息
            Map<String, Object> userInfo = new HashMap<>();
            // 将用户名放入Map中
            userInfo.put("username", username);
            // 返回一个HTTP状态码为200 OK的响应，并附带包含用户信息的Map
            return ResponseEntity.ok(userInfo);
        // if语句块结束
        }
        // 如果用户未登录或认证失败，返回一个HTTP状态码为401 Unauthorized的响应，并附带提示信息
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
    // getCurrentUser方法结束
    }
// UserController类结束
}