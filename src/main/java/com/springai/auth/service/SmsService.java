package com.springai.auth.service;

/**
 * 短信服务接口
 * 
 * @author yanwenjie
 */
public interface SmsService {

    /**
     * 发送验证码短信
     * 
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String phoneNumber, String code);
}

