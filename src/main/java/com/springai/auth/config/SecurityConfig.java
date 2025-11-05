package com.springai.auth.config;

import com.springai.auth.service.TokenValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 *
 * @author yanwenjie
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 密码编码器（虽然验证码登录不需要，但 OAuth2 可能需要）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security 过滤器链配置
     * 匹配除 OAuth2 之外的所有请求
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Autowired JwtDecoder jwtDecoder,
            @Autowired TokenValidationService tokenValidationService) throws Exception {

        // 创建带 Redis 验证的 JWT 解码器
        JwtDecoder redisAwareJwtDecoder = createRedisAwareJwtDecoder(jwtDecoder, tokenValidationService);

        http
            .securityMatcher("/api/**", "/actuator/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(redisAwareJwtDecoder))
            );

        log.info("Spring Security 配置完成 - 已配置带 Redis 验证的 JWT 解码器和 CORS 支持");
        return http.build();
    }

    /**
     * 创建带 Redis 验证的 JWT 解码器
     * 在验证 JWT 签名之前，先检查 Redis 中是否存在对应的授权信息
     */
    private JwtDecoder createRedisAwareJwtDecoder(JwtDecoder jwtDecoder, TokenValidationService tokenValidationService) {
        return token -> {
            if (!tokenValidationService.isTokenExists(token)) {
                log.warn("JWT 令牌在 Redis 中不存在，拒绝访问");
                // HTTP响应头不能包含非ASCII字符，使用英文错误消息
                OAuth2Error oauth2Error = new OAuth2Error(
                    org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN,
                    "Token invalid or expired",
                    null
                );
                throw new OAuth2AuthenticationException(oauth2Error);
            }

            return jwtDecoder.decode(token);
        };
    }
}
