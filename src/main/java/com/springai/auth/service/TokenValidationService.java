package com.springai.auth.service;

/**
 * Token 验证服务接口
 * 用于检查 token 是否在 Redis 中存在（即是否有效）
 * 
 * @author yanwenjie
 */
public interface TokenValidationService {

    /**
     * 检查 token 是否在 Redis 中存在
     * 
     * @param token JWT 令牌值
     * @return true 如果 token 在 Redis 中存在，false 如果不存在
     */
    boolean isTokenExists(String token);
}

