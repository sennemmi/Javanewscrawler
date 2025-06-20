package com.hhu.javawebcrawler.demo.DTO;

import lombok.Data;

// @Data是Lombok库的注解，它会自动为所有字段生成getter、setter方法
@Data
// 定义一个名为LoginRequest的公共类，用于封装登录请求的数据
public class LoginRequest {
    // 声明一个私有的String类型字段，用于存储用户名
    private String username;
    // 声明一个私有的String类型字段，用于存储密码
    private String password;
// LoginRequest类定义结束
}