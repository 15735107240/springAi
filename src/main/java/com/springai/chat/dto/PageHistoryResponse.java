package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 分页查询会话历史响应
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageHistoryResponse {
    
    private String conversationId;
    private Integer page;
    private Integer size;
    private Integer totalMessages;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private List<Message> messages;
}

