package com.springai.auth.service.impl;

import com.springai.auth.service.TokenValidationService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Token 验证服务实现
 * 通过检查 Redis 中是否存在对应的授权信息来验证 token 是否有效
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class TokenValidationServiceImpl implements TokenValidationService {

    private static final String TOKEN_KEY_PREFIX = "oauth2:token:";
    private static final String ACCESS_TOKEN_SUFFIX = "access:";

    private final RedissonClient redissonClient;

    public TokenValidationServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean isTokenExists(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String tokenKey = TOKEN_KEY_PREFIX + ACCESS_TOKEN_SUFFIX + token;
            RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
            
            if (!tokenBucket.isExists()) {
                return false;
            }

            String authorizationId = tokenBucket.get();
            if (authorizationId == null || authorizationId.trim().isEmpty()) {
                return false;
            }

            String authorizationKey = "oauth2:authorization:" + authorizationId;
            RBucket<Object> authorizationBucket = redissonClient.getBucket(authorizationKey);
            return authorizationBucket.isExists();
        } catch (Exception e) {
            log.error("检查 token 是否存在时发生错误", e);
            return false;
        }
    }

}

