package com.springai.auth.service.impl;

import com.springai.auth.entity.User;
import com.springai.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户服务实现
 * 使用 Redis 存储用户信息（临时方案，生产环境建议使用数据库）
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final RedissonClient redissonClient;
    
    @Value("${auth.user.key-prefix:user:phone:}")
    private String userKeyPrefix;
    
    @Value("${auth.user.ttl-days:30}")
    private long userTtlDays;

    public UserServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUserByPhoneNumber(username);
    }

    @Override
    public User createOrGetUser(String phoneNumber) {
        String userKey = userKeyPrefix + phoneNumber;
        RBucket<User> userBucket = redissonClient.getBucket(userKey);
        
        User user = userBucket.get();
        if (user == null) {
            // 创建新用户
            user = new User(phoneNumber);
            user.setUsername("user_" + phoneNumber.substring(7)); // 使用手机号后4位作为用户名
            userBucket.set(user, Duration.ofDays(userTtlDays));
            log.info("创建新用户 - 手机号: {}", phoneNumber);
        } else {
            // 刷新过期时间
            userBucket.expire(Duration.ofDays(userTtlDays));
            log.debug("获取已存在用户 - 手机号: {}", phoneNumber);
        }
        
        return user;
    }

    @Override
    public User loadUserByPhoneNumber(String phoneNumber) {
        String userKey = userKeyPrefix + phoneNumber;
        RBucket<User> userBucket = redissonClient.getBucket(userKey);
        
        User user = userBucket.get();
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + phoneNumber);
        }
        
        return user;
    }
}

