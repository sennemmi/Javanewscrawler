package com.hhu.javawebcrawler.demo.service.impl;

import com.hhu.javawebcrawler.demo.entity.User;
import com.hhu.javawebcrawler.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 从数据库根据用户名查询用户实体
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户 " + username + " 不存在"));

        // 2. 将User实体，转换成Spring Security需要的UserDetails对象
        // Spring Security的User对象需要用户名、密码和权限集合
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), //这里传递的是数据库中已加密的密码
                Collections.emptyList() // 权限列表，暂时为空
        );
    }
}
