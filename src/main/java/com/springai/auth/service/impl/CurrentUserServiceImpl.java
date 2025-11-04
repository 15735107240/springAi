package com.springai.auth.service.impl;

import com.springai.auth.entity.User;
import com.springai.auth.service.CurrentUserService;
import com.springai.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * 当前用户服务实现
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserService userService;

    public CurrentUserServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String getCurrentPhoneNumber() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // 从 JWT token 中获取用户信息
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            // JWT 的 subject 通常是客户端 ID，但我们在生成 token 时将 principalName 设置为手机号
            // 检查是否有自定义的 username claim
            String username = jwt.getClaimAsString("username");
            if (username != null) {
                return username;
            }
            // 如果没有 username claim，尝试从 subject 获取
            String subject = jwt.getSubject();
            // 如果 subject 是手机号格式，直接返回
            if (subject != null && subject.matches("^1[3-9]\\d{9}$")) {
                return subject;
            }
            // 否则从 principalName 获取（在 OAuth2Authorization 中设置）
            String principalName = authentication.getName();
            if (principalName != null && principalName.matches("^1[3-9]\\d{9}$")) {
                return principalName;
            }
        }
        
        // 如果认证对象有 principalName，尝试使用
        String principalName = authentication.getName();
        if (principalName != null && principalName.matches("^1[3-9]\\d{9}$")) {
            return principalName;
        }
        
        return null;
    }

    @Override
    public User getCurrentUser() {
        String phoneNumber = getCurrentPhoneNumber();
        if (phoneNumber == null) {
            return null;
        }
        
        try {
            return userService.loadUserByPhoneNumber(phoneNumber);
        } catch (Exception e) {
            log.warn("获取当前用户信息失败 - 手机号: {}", phoneNumber, e);
            return null;
        }
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null 
                && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal().toString());
    }
}

