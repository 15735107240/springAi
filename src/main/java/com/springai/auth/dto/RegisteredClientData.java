package com.springai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * RegisteredClient 的数据传输对象
 * 用于 Redis 序列化存储
 * 
 * @author yanwenjie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredClientData implements Serializable {
    
    private String id;
    private String clientId;
    private String clientSecret;
    private Set<String> clientAuthenticationMethods;
    private Set<String> authorizationGrantTypes;
    private Set<String> redirectUris;
    private Set<String> scopes;
    @SuppressWarnings("java:S1948")
    private Map<String, Object> tokenSettings;
    @SuppressWarnings("java:S1948")
    private Map<String, Object> clientSettings;
}

