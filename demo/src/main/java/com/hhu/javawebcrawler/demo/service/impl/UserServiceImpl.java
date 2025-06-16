package com.hhu.javawebcrawler.demo.service.impl;

import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.repository.UserRepository;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

 @Service // 标记这是一个Service组件，Spring会自动扫描并注册它
 public class UserServiceImpl implements UserService {

     @Autowired // 自动注入UserRepository的实例
     private UserRepository userRepository;

     @Autowired // 自动注入配置好的PasswordEncoder
     private PasswordEncoder passwordEncoder;

     @Override
     public User register(User userToRegister) throws Exception {
         // 1. 检查用户名是否已存在
         if (userRepository.findByUsername(userToRegister.getUsername()).isPresent()) {
             throw new Exception("用户名 '" + userToRegister.getUsername() + "' 已被注册！");
         }
         
         // 2. 对密码进行加密
         String encodedPassword = passwordEncoder.encode(userToRegister.getPassword());
         userToRegister.setPassword(encodedPassword);

         // 3. 保存到数据库
         return userRepository.save(userToRegister);
     }

     @Override
     public User login(String username, String password) throws Exception {
         // 1. 根据用户名查找用户
         User user = userRepository.findByUsername(username)
                 .orElseThrow(() -> new Exception("用户不存在！"));

         // 2. 验证密码
         if (!passwordEncoder.matches(password, user.getPassword())) {
             throw new Exception("密码错误！");
         }

         return user; // 登录成功，返回用户信息
     }

     @Override
     public User findByUsername(String username) throws Exception {
         return userRepository.findByUsername(username)
                 .orElseThrow(() -> new Exception("用户 " + username + " 不存在"));
     }

 }
