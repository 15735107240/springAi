package com.springai.chat.service.impl;

import com.springai.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现类
 * 使用 Spring AI ChatModel 实现基本对话功能，支持对话记忆
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    
    private static final String SYSTEM_PROMPT = "你是一个友好的AI助手，请用简洁明了的方式回答用户的问题。";

    /**
     * 构造函数
     * @param chatModel ChatModel，由 Spring AI 自动注入
     * @param chatMemory ChatMemory，用于保存对话记忆（可选）
     */
    public ChatServiceImpl(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        log.info("ChatServiceImpl initialized with ChatModel: {}, ChatMemory: {}", 
                chatModel.getClass().getSimpleName(), 
                chatMemory != null ? chatMemory.getClass().getSimpleName() : "none");
    }

    @Override
    public Flux<ChatResponse> chat(String query, String conversantId) {
        log.info("Received chat request - query: {}, conversantId: {}", query, conversantId);
        
        // 构建消息列表
        List<Message> messages = new ArrayList<>();
        
        // 1. 添加系统消息
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        
        // 2. 从 ChatMemory 获取历史对话（如果存在）
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            List<Message> history = chatMemory.get(conversantId);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
                log.debug("Loaded {} messages from conversation history for conversantId: {}", 
                        history.size(), conversantId);
            }
        }
        
        // 3. 添加当前用户消息
        UserMessage userMessage = new UserMessage(query);
        messages.add(userMessage);
        
        // 4. 创建 Prompt
        Prompt prompt = new Prompt(messages);
        
        // 5. 使用 ChatModel 进行流式对话
        Flux<ChatResponse> responseFlux = chatModel.stream(prompt);
        
        // 6. 保存对话到 ChatMemory（在响应流完成后）
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            responseFlux = responseFlux
                    .collectList()
                    .flatMapMany(responses -> {
                        // 保存用户消息和所有助手响应到 ChatMemory
                        List<Message> messagesToSave = new ArrayList<>();
                        messagesToSave.add(userMessage);
                        
                        // 从所有响应中提取助手消息
                        StringBuilder assistantContent = new StringBuilder();
                        for (ChatResponse response : responses) {
                            if (response.getResult() != null && response.getResult().getOutput() != null) {
                                // 获取 Generation 对象
                                var generation = response.getResult().getOutput();
                                // Generation 对象的文本内容通常在 toString() 中或通过反射获取
                                String content = generation.toString();
                                // 如果 toString() 返回的是对象描述，尝试通过反射获取实际文本
                                if (content != null && !content.trim().isEmpty() && !content.startsWith("org.springframework")) {
                                    assistantContent.append(content);
                                }
                            }
                        }
                        
                        // 如果有助手回复，添加到消息列表
                        if (!assistantContent.isEmpty()) {
                            AssistantMessage assistantMessage = new AssistantMessage(assistantContent.toString());
                            messagesToSave.add(assistantMessage);
                        }
                        
                        if (!messagesToSave.isEmpty()) {
                            chatMemory.add(conversantId, messagesToSave);
                            log.debug("Saved {} messages to conversation history for conversantId: {}", 
                                    messagesToSave.size(), conversantId);
                        }
                        
                        // 返回响应流
                        return Flux.fromIterable(responses);
                    });
        }
        
        return responseFlux;
    }
}

