package com.springai.auth.filter;

import com.springai.auth.context.UserContext;
import com.springai.auth.service.CurrentUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 自动从请求中提取用户信息并存储到 ThreadLocal
 * 注意：此过滤器应在 Spring Security 过滤器之后执行，确保用户信息已被解析
 * 
 * @author yanwenjie
 */
@Slf4j
@Component
@Order(100)
public class UserContextFilter extends OncePerRequestFilter {

    private final CurrentUserService currentUserService;

    public UserContextFilter(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 清除之前的上下文（防止线程复用导致的问题）
            UserContext.clear();
            
            // 如果用户已认证，获取用户信息并存储到 ThreadLocal
            if (currentUserService.isAuthenticated()) {
                var user = currentUserService.getCurrentUser();
                if (user != null) {
                    UserContext.setUser(user);
                    log.debug("用户上下文已设置 - 手机号: {}", user.getPhoneNumber());
                }
            }
            
            // 继续执行过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清除 ThreadLocal，避免内存泄漏
            UserContext.clear();
        }
    }
}

