package com.springai.auth.service;

import java.security.KeyPair;

/**
 * JWT 密钥管理服务接口
 * 用于持久化和加载 RSA 密钥对
 * 
 * @author yanwenjie
 */
public interface JwkKeyService {

    /**
     * 获取或生成 RSA 密钥对
     * 优先从 Redis 加载，如果不存在则生成新的并保存
     * 
     * @return RSA 密钥对
     */
    KeyPair getOrGenerateKeyPair();

    /**
     * 获取密钥 ID
     * 
     * @return 密钥 ID
     */
    String getKeyId();
}

