package com.springai.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springai.auth.dto.RegisteredClientData;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的注册客户端仓库实现
 * 使用 Redisson 存储 RegisteredClient 对象
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
@Primary
public class RedisRegisteredClientRepository implements RegisteredClientRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private static final String CLIENT_ID_KEY_PREFIX = "oauth2:client:clientId:";
    private static final String CLIENT_ID_INDEX_KEY = "oauth2:client:index";

    public RedisRegisteredClientRepository(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        log.info("Redis 注册客户端仓库初始化完成");
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        try {
            String clientIdKey = CLIENT_ID_KEY_PREFIX + registeredClient.getClientId();
            
            RegisteredClientData clientData = toData(registeredClient);
            
            RBucket<String> bucket = redissonClient.getBucket(clientIdKey, new JsonJacksonCodec());
            String clientJson = objectMapper.writeValueAsString(clientData);
            bucket.set(clientJson);
            
            bucket.expire(Duration.ofDays(30));
            
            RBucket<Map<String, String>> indexBucket = redissonClient.getBucket(CLIENT_ID_INDEX_KEY);
            Map<String, String> index = indexBucket.get();
            if (index == null) {
                index = new HashMap<>();
            }
            index.put(registeredClient.getClientId(), registeredClient.getId());
            indexBucket.set(index);
            indexBucket.expire(Duration.ofDays(30));
            
            log.info("保存注册客户端 - ClientId: {}, ID: {}", 
                    registeredClient.getClientId(), registeredClient.getId());
        } catch (JsonProcessingException e) {
            log.error("序列化注册客户端失败 - ClientId: {}", registeredClient.getClientId(), e);
            throw new RuntimeException("保存注册客户端失败", e);
        } catch (Exception e) {
            log.error("保存注册客户端失败 - ClientId: {}", registeredClient.getClientId(), e);
            throw new RuntimeException("保存注册客户端失败", e);
        }
    }
    
    private RegisteredClientData toData(RegisteredClient client) {
        RegisteredClientData data = new RegisteredClientData();
        data.setId(client.getId());
        data.setClientId(client.getClientId());
        data.setClientSecret(client.getClientSecret());
        data.setClientAuthenticationMethods(
                client.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .collect(Collectors.toSet()));
        data.setAuthorizationGrantTypes(
                client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .collect(Collectors.toSet()));
        data.setRedirectUris(client.getRedirectUris());
        data.setScopes(client.getScopes());
        data.setTokenSettings(client.getTokenSettings().getSettings());
        data.setClientSettings(client.getClientSettings().getSettings());
        return data;
    }
    
    private RegisteredClient fromData(RegisteredClientData data) {
        RegisteredClient.Builder builder = RegisteredClient.withId(data.getId())
                .clientId(data.getClientId())
                .clientSecret(data.getClientSecret());
        
        if (data.getClientAuthenticationMethods() != null) {
            for (String method : data.getClientAuthenticationMethods()) {
                builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
            }
        }
        
        if (data.getAuthorizationGrantTypes() != null) {
            for (String grantType : data.getAuthorizationGrantTypes()) {
                builder.authorizationGrantType(new AuthorizationGrantType(grantType));
            }
        }
        
        if (data.getRedirectUris() != null) {
            data.getRedirectUris().forEach(builder::redirectUri);
        }
        
        if (data.getScopes() != null) {
            data.getScopes().forEach(builder::scope);
        }
        
        if (data.getTokenSettings() != null) {
            Map<String, Object> tokenSettingsMap = new HashMap<>(data.getTokenSettings());
            
            tokenSettingsMap.remove("settings.token.access-token-format");
            
            TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
            
            Object ttlObj = tokenSettingsMap.get("settings.token.access-token-time-to-live");
            if (ttlObj instanceof Number number) {
                tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(number.longValue()));
            } else if (ttlObj instanceof Duration duration) {
                tokenSettingsBuilder.accessTokenTimeToLive(duration);
            } else if (ttlObj instanceof String string) {
                try {
                    Duration duration = Duration.parse(string);
                    tokenSettingsBuilder.accessTokenTimeToLive(duration);
                } catch (Exception e) {
                    log.warn("无法解析 accessTokenTimeToLive 字符串: {}, 使用默认值 1 小时", string);
                    tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofHours(1));
                }
            } else {
                tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofHours(1));
            }
            
            Object refreshTtlObj = tokenSettingsMap.get("settings.token.refresh-token-time-to-live");
            if (refreshTtlObj instanceof Number number) {
                tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(number.longValue()));
            } else if (refreshTtlObj instanceof Duration duration) {
                tokenSettingsBuilder.refreshTokenTimeToLive(duration);
            } else if (refreshTtlObj instanceof String string) {
                try {
                    Duration duration = Duration.parse(string);
                    tokenSettingsBuilder.refreshTokenTimeToLive(duration);
                } catch (Exception e) {
                    log.warn("无法解析 refreshTokenTimeToLive 字符串: {}, 使用默认值 7 天", string);
                    tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofDays(7));
                }
            } else {
                tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofDays(7));
            }
            
            Object reuseObj = tokenSettingsMap.get("settings.token.reuse-refresh-tokens");
            if (reuseObj instanceof Boolean booleanValue) {
                tokenSettingsBuilder.reuseRefreshTokens(booleanValue);
            }
            
            tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
            
            builder.tokenSettings(tokenSettingsBuilder.build());
        } else {
            builder.tokenSettings(TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .build());
        }
        
        if (data.getClientSettings() != null) {
            builder.clientSettings(ClientSettings.withSettings(data.getClientSettings()).build());
        }
        
        return builder.build();
    }

    @Override
    public RegisteredClient findById(String id) {
        try {
            RBucket<Map<String, String>> indexBucket = redissonClient.getBucket(CLIENT_ID_INDEX_KEY);
            Map<String, String> index = indexBucket.get();
            if (index == null) {
                return null;
            }
            
            for (Map.Entry<String, String> entry : index.entrySet()) {
                if (entry.getValue().equals(id)) {
                    return findByClientId(entry.getKey());
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("根据ID查找注册客户端失败 - ID: {}", id, e);
            return null;
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        try {
            String clientIdKey = CLIENT_ID_KEY_PREFIX + clientId;
            RBucket<String> bucket = redissonClient.getBucket(clientIdKey, new JsonJacksonCodec());
            String clientJson = bucket.get();
            
            if (clientJson == null) {
                return null;
            }
            
            try {
                RegisteredClientData clientData = objectMapper.readValue(clientJson, RegisteredClientData.class);
                return fromData(clientData);
            } catch (JsonProcessingException e) {
                log.error("反序列化注册客户端失败 - ClientId: {}", clientId, e);
                bucket.delete();
                return null;
            }
        } catch (Exception e) {
            log.error("根据ClientId查找注册客户端失败 - ClientId: {}", clientId, e);
            return null;
        }
    }
}

