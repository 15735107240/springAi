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
        log.info("查询会话历史: conversationId={}, lastN={}", conversationId, lastN);
        if (lastN < 0) {
            return getAllMessages(conversationId);
        }
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.get(conversationId, lastN);
        }
        // 如果不是 RedissonChatMemory，使用默认方法
        List<Message> allMessages = chatMemory.get(conversationId);
        if (lastN > 0 && lastN < allMessages.size()) {
            int startIndex = Math.max(0, allMessages.size() - lastN);
            return allMessages.subList(startIndex, allMessages.size());
        }
        return allMessages;
    }

    @Override
    public void clearHistory(String conversationId) {
        log.info("清除会话历史: conversationId={}", conversationId);
        chatMemory.clear(conversationId);
    }

    @Override
    public int getMessageCount(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getMessageCount(conversationId);
        }
        log.warn("ChatMemory 不是 RedissonChatMemory，返回消息列表大小");
        List<Message> messages = chatMemory.get(conversationId);
        return messages != null ? messages.size() : 0;
    }

    @Override
    public boolean exists(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.exists(conversationId);
        }
        log.warn("ChatMemory 不是 RedissonChatMemory，检查消息列表是否为空");
        List<Message> messages = chatMemory.get(conversationId);
        return messages != null && !messages.isEmpty();
    }

    @Override
    public long getRemainingTtl(String conversationId) {
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getRemainingTtl(conversationId);
        }
        log.warn("ChatMemory 不是 RedissonChatMemory，返回 -1");
        return -1;
    }

    @Override
    public void refreshTtl(String conversationId) {
        log.info("刷新会话过期时间: conversationId={}", conversationId);
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            redissonChatMemory.refreshTtl(conversationId);
        } else {
            log.warn("ChatMemory 不是 RedissonChatMemory，无法刷新过期时间");
        }
    }

    @Override
    public List<Message> getAllMessages(String conversationId) {
        log.info("获取所有会话消息: conversationId={}", conversationId);
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getAllMessages(conversationId);
        }
        log.warn("ChatMemory 不是 RedissonChatMemory，返回标准get方法结果");
        return chatMemory.get(conversationId);
    }

    @Override
    public ChatHistoryService.PageResult getMessagesByPage(String conversationId, int page, int size) {
        log.info("分页查询会话消息: conversationId={}, page={}, size={}", conversationId, page, size);
        
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            int totalMessages = redissonChatMemory.getMessageCount(conversationId);
            List<Message> messages = redissonChatMemory.getMessagesByPage(conversationId, page, size);
            return new ChatHistoryService.PageResult(messages, page, size, totalMessages);
        }
        
        log.warn("ChatMemory 不是 RedissonChatMemory，返回空分页结果");
        return new ChatHistoryService.PageResult(List.of(), page, size, 0);
    }

    @Override
    public List<String> getAllConversationIds() {
        log.info("获取所有会话ID列表");
        
        if (chatMemory instanceof RedissonChatMemory redissonChatMemory) {
            return redissonChatMemory.getAllConversationIds();
        }
        
        log.warn("ChatMemory 不是 RedissonChatMemory，返回空列表");
        return List.of();
    }
}

