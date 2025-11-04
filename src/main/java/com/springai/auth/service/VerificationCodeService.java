package com.springai.auth.service;

/**
 * 验证码服务接口
 * 
 * @author yanwenjie
 */
public interface VerificationCodeService {

    /**
     * 生成并发送验证码
     * 
     * @param phoneNumber 手机号
     * @return 验证码（仅用于测试，生产环境不应返回）
     */
    String generateAndSendCode(String phoneNumber);

    /**
     * 验证验证码
     * 
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phoneNumber, String code);

    /**
     * 检查手机号是否在冷却期内（防止频繁发送）
     * 
     * @param phoneNumber 手机号
     * @return 是否在冷却期内
     */
    boolean isInCooldown(String phoneNumber);
}

