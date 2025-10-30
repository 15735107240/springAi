package com.springai.chat.exception;

/**
 * 聊天记忆相关异常
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
public class ChatMemoryException extends RuntimeException {

    public ChatMemoryException(String message) {
        super(message);
    }

    public ChatMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

