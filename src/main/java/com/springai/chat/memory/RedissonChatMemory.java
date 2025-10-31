package com.springai.chat.memory;

import com.springai.chat.exception.ChatMemoryException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.redisson.api.RKeys;

/**
 * 基于Redisson实现的分布式ChatMemory
 * 支持持久化会话历史和自动过期
 * 
 * @author yanwenjie
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
        log.info("RedissonChatMemory 初始化完成 - 键前缀: {}, 过期时间: {} 秒", keyPrefix, ttlSeconds);
    }

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            // 添加消息到列表
            messageList.addAll(messages);
            
            // 设置过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("已添加 {} 条消息到会话: {}", messages.size(), conversationId);
        } catch (Exception e) {
            log.error("添加消息到会话失败: {}", conversationId, e);
            throw new ChatMemoryException("添加消息到Redis失败: " + conversationId, e);
        }
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                log.debug("未找到会话消息: {}", conversationId);
                return new ArrayList<>();
            }
            
            int size = messageList.size();
            if (size == 0) {
                return new ArrayList<>();
            }
            
            // 获取所有消息
            List<Message> messages = new ArrayList<>(messageList.readAll());
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("从会话中获取了 {} 条消息: {}", messages.size(), conversationId);
            return messages;
        } catch (Exception e) {
            log.error("从会话获取消息失败: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clear(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            messageList.delete();
            
            log.info("已清除会话: {}", conversationId);
        } catch (Exception e) {
            log.error("清除会话失败: {}", conversationId, e);
            throw new ChatMemoryException("从Redis清除会话失败: " + conversationId, e);
        }
    }

    /**
     * 获取会话历史（支持限制数量）
     * @param conversationId 会话ID
     * @param lastN 获取最近N条消息，-1表示获取全部
     * @return 消息列表
     */
    public List<Message> get(@NonNull String conversationId, int lastN) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                log.debug("未找到会话消息: {}", conversationId);
                return new ArrayList<>();
            }
            
            int size = messageList.size();
            if (size == 0) {
                return new ArrayList<>();
            }
            
            List<Message> messages;
            if (lastN > 0 && lastN < size) {
                // 获取最近N条消息
                messages = new ArrayList<>(messageList.readAll());
                // 取最后N条
                int startIndex = Math.max(0, messages.size() - lastN);
                messages = messages.subList(startIndex, messages.size());
            } else {
                // 获取所有消息
                messages = new ArrayList<>(messageList.readAll());
            }
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("从会话中获取了 {} 条消息: {}", messages.size(), conversationId);
            return messages;
        } catch (Exception e) {
            log.error("从会话获取消息失败: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有消息（不限制数量）
     * @param conversationId 会话ID
     * @return 所有消息列表
     */
    public List<Message> getAllMessages(@NonNull String conversationId) {
        return get(conversationId);
    }

    /**
     * 获取会话消息总数
     * @param conversationId 会话ID
     * @return 消息总数
     */
    public int getMessageCount(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            if (!messageList.isExists()) {
                return 0;
            }
            return messageList.size();
        } catch (Exception e) {
            log.error("获取会话消息总数失败: {}", conversationId, e);
            return 0;
        }
    }

    /**
     * 检查会话是否存在
     * @param conversationId 会话ID
     * @return 是否存在
     */
    public boolean exists(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            return messageList.isExists();
        } catch (Exception e) {
            log.error("检查会话是否存在失败: {}", conversationId, e);
            return false;
        }
    }

    /**
     * 获取会话剩余过期时间（秒）
     * @param conversationId 会话ID
     * @return 剩余过期时间（秒），-1表示不存在或出错
     */
    public long getRemainingTtl(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            if (!messageList.isExists()) {
                return -1;
            }
            return messageList.remainTimeToLive() / 1000; // 转换为秒
        } catch (Exception e) {
            log.error("获取会话剩余过期时间失败: {}", conversationId, e);
            return -1;
        }
    }

    /**
     * 刷新会话过期时间
     * @param conversationId 会话ID
     */
    public void refreshTtl(@NonNull String conversationId) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            if (messageList.isExists()) {
                messageList.expire(Duration.ofSeconds(ttlSeconds));
                log.debug("已刷新会话过期时间: {}", conversationId);
            }
        } catch (Exception e) {
            log.error("刷新会话过期时间失败: {}", conversationId, e);
        }
    }

    /**
     * 分页获取消息
     * @param conversationId 会话ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 消息列表
     */
    public List<Message> getMessagesByPage(@NonNull String conversationId, int page, int size) {
        try {
            String key = getKey(conversationId);
            RList<Message> messageList = redissonClient.getList(key);
            
            if (!messageList.isExists()) {
                return new ArrayList<>();
            }
            
            int totalSize = messageList.size();
            if (totalSize == 0) {
                return new ArrayList<>();
            }
            
            // 计算分页范围（从旧到新，所以要反转）
            // 第0页应该是最新的消息，需要从后往前取
            int startIndex = Math.max(0, totalSize - (page + 1) * size);
            int endIndex = Math.max(0, totalSize - page * size);
            
            if (startIndex >= endIndex || startIndex < 0) {
                return new ArrayList<>();
            }
            
            List<Message> allMessages = new ArrayList<>(messageList.readAll());
            // 取需要的范围，然后反转以保持时间顺序（新的在前）
            List<Message> pageMessages = allMessages.subList(startIndex, endIndex);
            // 反转列表，让最新的消息在前面
            List<Message> result = new ArrayList<>(pageMessages);
            java.util.Collections.reverse(result);
            
            // 刷新过期时间
            messageList.expire(Duration.ofSeconds(ttlSeconds));
            
            log.debug("分页获取会话消息: conversationId={}, page={}, size={}, 返回={}条", 
                    conversationId, page, size, result.size());
            return result;
        } catch (Exception e) {
            log.error("分页获取会话消息失败: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有会话ID列表
     * @return 所有会话ID列表
     */
    public List<String> getAllConversationIds() {
        try {
            RKeys keys = redissonClient.getKeys();
            String pattern = keyPrefix + "*";
            Iterable<String> keysIterable = keys.getKeysByPattern(pattern);
            
            List<String> conversationIds = new ArrayList<>();
            for (String key : keysIterable) {
                // 移除前缀，得到会话ID
                String conversationId = key.substring(keyPrefix.length());
                conversationIds.add(conversationId);
            }
            
            log.debug("获取到 {} 个会话ID", conversationIds.size());
            return conversationIds;
        } catch (Exception e) {
            log.error("获取所有会话ID列表失败", e);
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

