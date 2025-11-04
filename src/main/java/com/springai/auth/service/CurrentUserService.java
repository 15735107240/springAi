package com.springai.auth.service;

import com.springai.auth.entity.User;

/**
 * 当前用户服务接口
 * 用于获取当前登录用户信息
 * 
 * @author yanwenjie
 */
public interface CurrentUserService {
    
    /**
     * 获取当前登录用户的手机号
     * 
     * @return 手机号，如果未登录则返回 null
     */
    String getCurrentPhoneNumber();
    
    /**
     * 获取当前登录用户完整信息
     * 
     * @return 用户对象，如果未登录则返回 null
     */
    User getCurrentUser();
    
    /**
     * 检查当前是否有用户登录
     * 
     * @return 如果已登录返回 true，否则返回 false
     */
    boolean isAuthenticated();
}

