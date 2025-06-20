package com.hhu.javawebcrawler.demo.service.impl;

import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.repository.UserRepository;
import com.hhu.javawebcrawler.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

 // @Service注解，标记这个类是Spring的一个服务层组件
 @Service
 // 定义一个名为UserServiceImpl的公共类，并实现UserService接口
 public class UserServiceImpl implements UserService {

     // @Autowired注解，自动从Spring容器中注入UserRepository的实例
     @Autowired
     // 声明一个私有的UserRepository成员变量，用于与数据库交互
     private UserRepository userRepository;

     // @Autowired注解，自动从Spring容器中注入PasswordEncoder的实例
     @Autowired
     // 声明一个私有的PasswordEncoder成员变量，用于密码加密和验证
     private PasswordEncoder passwordEncoder;

     // @Override注解，表示此方法重写了父接口UserService中的方法
     @Override
     // 定义一个公开的注册方法，接收一个User对象，并可能抛出Exception
     public User register(User userToRegister) throws Exception {
         // 检查数据库中是否已存在具有相同用户名的用户
         if (userRepository.findByUsername(userToRegister.getUsername()).isPresent()) {
             // 如果用户名已存在，则抛出一个异常
             throw new Exception("用户名 '" + userToRegister.getUsername() + "' 已被注册！");
         // if语句结束
         }
         
         // 使用passwordEncoder对用户的明文密码进行加密
         String encodedPassword = passwordEncoder.encode(userToRegister.getPassword());
         // 将加密后的密码设置回User对象
         userToRegister.setPassword(encodedPassword);

         // 调用userRepository的save方法，将新用户信息持久化到数据库，并返回保存后的User对象
         return userRepository.save(userToRegister);
     // register方法结束
     }

     // @Override注解，表示此方法重写了父接口UserService中的方法
     @Override
     // 定义一个公开的登录方法，接收用户名和密码，并可能抛出Exception
     public User login(String username, String password) throws Exception {
         // 根据用户名从数据库查找用户，如果找不到则抛出异常
         User user = userRepository.findByUsername(username)
                 .orElseThrow(() -> new Exception("用户不存在！"));

         // 使用passwordEncoder的matches方法，验证传入的明文密码是否与数据库中存储的加密密码匹配
         if (!passwordEncoder.matches(password, user.getPassword())) {
             // 如果密码不匹配，则抛出异常
             throw new Exception("密码错误！");
         // if语句结束
         }

         // 如果验证成功，返回查找到的User对象
         return user;
     // login方法结束
     }

     // @Override注解，表示此方法重写了父接口UserService中的方法
     @Override
     // 定义一个公开的按用户名查找用户的方法，并可能抛出Exception
     public User findByUsername(String username) throws Exception {
         // 根据用户名从数据库查找用户，如果找不到则抛出一个包含具体用户名的异常
         return userRepository.findByUsername(username)
                 .orElseThrow(() -> new Exception("用户 " + username + " 不存在"));
     // findByUsername方法结束
     }

// UserServiceImpl类结束
 }