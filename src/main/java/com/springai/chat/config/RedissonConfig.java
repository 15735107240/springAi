package com.springai.chat.config;

import com.springai.chat.memory.RedissonChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 配置RedissonClient和基于Redis的ChatMemory
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${redisson.threads:16}")
    private int threads;

    @Value("${redisson.netty-threads:32}")
    private int nettyThreads;

    @Value("${redisson.single-server-config.connection-pool-size:64}")
    private int connectionPoolSize;

    @Value("${redisson.single-server-config.connection-minimum-idle-size:10}")
    private int connectionMinimumIdleSize;

    @Value("${redisson.single-server-config.idle-connection-timeout:10000}")
    private int idleConnectionTimeout;

    @Value("${redisson.single-server-config.timeout:3000}")
    private int timeout;

    @Value("${chat.memory.redis.key-prefix:chat:memory:}")
    private String chatMemoryKeyPrefix;

    @Value("${chat.memory.redis.ttl:604800}")
    private long chatMemoryTtl;

    /**
     * 配置RedissonClient
     * 使用Kryo编解码器以支持Spring AI Message对象的序列化
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 使用Kryo编解码器，它不需要默认构造函数，更适合复杂对象序列化
        config.setCodec(new Kryo5Codec());
        
        // 配置单节点模式
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setTimeout(timeout);
        
        // 如果设置了密码
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        // 配置线程池
        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);
        
        log.info("Redisson client configured with Kryo5 codec, address: {}, database: {}", address, database);
        
        return Redisson.create(config);
    }

    /**
     * 配置基于Redisson的ChatMemory
     */
    @Bean
    public ChatMemory chatMemory(RedissonClient redissonClient) {
        log.info("Creating RedissonChatMemory with keyPrefix: {}, ttl: {} seconds", 
                chatMemoryKeyPrefix, chatMemoryTtl);
        return new RedissonChatMemory(redissonClient, chatMemoryKeyPrefix, chatMemoryTtl);
    }
}

