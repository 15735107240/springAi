package com.springai.auth.service.impl;

import com.springai.auth.dto.LoginResponse;
import com.springai.auth.entity.User;
import com.springai.auth.service.AuthService;
import com.springai.auth.service.UserService;
import com.springai.auth.service.VerificationCodeService;
import com.springai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 认证服务实现
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final VerificationCodeService verificationCodeService;
    private final UserService userService;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationService authorizationService;
    private final AuthorizationServerSettings authorizationServerSettings;
    private final RedissonClient redissonClient;
    @SuppressWarnings("rawtypes")
    private final OAuth2TokenGenerator tokenGenerator;

    @SuppressWarnings("rawtypes")
    public AuthServiceImpl(
            VerificationCodeService verificationCodeService,
            UserService userService,
            RegisteredClientRepository registeredClientRepository,
            OAuth2AuthorizationService authorizationService,
            AuthorizationServerSettings authorizationServerSettings,
            RedissonClient redissonClient,
            OAuth2TokenGenerator tokenGenerator) {
        this.verificationCodeService = verificationCodeService;
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.authorizationServerSettings = authorizationServerSettings;
        this.redissonClient = redissonClient;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public LoginResponse login(String phoneNumber, String code) {
        // 验证手机号格式
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 验证验证码
        if (!verificationCodeService.verifyCode(phoneNumber, code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        
        // 创建或获取用户
        User user = userService.createOrGetUser(phoneNumber);
        
        // 生成访问令牌
        String accessToken = generateAccessToken(user);
        
        // 从授权信息中获取过期时间
        // 默认1小时
        long expiresIn = 3600L;
        
        // 构建响应
        LoginResponse response = LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
        
        log.info("用户登录成功 - 手机号: {}", maskPhoneNumber(phoneNumber));
        return response;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException("令牌不能为空");
        }
        
        // 由于Kryo序列化问题，直接使用兜底方案删除token（更可靠）
        // 避免尝试读取完整的授权信息导致序列化异常
        boolean deleted = deleteTokenDirectly(token);
        if (deleted) {
            log.info("用户登出成功");
        } else {
            // 如果直接删除失败，尝试通过标准方式（可能已经过期或不存在）
            try {
                OAuth2Authorization authorization = authorizationService.findByToken(
                        token, OAuth2TokenType.ACCESS_TOKEN);
                if (authorization != null) {
                    authorizationService.remove(authorization);
                    String phoneNumber = authorization.getPrincipalName();
                    log.info("用户登出成功 - 手机号: {}", maskPhoneNumber(phoneNumber));
                } else {
                    throw new BusinessException("令牌无效或已失效");
                }
            } catch (Exception e) {
                // 如果标准方式也失败，可能是token已失效
                log.debug("标准方式删除失败，token可能已失效", e);
                throw new BusinessException("令牌无效或已失效");
            }
        }
    }
    
    /**
     * 直接删除token的Redis记录（绕过序列化问题）
     * 从token key中获取authorizationId，然后删除authorization和token记录
     */
    private boolean deleteTokenDirectly(String token) {
        try {
            // 直接访问Redis删除token记录
            // token key格式: oauth2:token:access:{tokenValue}
            // 存储的是 authorizationId
            String tokenKey = "oauth2:token:access:" + token;
            RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
            
            String authorizationId = tokenBucket.get();
            if (authorizationId == null || authorizationId.trim().isEmpty()) {
                return false;
            }
            
            // 删除token记录
            tokenBucket.delete();
            
            // 删除authorization记录
            String authorizationKey = "oauth2:authorization:" + authorizationId;
            RBucket<Object> authorizationBucket = redissonClient.getBucket(authorizationKey);
            authorizationBucket.delete();
            
            log.info("直接删除token和authorization记录成功 - authorizationId: {}", authorizationId);
            return true;
        } catch (Exception e) {
            log.error("直接删除token记录失败", e);
            return false;
        }
    }

    /**
     * 生成 OAuth2 访问令牌
     */
    private String generateAccessToken(User user) {
        // 获取注册的客户端（使用默认的客户端ID）
        String clientId = "spring-ai-client";
        log.debug("查找注册的 OAuth2 客户端 - ClientId: {}", clientId);
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.error("未找到注册的 OAuth2 客户端 - ClientId: {}", clientId);
            log.error("请检查 Redis 中是否存在客户端配置，或重启应用以触发 @PostConstruct 初始化");
            throw new IllegalStateException("未找到注册的 OAuth2 客户端");
        }
        log.debug("找到注册的 OAuth2 客户端 - ClientId: {}, Scopes: {}", 
                registeredClient.getClientId(), registeredClient.getScopes());
        
        // 验证必需字段
        if (user == null || user.getPhoneNumber() == null) {
            log.error("用户信息不完整 - user: {}, phoneNumber: {}", user, user != null ? user.getPhoneNumber() : null);
            throw new IllegalArgumentException("用户信息不完整");
        }
        
        // 验证客户端配置
        if (registeredClient.getClientId() == null) {
            log.error("客户端 ID 为空");
            throw new IllegalStateException("客户端配置错误：ClientId 为空");
        }
        
        if (registeredClient.getClientSecret() == null) {
            log.error("客户端密钥为空 - ClientId: {}", registeredClient.getClientId());
            throw new IllegalStateException("客户端配置错误：ClientSecret 为空");
        }
        
        // 创建授权上下文
        String authorizationId = "auth-" + user.getPhoneNumber() + "-" + System.currentTimeMillis();
        String principalName = user.getPhoneNumber();
        
        log.debug("创建 OAuth2 授权 - ID: {}, PrincipalName: {}, ClientId: {}", 
                authorizationId, principalName, registeredClient.getClientId());
        
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(authorizationId)
                .principalName(principalName)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .attribute(OAuth2ParameterNames.USERNAME, principalName)
                .attribute("phone_number", principalName);
        
        OAuth2Authorization authorization = authorizationBuilder.build();
        
        log.debug("OAuth2 授权创建成功 - ID: {}, PrincipalName: {}", 
                authorization.getId(), authorization.getPrincipalName());
        
        // 创建客户端认证令牌
        String clientSecret = registeredClient.getClientSecret();
        // 移除 {noop} 前缀
        if (clientSecret != null && clientSecret.startsWith("{noop}")) {
            clientSecret = clientSecret.substring(7);
        }
        
        Authentication clientAuthentication = 
                new UsernamePasswordAuthenticationToken(
                        registeredClient.getClientId(), 
                        clientSecret != null ? clientSecret : "",
                        Collections.emptyList());
        
        // 创建客户端凭证认证令牌
        Set<String> clientScopes = registeredClient.getScopes();
        Set<String> requestedScopes;
        if (clientScopes != null && !clientScopes.isEmpty()) {
            requestedScopes = new HashSet<>(clientScopes);
        } else {
            // 如果没有配置 scopes，使用默认的 "read" scope
            requestedScopes = new HashSet<>();
            requestedScopes.add("read");
        }
        
        log.debug("准备创建客户端凭证认证令牌 - Scopes: {}, ClientId: {}", 
                requestedScopes, registeredClient.getClientId());
        
        OAuth2ClientCredentialsAuthenticationToken clientCredentialsAuth =
                new OAuth2ClientCredentialsAuthenticationToken(
                        clientAuthentication,
                        requestedScopes,
                        Collections.emptyMap()
                );
        
        log.debug("客户端凭证认证令牌创建成功");
        
        // 创建令牌上下文
        log.debug("准备创建 OAuth2TokenContext - RegisteredClient: {}, AuthorizationId: {}, Principal: {}", 
                registeredClient.getClientId(), 
                authorization.getId(),
                clientCredentialsAuth != null ? clientCredentialsAuth.getClass().getSimpleName() : "null");
        
        // 验证所有必需字段
        if (registeredClient == null) {
            throw new IllegalArgumentException("RegisteredClient cannot be null");
        }
        if (clientCredentialsAuth == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }
        if (authorization == null) {
            throw new IllegalArgumentException("Authorization cannot be null");
        }
        
        OAuth2TokenContext tokenContext;
        try {
            // 手动创建 AuthorizationServerContext（因为不在 OAuth2 授权服务器的请求上下文中）
            AuthorizationServerContext authorizationServerContext = 
                    new AuthorizationServerContext() {
                        @Override
                        public String getIssuer() {
                            return authorizationServerSettings.getIssuer();
                        }
                        
                        @Override
                        public AuthorizationServerSettings getAuthorizationServerSettings() {
                            return authorizationServerSettings;
                        }
                    };
            
            log.debug("创建 AuthorizationServerContext - Issuer: {}", authorizationServerContext.getIssuer());
            
            tokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(registeredClient)
                    .principal(clientCredentialsAuth)
                    .authorization(authorization)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrant(clientCredentialsAuth)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationServerContext(authorizationServerContext)
                    .build();
            
            log.debug("OAuth2TokenContext 创建成功");
        } catch (IllegalArgumentException e) {
            log.error("创建 OAuth2TokenContext 失败 - RegisteredClient: {}, AuthorizationId: {}, Error: {}", 
                    registeredClient.getClientId(), authorization.getId(), e.getMessage(), e);
            throw e;
        }
        
        // 生成访问令牌
        log.debug("开始生成访问令牌 - TokenContext: {}", tokenContext.getClass().getSimpleName());
        OAuth2Token generatedToken;
        try {
            generatedToken = tokenGenerator.generate(tokenContext);
            log.debug("访问令牌生成完成 - TokenType: {}", 
                    generatedToken != null ? generatedToken.getClass().getSimpleName() : "null");
        } catch (IllegalArgumentException e) {
            log.error("生成访问令牌失败 - RegisteredClient: {}, AuthorizationId: {}, Error: {}", 
                    registeredClient.getClientId(), authorization.getId(), e.getMessage(), e);
            throw e;
        }
        
        if (generatedToken == null) {
            log.error("TokenGenerator 返回 null - ClientId: {}", registeredClient.getClientId());
            throw new IllegalStateException("无法生成访问令牌：TokenGenerator 返回 null");
        }
        
        // 检查生成的令牌类型并转换为 OAuth2AccessToken
        OAuth2AccessToken accessToken;
        if (generatedToken instanceof Jwt jwt) {
            accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwt.getTokenValue(),
                    jwt.getIssuedAt() != null ? jwt.getIssuedAt() : Instant.now(),
                    jwt.getExpiresAt() != null ? jwt.getExpiresAt() : Instant.now().plusSeconds(3600)
            );
        } else if (generatedToken instanceof OAuth2AccessToken oauth2AccessToken) {
            accessToken = oauth2AccessToken;
        } else {
            log.error("生成的令牌类型不支持 - 期望: OAuth2AccessToken 或 Jwt, 实际: {}", 
                    generatedToken.getClass().getName());
            throw new IllegalStateException("生成的令牌类型不支持：" + generatedToken.getClass().getName());
        }
        
        // 保存授权信息（包含访问令牌）
        authorization = OAuth2Authorization.from(authorization)
                .token(accessToken)
                .build();
        authorizationService.save(authorization);
        
        return accessToken.getTokenValue();
    }

    /**
     * 验证手机号格式
     * 中国大陆手机号：11位数字，1开头
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return phoneNumber.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 隐藏手机号中间4位
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() != 11) {
            return phoneNumber;
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
    }
}

