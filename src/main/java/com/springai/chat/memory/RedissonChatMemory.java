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
            
            // 获取最后N条消息（最新的）
            int fromIndex = Math.max(0, size - lastN);
            List<Message> messages = new ArrayList<>(messageList.range(fromIndex, size - 1));
            
            // 反转顺序，使最新的消息在前
            List<Message> reversedMessages = new ArrayList<>(messages);
            java.util.Collections.reverse(reversedMessages);
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved {} messages (last {}) from conversation: {}", reversedMessages.size(), lastN, conversationId);
            return reversedMessages;
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
            
            // 反转顺序，使最新的消息在前
            List<Message> reversedMessages = new ArrayList<>(messages);
            java.util.Collections.reverse(reversedMessages);
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved all {} messages from conversation: {}", reversedMessages.size(), conversationId);
            return reversedMessages;
        } catch (Exception e) {
            log.error("Failed to get all messages from conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 分页获取消息（按时间倒序，最新的在前）
     * @param conversationId 会话ID
     * @param page 页码（从0开始，0表示最新的一页）
     * @param size 每页大小
     * @return 当前页的消息列表（最新的在前）
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
            
            // 按倒序计算索引（最新的消息在列表末尾）
            // 第0页：从末尾开始，获取最后size条（最新的）
            // 第1页：从倒数第size+1条开始，获取前size条（较旧的）
            // 计算应该获取的结束索引（从后往前）
            int toIndex = totalSize - page * size - 1;
            int fromIndex = totalSize - (page + 1) * size;
            
            // 如果toIndex小于0，说明这个页码已经完全超出范围
            if (toIndex < 0) {
                log.debug("Page {} is completely out of range for conversation: {} (totalSize: {}, size: {})", 
                        page, conversationId, totalSize, size);
                return new ArrayList<>();
            }
            
            // 如果fromIndex小于0，说明数据不足，从索引0开始
            if (fromIndex < 0) {
                fromIndex = 0;
                log.debug("Page {} requesting more data than available, adjusting fromIndex to 0 (totalSize: {}, size: {})", 
                        page, conversationId, totalSize, size);
            }
            
            // 如果起始索引超出总数，返回空列表
            if (fromIndex >= totalSize) {
                log.debug("Page {} is out of range for conversation: {} (totalSize: {})", 
                        page, conversationId, totalSize);
                return new ArrayList<>();
            }
            
            // 确保toIndex不超过列表范围
            if (toIndex >= totalSize) {
                toIndex = totalSize - 1;
            }
            
            // 确保fromIndex <= toIndex
            if (fromIndex > toIndex) {
                log.debug("Invalid range for page {} in conversation: {} (fromIndex: {}, toIndex: {})", 
                        page, conversationId, fromIndex, toIndex);
                return new ArrayList<>();
            }
            
            // 从Redis获取消息（从旧到新）
            List<Message> messages = new ArrayList<>(messageList.range(fromIndex, toIndex));
            
            // 反转顺序，使最新的消息在前
            List<Message> reversedMessages = new ArrayList<>(messages);
            java.util.Collections.reverse(reversedMessages);
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("Retrieved {} messages (page {}, size {}) from conversation: {} (reverse order)", 
                    reversedMessages.size(), page, size, conversationId);
            return reversedMessages;
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

