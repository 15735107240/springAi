package com.springai.auth.service;

import com.springai.auth.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 用户服务接口
 * 
 * @author yanwenjie
 */
public interface UserService extends UserDetailsService {
    
    /**
     * 根据手机号创建或获取用户
     * 
     * @param phoneNumber 手机号
     * @return 用户对象
     */
    User createOrGetUser(String phoneNumber);
    
    /**
     * 根据手机号加载用户
     * 
     * @param phoneNumber 手机号
     * @return 用户对象
     */
    User loadUserByPhoneNumber(String phoneNumber);
}

