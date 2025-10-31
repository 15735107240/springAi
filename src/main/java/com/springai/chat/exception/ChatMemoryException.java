package com.springai.chat.exception;

/**
 * ChatMemory 操作异常
 * 
 * @author yanwenjie
 */
public class ChatMemoryException extends RuntimeException {

    public ChatMemoryException(String message) {
        super(message);
    }

    public ChatMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

