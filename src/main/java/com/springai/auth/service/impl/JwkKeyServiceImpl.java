package com.springai.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springai.auth.service.JwkKeyService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 密钥管理服务实现
 * 将 RSA 密钥对持久化到 Redis，确保应用重启后密钥一致
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class JwkKeyServiceImpl implements JwkKeyService {

    private static final String KEY_PAIR_REDIS_KEY = "oauth2:jwk:keypair";
    private static final String KEY_ID_REDIS_KEY = "oauth2:jwk:keyid";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private KeyPair cachedKeyPair;
    private String cachedKeyId;

    public JwkKeyServiceImpl(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public KeyPair getOrGenerateKeyPair() {
        // 如果缓存存在，直接返回
        if (cachedKeyPair != null) {
            return cachedKeyPair;
        }

        try {
            // 尝试从 Redis 加载
            KeyPair keyPair = loadKeyPairFromRedis();
            if (keyPair != null) {
                log.info("从 Redis 加载 RSA 密钥对成功");
                cachedKeyPair = keyPair;
                cachedKeyId = loadKeyIdFromRedis();
                return keyPair;
            }

            // Redis 中没有，生成新的密钥对
            log.info("Redis 中未找到密钥对，生成新的 RSA 密钥对");
            keyPair = generateRsaKey();
            String keyId = UUID.randomUUID().toString();

            // 保存到 Redis
            saveKeyPairToRedis(keyPair, keyId);
            cachedKeyPair = keyPair;
            cachedKeyId = keyId;

            return keyPair;
        } catch (Exception e) {
            log.error("获取或生成 RSA 密钥对失败", e);
            // 如果 Redis 操作失败，仍然生成密钥对（应用可以启动，但重启后会失效）
            KeyPair keyPair = generateRsaKey();
            cachedKeyPair = keyPair;
            cachedKeyId = UUID.randomUUID().toString();
            log.warn("由于 Redis 操作失败，密钥对未持久化，应用重启后旧 token 将失效");
            return keyPair;
        }
    }

    @Override
    public String getKeyId() {
        if (cachedKeyId != null) {
            return cachedKeyId;
        }

        // 尝试从 Redis 加载
        cachedKeyId = loadKeyIdFromRedis();
        if (cachedKeyId == null) {
            // 如果密钥对已生成但 keyId 未保存，生成新的
            cachedKeyId = UUID.randomUUID().toString();
            saveKeyIdToRedis(cachedKeyId);
        }
        return cachedKeyId;
    }

    /**
     * 从 Redis 加载密钥对
     */
    private KeyPair loadKeyPairFromRedis() {
        try {
            RBucket<String> bucket = redissonClient.getBucket(KEY_PAIR_REDIS_KEY, new JsonJacksonCodec());
            String keyPairJson = bucket.get();

            if (keyPairJson == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> keyPairMap = objectMapper.readValue(keyPairJson, Map.class);
            String publicKeyBase64 = keyPairMap.get("publicKey");
            String privateKeyBase64 = keyPairMap.get("privateKey");

            if (publicKeyBase64 == null || privateKeyBase64 == null) {
                log.warn("Redis 中的密钥对数据不完整");
                return null;
            }

            // 反序列化密钥
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(publicKeyBytes));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(privateKeyBytes));

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            log.error("从 Redis 加载密钥对失败", e);
            return null;
        }
    }

    /**
     * 保存密钥对到 Redis
     */
    private void saveKeyPairToRedis(KeyPair keyPair, String keyId) {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            // 序列化密钥
            String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            Map<String, String> keyPairMap = new HashMap<>();
            keyPairMap.put("publicKey", publicKeyBase64);
            keyPairMap.put("privateKey", privateKeyBase64);

            String keyPairJson = objectMapper.writeValueAsString(keyPairMap);
            RBucket<String> bucket = redissonClient.getBucket(KEY_PAIR_REDIS_KEY, new JsonJacksonCodec());
            bucket.set(keyPairJson);
            bucket.expire(Duration.ofDays(365));

            // 保存 keyId
            saveKeyIdToRedis(keyId);

            log.info("RSA 密钥对已保存到 Redis，有效期 365 天");
        } catch (Exception e) {
            log.error("保存密钥对到 Redis 失败", e);
            throw new RuntimeException("保存密钥对失败", e);
        }
    }

    /**
     * 从 Redis 加载密钥 ID
     */
    private String loadKeyIdFromRedis() {
        try {
            RBucket<String> bucket = redissonClient.getBucket(KEY_ID_REDIS_KEY, new JsonJacksonCodec());
            return bucket.get();
        } catch (Exception e) {
            log.error("从 Redis 加载密钥 ID 失败", e);
            return null;
        }
    }

    /**
     * 保存密钥 ID 到 Redis
     */
    private void saveKeyIdToRedis(String keyId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(KEY_ID_REDIS_KEY, new JsonJacksonCodec());
            bucket.set(keyId);
            bucket.expire(Duration.ofDays(365));
        } catch (Exception e) {
            log.error("保存密钥 ID 到 Redis 失败", e);
        }
    }

    /**
     * 生成新的 RSA 密钥对
     */
    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法生成 RSA 密钥对", e);
        }
    }
}

