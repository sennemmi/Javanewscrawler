package com.hhu.javawebcrawler.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(
                                        "/", "/index.html", "/login.html", "/register.html",
                                        "/api/user/register", // 注册API依然需要放行
                                        "/css/**", "/js/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/login.html") // 指定我们自己的登录页面
                                // **重要：指定Spring Security处理登录的URL，前端表单的action要指向这里**
                                .loginProcessingUrl("/login")
                                // **重要：告诉Spring Security用户名和密码的表单字段名是什么**
                                .usernameParameter("username")
                                .passwordParameter("password")
                                .defaultSuccessUrl("/index.html", true) // 登录成功后强制跳转到主页
                                .failureUrl("/login.html?error") // 登录失败后跳转回登录页并带上error参数
                                .permitAll()
                )
                .logout(logout ->
                        logout
                                .logoutUrl("/logout") // 使用Spring Security默认的logout
                                .logoutSuccessUrl("/login.html?logout")
                                .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
