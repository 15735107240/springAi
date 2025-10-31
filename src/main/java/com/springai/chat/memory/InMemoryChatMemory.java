package com.springai.chat.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存 ChatMemory 实现
 * 当 Redis 不可用时作为后备使用
 * 注意：数据不会持久化，应用重启后数据会丢失
 * 
 * @author yanwenjie
 */
@Slf4j
public class InMemoryChatMemory implements ChatMemory {

    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        conversations.compute(conversationId, (key, existingMessages) -> {
            List<Message> messageList = existingMessages != null 
                ? new ArrayList<>(existingMessages) 
                : new ArrayList<>();
            messageList.addAll(messages);
            return messageList;
        });
        log.debug("已添加 {} 条消息到会话（内存）: {}", messages.size(), conversationId);
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        List<Message> messages = conversations.getOrDefault(conversationId, List.of());
        log.debug("从会话（内存）获取了 {} 条消息: {}", messages.size(), conversationId);
        return messages;
    }

    @Override
    public void clear(@NonNull String conversationId) {
        conversations.remove(conversationId);
        log.debug("已清除会话（内存）: {}", conversationId);
    }
}

