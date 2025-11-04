package com.springai.auth.service.impl;

import com.springai.auth.service.SmsService;
import com.springai.auth.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * 验证码服务实现
 * 使用 Redis 存储验证码，支持过期和冷却期
 *
 * @author yanwenjie
 */
@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final RedissonClient redissonClient;
    private final SmsService smsService;

    @Value("${auth.verification-code.expire-seconds:300}")
    private long codeExpireSeconds; // 验证码过期时间，默认5分钟

    @Value("${auth.verification-code.cooldown-seconds:60}")
    private long cooldownSeconds; // 冷却期，默认60秒

    @Value("${auth.verification-code.key-prefix:sms:code:}")
    private String codeKeyPrefix;

    @Value("${auth.verification-code.cooldown-prefix:sms:cooldown:}")
    private String cooldownKeyPrefix;

    public VerificationCodeServiceImpl(RedissonClient redissonClient, SmsService smsService) {
        this.redissonClient = redissonClient;
        this.smsService = smsService;
    }

    @Override
    public String generateAndSendCode(String phoneNumber) {
        // 检查冷却期
/*        if (isInCooldown(phoneNumber)) {
            RBucket<Long> cooldownBucket = redissonClient.getBucket(cooldownKeyPrefix + phoneNumber);
            long remaining = cooldownBucket.remainTimeToLive() / 1000;
            throw new IllegalStateException("验证码发送过于频繁，请 " + remaining + " 秒后再试");
        }*/

        // 生成6位数字验证码
        String code = generateCode();

        // 保存验证码到 Redis（5分钟过期）
        String codeKey = codeKeyPrefix + phoneNumber;
        RBucket<String> codeBucket = redissonClient.getBucket(codeKey);
        codeBucket.set(code, Duration.ofSeconds(codeExpireSeconds));

        // 设置冷却期（60秒）
        String cooldownKey = cooldownKeyPrefix + phoneNumber;
        RBucket<Long> cooldownBucket = redissonClient.getBucket(cooldownKey);
        cooldownBucket.set(System.currentTimeMillis(), Duration.ofSeconds(cooldownSeconds));

        // 发送短信
        boolean sent = smsService.sendVerificationCode(phoneNumber, code);
        if (!sent) {
            // 如果发送失败，删除已保存的验证码
            codeBucket.delete();
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }

        log.info("验证码生成并发送成功 - 手机号: {}, 过期时间: {} 秒", phoneNumber, codeExpireSeconds);
        return code; // 仅用于测试，生产环境应移除返回值
    }

    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        if (phoneNumber == null || code == null || code.trim().isEmpty()) {
            return false;
        }

        String codeKey = codeKeyPrefix + phoneNumber;
        RBucket<String> codeBucket = redissonClient.getBucket(codeKey);

        String storedCode = codeBucket.get();
        if (storedCode == null) {
            log.warn("验证码已过期或不存在 - 手机号: {}", phoneNumber);
            return false;
        }

        if (storedCode.equals(code.trim())) {
            // 验证成功后删除验证码（一次性使用）
            codeBucket.delete();
            log.info("验证码验证成功 - 手机号: {}", phoneNumber);
            return true;
        } else {
            log.warn("验证码错误 - 手机号: {}, 输入的验证码: {}", phoneNumber, code);
            return false;
        }
    }

    @Override
    public boolean isInCooldown(String phoneNumber) {
        String cooldownKey = cooldownKeyPrefix + phoneNumber;
        RBucket<Long> cooldownBucket = redissonClient.getBucket(cooldownKey);
        return cooldownBucket.isExists();
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 生成100000-999999之间的随机数
        return String.valueOf(code);
    }
}

