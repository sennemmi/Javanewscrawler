package com.hhu.javawebcrawler.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    /**
     * 配置密码编码器Bean
     * 使用BCrypt强哈希算法进行密码加密
     * BCryptPasswordEncoder实例，用于密码的加密和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许访问静态资源和特定页面
                .requestMatchers(
                    "/", "/index.html", "/login.html", "/register.html",
                    "/crawler.html",
                    "/css/**", "/js/**", "/fonts/**",
                    // Swagger UI 相关的路径
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // 允许访问处理登录、注册、检查当前用户状态的API
                .requestMatchers(
                    "/api/user/register", 
                    "/api/user/login", 
                    "/api/user/current" // **【重要】放行此URL，以便前端检查登录状态**
                ).permitAll()
                // 其他任何请求都需要身份验证
                .anyRequest().authenticated()
            )
            // 2. 配置表单登录
            .formLogin(form -> form
                .loginProcessingUrl("/api/user/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(jsonAuthenticationSuccessHandler()) 
                .failureHandler(jsonAuthenticationFailureHandler()) 
                .permitAll()
            )
            // 3. 配置退出登录
            .logout(logout -> logout
                .logoutUrl("/api/user/logout") // **【重要】处理退出请求的URL，与前端fetch一致**
                .logoutSuccessHandler(jsonLogoutSuccessHandler()) // **【修改】使用自定义JSON成功处理器**
                .deleteCookies("JSESSIONID") // 退出时删除cookie
            )
            // 4. 禁用CSRF保护
            .csrf(csrf -> csrf.disable())
            // 5. 配置异常处理，特别是对于未认证的访问
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> 
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未认证或会话已过期"))
            );

        return http.build();
    }
    
    // 自定义登录成功处理器，返回JSON
    private AuthenticationSuccessHandler jsonAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            // 返回当前用户信息或其他成功信息
            // 注意：authentication.getPrincipal() 返回的是 UserDetails 对象
            // 如果你需要返回更多用户信息，需要从 UserDetails 中获取
            String json = String.format("{\"message\": \"登录成功\", \"username\": \"%s\"}", authentication.getName());
            response.getWriter().write(json);
        };
    }

    // 自定义登录失败处理器，返回JSON
    private AuthenticationFailureHandler jsonAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("{\"message\": \"用户名或密码错误\"}");
        };
    }

    // 自定义退出成功处理器，返回JSON
    private LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"message\": \"退出成功\"}");
        };
    }
}