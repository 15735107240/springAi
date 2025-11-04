package com.springai.auth.controller;

import com.springai.auth.context.UserContext;
import com.springai.auth.dto.LoginResponse;
import com.springai.auth.dto.SendCodeResponse;
import com.springai.auth.dto.UserInfoResponse;
import com.springai.auth.service.AuthService;
import com.springai.auth.service.VerificationCodeService;
import com.springai.chat.dto.ApiResponse;
import com.springai.common.exception.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供获取验证码和登录接口
 * 
 * @author yanwenjie
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final VerificationCodeService verificationCodeService;
    private final AuthService authService;

    public AuthController(
            VerificationCodeService verificationCodeService,
            AuthService authService) {
        this.verificationCodeService = verificationCodeService;
        this.authService = authService;
    }

    /**
     * 获取验证码
     * POST /api/auth/code
     * 
     * @param request 包含手机号的请求体
     * @return 发送结果
     */
    @PostMapping("/code")
    public SendCodeResponse sendVerificationCode(
            @Validated @RequestBody SendCodeRequest request) {
        // 验证手机号格式
        if (!isValidPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 生成并发送验证码（异常会由全局异常处理器处理）
        String code = verificationCodeService.generateAndSendCode(request.getPhoneNumber());
        
        // 仅用于开发测试，生产环境应移除
        SendCodeResponse response = new SendCodeResponse(
                true,
                "验证码已发送",
                maskPhoneNumber(request.getPhoneNumber()),
                code
        );
        
        log.info("验证码发送成功 - 手机号: {}", maskPhoneNumber(request.getPhoneNumber()));
        return response;
    }

    /**
     * 手机号+验证码登录
     * POST /api/auth/login
     * 
     * @param request 包含手机号和验证码的请求体
     * @return 登录结果和访问令牌
     */
    @PostMapping("/login")
    public LoginResponse login(@Validated @RequestBody LoginRequest request) {
        // 验证手机号格式
        if (!isValidPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("手机号格式不正确");
        }
        
        // 调用业务服务处理登录
        return authService.login(request.getPhoneNumber(), request.getCode());
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/user
     * 
     * @return 用户信息
     */
    @GetMapping("/user")
    public UserInfoResponse getCurrentUser() {
        // 使用 UserContext 获取当前用户（更简洁的方式）
        var user = UserContext.getUser();
        if (user == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        
        return UserInfoResponse.builder()
                .success(true)
                .message("获取用户信息成功")
                .user(UserInfoResponse.UserInfo.builder()
                        .phoneNumber(maskPhoneNumber(user.getPhoneNumber()))
                        .username(user.getUsername())
                        .enabled(user.isEnabled())
                        .build())
                .build();
    }

    /**
     * 登出接口
     * POST /api/auth/logout
     * 
     * @param authentication 当前认证信息（由Spring Security自动注入）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        // 从认证信息中获取token
        String token = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            token = jwt.getTokenValue();
        }
        
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException("未找到有效的访问令牌");
        }
        
        // 调用服务执行登出
        authService.logout(token);
        
        return ApiResponse.<Void>builder()
                .success(true)
                .message("登出成功")
                .build();
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

    /**
     * 发送验证码请求体
     */
    @Data
    public static class SendCodeRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phoneNumber;
    }

    /**
     * 登录请求体
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phoneNumber;
        
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
        private String code;
    }
}

