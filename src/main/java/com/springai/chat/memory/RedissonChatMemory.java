package com.springai.chat.memory;

import com.springai.chat.exception.ChatMemoryException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Redisson实现的分布式ChatMemory
 * 支持持久化会话历史和自动过期
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
@Slf4j
public class RedissonChatMemory implements ChatMemory {

    private final RedissonClient redissonClient;
    
    private final String keyPrefix;
    
    private final long ttlSeconds;

    /**
     * 构造函数
     * @param redissonClient Redisson客户端
     * @param keyPrefix Redis键前缀
     * @param ttlSeconds 会话过期时间（秒）
     */
    public RedissonChatMemory(RedissonClient redissonClient, String keyPrefix, long ttlSeconds) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.ttlSeconds = ttlSeconds;
        log.info("RedissonChatMemory initialized with keyPrefix: {}, ttl: {} seconds", keyPrefix, ttlSeconds);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            // 添加消息到列表
            messageList.addAll(messages);
            
            // 设置过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Added {} messages to conversation: {}", messages.size(), conversationId);
        } catch (Exception e) {
            log.error("Failed to add messages to conversation: {}", conversationId, e);
            throw new ChatMemoryException("Failed to add messages to Redis: " + conversationId, e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                log.debug("No messages found for conversation: {}", conversationId);
                return new ArrayList<>();
            }
            
            int size = messageList.size();
            if (size == 0) {
                return new ArrayList<>();
            }
            
            // 获取最后N条消息
            int fromIndex = Math.max(0, size - lastN);
            List<Message> messages = new ArrayList<>(messageList.range(fromIndex, size - 1));
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved {} messages (last {}) from conversation: {}", messages.size(), lastN, conversationId);
            return messages;
        } catch (Exception e) {
            log.error("Failed to get messages from conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            messageList.delete();
            
            log.info("Cleared conversation: {}", conversationId);
        } catch (Exception e) {
            log.error("Failed to clear conversation: {}", conversationId, e);
            throw new ChatMemoryException("Failed to clear conversation from Redis: " + conversationId, e);
        }
    }

    /**
     * 获取会话的消息总数
     */
    public int getMessageCount(String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            return messageList.size();
        } catch (Exception e) {
            log.error("Failed to get message count for conversation: {}", conversationId, e);
            return 0;
        }
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String conversationId) {
        try {
            String key = getKey(conversationId);
            return redissonClient.getList(key).isExists();
        } catch (Exception e) {
            log.error("Failed to check existence of conversation: {}", conversationId, e);
            return false;
        }
    }

    /**
     * 获取会话剩余过期时间（秒）
     */
    public long getRemainingTtl(String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            return messageList.remainTimeToLive() / 1000; // 转换为秒
        } catch (Exception e) {
            log.error("Failed to get remaining TTL for conversation: {}", conversationId, e);
            return -1;
        }
    }

    /**
     * 刷新会话过期时间
     */
    public void refreshTtl(String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            if (messageList.isExists()) {
                messageList.expire(Duration.ofSeconds(ttlSeconds));
                log.debug("Refreshed TTL for conversation: {}", conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to refresh TTL for conversation: {}", conversationId, e);
        }
    }

    /**
     * 获取所有消息（不限制数量）
     */
    public List<Message> getAllMessages(String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                return new ArrayList<>();
            }
            
            List<Message> messages = new ArrayList<>(messageList.readAll());
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved all {} messages from conversation: {}", messages.size(), conversationId);
            return messages;
        } catch (Exception e) {
            log.error("Failed to get all messages from conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 分页获取消息
     * @param conversationId 会话ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 当前页的消息列表
     */
    public List<Message> getMessagesByPage(String conversationId, int page, int size) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                log.debug("No messages found for conversation: {}", conversationId);
                return new ArrayList<>();
            }
            
            int totalSize = messageList.size();
            if (totalSize == 0) {
                return new ArrayList<>();
            }
            
            // 计算起始和结束索引
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalSize) - 1;
            
            // 如果起始索引超出范围，返回空列表
            if (fromIndex >= totalSize) {
                log.debug("Page {} is out of range for conversation: {}", page, conversationId);
                return new ArrayList<>();
            }
            
            List<Message> messages = new ArrayList<>(messageList.range(fromIndex, toIndex));
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved {} messages (page {}, size {}) from conversation: {}", 
                    messages.size(), page, size, conversationId);
            return messages;
        } catch (Exception e) {
            log.error("Failed to get messages by page from conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有会话ID列表
     * @return 所有会话ID列表
     */
    public List<String> getAllConversationIds() {
        try {
            // 获取所有匹配的键
            Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(keyPrefix + "*");
            List<String> conversationIds = new ArrayList<>();
            
            for (String key : keys) {
                // 移除前缀，获取conversationId
                String conversationId = key.substring(keyPrefix.length());
                conversationIds.add(conversationId);
            }
            
            log.debug("Found {} conversation IDs", conversationIds.size());
            return conversationIds;
        } catch (Exception e) {
            log.error("Failed to get all conversation IDs", e);
            return new ArrayList<>();
        }
    }

    /**
     * 生成Redis键
     */
    private String getKey(String conversationId) {
        return keyPrefix + conversationId;
    }
}

