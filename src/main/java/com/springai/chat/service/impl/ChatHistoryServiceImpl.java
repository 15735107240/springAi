package com.springai.chat.service.impl;

import com.springai.chat.memory.RedissonChatMemory;
import com.springai.chat.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话历史服务实现
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatMemory chatMemory;

    public ChatHistoryServiceImpl(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public List<Message> getHistory(String conversationId, int lastN) {
        log.info("Getting history for conversation: {}, lastN: {}", conversationId, lastN);
        if (lastN < 0) {
            return getAllMessages(conversationId);
        }
        return chatMemory.get(conversationId, lastN);
    }

    @Override
    public void clearHistory(String conversationId) {
        log.info("Clearing history for conversation: {}", conversationId);
        chatMemory.clear(conversationId);
    }

    @Override
    public int getMessageCount(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getMessageCount(conversationId);
        }
        log.warn("ChatMemory is not RedissonChatMemory, returning 0");
        return 0;
    }

    @Override
    public boolean exists(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.exists(conversationId);
        }
        log.warn("ChatMemory is not RedissonChatMemory, returning false");
        return false;
    }

    @Override
    public long getRemainingTtl(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getRemainingTtl(conversationId);
        }
        log.warn("ChatMemory is not RedissonChatMemory, returning -1");
        return -1;
    }

    @Override
    public void refreshTtl(String conversationId) {
        log.info("Refreshing TTL for conversation: {}", conversationId);
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            redissonChatMemory.refreshTtl(conversationId);
        } else {
            log.warn("ChatMemory is not RedissonChatMemory, cannot refresh TTL");
        }
    }

    @Override
    public List<Message> getAllMessages(String conversationId) {
        log.info("Getting all messages for conversation: {}", conversationId);
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getAllMessages(conversationId);
        }
        log.warn("ChatMemory is not RedissonChatMemory, returning empty list");
        return List.of();
    }

    @Override
    public ChatHistoryService.PageResult getMessagesByPage(String conversationId, int page, int size) {
        log.info("Getting messages by page for conversation: {}, page: {}, size: {}", conversationId, page, size);
        
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            int totalMessages = redissonChatMemory.getMessageCount(conversationId);
            List<Message> messages = redissonChatMemory.getMessagesByPage(conversationId, page, size);
            return new ChatHistoryService.PageResult(messages, page, size, totalMessages);
        }
        
        log.warn("ChatMemory is not RedissonChatMemory, returning empty page result");
        return new ChatHistoryService.PageResult(List.of(), page, size, 0);
    }

    @Override
    public List<String> getAllConversationIds() {
        log.info("Getting all conversation IDs");
        
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getAllConversationIds();
        }
        
        log.warn("ChatMemory is not RedissonChatMemory, returning empty list");
        return List.of();
    }
}

