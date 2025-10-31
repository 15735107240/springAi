package com.springai.chat.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天配置类
 * 配置 ChatMemory Bean 用于保存对话记忆
 * 
 * @author yanwenjie
 */
@Slf4j
@Configuration
public class ChatConfig {

    /**
     * 创建简单的内存 ChatMemory Bean
     * 用于保存对话历史
     * 
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory() {
        log.info("Creating in-memory ChatMemory bean");
        return new SimpleInMemoryChatMemory();
    }

    /**
     * 简单的内存实现 ChatMemory
     */
    private static class SimpleInMemoryChatMemory implements ChatMemory {
        private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

        @Override
        public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
            conversations.compute(conversationId, (key, existingMessages) -> {
                List<Message> messageList = existingMessages != null 
                    ? new java.util.ArrayList<>(existingMessages) 
                    : new java.util.ArrayList<>();
                messageList.addAll(messages);
                return messageList;
            });
            log.debug("Added {} messages to conversation: {}", messages.size(), conversationId);
        }

        @Override
        @NonNull
        public List<Message> get(@NonNull String conversationId) {
            List<Message> messages = conversations.getOrDefault(conversationId, List.of());
            log.debug("Retrieved {} messages from conversation: {}", messages.size(), conversationId);
            return messages;
        }

        @Override
        public void clear(@NonNull String conversationId) {
            conversations.remove(conversationId);
            log.debug("Cleared conversation: {}", conversationId);
        }
    }
}

