package com.hhu.javawebcrawler.demo.controller;

import com.hhu.javawebcrawler.demo.DTO.LoginRequest;
import com.hhu.javawebcrawler.demo.DTO.RegistrationRequest;
import com.hhu.javawebcrawler.demo.entity.User;
 import com.hhu.javawebcrawler.demo.service.UserService;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
 import jakarta.servlet.http.HttpSession;
 import java.util.HashMap;
 import java.util.Map;

 @RestController // 声明这是一个RESTful Controller，所有方法默认返回JSON
 @RequestMapping("/api/user") // 所有该Controller下的接口都以/api/user开头
 public class UserController {

     @Autowired // 自动注入UserService
     private UserService userService;

     @PostMapping("/register")// 注册接口
     public ResponseEntity<?> registerUser(@RequestBody RegistrationRequest registrationRequest) {
         try {
             User newUser = new User();
             newUser.setUsername(registrationRequest.getUsername());
             newUser.setPassword(registrationRequest.getPassword());
             
             userService.register(newUser);

             return ResponseEntity.ok("注册成功！");
         } catch (Exception e) {
             // 返回一个更友好的错误信息
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
         }
     }

     @PostMapping("/login")// 登录接口
     public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, HttpSession session) {
         try {
             User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
             
             // 登录成功，将用户信息存入Session
             session.setAttribute("user", user);

             Map<String, Object> response = new HashMap<>();
             response.put("message", "登录成功！");
             response.put("username", user.getUsername());
             
             return ResponseEntity.ok(response);
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
         }
     }

    // 获取当前用户信息的接口，需要从SecurityContextHolder获取
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            return ResponseEntity.ok(userInfo);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
    }

 }
