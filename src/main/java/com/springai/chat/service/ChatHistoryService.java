package com.springai.chat.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 会话历史服务接口
 * 提供会话历史的查询、管理功能
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
public interface ChatHistoryService {

    /**
     * 获取会话历史
     * @param conversationId 会话ID
     * @param lastN 获取最近N条消息，-1表示获取全部
     * @return 消息列表
     */
    List<Message> getHistory(String conversationId, int lastN);

    /**
     * 清除会话历史
     * @param conversationId 会话ID
     */
    void clearHistory(String conversationId);

    /**
     * 获取会话消息总数
     * @param conversationId 会话ID
     * @return 消息总数
     */
    int getMessageCount(String conversationId);

    /**
     * 检查会话是否存在
     * @param conversationId 会话ID
     * @return 是否存在
     */
    boolean exists(String conversationId);

    /**
     * 获取会话剩余过期时间（秒）
     * @param conversationId 会话ID
     * @return 剩余过期时间（秒）
     */
    long getRemainingTtl(String conversationId);

    /**
     * 刷新会话过期时间
     * @param conversationId 会话ID
     */
    void refreshTtl(String conversationId);

    /**
     * 获取所有消息（不限制数量）
     * @param conversationId 会话ID
     * @return 所有消息列表
     */
    List<Message> getAllMessages(String conversationId);

    /**
     * 分页获取消息
     * @param conversationId 会话ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult getMessagesByPage(String conversationId, int page, int size);

    /**
     * 获取所有会话ID列表（管理员功能）
     * @return 所有会话ID列表
     */
    List<String> getAllConversationIds();

    /**
     * 分页结果类
     */
    class PageResult {
        private final List<Message> messages;
        private final int page;
        private final int size;
        private final int totalMessages;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public PageResult(List<Message> messages, int page, int size, int totalMessages) {
            this.messages = messages;
            this.page = page;
            this.size = size;
            this.totalMessages = totalMessages;
            this.totalPages = (int) Math.ceil((double) totalMessages / size);
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int getTotalMessages() {
            return totalMessages;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }
    }
}

