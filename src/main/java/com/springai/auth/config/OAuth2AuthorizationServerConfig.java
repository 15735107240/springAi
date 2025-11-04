package com.springai.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.springai.auth.service.impl.RedisRegisteredClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import jakarta.annotation.PostConstruct;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 授权服务器配置
 *
 * @author yanwenjie
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class OAuth2AuthorizationServerConfig {

    /**
     * 配置 OAuth2 授权服务器的安全过滤器链
     * 只匹配 OAuth2 相关的路径
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        http.with(authorizationServerConfigurer, Customizer.withDefaults());

        authorizationServerConfigurer
            .oidc(Customizer.withDefaults()); // 启用 OpenID Connect 1.0

        http
            .securityMatcher("/oauth2/**", "/.well-known/**")  // 只匹配 OAuth2 相关路径
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
            )
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
            );

        log.info("OAuth2 授权服务器安全配置完成");
        return http.build();
    }

    private final RedisRegisteredClientRepository redisRegisteredClientRepository;
    private final com.springai.auth.service.JwkKeyService jwkKeyService;

    public OAuth2AuthorizationServerConfig(
            RedisRegisteredClientRepository redisRegisteredClientRepository,
            com.springai.auth.service.JwkKeyService jwkKeyService) {
        this.redisRegisteredClientRepository = redisRegisteredClientRepository;
        this.jwkKeyService = jwkKeyService;
    }

    /**
     * 初始化默认 OAuth2 客户端到 Redis
     * 在 Bean 创建后执行
     */
    @PostConstruct
    public void initializeDefaultClient() {
        try {
            log.info("开始初始化默认 OAuth2 客户端...");
            // 检查是否已存在客户端
            RegisteredClient existingClient = redisRegisteredClientRepository.findByClientId("spring-ai-client");
            if (existingClient == null) {
                log.info("未找到已存在的客户端，创建新的默认客户端...");
                // 创建并保存默认客户端
                RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("spring-ai-client")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://localhost:10010/login/oauth2/code/spring-ai-client")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope("chat")
                    .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                    .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                    .build();

                redisRegisteredClientRepository.save(registeredClient);
                log.info("初始化并保存 OAuth2 客户端到 Redis - ClientId: spring-ai-client");

                // 验证保存是否成功
                RegisteredClient savedClient = redisRegisteredClientRepository.findByClientId("spring-ai-client");
                if (savedClient != null) {
                    log.info("验证成功：OAuth2 客户端已保存到 Redis - ClientId: {}, Scopes: {}",
                            savedClient.getClientId(), savedClient.getScopes());
                } else {
                    log.error("验证失败：OAuth2 客户端保存后无法从 Redis 中读取");
                }
            } else {
                log.info("OAuth2 客户端已存在于 Redis - ClientId: spring-ai-client, Scopes: {}",
                        existingClient.getScopes());
            }
        } catch (Exception e) {
            log.error("初始化默认 OAuth2 客户端失败", e);
        }
    }

    /**
     * JWK 源（用于 JWT 签名和验证）
     * 使用持久化的密钥对，确保应用重启后仍能验证旧的 JWT
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = jwkKeyService.getOrGenerateKeyPair();
        String keyId = jwkKeyService.getKeyId();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(keyId)
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        log.info("加载 RSA 密钥对用于 JWT 签名 - KeyId: {}", keyId);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT 解码器
     * 使用完整的 JWK Set URI，包括协议和主机
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource, AuthorizationServerSettings authorizationServerSettings) {
        // getJwkSetEndpoint() 返回的是相对路径，需要拼接完整的 URL
        String jwkSetUri = "http://localhost:10010" + authorizationServerSettings.getJwkSetEndpoint();
        log.info("配置 JWT 解码器 - JWK Set URI: {}", jwkSetUri);
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * 授权服务器设置
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://localhost:10010") // 颁发者URI
            .build();
    }

    /**
     * JWT 编码器
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * OAuth2 Token 生成器
     * 包含访问令牌生成器和 JWT 生成器
     * 注意：DelegatingOAuth2TokenGenerator 的顺序很重要，应该先尝试 JWT，再尝试访问令牌
     */
    @Bean
    @SuppressWarnings("rawtypes")
    public OAuth2TokenGenerator tokenGenerator(JwtEncoder jwtEncoder) {
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

        // 创建自定义 JWT 生成器，添加用户手机号到 claims
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(context -> {
            try {
                // 确保 claims 不为 null
                if (context.getClaims() == null) {
                    log.warn("JWT Context claims 为 null，跳过自定义 claims");
                    return;
                }

                // 从 authorization 的 attributes 中获取手机号
                var authorization = context.getAuthorization();
                if (authorization != null) {
                    String phoneNumber = authorization.getAttribute("phone_number");
                    if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        context.getClaims().claim("username", phoneNumber);
                        context.getClaims().claim("phone_number", phoneNumber);
                        log.debug("JWT 定制器：添加手机号到 claims - {}", phoneNumber);
                    } else {
                        // 如果没有 phone_number，尝试从 principalName 获取
                        String principalName = authorization.getPrincipalName();
                        if (principalName != null && !principalName.trim().isEmpty()
                                && principalName.matches("^1[3-9]\\d{9}$")) {
                            context.getClaims().claim("username", principalName);
                            context.getClaims().claim("phone_number", principalName);
                            log.debug("JWT 定制器：从 principalName 添加手机号到 claims - {}", principalName);
                        }
                    }
                } else {
                    log.debug("JWT 定制器：Authorization 为 null，跳过自定义 claims");
                }
            } catch (Exception e) {
                // JWT 定制器中的异常不应该阻止令牌生成，只记录日志
                log.warn("JWT 定制器执行失败，将使用默认 claims: {}", e.getMessage(), e);
            }
        });

        // 顺序：先 JWT，再访问令牌生成器
        // JWT 生成器会尝试生成 JWT，如果失败则使用访问令牌生成器
        // 注意：RegisteredClient 的 TokenSettings 中必须设置 accessTokenFormat 为 SELF_CONTAINED 才能使用 JWT
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                accessTokenGenerator
        );
    }

}

