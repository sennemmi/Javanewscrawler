package com.hhu.javawebcrawler.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

 @Configuration
 public class SecurityConfig {
    /**
      * 配置密码编码器Bean
      * 使用BCrypt强哈希算法进行密码加密
      * BCryptPasswordEncoder实例用于密码的加密和验证
    */
     @Bean
     public PasswordEncoder passwordEncoder() {
         return new BCryptPasswordEncoder();
     }
 }
