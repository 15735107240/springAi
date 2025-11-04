package com.springai.auth.service;

import com.springai.auth.dto.LoginResponse;

/**
 * 认证服务接口
 * 
 * @author yanwenjie
 */
public interface AuthService {
    
    /**
     * 手机号+验证码登录
     * 
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 登录响应（包含访问令牌）
     */
    LoginResponse login(String phoneNumber, String code);
    
    /**
     * 登出
     * 删除当前用户的访问令牌和授权信息
     * 
     * @param token 访问令牌
     */
    void logout(String token);
}

