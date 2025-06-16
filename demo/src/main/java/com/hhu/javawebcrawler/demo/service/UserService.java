package com.hhu.javawebcrawler.demo.service;

import com.hhu.javawebcrawler.demo.entity.User;
 
 public interface UserService {
    // 注册
     User register(User userToRegister) throws Exception;

    // 登录
    User login(String username, String password) throws Exception;

    // 根据用户名查找用户
    User findByUsername(String username) throws Exception;
 }
