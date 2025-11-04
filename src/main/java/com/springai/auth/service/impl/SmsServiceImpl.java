package com.springai.auth.service.impl;

import com.springai.auth.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信服务实现（Mock）
 * 生产环境可以替换为真实的短信服务（如阿里云短信、腾讯云短信等）
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Override
    public boolean sendVerificationCode(String phoneNumber, String code) {
        try {
            // Mock 实现：只记录日志，不实际发送短信
            log.info("发送验证码短信 - 手机号: {}, 验证码: {}", phoneNumber, code);
            log.info("【Mock短信】您的验证码是：{}，有效期5分钟，请勿泄露给他人。", code);
            
            // 生产环境可以替换为真实的短信服务调用
            // 例如：阿里云短信、腾讯云短信、云片网等
            
            return true;
        } catch (Exception e) {
            log.error("发送验证码短信失败 - 手机号: {}", phoneNumber, e);
            return false;
        }
    }
}

