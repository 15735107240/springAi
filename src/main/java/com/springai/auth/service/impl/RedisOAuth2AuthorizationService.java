package com.springai.auth.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 的 OAuth2 授权服务实现
 * 使用 Redisson 存储 OAuth2Authorization 对象
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
@Primary
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final RedissonClient redissonClient;
    private static final String KEY_PREFIX = "oauth2:authorization:";
    private static final String TOKEN_KEY_PREFIX = "oauth2:token:";
    private static final String ACCESS_TOKEN_SUFFIX = "access:";
    private static final String REFRESH_TOKEN_SUFFIX = "refresh:";

    public RedisOAuth2AuthorizationService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        log.info("Redis OAuth2 授权服务初始化完成");
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        try {
            String id = authorization.getId();
            String key = KEY_PREFIX + id;
            
            RBucket<OAuth2Authorization> bucket = redissonClient.getBucket(key);
            bucket.set(authorization);
            
            long authorizationExpiresIn = 3600L;
            if (authorization.getAccessToken() != null && authorization.getAccessToken().getToken() != null) {
                var expiresAt = authorization.getAccessToken().getToken().getExpiresAt();
                if (expiresAt != null) {
                    long epochSeconds = expiresAt.getEpochSecond();
                    long currentSeconds = System.currentTimeMillis() / 1000;
                    authorizationExpiresIn = Math.max(3600L, epochSeconds - currentSeconds);
                }
            }
            bucket.expire(Duration.ofSeconds(authorizationExpiresIn));
            
            if (authorization.getAccessToken() != null && authorization.getAccessToken().getToken() != null) {
                String tokenValue = authorization.getAccessToken().getToken().getTokenValue();
                String accessTokenKey = TOKEN_KEY_PREFIX + ACCESS_TOKEN_SUFFIX + tokenValue;
                RBucket<String> tokenBucket = redissonClient.getBucket(accessTokenKey);
                tokenBucket.set(id);
                
                var accessToken = authorization.getAccessToken().getToken();
                long expiresIn = 3600L;
                
                if (accessToken.getExpiresAt() != null) {
                    long epochSeconds = accessToken.getExpiresAt().getEpochSecond();
                    long currentSeconds = System.currentTimeMillis() / 1000;
                    expiresIn = epochSeconds - currentSeconds;
                    expiresIn = Math.max(60L, expiresIn);
                }
                
                tokenBucket.expire(Duration.ofSeconds(expiresIn));
            }
            
        } catch (Exception e) {
            log.error("保存 OAuth2 授权信息失败 - ID: {}", authorization.getId(), e);
            throw new RuntimeException("保存 OAuth2 授权信息失败", e);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        try {
            String id = authorization.getId();
            String key = KEY_PREFIX + id;
            
            RBucket<OAuth2Authorization> bucket = redissonClient.getBucket(key);
            if (bucket.isExists()) {
                bucket.delete();
                
                if (authorization.getAccessToken() != null && authorization.getAccessToken().getToken() != null) {
                    String accessTokenKey = TOKEN_KEY_PREFIX + ACCESS_TOKEN_SUFFIX + 
                            authorization.getAccessToken().getToken().getTokenValue();
                    redissonClient.getBucket(accessTokenKey).delete();
                }
            }
        } catch (Exception e) {
            log.error("删除 OAuth2 授权信息失败 - ID: {}", authorization.getId(), e);
            throw new RuntimeException("删除 OAuth2 授权信息失败", e);
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {
        try {
            String key = KEY_PREFIX + id;
            RBucket<OAuth2Authorization> bucket = redissonClient.getBucket(key);
            return bucket.get();
        } catch (Exception e) {
            // 如果是Kryo序列化错误（UnmodifiableMap），降级为DEBUG日志，因为这是已知问题
            if (e.getMessage() != null && e.getMessage().contains("UnmodifiableMap")) {
                log.debug("查找 OAuth2 授权信息失败（序列化问题） - ID: {}", id);
            } else {
                log.error("查找 OAuth2 授权信息失败 - ID: {}", id, e);
            }
            return null;
        }
    }

    @Override
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        try {
            String tokenKeyPrefix;
            if (tokenType == null) {
                tokenKeyPrefix = TOKEN_KEY_PREFIX + ACCESS_TOKEN_SUFFIX;
            } else {
                tokenKeyPrefix = TOKEN_KEY_PREFIX + 
                        (tokenType.getValue().equals("access_token") ? ACCESS_TOKEN_SUFFIX : REFRESH_TOKEN_SUFFIX);
            }
            
            String tokenKey = tokenKeyPrefix + token;
            RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
            String authorizationId = tokenBucket.get();
            
            if (authorizationId == null) {
                return null;
            }
            
            return findById(authorizationId);
        } catch (Exception e) {
            // 如果是Kryo序列化错误，降级为DEBUG日志，因为这是已知问题且已通过直接删除方式解决
            if (e.getMessage() != null && (e.getMessage().contains("UnmodifiableMap") || 
                    (e.getCause() != null && e.getCause().getMessage() != null && 
                     e.getCause().getMessage().contains("UnmodifiableMap")))) {
                log.debug("根据令牌查找 OAuth2 授权信息失败（序列化问题） - 令牌类型: {}", 
                        tokenType != null ? tokenType.getValue() : "unknown");
            } else {
                log.error("根据令牌查找 OAuth2 授权信息失败 - 令牌类型: {}", 
                        tokenType != null ? tokenType.getValue() : "unknown", e);
            }
            return null;
        }
    }
}

