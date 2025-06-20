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

// 标记这个类是一个Spring配置类
@Configuration
// 启用Spring Security的Web安全支持
@EnableWebSecurity
// 定义Web安全配置类
public class WebSecurityConfig {

    // 将该方法的返回值注册为一个Spring Bean
    @Bean
    // 定义一个名为passwordEncoder的Bean，用于提供密码编码器实例
    public PasswordEncoder passwordEncoder() {
        // 返回一个BCryptPasswordEncoder实例，用于密码的加密和验证
        return new BCryptPasswordEncoder();
    }
    
    // 将该方法的返回值注册为一个Spring Bean，特别是安全过滤器链
    @Bean
    // 配置安全过滤器链，定义HTTP请求的安全规则
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 开始配置HttpSecurity
        http
            // 1. 配置HTTP请求的授权规则
            .authorizeHttpRequests(auth -> auth
                // 匹配以下指定的URL路径
                .requestMatchers(
                    // 允许访问根路径和几个核心HTML页面
                    "/", "/index.html", "/login.html", "/register.html",
                    // 允许访问爬虫主页面
                    "/crawler.html",
                    // 允许访问所有静态资源目录
                    "/css/**", "/js/**", "/fonts/**",
                    // 允许访问Swagger UI的HTML页面
                    "/swagger-ui.html",
                    // 允许访问Swagger UI的相关资源
                    "/swagger-ui/**",
                    // 允许访问Swagger API文档
                    "/v3/api-docs/**"
                // 对以上匹配的请求，允许所有用户访问
                ).permitAll()
                // 匹配以下指定的API端点
                .requestMatchers(
                    // 允许访问用户注册API
                    "/api/user/register", 
                    // 允许访问用户登录API
                    "/api/user/login", 
                    // 允许访问检查当前用户状态的API，以便前端判断登录状态
                    "/api/user/current"
                // 对以上匹配的API请求，允许所有用户访问
                ).permitAll()
                // 对于任何其他未匹配的请求，都必须进行身份验证
                .anyRequest().authenticated()
            )
            // 2. 配置表单登录
            .formLogin(form -> form
                // 指定处理登录请求的URL
                .loginProcessingUrl("/api/user/login")
                // 指定登录表单中用户名字段的参数名
                .usernameParameter("username")
                // 指定登录表单中密码字段的参数名
                .passwordParameter("password")
                // 设置登录成功后使用的自定义处理器
                .successHandler(jsonAuthenticationSuccessHandler()) 
                // 设置登录失败后使用的自定义处理器
                .failureHandler(jsonAuthenticationFailureHandler()) 
                // 允许所有用户访问登录页面和登录处理URL
                .permitAll()
            )
            // 3. 配置退出登录功能
            .logout(logout -> logout
                // 指定处理退出登录请求的URL
                .logoutUrl("/api/user/logout")
                // 设置退出成功后使用的自定义处理器
                .logoutSuccessHandler(jsonLogoutSuccessHandler())
                // 在退出成功后删除名为JSESSIONID的cookie
                .deleteCookies("JSESSIONID")
            )
            // 4. 禁用CSRF（跨站请求伪造）保护，常用于API服务
            .csrf(csrf -> csrf.disable())
            // 5. 配置异常处理
            .exceptionHandling(e -> e
                // 设置一个认证入口点，处理未认证用户访问受保护资源的情况
                .authenticationEntryPoint((request, response, authException) -> 
                    // 当需要认证时，返回401未授权错误和消息
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未认证或会话已过期"))
            );

        // 构建并返回SecurityFilterChain实例
        return http.build();
    }
    
    // 定义一个私有方法，创建并返回一个自定义的登录成功处理器
    private AuthenticationSuccessHandler jsonAuthenticationSuccessHandler() {
        // 使用Lambda表达式实现AuthenticationSuccessHandler接口
        return (request, response, authentication) -> {
            // 设置响应的内容类型为JSON，并指定字符集为UTF-8
            response.setContentType("application/json;charset=UTF-8");
            // 设置HTTP响应状态码为200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);
            // 创建一个包含成功消息和用户名的JSON字符串
            String json = String.format("{\"message\": \"登录成功\", \"username\": \"%s\"}", authentication.getName());
            // 将JSON字符串写入响应体
            response.getWriter().write(json);
        };
    }

    // 定义一个私有方法，创建并返回一个自定义的登录失败处理器
    private AuthenticationFailureHandler jsonAuthenticationFailureHandler() {
        // 使用Lambda表达式实现AuthenticationFailureHandler接口
        return (request, response, exception) -> {
            // 设置响应的内容类型为JSON，并指定字符集为UTF-8
            response.setContentType("application/json;charset=UTF-8");
            // 设置HTTP响应状态码为401 (Unauthorized)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // 将包含错误消息的JSON字符串写入响应体
            response.getWriter().write("{\"message\": \"用户名或密码错误\"}");
        };
    }

    // 定义一个私有方法，创建并返回一个自定义的退出成功处理器
    private LogoutSuccessHandler jsonLogoutSuccessHandler() {
        // 使用Lambda表达式实现LogoutSuccessHandler接口
        return (request, response, authentication) -> {
            // 设置响应的内容类型为JSON，并指定字符集为UTF-8
            response.setContentType("application/json;charset=UTF-8");
            // 设置HTTP响应状态码为200 (OK)
            response.setStatus(HttpServletResponse.SC_OK);
            // 将包含成功消息的JSON字符串写入响应体
            response.getWriter().write("{\"message\": \"退出成功\"}");
        };
    }
}